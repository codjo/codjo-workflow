package net.codjo.workflow.common.util;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.test.SubStep;
import net.codjo.workflow.common.message.JobRequest;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
/**
 *
 */
public class AssertRequestType implements SubStep {
    private final String requestType;


    public AssertRequestType(String requestType) {
        this.requestType = requestType;
    }


    public void run(Agent agent, AclMessage message)
          throws AssertionFailedError {
        JobRequest request = (JobRequest)message.getContentObject();
        Assert.assertEquals(requestType, request.getType());
    }
}
