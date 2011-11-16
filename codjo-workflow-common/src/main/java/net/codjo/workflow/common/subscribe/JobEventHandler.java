/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.JobRequestTemplate;
import java.util.ArrayList;
import java.util.List;
/**
 * Ce handler permet de gérer les évènements produits par l'exécution d'un 'Job' (audit et request). Cette
 * classe utilise le pattern 'Chain of Responsability'.   Les messages ont une structure en arbre ex:
 * <pre>
 * import                 PRE / MID* / POST
 * |- control             PRE / MID* / POST
 * |   | - computeVL      PRE / MID* / POST
 * |   | - computeBench   PRE / MID* / POST
 * |- computeAudit        PRE / MID* / POST
 * </pre>
 *
 * @see net.codjo.workflow.common.message.JobEvent
 * @see ProtocolErrorEvent
 */
public abstract class JobEventHandler {
    private static final int MAX_REQUEST = 100;
    private final JobRequestTemplate template;
    private JobEventHandler fatherEventHandler;
    private JobEventHandler sonEventHandler;
    private List matchedRequestIds = new ArrayList(MAX_REQUEST + 1);


    protected JobEventHandler(JobRequestTemplate template) {
        this.template = JobRequestTemplate.and(matchedByFather(), template);
    }


    protected JobEventHandler() {
        this(JobRequestTemplate.matchAll());
    }


    public final void next(JobEventHandler jobEventHandler) {
        if (sonEventHandler != null) {
            throw new UnsupportedOperationException("Not Yet Required !");
        }
        sonEventHandler = jobEventHandler;
        jobEventHandler.fatherEventHandler = this;
    }


    public boolean receiveError(ProtocolErrorEvent event) {
        if (sonEventHandler != null) {
            return sonEventHandler.receiveError(event);
        }
        else {
            return false;
        }
    }


    public boolean receive(JobEvent event) {
        if (event.isAudit()) {
            return receive(event.getAudit());
        }
        else {
            return receive(event.getRequest());
        }
    }


    protected void handleRequest(JobRequest request) {
    }


    protected void handleAudit(JobAudit audit) {
    }


    private boolean receive(JobRequest request) {
        if (template.match(request)) {
            matchedRequestIds.add(request.getId());
            if (matchedRequestIds.size() > MAX_REQUEST) {
                matchedRequestIds.remove(0);
            }
            handleRequest(request);
            return true;
        }
        else if (sonEventHandler != null) {
            return sonEventHandler.receive(request);
        }
        return false;
    }


    private boolean receive(JobAudit audit) {
        if (isMatchedRequest(audit.getRequestId())) {
            handleAudit(audit);
            return true;
        }
        else if (sonEventHandler != null) {
            return sonEventHandler.receive(audit);
        }
        return false;
    }


    private JobRequestTemplate matchedByFather() {
        return JobRequestTemplate.matchCustom(new JobRequestTemplate.MatchExpression() {
            public boolean match(JobRequest request) {
                if (fatherEventHandler == null) {
                    return true;
                }
                return fatherEventHandler.isMatchedRequest(request.getParentId());
            }
        });
    }


    private boolean isMatchedRequest(String requestId) {
        return matchedRequestIds.contains(requestId);
    }
}
