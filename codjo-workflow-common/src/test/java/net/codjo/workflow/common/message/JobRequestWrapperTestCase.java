/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.util.Date;
import junit.framework.TestCase;
/**
 *
 */
public abstract class JobRequestWrapperTestCase extends TestCase {
    public void test_proxyMethods() {
        JobRequest jobRequest = new JobRequest("", new Arguments());
        jobRequest.setInitiatorLogin("initiator");
        jobRequest.setDate(new Date());
        JobRequestWrapper request = createWrapper(jobRequest);

        assertEquals(jobRequest.getInitiatorLogin(), request.getInitiatorLogin());
        assertEquals(jobRequest.getDate(), request.getDate());

        request.setDate(null);
        assertEquals(null, request.getDate());

        request.setInitiatorLogin(null);
        assertEquals(null, request.getInitiatorLogin());

        request.setId("id");
        assertEquals("id", request.getId());

        assertEquals(getJobRequestType(), request.getType());
        assertNotNull(request.getArguments());
    }


    public void test_toRequest() {
        JobRequest jobRequest = new JobRequest();
        JobRequestWrapper request = createWrapper(jobRequest);

        assertSame(jobRequest, request.toRequest());
        assertEquals(getJobRequestType(), jobRequest.getType());
    }


    public void test_setArgument() {
        JobRequestWrapper request = createWrapper(new JobRequest());
        request.setArgument("key", "value");
        assertSame("value", request.getArgument("key"));
    }


    protected abstract String getJobRequestType();


    protected abstract JobRequestWrapper createWrapper(JobRequest jobRequest);
}
