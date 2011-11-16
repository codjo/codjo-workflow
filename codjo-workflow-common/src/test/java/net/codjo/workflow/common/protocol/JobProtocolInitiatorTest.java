/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.protocol;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentMock;
import net.codjo.agent.Aid;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.DummyAgent;
import net.codjo.agent.test.Semaphore;
import net.codjo.agent.test.Story.ConnectionType;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.util.RequestUtil;
import java.net.InetAddress;
import java.util.Iterator;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobProtocolInitiator}.
 */
public class JobProtocolInitiatorTest extends TestCase {
    private LogString log = new LogString();
    private AgentContainerFixture fixture = new AgentContainerFixture();
    private AgentMock initiatorAgentMock;
    private Semaphore semaphore = new Semaphore();
    private UserId userId;


    public void test_sendRequest() throws Exception {
        int timeout = 600000;
        JobProtocolInitiator initiator =
              createProtocolInitiator(new JobRequest("import"), new Aid("participant"), timeout);
        initiator.setAgent(initiatorAgentMock);

        assertFalse(initiator.done());

        long sentDate = System.currentTimeMillis();
        initiator.action();

        // Assert sent Message
        AclMessage sentMessage = initiatorAgentMock.getLastSentMessage();
        assertNotNull(sentMessage);
        assertEquals(AclMessage.Performative.REQUEST, sentMessage.getPerformative());
        assertEquals(JobProtocol.ID, sentMessage.getProtocol());
        assertReceiverLocalName("participant", sentMessage);
        assertSeemsGood(RequestUtil.generateConversationId(System.currentTimeMillis(), initiator),
                        sentMessage.getConversationId());
        assertEquals(userId, sentMessage.decodeUserId());
        assertTrue("Date doit être similaire !",
                   Math.abs(sentDate + timeout - sentMessage.getReplyByDate().getTime()) < 2000);

        // Assert sent Request
        JobRequest request = (JobRequest)sentMessage.getContentObject();
        assertEquals(sentMessage.getConversationId(), request.getId());
        assertEquals("import", request.getType());

        // Assert Protocole not done
        assertFalse(initiator.done());
    }


    public void test_sendRequest_useConversationId()
          throws Exception {
        JobRequest jobRequest = new JobRequest("import");
        jobRequest.setId("provided-conversation-id");

        JobProtocolInitiator initiator =
              createProtocolInitiator(jobRequest, new Aid("participant"));
        initiator.setAgent(initiatorAgentMock);

        initiator.action();

        // Assert sent Message
        AclMessage sentMessage = initiatorAgentMock.getLastSentMessage();
        assertEquals("provided-conversation-id", sentMessage.getConversationId());

        // Assert sent Request
        JobRequest request = (JobRequest)sentMessage.getContentObject();
        assertEquals(sentMessage.getConversationId(), request.getId());
    }


    public void test_receiveInform() throws Exception {
        JobProtocolInitiator initiator = new JobProtocolInitiatorMock();
        initiator.setAgent(initiatorAgentMock);

        initiator.action();
        initiatorAgentMock.getLog().clear();

        // Create inform
        AclMessage inform = createInform(JobAudit.Type.PRE);
        initiatorAgentMock.mockReceive(inform);

        initiator.action();

        initiatorAgentMock.getLog().assertContent("agent.receive((ConversationId: "
                                                  + initiatorAgentMock.getLastSentMessage()
              .getConversationId() + "))");
        log.assertContent("initiator.handleInform(INFORM)");
        assertFalse(initiator.done());
    }


    public void test_receiveInform_preFailure() throws Exception {
        JobProtocolInitiator initiator = new JobProtocolInitiatorMock();
        initiator.setAgent(initiatorAgentMock);

        // Send request
        initiator.action();

        // Create inform
        initiatorAgentMock.mockReceive(createInformFailure(JobAudit.Type.PRE, "erreur"));

        initiator.action();
        log.assertContent("initiator.handleInform(erreur)");
        assertTrue(initiator.done());
    }


    public void test_receiveInform_post() throws Exception {
        JobProtocolInitiator initiator = new JobProtocolInitiatorMock();
        initiator.setAgent(initiatorAgentMock);
        initiator.action();

        // Create inform
        AclMessage inform = createInform(JobAudit.Type.POST);
        initiatorAgentMock.mockReceive(inform);

        initiator.action();

        log.assertContent("initiator.handleInform(INFORM)");
        assertTrue(initiator.done());
    }


