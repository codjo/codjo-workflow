/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.leader;
import net.codjo.agent.AclMessage;
import net.codjo.agent.protocol.SubscribeParticipant;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import junit.framework.TestCase;
/**
 * Classe de test de {@link net.codjo.workflow.server.leader.JobLeaderSubscribeHandler}.
 */
public class JobLeaderSubscribeHandlerTest extends TestCase {
    private JobLeaderSubscribeHandler handlerLeader;
    private LogString log = new LogString();


    public void test_handleSubscribe_sendRequestNotification()
          throws Exception {
        handlerLeader.handleSubscribe(new SubscriptionMock(
              new LogString("agent-subscription", log)));

        handlerLeader.sendNotification(createJobRequest("import"));

        log.assertContent("agent-subscription.reply(aclMessage(INFORM, request[import]))");
    }


    public void test_handleSubscribe_sendAuditNotification()
          throws Exception {
        handlerLeader.handleSubscribe(new SubscriptionMock(
              new LogString("agent-subscription", log)));

        handlerLeader.sendNotification(createJobAudit(JobAudit.Type.PRE));

        log.assertContent("agent-subscription.reply(aclMessage(INFORM, audit[PRE]))");
    }


    public void test_handleCancel_sendNotification()
          throws Exception {
        SubscriptionMock subscription =
              new SubscriptionMock(new LogString("agent-subscription", log));
        handlerLeader.handleSubscribe(subscription);
        handlerLeader.handleCancel(subscription);

        handlerLeader.sendNotification(createJobRequest("import"));

        log.assertContent("");
    }


    public void test_removeSubscription() throws Exception {
        SubscriptionMock subscription =
              new SubscriptionMock(new LogString("agent-subscription", log));
        subscription.mockConversationId("my-conversation-id");
        handlerLeader.handleSubscribe(subscription);

        handlerLeader.removeSubscription("my-conversation-id");
        log.assertContent("agent-subscription.close()");
        log.clear();

        handlerLeader.sendNotification(createJobRequest("import"));
        log.assertContent("");
    }


    private JobEvent createJobRequest(String jobType) {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setType(jobType);
        return new JobEvent(jobRequest);
    }


    private JobEvent createJobAudit(JobAudit.Type auditType) {
        return new JobEvent(new JobAudit(auditType));
    }


    @Override
    protected void setUp() throws Exception {
        handlerLeader = new JobLeaderSubscribeHandler();
    }


    public static class SubscriptionMock implements SubscribeParticipant.Subscription {
        private LogString log;
        private AclMessage subscribeMessage = new AclMessage(AclMessage.Performative.SUBSCRIBE);


        public SubscriptionMock(LogString log) {
            this.log = log;
        }


        public AclMessage getMessage() {
            return subscribeMessage;
        }


        public void reply(AclMessage messageToSend) {
            log.call("reply", toString(messageToSend));
        }


        public void close() {
            log.call("close");
        }


        private String toString(AclMessage messageToSend) {
            StringBuilder buffer = new StringBuilder("aclMessage(");

            JobEvent event = (JobEvent)messageToSend.getContentObject();
            String performative = AclMessage.performativeToString(messageToSend.getPerformative());

            buffer.append(performative).append(", ");
            if (event.isRequest()) {
                buffer.append("request[").append(event.getRequest().getType()).append("]");
            }
            else {
                buffer.append("audit[").append(event.getAudit().getType()).append("]");
            }

            return buffer.append(")").toString();
        }


        public void mockConversationId(String conversationId) {
            subscribeMessage.setConversationId(conversationId);
        }
    }
}
