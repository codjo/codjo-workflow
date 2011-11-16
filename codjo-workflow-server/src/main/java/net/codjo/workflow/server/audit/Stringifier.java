package net.codjo.workflow.server.audit;
import net.codjo.workflow.common.message.JobRequest;

public interface Stringifier {
    String toString(JobRequest jobRequest);
}
