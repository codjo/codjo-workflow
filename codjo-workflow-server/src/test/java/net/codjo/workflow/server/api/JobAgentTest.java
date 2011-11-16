/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.AclMessage.Performative;
import net.codjo.agent.Agent;
import net.codjo.agent.AgentController;
import net.codjo.agent.Aid;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.MessageTemplate;
import net.codjo.agent.MessageTemplate.MatchExpression;
import net.codjo.agent.test.AgentAssert;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.DummyAgent;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocol;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent.MODE;
import java.util.Date;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobAgent}.
 */
public class JobAgentTest extends TestCase {
    private AgentContainerFixture fixture;
    private JobAgent jobAgent;
    private Agent searchAgent;
    private JobProtocolParticipant jobBehaviour;
    private AgentDescription jobAgentDescription;
    private LogString logString;
    private Story story = new Story();


    public void test_register() throws Exception {
        fixture.startNewAgent("import-agent", jobAgent);

        DFService.AgentDescription[] descriptions = searchForService("import");
        assertEquals(1, descriptions.length);
        assertEquals("import-agent", descriptions[0].getAID().getLocalName());
    }


    public void test_register_failure() throws Exception {
        fixture.startNewAgent("import-agent", new JobAgent(jobBehaviour, null));

        fixture.waitForAgentDeath("import-agent");

        DFService.AgentDescription[] descriptions = searchForService("import");
        assertEquals(0, descriptions.length);
    }


    public void test_deregister() throws Exception {
        AgentController controller = fixture.startNewAgent("import-agent", jobAgent);

        controller.kill();
        fixture.waitForAgentDeath("import-agent");

        DFService.AgentDescription[] descriptions = searchForService("import");
        assertEquals(0, descriptions.length);
    }


    public void test_receiveRequest() throws Exception {
        DummyAgent initiatorAgent = new DummyAgent();
        fixture.startNewAgent("initiatior-agent", initiatorAgent);
        fixture.startNewAgent("import-agent", jobAgent);

        AclMessage requestMessage = new AclMessage(AclMessage.Performative.REQUEST);
        requestMessage.setConversationId("conversation-id");
        requestMessage.setProtocol(JobProtocol.ID);
        requestMessage.addReceiver(new Aid("import-agent"));

        JobRequest request = new JobRequest("import");
        request.setId(requestMessage.getConversationId());
        requestMessage.setContentObject(request);

        fixture.sendMessage(initiatorAgent, requestMessage);

        AclMessage message = fixture.receiveMessage(initiatorAgent);
        fixture.assertMessage(message, JobProtocol.ID, AclMessage.Performative.INFORM);
    }


    public void test_receiveRequest_delegateMode() throws Exception {
        story.record()
              .startAgent("import-agent", new JobAgent(jobBehaviour, jobAgentDescription, MODE.DELEGATE));

        AclMessage requestMessage = new AclMessage(AclMessage.Performative.REQUEST);
        requestMessage.setConversationId("conversation-id");
        requestMessage.setProtocol(JobProtocol.ID);
        requestMessage.addReceiver(new Aid("import-agent"));

        JobRequest request = new JobRequest("import");
        request.setDate(new Date(0));
        request.setId(requestMessage.getConversationId());
        requestMessage.setContentObject(request);

        story.record().startTester("initiatior-agent")
              .sendMessage(requestMessage)
              .then()
              .receiveMessage(MessageTemplate.and(
                    MessageTemplate.matchPerformative(Performative.INFORM),
                    MessageTemplate.matchWith(new MatchExpression() {
                        public boolean match(AclMessage aclMessage) {
                            JobAudit audit
                                  = (JobAudit)aclMessage.getContentObject();
                            return audit.getType().equals(JobAudit.Type.PRE);
                        }
                    })))
              .then()
              .receiveMessage(MessageTemplate.and(
                    MessageTemplate.matchPerformative(Performative.INFORM),
                    MessageTemplate.matchWith(new MatchExpression() {
                        public boolean match(AclMessage aclMessage) {
                            JobAudit audit
                                  = (JobAudit)aclMessage.getContentObject();
                            return audit.getType().equals(JobAudit.Type.POST);
                        }
                    })));

        story.record()
              .addAssert(AgentAssert.log(logString,
                                         "executeJob(JobRequest{initiatorLogin='null', type='import', date=Thu Jan 01 01:00:00 CET 1970, arguments=null, id='conversation-id', parentId='null', loggable='true'})"));

        story.execute();
    }


    @Override
    protected void setUp() throws Exception {
        logString = new LogString();

        jobBehaviour =
              new JobProtocolParticipant() {
                  @Override
                  protected void executeJob(JobRequest request) {
                      logString.call("executeJob", request);
                  }
              };

        jobAgentDescription = new AgentDescription();
        jobAgentDescription.addService(new DFService.ServiceDescription("import",
                                                                        "standard-import"));

        jobAgent = new JobAgent(jobBehaviour, jobAgentDescription);
        searchAgent = new DummyAgent();

        story.doSetUp();
        fixture = story.getAgentContainerFixture();
        startSearchAgent();
    }


    @Override
    protected void tearDown() throws Exception {
        story.doTearDown();
    }


    private DFService.AgentDescription[] searchForService(String type)
          throws DFService.DFServiceException {
        DFService.AgentDescription description =
              new DFService.AgentDescription(new DFService.ServiceDescription(type));
        return DFService.search(searchAgent, description);
    }


    private void startSearchAgent() throws Exception {
        try {
            fixture.startNewAgent("searcher", searchAgent);
        }
        catch (ContainerFailureException e) {
            fixture.doTearDown();
        }
    }
}
