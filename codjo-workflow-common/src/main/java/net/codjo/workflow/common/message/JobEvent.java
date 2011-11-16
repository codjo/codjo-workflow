/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.Serializable;
/**
 * Décrit un event suite à l'exécution d'un Job.
 */
public class JobEvent implements Serializable {
    private final JobAudit audit;
    private final JobRequest request;


    public JobEvent(JobAudit audit) {
        this.audit = audit;
        this.request = null;
    }


    public JobEvent(JobRequest request) {
        this.audit = null;
        this.request = request;
    }


    public boolean isAudit() {
        return (audit != null);
    }


    public boolean isRequest() {
        return (request != null);
    }


    public JobAudit getAudit() {
        return audit;
    }


    public JobRequest getRequest() {
        return request;
    }
}
