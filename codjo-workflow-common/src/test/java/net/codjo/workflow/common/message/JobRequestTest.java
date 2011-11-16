/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.util.Date;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobRequest}.
 */
public class JobRequestTest extends TestCase {
    private JobRequest request;


    public void test_serializable() throws Exception {
        request.setInitiatorLogin("gonnot");
        request.setType("import");
        request.setArguments(new Arguments());
        request.setId("id");
        request.setParentId("parentId");

        byte[] serializedRequest = TestUtil.toByteArray(request);

        JobRequest deserializedRequest = (JobRequest)TestUtil.toObject(serializedRequest);
        assertNotNull(deserializedRequest);
    }


    public void test_initializeFromPreviousRequest() throws Exception {
        request.setInitiatorLogin("gonnot");
        request.setType("import");
        request.setArguments(new Arguments());
        request.setId("id");
        request.setParentId("parentId");

        JobRequest nextRequest = new JobRequest("next-request-type");
        nextRequest.setArguments(new Arguments("next-argument", "value"));

        nextRequest.initializeFromPreviousRequest(request);

        assertEquals(request.getInitiatorLogin(), nextRequest.getInitiatorLogin());
        assertEquals(request.getId(), nextRequest.getParentId());
        assertEquals("next-request-type", nextRequest.getType());
        assertEquals("value", nextRequest.getArguments().get("next-argument"));
    }


    public void test_constructeur() throws Exception {
        request = new JobRequest("import");
        assertEquals("import", request.getType());
        assertNull(request.getArguments());
        assertTrue(request.isLoggable());

        request = new JobRequest("import", new Arguments());
        assertEquals("import", request.getType());
        assertNotNull(request.getArguments());
        assertTrue(request.isLoggable());
    }


    public void test_content() throws Exception {
        request.setInitiatorLogin("gonnot");
        assertEquals("gonnot", request.getInitiatorLogin());

        request.setType("import");
        assertEquals("import", request.getType());

        request.setId("id");
        assertEquals("id", request.getId());

        request.setParentId("parentId");
        assertEquals("parentId", request.getParentId());

        Arguments jobArguments = new Arguments();
        request.setArguments(jobArguments);
        assertSame(jobArguments, request.getArguments());
    }


    public void test_getDate() throws Exception {
        Date date = new Date();
        request.setDate(date);
        assertSame(date, request.getDate());
    }


    public void test_getDate_default() throws Exception {
        long now = System.currentTimeMillis();
        long requestTime = request.getDate().getTime();

        assertTrue(now - 1000 < requestTime);
        assertTrue(requestTime < now + 1000);
    }


    @Override
    protected void setUp() throws Exception {
        request = new JobRequest();
    }
}
