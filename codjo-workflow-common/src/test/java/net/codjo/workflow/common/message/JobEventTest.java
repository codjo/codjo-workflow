/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobEvent}.
 */
public class JobEventTest extends TestCase {
    public void test_isAudit() throws Exception {
        JobAudit audit = new JobAudit();
        JobEvent jobEvent = new JobEvent(audit);

        assertTrue(jobEvent.isAudit());
        assertFalse(jobEvent.isRequest());

        assertSame(audit, jobEvent.getAudit());
        assertNull(jobEvent.getRequest());
    }


    public void test_isRequest() throws Exception {
        JobRequest request = new JobRequest();
        JobEvent jobEvent = new JobEvent(request);

        assertFalse(jobEvent.isAudit());
        assertTrue(jobEvent.isRequest());

        assertNull(jobEvent.getAudit());
        assertSame(request, jobEvent.getRequest());
    }


    public void test_serializable() throws Exception {
        JobEvent jobEvent = new JobEvent(new JobRequest());

        byte[] serializedEvent = TestUtil.toByteArray(jobEvent);

        JobEvent deserializedEvent = (JobEvent)TestUtil.toObject(serializedEvent);
        assertNotNull(deserializedEvent);
    }
}
