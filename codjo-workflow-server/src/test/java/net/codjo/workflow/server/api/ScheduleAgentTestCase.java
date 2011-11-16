package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.Aid;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.UserId;
import net.codjo.agent.protocol.ContractNetProtocol;
import net.codjo.agent.test.AssertMatchExpression;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.StoryPart;
import net.codjo.agent.test.TesterAgentRecorder;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import junit.framework.TestCase;
/**
 *
 */
public abstract class ScheduleAgentTestCase extends TestCase {
    private Story story = new Story();
    protected ScheduleStoryRecorder scheduleStoryRecorder;
    protected static final String SCHEDULER = "scheduler";


    protected final void setUp() throws Exception {
        story.doSetUp();
        scheduleStoryRecorder = new ScheduleStoryRecorder();
    }


    protected final void tearDown() throws Exception {
        story.doTearDown();
    }


    public ScheduleStoryRecorder record() {
        story.record();
        return scheduleStoryRecorder;
    }


    public AgentContainer getContainer() {
        return story.getContainer();
    }


    protected void executeStory() throws ContainerFailureException {
        story.execute();
    }


    protected AclMessage createScheduleContractMessage(ScheduleContract contract) {
        AclMessage cfp = new AclMessage(AclMessage.Performative.CFP);
        cfp.setProtocol(ContractNetProtocol.ID);
        cfp.addReceiver(new Aid(SCHEDULER));
        cfp.setContentObject(contract);
        cfp.setConversationId("test-" + System.currentTimeMillis());
        cfp.encodeUserId(UserId.createId(contract.getRequest().getInitiatorLogin(), "password"));
        return cfp;
    }


    protected ScheduleContract createContract(String jobType, ScheduleContract previousContract) {
        ScheduleContract contract = new ScheduleContract(new JobRequest(jobType), new JobAudit());
        contract.setPreviousContract(previousContract);
        contract.getRequest().setInitiatorLogin(previousContract.getRequest().getInitiatorLogin());
        return contract;
    }


    protected ScheduleContract createContract(String jobType, String initiatorLogin) {
        JobRequest request = new JobRequest(jobType);
        request.setInitiatorLogin(initiatorLogin);
        return new ScheduleContract(request, new JobAudit());
    }


    protected ScheduleContract createContract(String jobType, Arguments arguments) {
        JobRequest request = new JobRequest(jobType);
        request.setArguments(arguments);
        return new ScheduleContract(request, new JobAudit());
    }


    protected ScheduleContract createContract(JobRequest request) {
        return new ScheduleContract(request, new JobAudit());
    }


    protected MessageTemplate and(MessageTemplate left, MessageTemplate right) {
        return MessageTemplate.and(left, right);
    }


    protected MessageTemplate matchRequestType(final String requestType) {
        return match(new AssertJobRequest("requestType", requestType) {
            protected String extractActual(JobRequest request) {
                return request.getType();
            }
        });
    }


    protected MessageTemplate matchInitiator(final String initiator) {
        return match(new AssertJobRequest("initiatorLogin", initiator) {
            protected String extractActual(JobRequest request) {
                return request.getInitiatorLogin();
            }
        });
    }


    protected MessageTemplate match(AssertJobRequest assertExpression) {
        return MessageTemplate.matchWith(assertExpression);
    }


    protected ScheduleAgent.KnowledgeLevel getAgentKnowledgeLevel() {
        return ScheduleAgent.KnowledgeLevel.DEFAULT;
    }


    protected StoryPart receiveAndAcceptProposal() {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder
                      .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.PROPOSE))
                      .assertReceivedMessage(MessageTemplate.matchContent(getAgentLevel()))
                      .replyWith(AclMessage.Performative.ACCEPT_PROPOSAL, "");
            }
        };
    }


    protected StoryPart receiveRefuseContract() {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder
                      .receiveMessage()
                      .assertReceivedMessage(
                            MessageTemplate.matchPerformative(AclMessage.Performative.REFUSE));
            }
        };
    }


    protected StoryPart receiveNotUnderstoodContract() {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder
                      .receiveMessage()
                      .assertReceivedMessage(
                            MessageTemplate.matchPerformative(AclMessage.Performative.NOT_UNDERSTOOD));
            }
        };
    }


    protected StoryPart receiveResult(final String requestType, final MessageTemplate expectedResult) {
        return new StoryPart() {
            public void record(TesterAgentRecorder recorder) {
                recorder
                      .receiveMessage(MessageTemplate.matchPerformative(AclMessage.Performative.INFORM))
                      .assertReceivedMessage(matchRequestType(requestType))
                      .assertReceivedMessage(expectedResult);
            }
        };
    }


    private String getAgentLevel() {
        return "" + getAgentKnowledgeLevel().getLevel();
    }


    public class ScheduleStoryRecorder extends Story.StoryRecorder {

        protected ScheduleStoryRecorder() {
            super(story);
        }


        public void startScheduleAgent(ScheduleAgent scheduleAgent) {
            startAgent(SCHEDULER, scheduleAgent);
        }
    }
    public static abstract class AssertJobRequest extends AssertMatchExpression {

        protected AssertJobRequest(String label, String expected) {
            super(label, expected);
        }


        protected final Object extractActual(AclMessage message) {
            JobRequest request = (JobRequest)message.getContentObject();
            return extractActual(request);
        }


        protected abstract String extractActual(JobRequest request);
    }
}
