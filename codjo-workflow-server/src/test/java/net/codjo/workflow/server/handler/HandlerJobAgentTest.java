package net.codjo.workflow.server.handler;
import net.codjo.agent.Aid;
import static net.codjo.agent.test.AgentAssert.log;
import net.codjo.workflow.common.message.HandlerJobRequest;
import net.codjo.workflow.common.message.JobAudit.Status;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.api.JobAgent.MODE;
import net.codjo.workflow.server.api.WorkflowTestCase;
/**
 *
 */
public class HandlerJobAgentTest extends WorkflowTestCase {
    private DefaultHandlerContextManager handlerProcessorManagerMock = new DefaultHandlerContextManager();


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        startHandlerJobAgent();
    }


    public void test_nominal() throws Exception {
        handlerProcessorManagerMock.setHandlerExecutorCommand(userId,
                                                              "requestId",
                                                              new HandlerExecutorCommandMock(log));

        story.record().startTester("initiator")
              .sendMessage(createJobRequestMessage(createHandlerJobRequest("requestId", "user1", "<xml/>"),
                                                   new Aid("handler-agent"))).then()
              .receiveMessage(containsAudit(Type.PRE, Status.OK)).then()
              .receiveMessage(containsAudit(Type.POST, Status.OK));

        story.record().addAssert(log(log, "execute()"));

        story.execute();
    }


    private void startHandlerJobAgent() throws Exception {
        story.record()
              .startAgent("handler-agent",
                          new HandlerJobAgent(handlerProcessorManagerMock, MODE.NOT_DELEGATE));
        story.record().assertNumberOfAgentWithService(1, HandlerJobRequest.HANDLER_JOB_TYPE);
    }


    private JobRequest createHandlerJobRequest(String id, String initiatorLogin, String xmlContent) {
        HandlerJobRequest handlerJobRequest = new HandlerJobRequest();
        handlerJobRequest.setId(id);
        handlerJobRequest.setInitiatorLogin(initiatorLogin);
        handlerJobRequest.setXmlContent(xmlContent);
        return handlerJobRequest.toRequest();
    }
}
