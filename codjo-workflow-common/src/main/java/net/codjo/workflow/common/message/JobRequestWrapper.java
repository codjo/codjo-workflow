/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.util.Date;
/**
 * Classe utilitaire permettant d'encapsuler une {@link JobRequest}. Cette classe est utile pour faire des
 * wrappers spécifique à un job donnée (pour manipuler plus facilement les arguments).
 */
public class JobRequestWrapper {
    private final JobRequest jobRequest;
    private final String jobRequestType;


    protected JobRequestWrapper(String jobRequestType, JobRequest request) {
        this.jobRequestType = jobRequestType;
        if (request == null) {
            jobRequest = new JobRequest();
        }
        else {
            jobRequest = request;
        }

        if (jobRequest.getArguments() == null) {
            jobRequest.setArguments(new Arguments());
        }
        jobRequest.setType(jobRequestType);
    }


    public JobRequest toRequest() {
        return jobRequest;
    }


    public void setInitiatorLogin(String user) {
        jobRequest.setInitiatorLogin(user);
    }


    public String getInitiatorLogin() {
        return jobRequest.getInitiatorLogin();
    }


    public void setDate(Date date) {
        jobRequest.setDate(date);
    }


    public Date getDate() {
        return jobRequest.getDate();
    }


    public void setId(String id) {
        jobRequest.setId(id);
    }


    public String getId() {
        return jobRequest.getId();
    }


    public Arguments getArguments() {
        return jobRequest.getArguments();
    }


    protected void setArgument(String key, String value) {
        jobRequest.getArguments().put(key, value);
    }


    protected String getArgument(String key) {
        return jobRequest.getArguments().get(key);
    }


    public String getType() {
        return jobRequestType;
    }
}
