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
public class AssertInitiator implements SubStep {
    private final String initiator;


    public AssertInitiator(String initiator) {
        this.initiator = initiator;
    }


    public void run(Agent agent, AclMessage message)
          throws AssertionFailedError {
        JobRequest request = (JobRequest)message.getContentObject();
        Assert.assertEquals(initiator, request.getInitiatorLogin());
    }
}
