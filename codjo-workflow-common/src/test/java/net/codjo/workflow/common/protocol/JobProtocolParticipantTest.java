/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.protocol;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentMock;
import net.codjo.agent.Aid;
import net.codjo.agent.Behaviour;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.MessageTemplate.MatchExpression;
import static net.codjo.agent.MessageTemplate.matchWith;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.DummyAgent;
import static net.codjo.agent.test.MessageBuilder.message;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import net.codjo.test.common.fixture.CompositeFixture;
import net.codjo.test.common.fixture.DirectoryFixture;
import net.codjo.util.file.FileUtil;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobAudit.Type;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import java.io.File;
import java.util.Arrays;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
/**
 * Classe de test de {@link net.codjo.workflow.common.protocol.JobProtocolInitiator}.
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class JobProtocolParticipantTest {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String CONVERSATION_ID = "requestId";
    private AgentMock participantAgentMock;
    private AgentContainerFixture agentFixture = new AgentContainerFixture();
    private DirectoryFixture directoryFixture = DirectoryFixture.newTemporaryDirectoryFixture();
    private CompositeFixture compositeFixture = new CompositeFixture(directoryFixture, agentFixture);
    private JobProtocolParticipant participant;
    private FileAppender appender;
    private File logFile;
    private Story story = new Story();


    @Test
    public void test_getRequestMessage() throws Exception {
        assertNull(participant.getRequestMessage());
        assertNull(participant.getRequest());

        // Démarrage du protocol
        AclMessage request = createRequest();
        participantAgentMock.mockReceive(request);
        participant.action();
        participant.action();
        assertEquals(request, participant.getRequestMessage());
        assertEquals(((JobRequest)request.getContentObject()).getType(),
                     participant.getRequest().getType());

        // Bascule en SEND_POST puis en WAIT_FOR_REQUEST
        participant.declareJobDone();
        participantAgentMock.mockReceive(null);
        participant.action();
        assertNull(participant.getRequestMessage());
        assertNull(participant.getRequest());
    }


    @Test
    public void test_receiveRequest() throws Exception {
        assertFalse(participant.done());

        AclMessage request = createRequest();
        participantAgentMock.mockReceive(request);
        participant.action();
        participant.action();

        assertEquals(request, participant.getRequestMessage());
        participantAgentMock.getLog().assertContent(
              "agent.receive((( Protocol: workflow-job-protocol ) AND ( Perfomative: REQUEST )))");
    }


    @Test
    public void test_handlePRE_failure() throws Exception {
        final LogString log = new LogString();

        participant.logger.removeAppender(appender);
        participant = new JobProtocolParticipant() {
            @Override
            protected void handlePRE(JobRequest request) {
                // SendAudit failure
                log.call("handlePRE", "...");
                JobAudit audit = new JobAudit(JobAudit.Type.PRE);
                audit.setErrorMessage("zut !");
                sendAudit(audit);
            }
        };

        participant.logger.addAppender(appender);
        participant.logger.setLevel(Level.INFO);

        participant.setAgent(participantAgentMock);

        // state INITIALIZE_CFP
        participant.action();
        // state WAIT_FOR_REQUEST
        participantAgentMock.mockReceive(createRequest());
        participant.action();
        participantAgentMock.getLog().clear();
        assertEquals("INFO - (requestId) Requête de null" + NEW_LINE, FileUtil.loadContent(logFile));
        // state SEND_PRE
        participant.action();
        log.assertContent("handlePRE(...)");
        assertEquals("INFO - (requestId) Requête de null" + NEW_LINE
                     + "ERROR - (requestId) Audit PRE - zut !" + NEW_LINE,
                     FileUtil.loadContent(logFile));
        participantAgentMock.getLog().clear();
        // rebascule WAIT_FOR_REQUEST
        participant.action();
        participantAgentMock.getLog().assertContent(
              "agent.receive((( Protocol: workflow-job-protocol ) AND ( Perfomative: REQUEST )))");
        assertEquals("INFO - (requestId) Requête de null" + NEW_LINE
                     + "ERROR - (requestId) Audit PRE - zut !" + NEW_LINE
                     + "INFO - (requestId) Requête de null" + NEW_LINE,
                     FileUtil.loadContent(logFile));
    }


    @Test
    public void test_receiveRequest_callCycle() throws Exception {
        // Init
        Agent initiatorAgent = new DummyAgent();
        Agent participantAgent =
              new DummyAgent(new JobProtocolParticipant() {
                  @Override
                  protected void executeJob(JobRequest request) {
                      sendAudit(new JobAudit(JobAudit.Type.MID));
                  }
              });

        agentFixture.startNewAgent("initiatorAgent", initiatorAgent);
        agentFixture.startNewAgent("participantAgent", participantAgent);

        // Send Request
        AclMessage request = createRequest();
        request.addReceiver(participantAgent.getAID());
        agentFixture.sendMessage(initiatorAgent, request);

        // Assert notification
        AclMessage preAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.PRE, preAudit);

        AclMessage midAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.MID, midAudit);

        AclMessage postAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.POST, postAudit);
    }


    @Test
    public void test_receiveRequest_setExecuteJobBehaviour() throws Exception {
        participant = createParticipant();
        participant.setExecuteJobBehaviour(new Behaviour() {
            @Override
            protected void action() {
                participant.sendAudit(new JobAudit(JobAudit.Type.MID));
                participant.declareJobDone();
            }


            @Override
            public boolean done() {
                return true;
            }
        });

        // Init
        Agent initiatorAgent = new DummyAgent();
        Agent participantAgent = new DummyAgent(participant);

        agentFixture.startNewAgent("initiatorAgent", initiatorAgent);
        agentFixture.startNewAgent("participantAgent", participantAgent);

        // Send Request
        AclMessage request = createRequest();
        request.addReceiver(participantAgent.getAID());
        agentFixture.sendMessage(initiatorAgent, request);

        // Assert notification
        AclMessage preAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.PRE, preAudit);

        AclMessage midAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.MID, midAudit);

        AclMessage postAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.POST, postAudit);
    }


    @Test
    public void test_receiveRequest_failure() throws Exception {
        assertFailure(new JobException("job error", null));
    }


    @Test
    public void test_receiveRequest_error() throws Exception {
        assertFailure(new OutOfMemoryError("Achete de la RAM"));
    }


    private void assertFailure(final Throwable failure) throws ContainerFailureException {
        // Init
        Agent initiatorAgent = new DummyAgent();
        Agent participantAgent = new DummyAgent(new JobProtocolParticipant() {
            @Override
            protected void executeJob(JobRequest request) throws JobException {
                if (failure instanceof JobException) {
                    throw (JobException)failure;
                }
                else {
                    throw (Error)failure;
                }
            }
        });

        agentFixture.startNewAgent("initiatorAgent", initiatorAgent);
        agentFixture.startNewAgent("participantAgent", participantAgent);

        // Send Request
        AclMessage request = createRequest();
        request.addReceiver(participantAgent.getAID());
        agentFixture.sendMessage(initiatorAgent, request);

        // Assert notification
        AclMessage preAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.PRE, preAudit);

        AclMessage postAudit = agentFixture.receiveMessage(initiatorAgent);
        assertAuditMessage(JobAudit.Type.POST, postAudit);

        JobAudit audit = ((JobAudit)postAudit.getContentObject());
        assertTrue(audit.hasError());
        assertEquals(failure.getMessage(), audit.getErrorMessage());
        assertTrue(audit.getError().getDescription().contains(failure.getMessage()));
    }


    @Test
    public void test_sendAudit() throws Exception {
        AclMessage request = createRequest();
        participant.setRequestMessage(request);

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        participant.sendAudit(audit);

        assertEquals(request.getConversationId(), audit.getRequestId());
        AclMessage sentMessage = participantAgentMock.getLastSentMessage();
        assertEquals(request.getConversationId(), sentMessage.getConversationId());
        assertEquals(request.getProtocol(), sentMessage.getProtocol());
        assertEquals(AclMessage.Performative.INFORM, sentMessage.getPerformative());
        assertEquals(audit.getType(), ((JobAudit)sentMessage.getContentObject()).getType());
        assertEquals("INFO - (requestId) Requête de null" + NEW_LINE
                     + "INFO - (requestId) Audit PRE" + NEW_LINE, FileUtil.loadContent(logFile));
    }


    @Test
    public void test_logAudit_shortArgumentsSize() throws Exception {
        AclMessage request = createRequest();
        participant.setRequestMessage(request);

        JobAudit audit = new JobAudit(JobAudit.Type.POST);
        String shortArgumentValue = "begin end";
        audit.setArguments(new Arguments("shortArgumentKey", shortArgumentValue));
        participant.sendAudit(audit);

        String row1 = String.format("INFO - (requestId) Requête de null%s", NEW_LINE);
        String row2 = String.format("INFO - (requestId) Audit POST / shortArgumentKey=%s%s",
                                    shortArgumentValue,
                                    NEW_LINE);
        assertEquals(String.format("%s%s", row1, row2), FileUtil.loadContent(logFile));
    }


    @Test
    public void test_logAudit_longArgumentsSize() throws Exception {
        AclMessage request = createRequest();
        participant.setRequestMessage(request);

        JobAudit audit = new JobAudit(JobAudit.Type.POST);
        char[] blankCharArray = new char[200];
        Arrays.fill(blankCharArray, ' ');
        String longArgumentValue = String.format("begin %s end", new String(blankCharArray));
        audit.setArguments(new Arguments("longArgumentKey", longArgumentValue));
        participant.sendAudit(audit);

        String arguments = String.format("longArgumentKey=%s", longArgumentValue);
        String row1 = String.format("INFO - (requestId) Requête de null%s", NEW_LINE);
        String row2 = String.format("INFO - (requestId) Audit POST / %s%s",
                                    arguments.substring(0, 125) + "...",
                                    NEW_LINE);
        assertEquals(String.format("%s%s", row1, row2), FileUtil.loadContent(logFile));
    }


    @Test
    public void test_logsRequestOnlyIfNeeded() throws Exception {
        AclMessage request = createRequest(CONVERSATION_ID, false);
        participant.setRequestMessage(request);

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        participant.sendAudit(audit);

        assertEquals("", FileUtil.loadContent(logFile));

        request = createRequest(CONVERSATION_ID, true);
        participant.setRequestMessage(request);

        audit = new JobAudit(JobAudit.Type.PRE);
        participant.sendAudit(audit);

        assertEquals("INFO - (requestId) Requête de null" + NEW_LINE
                     + "INFO - (requestId) Audit PRE" + NEW_LINE, FileUtil.loadContent(logFile));
    }


    @Test
    public void test_setExecuteJobBehaviour_error() throws Exception {
        try {
            participant.setExecuteJobBehaviour(null);
        }
        catch (IllegalArgumentException ex) {
            // Ok
        }
    }


    @Test
    public void test_busyAgentSendsARefuse() throws Exception {
        participant = createParticipant(new Behaviour() {
            @Override
            protected void action() {
                participant.sendAudit(new JobAudit(Type.MID));
                block();
            }


            @Override
            public boolean done() {
                return false;
            }
        });
        story.record().startAgent("participant", new DummyAgent(participant));

        story.record().startTester("initiator")
              .send(message(Performative.REQUEST)
                    .to("participant")
                    .usingConversationId(CONVERSATION_ID)
                    .usingProtocol(JobProtocol.ID)
                    .withContent(createJobRequest(CONVERSATION_ID, true))).then()
              .receiveMessage(matchingAudit(Type.PRE)).then()
              .receiveMessage(matchingAudit(Type.MID)).then()
              .send(message(Performative.REQUEST)
                    .to("participant")
                    .usingConversationId("anotherRequestId")
                    .usingProtocol(JobProtocol.ID)
                    .withContent(createJobRequest("anotherRequestId", true))).then()
              .receiveMessage(matchWith(new MatchExpression() {
                  @SuppressWarnings({"RedundantIfStatement"})
                  public boolean match(AclMessage aclMessage) {
                      if (!aclMessage.getProtocol().equals(JobProtocol.ID)) {
                          return false;
                      }
                      if (aclMessage.getPerformative() != Performative.REFUSE) {
                          return false;
                      }
                      return true;
                  }
              }));

        story.execute();
    }


    private AclMessage createRequest() {
        return createRequest(CONVERSATION_ID);
    }


    private static AclMessage createRequest(String conversationId) {
        return createRequest(conversationId, true);
    }


    private static AclMessage createRequest(String conversationId, boolean isLoggable) {
        AclMessage request = new AclMessage(AclMessage.Performative.REQUEST);
        request.setConversationId(conversationId);
        request.setProtocol(JobProtocol.ID);
        request.setContentObject(createJobRequest(conversationId, isLoggable));
        return request;
    }


    private static JobRequest createJobRequest(String conversationId, boolean isLoggable) {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setId(conversationId);
        jobRequest.setType("import");
        jobRequest.setLoggable(isLoggable);
        return jobRequest;
    }


    @Before
    public void setUp() throws Exception {
        compositeFixture.doSetUp();

        participant = createParticipant();
        participantAgentMock = new AgentMock();
        participantAgentMock.mockGetAID(new Aid("participantAgentMock"));
        participant.setAgent(participantAgentMock);
        logFile = new File(directoryFixture, "log.txt");
        appender = new FileAppender(new SimpleLayout(), logFile.getPath());
        participant.logger.addAppender(appender);
        participant.logger.setLevel(Level.INFO);
    }


    @After
    public void tearDown() throws Exception {
        participant.logger.removeAppender(appender);
        appender.close();
        compositeFixture.doTearDown();
    }


    private void assertAuditMessage(JobAudit.Type auditType, AclMessage preAudit) {
        agentFixture.assertMessage(preAudit, JobProtocol.ID, AclMessage.Performative.INFORM);
        assertEquals(CONVERSATION_ID, preAudit.getConversationId());

        JobAudit jobAudit = (JobAudit)preAudit.getContentObject();
        assertEquals(auditType, jobAudit.getType());
        assertEquals(CONVERSATION_ID, jobAudit.getRequestId());
    }


    @SuppressWarnings({"OverlyComplexAnonymousInnerClass"})
    private MessageTemplate matchingAudit(final Type auditType) {
        return matchWith(new MatchExpression() {
            @SuppressWarnings({"RedundantIfStatement"})
            public boolean match(AclMessage aclMessage) {
                if (!JobProtocol.ID.equals(aclMessage.getProtocol())) {
                    return false;
                }
                if (aclMessage.getPerformative() != Performative.INFORM) {
                    return false;
                }
                if (!aclMessage.getConversationId().equals(CONVERSATION_ID)) {
                    return false;
                }
                JobAudit jobAudit = (JobAudit)aclMessage.getContentObject();
                if (jobAudit.getType() != auditType) {
                    return false;
                }
                if (!jobAudit.getRequestId().equals(CONVERSATION_ID)) {
                    return false;
                }
                return true;
            }
        });
    }


    private static JobProtocolParticipant createParticipant() {
        return new JobProtocolParticipant();
    }


    private static JobProtocolParticipant createParticipant(Behaviour behaviour) {
        JobProtocolParticipant jobProtocolParticipant = new JobProtocolParticipant();
        jobProtocolParticipant.setExecuteJobBehaviour(behaviour);
        return jobProtocolParticipant;
    }
}
