/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.JobRequestTemplate;
import junit.framework.TestCase;
/**
 * Classe de test de {@link net.codjo.workflow.common.subscribe.JobEventHandler}.
 */
public class JobEventHandlerTest extends TestCase {
    private LogString logString = new LogString();


    public void test_receiveError() throws Exception {
        JobEventHandlerMock handler = createRootHandler(null);

        ProtocolErrorEvent failure = createFailureEvent();

        boolean errorHandled = handler.receiveError(failure);

        assertTrue(errorHandled);
        logString.assertContent("root.receiveError(FAILURE)");
    }


    public void test_receiveError_default() throws Exception {
        JobEventHandler root = new JobEventHandler() {
        };

        assertFalse(root.receiveError(createFailureEvent()));
    }


    public void test_receiveError_chained() throws Exception {
        JobEventHandler root = new JobEventHandler() {
        };

        root.next(new JobEventHandlerMock(new LogString("first", logString), null));

        assertTrue(root.receiveError(createFailureEvent()));

        logString.assertContent("first.receiveError(FAILURE)");
    }


    public void test_receive_request() throws Exception {
        JobEventHandler importHandler =
              createRootHandler(JobRequestTemplate.matchType("import"));

        JobRequest request = new JobRequest();
        assertFalse(importHandler.receive(new JobEvent(request)));
        logString.assertContent("");

        request.setType("import");
        assertTrue(importHandler.receive(new JobEvent(request)));
        logString.assertContent("root.handleRequest(import)");
    }


    public void test_receive_chained() throws Exception {
        JobEventHandler importHandler =
              createRootHandler(JobRequestTemplate.matchType("import"));

        importHandler.next(new JobEventHandlerMock(new LogString("son", logString)));

        JobRequest importRequest = createRequest("rootRequest", "import");

        importHandler.receive(new JobEvent(importRequest));
        logString.assertContent("root.handleRequest(import)");
        logString.clear();

        JobRequest sonRequest = new JobRequest();
        sonRequest.setParentId("rootRequest");

        importHandler.receive(new JobEvent(sonRequest));
        logString.assertContent("son.handleRequest(null)");
    }


    public void test_receive_chained_multipleSons()
          throws Exception {
        JobEventHandler root = createRootHandler(JobRequestTemplate.matchAll());
        root.next(new JobEventHandlerMock(new LogString("sonA", logString)));

        try {
            root.next(new JobEventHandlerMock(new LogString("sonB", logString)));
            fail();
        }
        catch (UnsupportedOperationException ex) {
            assertEquals("Not Yet Required !", ex.getMessage());
        }
    }


    public void test_receive_chained_notMatched()
          throws Exception {
        JobEventHandler rootEventHandler =
              createRootHandler(JobRequestTemplate.matchType("import"));
        rootEventHandler.next(new JobEventHandlerMock(new LogString("son", logString)));

        JobRequest rootRequest = createRequest("rootRequest", "badType");

        rootEventHandler.receive(new JobEvent(rootRequest));
        logString.assertContent("");
        logString.clear();
    }


    public void test_receive_aLotOfRequest() throws Exception {
        JobEventHandler jobEventHandler = new JobEventHandlerMock(logString);

        JobRequest forgottenRequest = createRequest("my request id", null);
        jobEventHandler.receive(new JobEvent(forgottenRequest));

        for (int i = 0; i < 100; i++) {
            jobEventHandler.receive(new JobEvent(createRequest("id" + i, null)));
        }
        logString.clear();

        JobAudit audit = createAudit(forgottenRequest.getId(), JobAudit.Type.PRE);
        assertFalse(jobEventHandler.receive(new JobEvent(audit)));
        logString.assertContent("");
    }


    public void test_receive_audit() throws Exception {
        JobEventHandler jobEventHandler = new JobEventHandlerMock(logString);

        JobRequest request = createRequest("my request id", null);
        jobEventHandler.receive(new JobEvent(request));
        logString.clear();

        JobAudit audit = createAudit(null, JobAudit.Type.PRE);
        assertFalse(jobEventHandler.receive(new JobEvent(audit)));
        logString.assertContent("");

        audit.setRequestId(request.getId());
        assertTrue(jobEventHandler.receive(new JobEvent(audit)));
        logString.assertContent("handleAudit(PRE)");
    }


    public void test_receive_audit_chained() throws Exception {
        JobEventHandler root = createRootHandler(JobRequestTemplate.matchType("import"));
        root.next(new JobEventHandlerMock(new LogString("son", logString)));

        JobRequest rootRequest = createRequest("rootRequest", "import");
        root.receive(new JobEvent(rootRequest));

        JobRequest sonRequest = createRequest("sonRequest", null);
        sonRequest.setParentId("rootRequest");
        root.receive(new JobEvent(sonRequest));
        logString.clear();

        JobAudit sonAudit = createAudit(sonRequest.getId(), JobAudit.Type.PRE);
        root.receive(new JobEvent(sonAudit));
        logString.assertContent("son.handleAudit(PRE)");
    }


    private JobAudit createAudit(String requestId, JobAudit.Type type) {
        JobAudit sonAudit = new JobAudit(type);
        sonAudit.setRequestId(requestId);
        return sonAudit;
    }


    private JobRequest createRequest(String id, String jobType) {
        JobRequest rootRequest = new JobRequest();
        rootRequest.setId(id);
        rootRequest.setType(jobType);
        return rootRequest;
    }


    private JobEventHandlerMock createRootHandler(JobRequestTemplate template) {
        return new JobEventHandlerMock(new LogString("root", logString), template);
    }


    private ProtocolErrorEvent createFailureEvent() {
        return new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE,
                                      new AclMessage(AclMessage.Performative.FAILURE));
    }
}
