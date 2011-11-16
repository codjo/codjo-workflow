package net.codjo.workflow.server.handler;
import net.codjo.workflow.common.organiser.Job;
/**
 *
 */
public class HandlerJob extends Job {
    private String[] handlerIds;


    public HandlerJob(Job job) {
        super(job);
    }


    public String[] getHandlerIds() {
        return handlerIds;
    }


    public void setHandlerIds(String[] handlerIds) {
        this.handlerIds = handlerIds;
    }
}
