/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.workflow.common.message.JobAudit;
import java.io.PrintWriter;
import java.io.StringWriter;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobBatchException}.
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class JobBatchExceptionTest extends TestCase {
    public void test_constructor() throws Exception {
        JobAudit audit = new JobAudit();
        audit.setErrorMessage("boum");
        JobBatchException jobException = new JobBatchException(audit);

        assertEquals("boum", jobException.getMessage());
        assertSame(audit, jobException.getAudit());
    }


    public void test_constructor_withCause() throws Exception {
        JobAudit audit = new JobAudit();
        audit.setError(new JobAudit.Anomaly("boum", new Throwable("boum Boum")));
        JobBatchException jobException = new JobBatchException(audit);

        assertEquals("boum", jobException.getMessage());
        assertTrue(toString(jobException).contains(audit.getError().getDescription()));
    }


    public void test_printStackTrace_withoutDescription() throws Exception {
        JobAudit audit = new JobAudit();
        audit.setError(new JobAudit.Anomaly("boum"));
        JobBatchException jobException = new JobBatchException(audit);

        assertFalse("Affiche [Caused by] que lorsque l'audit a une description",
                    toString(jobException).contains("[Caused by]"));
    }


    private String toString(Throwable exception) {
        StringWriter result = new StringWriter();
        exception.printStackTrace(new PrintWriter(result));
        return result.toString();
    }
}
