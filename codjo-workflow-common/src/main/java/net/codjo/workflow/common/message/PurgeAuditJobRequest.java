package net.codjo.workflow.common.message;
import java.util.Date;
/**
 *
 */
public class PurgeAuditJobRequest extends JobRequestWrapper {
    public static final String PURGE_AUDIT_JOB_TYPE = "purge-audit";
    public static final String PERIOD = "period";


    public PurgeAuditJobRequest(JobRequest request) {
        super(PURGE_AUDIT_JOB_TYPE, request);
    }


    public Date getBeforeDate() {
        return DateUtil.computeSqlDateFromPeriod(getPeriod());
    }


    public String getPeriod() {
        return getArguments().get(PERIOD);
    }
}
