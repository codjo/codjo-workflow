/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.util;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import javax.swing.SwingUtilities;
import junit.framework.TestCase;
/**
 * Classe de test de {@link net.codjo.workflow.gui.util.SwingWrapper}.
 */
public class SwingWrapperTest extends TestCase {
    private LogString log = new LogString();


    public void test_wrapp_receiveError() throws Exception {
        JobEventHandler handler = new JobEventHandlerMock();

        JobEventHandler wrapper = SwingWrapper.wrapp(handler);
        assertNotNull(wrapper);

        boolean result =
              wrapper.receiveError(new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE,
                                                          null));
        log.assertContent("receiveError(FAILURE, isAwtThread=true)");
        assertTrue(result);
    }


    public void test_wrapp_receiveJobEvent() throws Exception {
        JobEventHandler handler = new JobEventHandlerMock();

        JobEventHandler wrapper = SwingWrapper.wrapp(handler);
        assertNotNull(wrapper);

        boolean result = wrapper.receive(new JobEvent(createRequest()));
        log.assertContent("handleRequest(import, isAwtThread=true)");
        assertTrue(result);

        log.clear();

        result = wrapper.receive(new JobEvent(createAudit()));
        log.assertContent("handleAudit(PRE, isAwtThread=true)");
        assertTrue(result);
    }


    private JobRequest createRequest() {
        JobRequest request = new JobRequest("import");
        request.setId("id");
        return request;
    }


    private JobAudit createAudit() {
        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("id");
        return audit;
    }


    private class JobEventHandlerMock extends JobEventHandler {
        @Override
        public boolean receiveError(ProtocolErrorEvent event) {
            log.call("receiveError", event.getType(), isAwtThread());
            return true;
        }


        @Override
        protected void handleRequest(JobRequest request) {
            log.call("handleRequest", request.getType(), isAwtThread());
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            log.call("handleAudit", audit.getType(), isAwtThread());
        }


        private String isAwtThread() {
            return "isAwtThread=" + SwingUtilities.isEventDispatchThread();
        }
    }
}
