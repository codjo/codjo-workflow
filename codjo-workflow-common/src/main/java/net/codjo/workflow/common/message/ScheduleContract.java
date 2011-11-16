package net.codjo.workflow.common.message;
import java.io.Serializable;
/**
 *
 */
public class ScheduleContract implements Serializable {
    private JobRequest request;
    private JobAudit postAudit;
    private ScheduleContract previousContract;


    public ScheduleContract(JobRequest request, JobAudit postAudit) {
        this.request = request;
        this.postAudit = postAudit;
    }


    public JobRequest getRequest() {
        return request;
    }


    public JobAudit getPostAudit() {
        return postAudit;
    }


    public ScheduleContract getPreviousContract() {
        return previousContract;
    }


    public void setPreviousContract(ScheduleContract previousContract) {
        this.previousContract = previousContract;
    }


    @Override
    public String toString() {
        if (previousContract != null) {
            return "ScheduleContract{" + request + ", " + postAudit + ", " + previousContract + '}';
        }
        else {
            return "ScheduleContract{" + request + ", " + postAudit + '}';
        }
    }
}
