package net.codjo.workflow.common.message;
import java.io.Serializable;
/**
 *
 */
public class JobContract implements Serializable {
    private JobRequest request;


    public JobContract(JobRequest request) {
        this.request = request;
    }


    public JobRequest getRequest() {
        return request;
    }


    @Override
    public String toString() {
        return "JobContract{" + request + '}';
    }
}
