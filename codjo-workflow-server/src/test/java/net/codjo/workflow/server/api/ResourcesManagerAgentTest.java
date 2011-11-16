package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import static net.codjo.agent.DFService.createAgentDescription;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.MessageTemplate.MatchExpression;
import net.codjo.agent.protocol.ContractNetProtocol;
import net.codjo.workflow.common.message.JobContract;
import net.codjo.workflow.common.message.JobContractResultOntology;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocol;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent.MODE;
import net.codjo.workflow.server.api.ResourcesManagerAgent.AgentFactory;
import org.junit.Test;
/**
 *
 */
public class ResourcesManagerAgentTest extends WorkflowTestCase {
    private static final String DO_STUFF_JOB_ID = "do-stuff";


    @Test
    public void test_delegate() throws Exception {
        story.record().startAgent("resources-manager-agent",
                                  new ResourcesManagerAgent(new DoStuffAgentFactory(),
                                                            createAgentDescription(DO_STUFF_JOB_ID)));

        AclMessage message = new AclMessage(Performative.CFP);
        message.setProtocol(ContractNetProtocol.ID);
        JobRequest request = new JobRequest(DO_STUFF_JOB_ID);
        message.setContentObject(new JobContract(request));
        message.addReceiver(aid("resources-manager-agent"));
        message.setConversationId("conversationID");

        AclMessage requestMessage = new AclMessage(Performative.REQUEST);
        requestMessage.setProtocol(JobProtocol.ID);
        requestMessage.setContentObject(request);
        requestMessage.addReceiver(aid("resources-manager-agent"));
        requestMessage.setConversationId("conversationID");

        story.record().startTester("job-leader").
              sendMessage(message)
              .then()
              .receiveMessage()
              .replyWith(Performative.ACCEPT_PROPOSAL, "")
              .then()
              .receiveMessage(MessageTemplate.matchWith(new MatchExpression() {
                  public boolean match(AclMessage aclMessage) {
                      return JobContractResultOntology.extractDelegate(aclMessage)
                            .startsWith("do-stuff-delegate-agent");
                  }
              }));

        story.record().assertNumberOfAgentWithService(1, DO_STUFF_JOB_ID);

        story.execute();
    }


    public static class DoStuffAgentFactory implements AgentFactory {
        public JobAgent create() throws Exception {
            return new JobAgent(new JobProtocolParticipant(),
                                createAgentDescription(DO_STUFF_JOB_ID),
                                MODE.DELEGATE);
        }
    }
}
