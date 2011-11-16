package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;

public interface JobFactory {

    Job createJob(JobRequest request, UserId userId) throws JobBuilderException;
}
