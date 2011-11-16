package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.organiser.Job;
import java.util.Date;

public class JobBuilder {
    int priority;
    private JobBuilder nextBuilder;


    public Job createJob(JobRequest jobRequest, UserId userId) {
        if (nextBuilder == null) {
            Job job = new Job(jobRequest.getId(), jobRequest.getType());
            if (jobRequest.getArguments() != null) {
                job.getArgs().putAll(jobRequest.getArguments().toMap());
            }
            job.setInitiator(userId.getLogin());
            job.setUserId(userId.encode());
            job.setDate(new Date());
            return job;
        }
        return nextBuilder.createJob(jobRequest, userId);
    }


    void setPriority(int priority) {
        this.priority = priority;
    }


    void setNextHandler(JobBuilder nextBuilder) {
        this.nextBuilder = nextBuilder;
    }


    JobBuilder add(JobBuilder newBuilder) {
        if (priority <= newBuilder.priority) {
            newBuilder.setNextHandler(this);
            return newBuilder;
        }
        if (nextBuilder == null) {
            nextBuilder = newBuilder;
        }
        else {
            nextBuilder = nextBuilder.add(newBuilder);
        }
        return this;
    }
}
