package net.codjo.workflow.common.util;
import net.codjo.agent.AclMessage;
import static net.codjo.agent.AclMessage.Performative.ACCEPT_PROPOSAL;
import static net.codjo.agent.AclMessage.Performative.CFP;
import static net.codjo.agent.AclMessage.Performative.INFORM;
import static net.codjo.agent.AclMessage.Performative.PROPOSE;
import static net.codjo.agent.AclMessage.Performative.REFUSE;
import net.codjo.agent.Agent;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.UserId;
import net.codjo.agent.test.Story;
import net.codjo.agent.test.SubStep;
import net.codjo.agent.test.SystemMock;
import net.codjo.agent.test.TesterAgentRecorder;
import static net.codjo.test.common.matcher.JUnitMatchers.*;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.ScheduleContract;
import net.codjo.workflow.common.protocol.JobProtocol;
import net.codjo.workflow.common.schedule.ScheduleLeaderAgent;

public class WorkflowSystem extends SystemMock {
    private static final String ORGANISER_AGENT = "organiser-agent";
    private Story story;
    private TesterAgentRecorder mockAgent;
    private JobRequestStringifier jobRequestStringifier = new JobRequestStringifier();


    public static WorkflowSystem workFlowSystem() {
        return new WorkflowSystem();
    }


    @Override
    protected void record(Story myStory) {
        this.story = myStory;
        mockAgent = story.record().startTester(ORGANISER_AGENT);
        mockAgent.registerToDF(Service.ORGANISER_SERVICE);
        story.record().assertAgentWithService(new String[]{ORGANISER_AGENT}, Service.ORGANISER_SERVICE);
    }


    public SimulateRecorder simulateJob(String expectedJobRequestDescription) {
        return simulateJob(expectedJobRequestDescription, new ReplyWithJobAudit(INFORM, Type.POST));
    }


    public SimulateRecorder simulateJob(String expectedJobRequestDescription, SubStep customBehavior) {
        final AssertJobRequest assertJobRequest = new AssertJobRequest(expectedJobRequestDescription);

        mockAgent.receiveMessage()
              .assertReceivedMessage(MessageTemplate.matchPerformative(AclMessage.Performative.REQUEST))
              .assertReceivedMessage(MessageTemplate.matchProtocol(JobProtocol.ID))
              .add(assertJobRequest)
              .add(new ReplyWithJobAudit(INFORM, Type.PRE))
              .add(customBehavior);

        return new SimulateRecorder() {
            public WorkflowSystem then() {
                return WorkflowSystem.this;
            }


            public SimulateRecorder forUser(UserId userId) {
                assertJobRequest.setExpectedUserId(userId);
                return this;
            }
        };
    }


    public Then simulateJobRefused(String expectedErrorMessage) {
        mockAgent.receiveMessage()
              .add(new ReplyWithJobAudit(INFORM, Type.PRE, expectedErrorMessage));

        return new Then() {
            public WorkflowSystem then() {
                return WorkflowSystem.this;
            }
        };
    }


    public Then simulateJobError(String expectedErrorMessage) {
        mockAgent.receiveMessage()
              .add(new ReplyWithJobAudit(INFORM, Type.PRE))
              .add(new ReplyWithJobAudit(INFORM, Type.POST, expectedErrorMessage));

        return new Then() {
            public WorkflowSystem then() {
                return WorkflowSystem.this;
            }
        };
    }


    public Then simulateJobWithoutPrePostReplies(String expectedJobRequestDescription) {
        final AssertJobRequest assertJobRequest = new AssertJobRequest(expectedJobRequestDescription);

        mockAgent.receiveMessage()
              .assertReceivedMessage(MessageTemplate.matchPerformative(AclMessage.Performative.REQUEST))
              .assertReceivedMessage(MessageTemplate.matchProtocol(JobProtocol.ID))
              .add(assertJobRequest);

        return new SimulateRecorder() {
            public WorkflowSystem then() {
                return WorkflowSystem.this;
            }


            public SimulateRecorder forUser(UserId userId) {
                assertJobRequest.setExpectedUserId(userId);
                return this;
            }
        };
    }


    public Then schedule(String schedulingPlan) {
        String[] strings = schedulingPlan.split("->");
        final String expectedPreviousJob = strings[0].trim();
        String nextJobToBePlanned = strings[1].trim();

        story.record().startTester("scheduleMock")
              .registerToDF(ScheduleLeaderAgent.WORKFLOW_SCHEDULE_SERVICE)
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(CFP))
              .add(new SubStep() {
                  public void run(Agent agent, AclMessage message) throws Exception {
                      ScheduleContract contract = (ScheduleContract)message.getContentObject();
                      assertThat(contract.getRequest().getType(), equalTo(expectedPreviousJob));
                  }
              })
              .replyWith(PROPOSE, "5")
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(ACCEPT_PROPOSAL))
              .replyWithContent(INFORM, new JobRequest(nextJobToBePlanned))
              .then()
              .receiveMessage(MessageTemplate.matchPerformative(CFP))
              .replyWith(REFUSE, "5");

        return new Then() {
            public WorkflowSystem then() {
                return WorkflowSystem.this;
            }
        };
    }


    public TesterAgentRecorder then() {
        return mockAgent;
    }


    public interface Then {
        WorkflowSystem then();
    }

    public interface SimulateRecorder extends Then {
        SimulateRecorder forUser(UserId userId);
    }

    public static class JobRequestStringifier {
        public String toString(JobRequest jobRequest, AclMessage message) {
            StringBuilder builder = new StringBuilder();

            builder.append("job<").append(jobRequest.getType()).append(">(");
            Arguments jobArguments = jobRequest.getArguments();
            if (jobArguments != null) {
                builder.append(jobArguments.encode().trim().replaceAll("\n", ", "));
            }
            builder.append(")");
            return builder.toString();
        }
    }

    private class AssertJobRequest implements SubStep {
        private final String expectedJobRequestDescription;
        private UserId expectedUserId;


        AssertJobRequest(String expectedJobRequestDescription) {
            this.expectedJobRequestDescription = expectedJobRequestDescription;
        }


        public void run(Agent agent, AclMessage message) throws Exception {
            JobRequest jobRequest = (JobRequest)message.getContentObject();
            assertThat(jobRequestStringifier.toString(jobRequest, message),
                       equalTo(expectedJobRequestDescription));

            if (expectedUserId != null) {
                assertThat(message.decodeUserId(),
                           equalTo(expectedUserId));
            }
        }


        public void setExpectedUserId(UserId expectedUserId) {
            this.expectedUserId = expectedUserId;
        }
    }
}
