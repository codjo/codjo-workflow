/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobAudit}.
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class JobAuditTest extends TestCase {
    private JobAudit audit;


    public void test_serializable() throws Exception {
        audit.setType(JobAudit.Type.POST);
        audit.setError(new JobAudit.Anomaly("msg", new Exception()));
        audit.setWarning(new JobAudit.Anomaly("msgbis", new Exception()));
        audit.setArguments(new Arguments());

        byte[] serializedAudit = TestUtil.toByteArray(audit);

        JobAudit deserializedAudit = (JobAudit)TestUtil.toObject(serializedAudit);
        assertNotNull(deserializedAudit);
    }


    public void test_serializable_bug() throws Exception {
        audit.setType(JobAudit.Type.POST);

        JobAudit deserializedAudit =
              (JobAudit)TestUtil.toObject(TestUtil.toByteArray(audit));

        assertEquals(JobAudit.Type.POST, deserializedAudit.getType());
    }


    public void test_constructor() throws Exception {
        audit = new JobAudit(JobAudit.Type.PRE);

        assertEquals(JobAudit.Type.PRE, audit.getType());
    }


    public void test_content() throws Exception {
        audit.setType(JobAudit.Type.PRE);
        assertEquals(JobAudit.Type.PRE, audit.getType());

        audit.setType(JobAudit.Type.MID);
        assertEquals(JobAudit.Type.MID, audit.getType());

        audit.setType(JobAudit.Type.POST);
        assertEquals(JobAudit.Type.POST, audit.getType());

        Arguments arguments = new Arguments();
        audit.setArguments(arguments);
        assertSame(arguments, audit.getArguments());

        assertFalse(audit.hasError());
    }


    public void test_setError() {
        audit.setErrorMessage("oups !!!");
        assertTrue(audit.hasError());
        assertEquals("oups !!!", audit.getErrorMessage());
        assertNull(audit.getError().getDescription());

        Throwable exception = new Exception("exception log");
        audit.setError(new JobAudit.Anomaly("msg", exception));
        assertTrue(audit.hasError());
        assertEquals("msg", audit.getErrorMessage());
        assertEquals(toString(exception), audit.getError().getDescription());

        audit.setError(new JobAudit.Anomaly(exception));
        assertTrue(audit.hasError());
        assertEquals(exception.getMessage(), audit.getErrorMessage());
        assertEquals(toString(exception), audit.getError().getDescription());

        exception = new NullPointerException();
        audit.setError(new JobAudit.Anomaly(exception));
        assertTrue(audit.hasError());
        assertEquals("Erreur technique (java.lang.NullPointerException)",
                     audit.getErrorMessage());
        assertEquals(toString(exception), audit.getError().getDescription());
    }


    public void test_setErrorWithDescription() {
        audit.setError(new JobAudit.Anomaly("oups !!!", "desc"));
        assertTrue(audit.hasError());
        assertEquals("oups !!!", audit.getErrorMessage());
        assertEquals("desc", audit.getError().getDescription());
    }


    public void test_setWarning() {
        JobAudit.Anomaly anomaly = new JobAudit.Anomaly("msg");

        audit.setWarning(anomaly);
        assertEquals(JobAudit.Status.WARNING, audit.getStatus());
        assertEquals("msg", audit.getWarningMessage());
        assertSame(anomaly, audit.getWarning());
    }


    public void test_setWarningMessage() {
        assertNull(audit.getWarningMessage());
        audit.setWarningMessage("msg");
        assertEquals(JobAudit.Status.WARNING, audit.getStatus());
        assertEquals("msg", audit.getWarningMessage());
    }


    public void test_warningAndError() {
        audit.setError(new JobAudit.Anomaly("msg"));
        audit.setWarning(new JobAudit.Anomaly("msg"));

        assertEquals(JobAudit.Status.ERROR, audit.getStatus());
    }


    public void test_status() {
        assertEquals(JobAudit.Status.OK, audit.getStatus());

        audit.setErrorMessage("oups !!!");
        assertEquals(JobAudit.Status.ERROR, audit.getStatus());
    }


    private String toString(Throwable exception) {
        StringWriter result = new StringWriter();
        exception.printStackTrace(new PrintWriter(result));
        return result.toString();
    }


    public void test_getDate() throws Exception {
        Date date = new Date();
        audit.setDate(date);
        assertSame(date, audit.getDate());
    }


    public void test_getDate_default() throws Exception {
        long now = System.currentTimeMillis();
        long auditTime = audit.getDate().getTime();

        assertTrue(now - 1000 < auditTime);
        assertTrue(auditTime < now + 1000);
    }


    public void test_getErrorMessage() throws Exception {
        assertNull(audit.getErrorMessage());
    }


    @Override
    protected void setUp() throws Exception {
        audit = new JobAudit();
    }
}
