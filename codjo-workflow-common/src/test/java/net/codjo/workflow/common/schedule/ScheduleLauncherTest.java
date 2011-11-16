/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.AgentController;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AgentAssert.Assertion;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.SubStep;
import net.codjo.plugin.batch.BatchException;
import net.codjo.plugin.batch.TimeoutBatchException;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import static net.codjo.workflow.common.util.WorkflowSystem.workFlowSystem;
import java.io.File;
import java.sql.Date;
import junit.framework.TestCase;
/**
 * Classe de test de {@link net.codjo.workflow.common.schedule.ScheduleLauncher}.
 */
public class ScheduleLauncherTest extends TestCase {
    private static final String EXPECTED_JOB_DESCRIPTION
          = "job<jobType>(broadcastDate=2004-01-25, file=directory\\fileName)";
    private Story story = new Story();
    private String id1, id2;


    public void test_scheduleLeaderName() throws Exception {
        ScheduleLauncher command = createLauncher();
        assertNull(command.getScheduleLeaderAgentName());
    }


    public void test_initiator() throws Exception {
        ScheduleLauncher command = new ScheduleLauncher(UserId.createId("gonnot", "mysecret"));
        assertEquals("gonnot", command.getInitiatorLogin());
    }


    public void test_execute_optimalCase() throws Exception {
        final ScheduleLauncher command = createLauncher();

        story.record().mock(workFlowSystem())
              .simulateJob(EXPECTED_JOB_DESCRIPTION)
              .forUser(UserId.decodeUserId("l/6e594b55386d41376c37593d/0/1"));

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                command.executeWorkflow(getContainer(), createRequest(new File("directory", "fileName"),
                                                                      toDate("2004-01-25")));
                id1 = command.getScheduleLeaderAgentName();
            }
        });

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                story.getAgentContainerFixture().assertNotContainsAgent(id1);
            }
        });

        story.execute();
    }


    public void test_twoConsecutiveLaunches() throws Exception {
        final ScheduleLauncher scheduleLauncher =
              new ScheduleLauncher(UserId.createId("gonnot", "6e594b55386d41376c37593d"));
        scheduleLauncher.setExecuteType(ScheduleLauncher.ExecuteType.SYNCHRONOUS);

        story.record().mock(workFlowSystem())
              .simulateJob(EXPECTED_JOB_DESCRIPTION).then()
              .simulateJob(EXPECTED_JOB_DESCRIPTION);

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                scheduleLauncher.executeWorkflow(getContainer(), createRequest());
                id1 = scheduleLauncher.getScheduleLeaderAgentName();
            }
        });

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                scheduleLauncher.executeWorkflow(getContainer(), createRequest());
                id2 = scheduleLauncher.getScheduleLeaderAgentName();
            }
        });

        story.execute();

        assertNotNull(id1);
        assertNotNull(id2);
        assertFalse(id1.equals(id2));
    }


    public void test_execute_asynchronously() throws Exception {
        final ScheduleLauncher command = new ScheduleLauncher(UserId.createId("james", "secret"));
        command.setExecuteType(ScheduleLauncher.ExecuteType.ASYNCHRONOUS);

        // On verifie le caractère asynchrone par le fait que le test se termine
        //   sans avoir besoin de lancer les messages PRE et POST
        story.record().mock(workFlowSystem())
              .simulateJobWithoutPrePostReplies(EXPECTED_JOB_DESCRIPTION);

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                command.executeWorkflow(getContainer(), createRequest());
            }
        });

        story.execute();
    }


    public void test_asynchronous() throws Exception {
        ScheduleLauncher launcher = new ScheduleLauncher(UserId.createId("james", "secret"));
        assertEquals(ScheduleLauncher.ExecuteType.SYNCHRONOUS, launcher.getExecuteType());
        launcher.setExecuteType(ScheduleLauncher.ExecuteType.ASYNCHRONOUS);
        assertEquals(ScheduleLauncher.ExecuteType.ASYNCHRONOUS, launcher.getExecuteType());
    }


    public void test_preFailure() throws Exception {
        story.record().mock(workFlowSystem())
              .simulateJobRefused("pas envie de le faire");

        assertCommandFailure("pas envie de le faire");
    }


    public void test_postFailure() throws Exception {
        story.record().mock(workFlowSystem())
              .simulateJobError("Toujours pas envie de le faire");

        assertCommandFailure("Toujours pas envie de le faire");
    }


    public void test_scheduleLeaderDieFailure() throws Exception {
        final ScheduleLauncher scheduleLauncher = createLauncher();

        story.record().mock(workFlowSystem())
              .simulateJob(EXPECTED_JOB_DESCRIPTION, new SubStep() {
                  public void run(Agent agent, AclMessage message) throws Exception {
                      AgentController scheduleLeaderAgent =
                            getContainer().getAgent(scheduleLauncher.getScheduleLeaderAgentName());
                      scheduleLeaderAgent.kill();
                  }
              });

        story.record().addAssert(assertTimeoutFailure(scheduleLauncher));

        story.execute();
    }


    public void test_timeoutFailure() throws Exception {
        final ScheduleLauncher scheduleLauncher = createLauncher();
        scheduleLauncher.getWorkflowConfiguration().setDefaultTimeout(100);

        story.record().mock(workFlowSystem())
              .simulateJob(EXPECTED_JOB_DESCRIPTION);

        story.record().addAssert(assertTimeoutFailure(scheduleLauncher));

        story.execute();
    }


    @Override
    protected void setUp() throws Exception {
        story.doSetUp();
    }


    @Override
    protected void tearDown() throws Exception {
        story.doTearDown();
    }


    private Assertion assertTimeoutFailure(final ScheduleLauncher scheduleLauncher) {
        return new Assertion() {
            public void check() throws Throwable {
                try {
                    scheduleLauncher.executeWorkflow(getContainer(), createRequest());
                    fail("Timeout attendue.");
                }
                catch (TimeoutBatchException exception) {
                    ;
                }
            }
        };
    }


    private ScheduleLauncher createLauncher() {
        return new ScheduleLauncher(UserId.decodeUserId("l/6e594b55386d41376c37593d/0/1"), "un-utilisateur");
    }


    private JobRequest createRequest() {
        return createRequest(new File("directory", "fileName"), toDate("2004-01-25"));
    }


    private JobRequest createRequest(File destinationFile, Date broadcastDate) {
        JobRequest request = new JobRequest();
        request.setType("jobType");
        request.setArguments(new Arguments());
        request.getArguments().put("file", destinationFile.getPath());
        request.getArguments().put("broadcastDate", broadcastDate.toString());
        return request;
    }


    private void assertCommandFailure(final String error)
          throws Exception {
        final ScheduleLauncher command = createLauncher();

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                try {
                    command.executeWorkflow(getContainer(), createRequest());
                    fail("BatchException attendue.");
                }
                catch (BatchException exception) {
                    assertEquals(error, exception.getLocalizedMessage());
                }
                finally {
                    id1 = command.getScheduleLeaderAgentName();
                }
            }
        });

        story.record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                story.getAgentContainerFixture().assertNotContainsAgent(id1);
            }
        });

        story.execute();
    }


    private Date toDate(String date) {
        return Date.valueOf(date);
    }


    private AgentContainer getContainer() {
        return story.getAgentContainerFixture().getContainer();
    }
}
