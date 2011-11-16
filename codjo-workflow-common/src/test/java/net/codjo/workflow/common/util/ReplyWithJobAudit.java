/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.util;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.agent.test.SubStep;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import junit.framework.AssertionFailedError;
/**
 * Action du framework de test permettant de repondre un message d'Audit.
 *
 * @see net.codjo.workflow.common.message.JobAudit
 * @see net.codjo.workflow.common.protocol.JobProtocol
 */
public class ReplyWithJobAudit implements SubStep {
    private final JobAudit.Type auditType;
    private final AclMessage.Performative performative;
    private final Arguments arguments;
    private final String errorMessage;


    public ReplyWithJobAudit(AclMessage.Performative performative, JobAudit.Type type) {
        this(performative, type, (String)null);
    }


    public ReplyWithJobAudit(AclMessage.Performative performative,
                             JobAudit.Type auditType,
                             String errorMessage) {
        this.auditType = auditType;
        this.performative = performative;
        this.errorMessage = errorMessage;
        arguments = null;
    }


    public ReplyWithJobAudit(AclMessage.Performative performative,
                             JobAudit.Type auditType,
                             Arguments arguments) {
        this.auditType = auditType;
        this.performative = performative;
        this.arguments = arguments;
        errorMessage = null;
    }


    public void run(Agent agent, AclMessage message)
          throws AssertionFailedError {
        JobRequest request = (JobRequest)message.getContentObject();

        JobAudit audit = new JobAudit(auditType);
        if (errorMessage != null) {
            audit.setErrorMessage(errorMessage);
        }
        if (arguments != null) {
            audit.setArguments(arguments);
        }
        audit.setRequestId(request.getId());

        AclMessage inform = message.createReply(performative);
        inform.setContentObject(audit);
        agent.send(inform);
    }
}
