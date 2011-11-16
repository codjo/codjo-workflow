/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.Behaviour;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.UserId;
import net.codjo.agent.protocol.ContractNetProtocol;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.DummyAgent;
import net.codjo.agent.test.Semaphore;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.SubStep;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.JobEventHandlerMock;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import net.codjo.workflow.common.util.AssertRequestType;
import net.codjo.workflow.common.util.ReplyWithJobAudit;
import static net.codjo.workflow.common.util.WorkflowSystem.workFlowSystem;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class ScheduleLeaderAgentTest extends TestCase {
    private static final String SCHEDULE_LEADER_AID = "somejob-leader-0123456789abcdef";
    private AgentContainerFixture fixture;
    private Story story = new Story();
    private final LogString log = new LogString();
    private final Semaphore semaphore = new Semaphore();
    private UserId userId;
    private WorkflowConfiguration configuration = new WorkflowConfiguration();


    public void test_createNickName() throws Exception {
        ScheduleLeaderAgent agent = new ScheduleLeaderAgent(new JobRequest("broadcast"),
                                                            new LogEventHandler(this.log),
                                                            null,
                                                            configuration);
        assertTrue(agent.createNickName("broadcast").contains(
              "broadcast-leader-" + Integer.toString(System.identityHashCode(agent), 36)));
    }


    public void test_sendRequest_withUserId() throws Exception {
        story.record().mock(workFlowSystem())
              .simulateJob("job<broadcast>()")
              .forUser(userId);

        story.record()
              .startAgent(SCHEDULE_LEADER_AID,
                          new ScheduleLeaderAgent(new JobRequest("broadcast"),
                                                  new EmptyJobEventHandler(),
                                                  userId,
                                                  configuration));

        story.execute();
    }


    public void test_scheduling_withUserId() throws Exception {
        story.record().startTester("scheduleAgent-afterBeer")
              .registerToDF(ScheduleLeaderAgent.WORKFLOW_SCHEDULE_SERVICE)
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.CFP))
              .assertReceivedMessageUserId(userId)
              .replyWith(AclMessage.Performative.PROPOSE, "5")
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.ACCEPT_PROPOSAL))
              .replyWithContent(AclMessage.Performative.INFORM, new JobRequest("go-to-toilet"));

        mockJobLeader("job<drink-beer>()", "job<go-to-toilet>()");

        story.record()
              .startAgent("scheduleLeader-drink",
                          new ScheduleLeaderAgent(new JobRequest("drink-beer"),
                                                  new JobEventHandlerMock(log),
                                                  userId, configuration));

        story.execute();
    }


    public void test_sendRequest_importWithError() throws Exception {
        startJobAgent(new LogProtocolErrorParticipant(new LogString("jobAgent", log)));

        startScheduleLeaderAgent(new EmptyJobEventHandler(), "import");

        semaphore.acquire();
        semaphore.acquire();
        fixture.assertNotContainsAgent("log/6e594b55386d41376c37593d/0/1");

        // Attend que le JobAgent reçoive la request du scheduleLeader
        log.assertContent("jobAgent.executeJob(import)");
    }


    public void test_receiveAudit() throws Exception {
        startJobAgent(new LogProtocolParticipant());

        startScheduleLeaderAgent(new LogEventHandler(new LogString("scheduleLeader", log)), "broadcast");

        semaphore.acquire(4);
        log.assertContent("scheduleLeader.handleRequest(broadcast), " + "scheduleLeader.handleAudit(PRE), "
                          + "scheduleLeader.handleAudit(POST)");
    }


    public void test_agentDieAfterPreWithError() throws Exception {
        assertThatAgentDieAfterPreWithError("broadcast");
    }


    public void test_agentDieAfterPreWithError_importCase() throws Exception {
        assertThatAgentDieAfterPreWithError("import");
    }


    public void test_agentDieAfterPostWithError() throws Exception {
        JobProtocolParticipant participant = new JobProtocolParticipant() {
            @Override
            protected void executeJob(JobRequest request) throws JobException {
                throw new JobException("marche pas");
            }
        };
        startJobAgent(participant);

        startScheduleLeaderAgent(new LogEventHandler(new LogString("scheduleLeader", log)), "broadcast");

        fixture.waitForAgentDeath(SCHEDULE_LEADER_AID);
        log.assertContent("scheduleLeader.handleRequest(broadcast)" + ", scheduleLeader.handleAudit(PRE)"
                          + ", scheduleLeader.handleAudit(POST)");
    }


    public void test_agentDieAfterPost() throws Exception {
        JobProtocolParticipant participant = new JobProtocolParticipant() {
        };
        startJobAgent(participant);

        startScheduleLeaderAgent(new LogEventHandler(new LogString("scheduleLeader", log)), "broadcast");

        fixture.waitForAgentDeath(SCHEDULE_LEADER_AID);
        log.assertContent("scheduleLeader.handleRequest(broadcast)" + ", scheduleLeader.handleAudit(PRE)"
                          + ", scheduleLeader.handleAudit(POST)");
    }


    public void test_receiveFailure() throws Exception {
        startScheduleLeaderAgent(new LogEventHandler(new LogString("scheduleLeader", log)), "broadcast");

        semaphore.acquire();
        semaphore.acquire();
        fixture.assertNotContainsAgent("log/6e594b55386d41376c37593d/0/1");

        fixture.waitForAgentDeath(SCHEDULE_LEADER_AID);
    }


    public void test_waitUntilFinished() throws Exception {
        startJobAgent(new LogProtocolParticipant(new LogString("seb", log)));

        ScheduleLeaderAgent agent = new ScheduleLeaderAgent(new JobRequest("faire-le-menage"),
                                                            new EmptyJobEventHandler(),
                                                            userId,
                                                            configuration);

        ScheduleLeaderAgent.waitUntilFinished("roxanne", agent, fixture.getContainer(), configuration);

        log.assertContent("seb.executeJob(faire-le-menage)");
        fixture.waitForAgentDeath("roxanne");
    }


    public void test_workflowSequence() throws Exception {
        story.record().startTester("scheduleAgent-afterBeer")
              .registerToDF(ScheduleLeaderAgent.WORKFLOW_SCHEDULE_SERVICE)
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.CFP))
              .assertReceivedMessage(MessageTemplate.matchProtocol(ContractNetProtocol.ID))
              .add(new AssertReplyByDateNotNull())
              .add(new AssertScheduleContract("drink-beer"))
              .replyWith(AclMessage.Performative.PROPOSE, "5")
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.ACCEPT_PROPOSAL))
              .replyWithContent(AclMessage.Performative.INFORM, new JobRequest("go-to-toilet"));

        mockJobLeader("job<drink-beer>()", "job<go-to-toilet>()");

        story.record()
              .startAgent("scheduleLeader-drink",
                          new ScheduleLeaderAgent(new JobRequest("drink-beer"), new JobEventHandlerMock(log),
                                                  userId, configuration));

        story.execute();
    }


    public void test_workflowSequence_2steps() throws Exception {
        story.record().startTester("schedule-agent")
              .registerToDF(ScheduleLeaderAgent.WORKFLOW_SCHEDULE_SERVICE).then().receiveMessage()
              .add(new AssertScheduleContract("step-1"))
              .replyWith(AclMessage.Performative.PROPOSE, "0")
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.ACCEPT_PROPOSAL))
              .replyWithContent(AclMessage.Performative.INFORM, new JobRequest("step-2")).then()
              .receiveMessage().add(new AssertScheduleContract("step-2", "step-1"))
              .replyWith(AclMessage.Performative.PROPOSE, "0");

        mockJobLeader("job<step-1>()", "job<step-2>()");

        story.record()
              .startAgent("scheduleLeader-drink",
                          new ScheduleLeaderAgent(new JobRequest("step-1"), new JobEventHandlerMock(log),
                                                  userId, configuration));

        story.execute();
    }


    public void test_workflowSequence_noScheduleAgent()
          throws Exception {
        story.record().startTester("organiser-agent-bidon")
              .registerToDF(Service.ORGANISER_SERVICE).then()
              .receiveMessage()
              .add(new AssertRequestType("drink-beer"))
              .add(new ReplyWithJobAudit(AclMessage.Performative.INFORM, JobAudit.Type.PRE))
              .add(new ReplyWithJobAudit(AclMessage.Performative.INFORM, JobAudit.Type.POST));

        story.record().assertNumberOfAgentWithService(1, Service.ORGANISER_SERVICE);

        story.record()
              .startAgent("scheduleLeader-drink",
                          new ScheduleLeaderAgent(new JobRequest("drink-beer"), new JobEventHandlerMock(),
                                                  userId, configuration));

        story.record().addAssert(new AgentAssert.Assertion() {
            public void check() throws AssertionFailedError {
                story.getAgentContainerFixture().waitForAgentDeath("scheduleLeader-drink");
            }
        });
        story.execute();
    }


    @Override
    protected void setUp() throws Exception {
        story.doSetUp();
        fixture = story.getAgentContainerFixture();
        userId = UserId.decodeUserId("log/6e594b55386d41376c37593d/0/1");
        configuration.setDefaultReplyTimeout(5000);
    }


    @Override
    protected void tearDown() throws Exception {
        story.doTearDown();
    }


    private void assertThatAgentDieAfterPreWithError(String jobRequestType)
          throws ContainerFailureException, DFServiceException {
        JobProtocolParticipant participant =
              new JobProtocolParticipant() {
                  @Override
                  protected void handlePRE(JobRequest request) {
                      JobAudit audit = new JobAudit(JobAudit.Type.PRE);
                      audit.setErrorMessage("pre-error");
                      sendAudit(audit);
                      restart();
                  }
              };
        startJobAgent(participant);

        startScheduleLeaderAgent(new LogEventHandler(new LogString("scheduleLeader", log)), jobRequestType);

        fixture.waitForAgentDeath(SCHEDULE_LEADER_AID);
        log.assertContent("scheduleLeader.handleRequest(" + jobRequestType
                          + "), scheduleLeader.handleAudit(PRE)");
    }


    private void startScheduleLeaderAgent(JobEventHandler jobEventHandler, String jobRequestType)
          throws ContainerFailureException {
        ScheduleLeaderAgent scheduleLeader =
              new ScheduleLeaderAgent(new JobRequest(jobRequestType), jobEventHandler, userId, configuration);
        fixture.startNewAgent(SCHEDULE_LEADER_AID, scheduleLeader);
    }


    private void startJobAgent(Behaviour behaviour) throws ContainerFailureException, DFServiceException {
        DummyAgent agent = new DummyAgent(behaviour);
        final String agentName = "organiser-dummy-agent";
        fixture.startNewAgent(agentName, agent); //TODO pourquoi pas remonter le register dans la fixture
        DFService.register(agent, DFService.createAgentDescription(Service.ORGANISER_SERVICE));
    }


    private void mockJobLeader(String firstJobType, String secondJobType) {
        story.record().mock(workFlowSystem())
              .simulateJob(firstJobType)
              .then()
              .simulateJob(secondJobType);
    }


    private class LogProtocolParticipant extends JobProtocolParticipant {
        private LogString localLog;


        LogProtocolParticipant() {
            this.localLog = new LogString();
        }


        LogProtocolParticipant(LogString localLog) {
            this.localLog = localLog;
        }


        @Override
        protected void executeJob(JobRequest request) {
            localLog.call("executeJob", request.getType());
            semaphore.release();
        }
    }

    private class LogProtocolErrorParticipant extends JobProtocolParticipant {
        private LogString localLog;


        LogProtocolErrorParticipant() {
            this.localLog = new LogString();
        }


        LogProtocolErrorParticipant(LogString localLog) {
            this.localLog = localLog;
        }


        @Override
        protected void executeJob(JobRequest request)
              throws JobException {
            localLog.call("executeJob", request.getType());
            semaphore.release();
            throw new ImportMockException("Error during import.");
        }
    }

    private class LogEventHandler extends JobEventHandler {
        private LogString localLog;


        LogEventHandler(LogString localLog) {
            this.localLog = localLog;
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            localLog.call("handleAudit", audit.getType());
            semaphore.release();
        }


        @Override
        protected void handleRequest(JobRequest request) {
            localLog.call("handleRequest", request.getType());
            semaphore.release();
        }


        @Override
        public boolean receiveError(ProtocolErrorEvent event) {
            localLog.call("receiveError", event.getType());
            semaphore.release();
            return true;
        }
    }

    private static class EmptyJobEventHandler extends JobEventHandler {
    }

    private static class ImportMockException extends JobException {
        ImportMockException(String message) {
            super(message);
        }
    }

    private static class AssertScheduleContract implements SubStep {
        private String requestType;
        private final String previousRequestType;


        AssertScheduleContract(String requestType) {
            this(requestType, null);
        }


        AssertScheduleContract(String requestType, String previousRequestType) {
            this.requestType = requestType;
            this.previousRequestType = previousRequestType;
        }


        public void run(Agent agent, AclMessage message)
              throws AssertionFailedError {
            ScheduleContract contract = (ScheduleContract)message.getContentObject();
            assertEquals(requestType, contract.getRequest().getType());
            assertEquals(JobAudit.Type.POST, contract.getPostAudit().getType());

            if (previousRequestType == null) {
                assertNull(contract.getPreviousContract());
            }
            else {
                assertNotNull(contract.getPreviousContract());
                assertEquals(previousRequestType, contract.getPreviousContract().getRequest().getType());
            }
        }
    }

    private static class AssertReplyByDateNotNull implements SubStep {
        public void run(Agent agent, AclMessage message)
              throws Exception {
            assertNotNull(message.getReplyByDate());
        }
    }
}
