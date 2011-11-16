/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.JobRequestTemplate;
/**
 * Classe de mock de {@link net.codjo.workflow.common.subscribe.JobEventHandler}.
 */
public class JobEventHandlerMock extends JobEventHandler {
    private LogString log;


    public JobEventHandlerMock() {
        this(new LogString());
    }


    public JobEventHandlerMock(LogString logString) {
        this.log = logString;
    }


    public JobEventHandlerMock(LogString logString, JobRequestTemplate requestTemplate) {
        super(requestTemplate);
        this.log = logString;
    }


    @Override
    protected void handleRequest(JobRequest request) {
        log.call("handleRequest", request.getType());
    }


    @Override
    protected void handleAudit(JobAudit audit) {
        log.call("handleAudit", audit.getType());
    }


    @Override
    public boolean receiveError(ProtocolErrorEvent event) {
        log.call("receiveError",
                 AclMessage.performativeToString(event.getACLMessage().getPerformative()));
        return true;
    }
}
