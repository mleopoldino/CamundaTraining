package com.camunda.training;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.init;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.taskService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

@Deployment(resources = "TwitterQAProcess.bpmn")
public class ProcessJUnitTest {
  @Rule @ClassRule
  //public ProcessEngineRule rule = new ProcessEngineRule();
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  @Before
  public void setup() {
    init(rule.getProcessEngine());
  }

  @Test
  public void testHappyPath() {

    /*
    Mocks.register("createTweetDelegate", new JavaDelegate() {

      private final Logger LOGGER = Logger.getLogger("ProcessJUnitTest");

      @Override
      public void execute(DelegateExecution delegateExecution) throws Exception {
          LOGGER.info("Chamando o Twitter...");
      }
    });*/

    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<String, Object>();
    //variables.put("approved", true);
    Random random = new Random();
    int rand = random.nextInt();

    variables.put("content", "Exercise test - MLS Happy path: " + rand);
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // Make assertions on the process instance

    assertThat(processInstance).isWaitingAt("ValidarQATask");

    List<Task> taskList = taskService()
            .createTaskQuery()
            .taskCandidateGroup("management")
            .processInstanceId(processInstance.getId())
            .list();

    assertThat(taskList).isNotNull();
    assertThat(taskList).hasSize(1);

    Task task = taskList.get(0);

    Map<String, Object> approvedMap = new HashMap<String, Object>();
    approvedMap.put("approved", true);
    taskService().complete(task.getId(), approvedMap);

    assertThat(processInstance).isWaitingAt("PublicarTweetTask");
    execute(job());

    assertThat(processInstance).isEnded();

  }
/*
  @Test
  public void testRejectionPath() {
    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<String, Object>();
    //variables.put("approved", false);
    variables.put("content", "Exercise test - MLS Rejection path 2");
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    // Make assertions on the process instance

    assertThat(processInstance).isWaitingAt("ValidarQATask").task().hasCandidateGroup("management");
    complete(task(), withVariables("approved", false));

    assertThat(processInstance).isEnded();

  }
*/
  @Test
  public void testTweetRejected() {

    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<String, Object>();
    //variables.put("approved", false);
    variables.put("content", "Exercise test - MLS Rejection path 2");
    variables.put("approved","false");

    ProcessInstance processInstance = runtimeService()
            .createProcessInstanceByKey("TwitterQAProcess")
            .setVariables(variables)
            .startAfterActivity("ValidarQATask")
            .execute();

    assertThat(processInstance).isWaitingAt("NotificarReprovacaoTask");
    complete(externalTask());

    assertThat(processInstance).isEnded().hasVariables("approved").hasPassed("NotificarReprovacaoTask");
  }

  @Test
  public void testTweetMsgSuperUser() {

    ProcessInstance processInstance = runtimeService()
            .createMessageCorrelation("superuserTweet")
            .setVariable("content", "My Exercise 11 Tweet MLS- " + System.currentTimeMillis())
            .correlateWithResult()
            .getProcessInstance();

    assertThat(processInstance).isStarted();

  }

  @Test
  public void testTweetWithdrawn() {
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("content", "Test tweetWithdrawn message");
    ProcessInstance processInstance = runtimeService()
            .startProcessInstanceByKey("TwitterQAProcess", varMap);
    assertThat(processInstance).isStarted().isWaitingAt("ValidarQATask");
    runtimeService()
            .createMessageCorrelation("tweetWithdrawn")
            .processInstanceVariableEquals("content", "Test tweetWithdrawn message")
            .correlateWithResult();
    assertThat(processInstance).isEnded();
  }

}