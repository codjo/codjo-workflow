package net.codjo.workflow.server.plugin;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.server.aspect.ExecuteAspectRequest;
/**
 *
 */
class ExecuteAspectStringifier extends StringifierImpl {

    ExecuteAspectStringifier() {
        super(ExecuteAspectRequest.JOB_ID);
    }


    public String toString(JobRequest jobRequest) {
        return new ExecuteAspectRequest(jobRequest).getAspectBranchId().getAspectId();
    }
}
