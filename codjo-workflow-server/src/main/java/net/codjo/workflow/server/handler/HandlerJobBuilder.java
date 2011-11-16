package net.codjo.workflow.server.handler;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.HandlerJobRequest;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.server.organiser.AbstractJobBuilder;
/**
 *
 */
public class HandlerJobBuilder extends AbstractJobBuilder {

    @Override
    public boolean accept(JobRequest jobRequest) {
        return HandlerJobRequest.HANDLER_JOB_TYPE.equals(jobRequest.getType());
    }


    @Override
    public Job createJob(JobRequest jobRequest, Job job, UserId userId) {
        HandlerJob handlerJob = new HandlerJob(job);
        handlerJob.setHandlerIds(new HandlerJobRequest(jobRequest).getHandlerIds());
        return handlerJob;
    }
}
