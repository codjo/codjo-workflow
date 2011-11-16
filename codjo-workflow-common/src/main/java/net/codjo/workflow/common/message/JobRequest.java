/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.Serializable;
import java.util.Date;
/**
 * Décrit une requête lors d'une demande d'exécution de tâche.
 */
public class JobRequest implements Serializable {
    private String initiatorLogin;
    private String type;
    private Arguments arguments;
    private String id;
    private String parentId;
    private Date date = new Date();
    private boolean loggable = true;

    public JobRequest() {
    }


    public JobRequest(String type) {
        this(type, null);
    }


    public JobRequest(String type, Arguments arguments) {
        setType(type);
        setArguments(arguments);
    }


    public void setInitiatorLogin(String name) {
        initiatorLogin = name;
    }


    public String getInitiatorLogin() {
        return initiatorLogin;
    }


    public void setType(String type) {
        this.type = type;
    }


    public String getType() {
        return type;
    }


    public void setArguments(Arguments arguments) {
        this.arguments = arguments;
    }


    public Arguments getArguments() {
        return arguments;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getId() {
        return id;
    }


    public void setParentId(String parentId) {
        this.parentId = parentId;
    }


    public String getParentId() {
        return parentId;
    }


    public void setDate(Date date) {
        this.date = date;
    }


    public Date getDate() {
        return date;
    }


    public boolean isLoggable() {
        return loggable;
    }


    public void setLoggable(boolean loggable) {
        this.loggable = loggable;
    }


    @Override
    public String toString() {
        return "JobRequest{" +
               "initiatorLogin='" + initiatorLogin + '\'' +
               ", type='" + type + '\'' +
               ", date=" + date +
               ", arguments=" + arguments +
               ", id='" + id + '\'' +
               ", parentId='" + parentId + '\'' +
               ", loggable='" + loggable + '\'' +
               '}';
    }


    public void initializeFromPreviousRequest(JobRequest request) {
        setInitiatorLogin(request.getInitiatorLogin());
        setParentId(request.getId());
    }
}
