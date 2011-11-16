package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;

public class DefaultJobFactory implements JobFactory {
    private JobBuilder head;


    public DefaultJobFactory() {
        head = new JobBuilder();
        head.setPriority(Integer.MIN_VALUE);
    }


    public void register(JobBuilder newBuilder, int priority) {
        newBuilder.setPriority(priority);
        head = head.add(newBuilder);
    }


    public Job createJob(JobRequest request, UserId userId) throws JobBuilderException {
        try {
            return head.createJob(request, userId);
        }
        catch (Exception e) {
            throw new JobBuilderException(e);
        }
    }
}
