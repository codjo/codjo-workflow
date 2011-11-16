package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Aid;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.UserId;
import net.codjo.agent.protocol.ContractNetProtocol;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import net.codjo.workflow.common.schedule.ScheduleLauncher;
import static net.codjo.workflow.common.util.WorkflowSystem.workFlowSystem;
import net.codjo.workflow.server.plugin.WorkflowServerPlugin;
import java.io.Serializable;
/**
 * Classe de test de {@link ScheduleAgent}.
 */
public class ScheduleAgentTest extends ScheduleAgentTestCase {
    private UserId userId = UserId.decodeUserId("l/6e594b55386d41376c37593d/0/1");


    public void test_bobo() throws Exception {
        record().mock(workFlowSystem())
              .simulateJob("job<do-stuff>()")
              .then()
              .simulateJob("job<do-next-stuff>()");

        record().startScheduleAgent(new ScheduleAgent(new ScheduleAgent.AbstractHandler() {
            public boolean acceptContract(ScheduleContract contract) {
                assertEquals(userId, getMessage().decodeUserId());
                return contract.getRequest().getType().equals("do-stuff");
            }


            public JobRequest createNextRequest(ScheduleContract contract) {
                assertEquals(userId, getMessage().decodeUserId());
                return new JobRequest("do-next-stuff");
            }
        }));

        record().addAction(new AgentContainerFixture.Runnable() {
            public void run() throws Exception {
                ScheduleLauncher command = new ScheduleLauncher(userId, "un-utilisateur");
                command.executeWorkflow(getContainer(), new JobRequest("do-stuff"));
            }
        });

        executeStory();
    }


    public void test_nominal() throws Exception {
        record().startScheduleAgent(new ScheduleAgent(new HandlerMock("nextJob")));

        record().startTester("initiator")
              .sendMessage(createScheduleContractMessage(createContract("imports", "catteao")))
              .then()
              .play(receiveAndAcceptProposal())
              .then()
              .play(receiveResult("nextJob", matchInitiator("catteao")));

        executeStory();
    }


    public void test_refuseContract() throws Exception {
        record().startAgent(SCHEDULER, new ScheduleAgent(new HandlerMock() {
            @Override
            public boolean acceptContract(ScheduleContract contract) {
                return false;
            }
        }));

        record().startTester("initiator")
              .sendMessage(createScheduleContractMessage(createContract("imports", "catteao")))
              .then()
              .play(receiveRefuseContract());

        executeStory();
    }


    public void test_badContract() throws Exception {
        record().startAgent(SCHEDULER, new ScheduleAgent(new HandlerMock()));

        record().startTester("initiator")
              .sendMessage(createCfpMessage(new Aid(SCHEDULER), new NotWorkContract()))
              .then()
              .play(receiveNotUnderstoodContract());

        executeStory();
    }


    public void test_handlerFailure_inAcceptContract() throws Exception {
        record().startAgent(SCHEDULER, new ScheduleAgent(new HandlerMock() {
            @Override
            public boolean acceptContract(ScheduleContract contract) {
                throw new NullPointerException();
            }
        }));

        record().startTester("initiator")
              .sendMessage(createScheduleContractMessage(createContract("imports", "catteao")))
              .then()
              .play(receiveNotUnderstoodContract());

        executeStory();
    }


    public void test_handlerFailure_inCreateNextRequest() throws Exception {
        record().startAgent(SCHEDULER, new ScheduleAgent(new HandlerMock() {
            @Override
            public JobRequest createNextRequest(ScheduleContract contract) {
                throw new NullPointerException();
            }
        }));

        record().startTester("initiator")
              .sendMessage(createScheduleContractMessage(createContract("imports", "catteao")))
              .then()
              .play(receiveAndAcceptProposal())
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.FAILURE));

        executeStory();
    }


    public void test_registerToDF() throws Exception {
        record().startAgent("scheduler-afterImport", new ScheduleAgent(new HandlerMock()));
        record().startAgent("scheduler-afterControl", new ScheduleAgent(new HandlerMock()));

        record().assertNumberOfAgentWithService(2, WorkflowServerPlugin.WORKFLOW_SCHEDULE_SERVICE);

        executeStory();
    }


    private AclMessage createCfpMessage(Aid receiver, Serializable content) {
        AclMessage cfp = new AclMessage(AclMessage.Performative.CFP);
        cfp.setProtocol(ContractNetProtocol.ID);
        cfp.addReceiver(receiver);
        cfp.setContentObject(content);
        cfp.setConversationId("test-" + System.currentTimeMillis());
        return cfp;
    }


    private static class HandlerMock extends ScheduleAgent.AbstractHandler {
        private String type;


        HandlerMock() {
        }


        HandlerMock(String nextJobType) {
            type = nextJobType;
        }


        public boolean acceptContract(ScheduleContract contract) {
            return true;
        }


        public JobRequest createNextRequest(ScheduleContract contract) {
            return new JobRequest(type);
        }
    }
    /**
     * @noinspection ClassMayBeInterface
     */
    private static class NotWorkContract implements Serializable {
    }
}
