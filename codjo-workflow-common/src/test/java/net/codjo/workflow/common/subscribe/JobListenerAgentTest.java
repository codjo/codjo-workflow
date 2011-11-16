/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.Behaviour;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.DFServiceException;
import net.codjo.agent.test.AgentContainerFixture;
import net.codjo.agent.test.DummyAgent;
import net.codjo.agent.test.Semaphore;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.Service;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import junit.framework.Assert;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobListenerAgent}.
 */
public class JobListenerAgentTest extends TestCase {
    private AgentContainerFixture fixture = new AgentContainerFixture();
    private LogString log = new LogString();
    private JobListenerAgent listenerAgent;
    private LogMessageBehaviour jobLeaderMockBehaviour;
    private Agent jobLeaderAgent;
    private JobEventHandlerMock eventHandlerMock;


    public void test_createNickName() throws Exception {
        assertEquals("listener-" + Integer.toHexString(System.identityHashCode(listenerAgent)),
                     listenerAgent.createNickName());
    }


    public void test_subscribe_cancel() throws Exception {
        startJobOrganizerMock();

        fixture.startNewAgent("job-listener", listenerAgent);

        jobLeaderMockBehaviour.waitReceivedMessage();
        log.assertContent("job-leader.receive(SUBSCRIBE, job-listener, fipa-subscribe)");
        log.clear();

        listenerAgent.die();

        jobLeaderMockBehaviour.waitReceivedMessage();
        log.assertContent("job-leader.receive(CANCEL, job-listener, fipa-subscribe)");
    }


    public void test_receiveEvent() throws Exception {
        startJobOrganizerMock();
        startListenerAgent();

        AclMessage inform =
              jobLeaderMockBehaviour.lastReceivedMessage.createReply(AclMessage.Performative.INFORM);
        inform.setContentObject(new JobEvent(createRequest("Import")));
        fixture.sendMessage(jobLeaderAgent, inform);

        eventHandlerMock.waitForEvent();

        log.assertContent("handler.handleRequest(Import)");
        log.clear();

        inform.setContentObject(new JobEvent(new JobAudit(JobAudit.Type.PRE)));
        fixture.sendMessage(jobLeaderAgent, inform);

        eventHandlerMock.waitForEvent();

        log.assertContent("handler.handleAudit(PRE)");
    }


    private JobRequest createRequest(String type) {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setType(type);
        return jobRequest;
    }


    @Override
    protected void setUp() throws Exception {
        eventHandlerMock = new JobEventHandlerMock(new LogString("handler", log));
        listenerAgent = new JobListenerAgent(eventHandlerMock);
        fixture.doSetUp();
    }


    @Override
    protected void tearDown() throws Exception {
        fixture.doTearDown();
    }


    private void startJobOrganizerMock() throws ContainerFailureException, DFServiceException {
        jobLeaderAgent = new DummyAgent();
        fixture.startNewAgent("job-leader-agent-bidon", jobLeaderAgent);
        DFService.register(jobLeaderAgent, DFService.createAgentDescription(Service.JOB_LEADER_SERVICE));

        jobLeaderMockBehaviour = new LogMessageBehaviour(new LogString("job-leader", log));
        jobLeaderAgent.addBehaviour(jobLeaderMockBehaviour);
    }


    private void startListenerAgent() throws ContainerFailureException {
        fixture.startNewAgent("job-listener", listenerAgent);

        jobLeaderMockBehaviour.waitSubscribeMessage();
        log.clear();
    }


    private static class LogMessageBehaviour extends Behaviour {
        private LogString log;
        private final Semaphore semaphore = new Semaphore();
        private AclMessage lastReceivedMessage;


        LogMessageBehaviour(LogString log) {
            this.log = log;
        }


        @Override
        protected void action() {
            AclMessage message = getAgent().receive();
            if (message == null) {
                block();
                return;
            }
            log.call("receive",
                     AclMessage.performativeToString(message.getPerformative()),
                     message.getSender().getLocalName(),
                     message.getProtocol());
            lastReceivedMessage = message;
            semaphore.release();
        }


        @Override
        public boolean done() {
            return false;
        }


        public void waitReceivedMessage() {
            semaphore.acquire();
        }


        public void waitSubscribeMessage() {
            semaphore.acquire();
            Assert.assertEquals(AclMessage.Performative.SUBSCRIBE,
                                lastReceivedMessage.getPerformative());
        }
    }

    private static class JobEventHandlerMock extends JobEventHandler {
        private LogString log;
        private final Semaphore semaphore = new Semaphore();


        JobEventHandlerMock(LogString log) {
            this.log = log;
        }


        @Override
        public boolean receiveError(ProtocolErrorEvent event) {
            log.call("receiveError", event);
            semaphore.release();
            return true;
        }


        @Override
        protected void handleRequest(JobRequest request) {
            log.call("handleRequest", request.getType());
            semaphore.release();
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            log.call("handleAudit", audit.getType());
            semaphore.release();
        }


        public void waitForEvent() {
            semaphore.acquire();
        }
    }
}
