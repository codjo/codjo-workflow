package net.codjo.workflow.common.message;
/**
 *
 */
public class HandlerJobRequestTest extends JobRequestWrapperTestCase {

    @Override
    protected String getJobRequestType() {
        return "handler";
    }


    @Override
    protected JobRequestWrapper createWrapper(JobRequest jobRequest) {
        return new HandlerJobRequest(jobRequest);
    }


    public void test_handlerJobRequestIsNotLoggable() throws Exception {
        JobRequest jobRequest = new JobRequest("handler");
        assertTrue(jobRequest.isLoggable());
        HandlerJobRequest handlerJobRequest = new HandlerJobRequest(jobRequest);
        assertFalse(jobRequest.isLoggable());
        assertNotNull(handlerJobRequest);
    }


    public void test_noHandlerIds() throws Exception {
        HandlerJobRequest handlerJobRequest = new HandlerJobRequest(new JobRequest("handler"));

        assertEquals(0, handlerJobRequest.getHandlerIds().length);
        assertEquals("[]", handlerJobRequest.getArgument("handler-ids"));
    }


    public void test_handlerIds() throws Exception {
        HandlerJobRequest handlerJobRequest = new HandlerJobRequest(new JobRequest("handler"));

        handlerJobRequest.setHandlerIds("id1", "id2");

        assertEquals("[id1, id2]", handlerJobRequest.getArgument("handler-ids"));
        assertEquals(2, handlerJobRequest.getHandlerIds().length);
        assertEquals("id1", handlerJobRequest.getHandlerIds()[0]);
        assertEquals("id2", handlerJobRequest.getHandlerIds()[1]);
    }
}
