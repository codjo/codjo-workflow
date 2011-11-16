package net.codjo.workflow.server.plugin;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 *
 */
class PurgeAuditStringifier extends StringifierImpl {
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");


    PurgeAuditStringifier() {
        super(PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE);
    }


    public String toString(JobRequest jobRequest) {
        PurgeAuditJobRequest purgeAuditJobRequest = new PurgeAuditJobRequest(jobRequest);
        Date beforeDate = purgeAuditJobRequest.getBeforeDate();
        return new StringBuilder("Log < ").append(DATE_FORMAT.format(beforeDate)).toString();
    }
}
