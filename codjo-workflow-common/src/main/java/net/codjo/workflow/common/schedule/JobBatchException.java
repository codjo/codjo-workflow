/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.plugin.batch.BatchException;
import net.codjo.workflow.common.message.JobAudit;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
/**
 *
 */
public class JobBatchException extends BatchException {
    private final JobAudit audit;


    public JobBatchException(JobAudit audit) {
        super(audit.getErrorMessage());
        this.audit = audit;
    }


    public JobAudit getAudit() {
        return audit;
    }


    @Override
    public void printStackTrace(PrintStream stream) {
        printStackTrace(new PrintWriter(new OutputStreamWriter(stream)));
    }


    @Override
    public void printStackTrace(PrintWriter writer) {
        super.printStackTrace(writer);
        if (audit.getError().getDescription() != null) {
            writer.println("[Caused by]");
            writer.println(audit.getError().getDescription());
        }
    }
}
