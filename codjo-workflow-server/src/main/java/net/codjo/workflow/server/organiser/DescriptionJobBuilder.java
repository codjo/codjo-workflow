package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.server.audit.DiscriminentStringifier;
/**
 *
 */
public class DescriptionJobBuilder extends AbstractJobBuilder {
    private final DiscriminentStringifier discriminentStringifier;


    public DescriptionJobBuilder(DiscriminentStringifier discriminentStringifier) {
        this.discriminentStringifier = discriminentStringifier;
    }


    @Override
    public boolean accept(JobRequest jobRequest) {
        return true;
    }


    @Override
    public Job createJob(JobRequest jobRequest, Job job, UserId userId) {
        job.setDescription(discriminentStringifier.getDiscriminent(jobRequest));
        return job;
    }
}
