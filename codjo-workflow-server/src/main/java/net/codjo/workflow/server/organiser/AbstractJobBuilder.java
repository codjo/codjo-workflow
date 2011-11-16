package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;

public abstract class AbstractJobBuilder extends JobBuilder {

    public abstract boolean accept(JobRequest jobRequest);


    public abstract Job createJob(JobRequest jobRequest, Job job, UserId userId);


    @Override
    public final Job createJob(JobRequest jobRequest, UserId userId) {
        Job job = super.createJob(jobRequest, userId);
        if (accept(jobRequest)) {
            return createJob(jobRequest, job, userId);
        }
        return job;
    }
}
