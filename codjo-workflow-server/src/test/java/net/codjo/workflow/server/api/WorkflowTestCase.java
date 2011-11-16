package net.codjo.workflow.server.api;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Aid;
import net.codjo.agent.MessageTemplate;
import static net.codjo.agent.MessageTemplate.and;
import static net.codjo.agent.MessageTemplate.matchPerformative;
import net.codjo.agent.UserId;
import net.codjo.agent.test.AssertMatchExpression;
import net.codjo.agent.test.Story;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocol;
import junit.framework.TestCase;

public abstract class WorkflowTestCase extends TestCase {
    protected Story story;
    protected UserId userId = UserId.createId("user_dev", "secret");
    protected LogString log = new LogString();


    @Override
    protected void setUp() throws Exception {
        story = new Story();
        story.doSetUp();
    }


    @Override
    protected void tearDown() throws Exception {
        story.doTearDown();
    }


    protected AclMessage createJobRequestMessage(JobRequest jobRequest, Aid receiver) {
        AclMessage requestMessage = new AclMessage(AclMessage.Performative.REQUEST);
        requestMessage.setConversationId("conversation-id-" + System.identityHashCode(requestMessage));
        requestMessage.setProtocol(JobProtocol.ID);
        requestMessage.addReceiver(receiver);
        requestMessage.setContentObject(jobRequest);

        requestMessage.encodeUserId(userId);
        return requestMessage;
    }


    protected MessageTemplate hasAuditType(JobAudit.Type type) {
        return and(matchPerformative(AclMessage.Performative.INFORM),
                   matchAuditType(type));
    }


    protected MessageTemplate hasAuditStatus(JobAudit.Status status) {
        return MessageTemplate.matchWith(new AssertMatchExpression("audit.status", status) {
            @Override
            protected Object extractActual(AclMessage message) {
                JobAudit jobAudit = (JobAudit)message.getContentObject();
                return jobAudit.getStatus();
            }
        });
    }


    protected MessageTemplate hasAuditWarningMessage(final String warningMessage) {
        return MessageTemplate.matchWith(new AssertMatchExpression("warning", warningMessage) {
            @Override
            protected Object extractActual(AclMessage message) {
                JobAudit jobAudit = (JobAudit)message.getContentObject();
                return jobAudit.getWarningMessage();
            }
        });
    }


    protected MessageTemplate hasAuditErrorMessage(final String warningMessage) {
        return MessageTemplate.matchWith(new AssertMatchExpression("warning", warningMessage) {
            @Override
            protected Object extractActual(AclMessage message) {
                JobAudit jobAudit = (JobAudit)message.getContentObject();
                return jobAudit.getErrorMessage();
            }
        });
    }


    protected MessageTemplate containsAudit(JobAudit.Type type, JobAudit.Status status) {
        return containsAudit(type, status, null);
    }


    protected MessageTemplate containsAudit(JobAudit.Type type, JobAudit.Status status, String message) {
        MessageTemplate template = and(hasAuditType(type), hasAuditStatus(status));

        if (JobAudit.Status.WARNING == status && message != null) {
            return and(template, hasAuditWarningMessage(message));
        }

        if (JobAudit.Status.ERROR == status && message != null) {
            return and(template, hasAuditErrorMessage(message));
        }

        return template;
    }


    private MessageTemplate matchAuditType(final JobAudit.Type auditType) {
        return MessageTemplate.matchWith(new AssertMatchExpression("audit", auditType) {
            @Override
            protected Object extractActual(AclMessage message) {
                JobAudit jobAudit = (JobAudit)message.getContentObject();
                return jobAudit.getType();
            }
        });
    }


    @Deprecated
    protected MessageTemplate isWarningAudit(final String warningMessage) {
        return hasAuditWarningMessage(warningMessage);
    }


    protected Aid aid(String localName) {
        return new Aid(localName);
    }
}
