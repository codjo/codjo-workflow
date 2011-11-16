package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;

public class JobBuilderMock extends JobBuilder {
    private String requestType;
    private final Job broadcastJob;
    private UserId userId;


    public JobBuilderMock(String requestType, Job broadcastJob, UserId userId) {
        this.requestType = requestType;
        this.broadcastJob = broadcastJob;
        this.userId = userId;
    }


    @Override
    public Job createJob(JobRequest jobRequest, UserId someUserId) {
        if (requestType.equals(jobRequest.getType()) && this.userId == someUserId) {
            return broadcastJob;
        }
        return super.createJob(jobRequest, someUserId);
    }
}