    public void test_receiveOutOfSequence() throws Exception {
        JobProtocolInitiator initiator = new JobProtocolInitiatorMock();
        initiator.setAgent(initiatorAgentMock);

        initiator.action();

        // Create inform
        AclMessage message = createInform(JobAudit.Type.PRE);
        message.setPerformative(AclMessage.Performative.REQUEST);
        initiatorAgentMock.mockReceive(message);

        initiator.action();

        log.assertContent("initiator.handleOutOfSequence(REQUEST)");
        assertFalse(initiator.done());
    }


    public void test_receiveFailure() throws Exception {
        JobProtocolInitiator initiator = new JobProtocolInitiatorMock();

        Agent agent = new DummyAgent(initiator);
        fixture.startNewAgent("initiator", agent);

        semaphore.acquire();
        log.assertContent("initiator.handleFailure(message(FAILURE from " + agent.getAMS().toString() + "))");
        assertTrue(initiator.done());
    }


    public void test_generateConversationId() throws Exception {
        JobProtocolInitiator initiator =
              createProtocolInitiator(new JobRequest("import"), new Aid("participant"));

        String expected = "C-";
        byte[] address = InetAddress.getLocalHost().getAddress();
        expected += Integer.toString(address[2], 36);
        expected += Integer.toString(address[3], 36);
        expected += Integer.toString(System.identityHashCode(initiator), 36);
        expected += Long.toString(Long.MAX_VALUE, 36);

        assertEquals(expected, RequestUtil.generateConversationId(Long.MAX_VALUE, initiator));
    }


    @Override
    protected void setUp() throws Exception {
        initiatorAgentMock = new AgentMock();
        initiatorAgentMock.mockGetAID(new Aid("initiatorAgentMock"));
        userId = UserId.createId("login", "password");
        fixture.doSetUp();
        fixture.startContainer(ConnectionType.NO_CONNECTION);
    }


    @Override
    protected void tearDown() throws Exception {
        fixture.doTearDown();
    }


    private void assertReceiverLocalName(String expected, AclMessage msg) {
        Iterator allReceiver = msg.getAllReceiver();
        assertTrue(allReceiver.hasNext());
        assertEquals(expected, ((Aid)allReceiver.next()).getLocalName());
    }


    private void assertSeemsGood(String expected, String conversationId) {
        assertEquals(expected.substring(0, 10), conversationId.substring(0, 10));
    }


    private AclMessage createInform(JobAudit.Type pre) {
        AclMessage requestMessage = initiatorAgentMock.getLastSentMessage();
        AclMessage inform = requestMessage.createReply(AclMessage.Performative.INFORM);
        inform.setContentObject(new JobAudit(pre));
        return inform;
    }


    private AclMessage createInformFailure(JobAudit.Type pre, String errorMessage) {
        AclMessage requestMessage = initiatorAgentMock.getLastSentMessage();
        AclMessage inform = requestMessage.createReply(AclMessage.Performative.INFORM);
        JobAudit audit = new JobAudit(pre);
        audit.setErrorMessage(errorMessage);
        inform.setContentObject(audit);
        return inform;
    }


    private JobProtocolInitiator createProtocolInitiator(JobRequest request, Aid participantAID) {
        return createProtocolInitiator(request, participantAID, 60000);
    }


    private JobProtocolInitiator createProtocolInitiator(final JobRequest request,
                                                         final Aid participantAID,
                                                         int replyTimeout) {
        return new JobProtocolInitiator(request, participantAID, userId, replyTimeout) {
            @Override
            protected void handleInform(AclMessage inform) {
            }
        };
    }


    private class JobProtocolInitiatorMock extends JobProtocolInitiator {
        private LogString logBehaviour = new LogString("initiator", log);


        JobProtocolInitiatorMock() {
            super(new JobRequest("import"), new Aid("participant"), userId, 60000);
        }


        @Override
        protected void handleInform(AclMessage inform) {
            JobAudit audit = (JobAudit)inform.getContentObject();
            if (audit.hasError()) {
                logBehaviour.call("handleInform", audit.getErrorMessage());
            }
            else {
                logBehaviour.call("handleInform",
                                  AclMessage.performativeToString(inform.getPerformative()));
            }
        }


        @Override
        protected void handleFailure(AclMessage failure) {
            logBehaviour.call("handleFailure",
                              "message(" + AclMessage.performativeToString(failure.getPerformative())
                              + " from " + failure.getSender() + ")");
            semaphore.release();
        }


        @Override
        protected void handleOutOfSequence(AclMessage outOfSequence) {
            logBehaviour.call("handleOutOfSequence",
                              AclMessage.performativeToString(outOfSequence.getPerformative()));
            semaphore.release();
        }
    }
}
