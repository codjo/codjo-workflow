/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.ObjectStreamException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
/**
 * Message d'audit.
 */
public class JobAudit implements Serializable {
    private Type type;
    private Anomaly error;
    private Arguments arguments;
    private String requestId;
    private Date date = new Date();
    private Anomaly warning;


    public JobAudit() {
    }


    public JobAudit(Type type) {
        this.type = type;
    }


    public void setType(Type type) {
        this.type = type;
    }


    public Type getType() {
        return type;
    }


    public void setArguments(Arguments arguments) {
        this.arguments = arguments;
    }


    public Arguments getArguments() {
        return arguments;
    }


    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }


    public String getRequestId() {
        return requestId;
    }


    public void setDate(Date date) {
        this.date = date;
    }


    public Date getDate() {
        return date;
    }


    @Override
    public String toString() {
        return "JobAudit{" +
               "type=" + type +
               ", date=" + date +
               ", arguments=" + arguments +
               ", error=" + getError() +
               ", requestId='" + requestId + "\'}";
    }


    public boolean hasError() {
        return getStatus() == Status.ERROR;
    }


    public Status getStatus() {
        if (error != null) {
            return Status.ERROR;
        }
        else if (warning != null) {
            return Status.WARNING;
        }
        return Status.OK;
    }


    public void setError(Anomaly error) {
        this.error = error;
    }


    public Anomaly getError() {
        return error;
    }


    public void setErrorMessage(String errorMessage) {
        setError(new Anomaly(errorMessage));
    }


    public String getErrorMessage() {
        if (getError() == null) {
            return null;
        }
        return getError().getMessage();
    }


    public void setWarning(Anomaly warning) {
        this.warning = warning;
    }


    public Anomaly getWarning() {
        return warning;
    }


    public void setWarningMessage(String message) {
        setWarning(new Anomaly(message));
    }


    public String getWarningMessage() {
        return warning != null ? warning.getMessage() : null;
    }


    public static JobAudit createAudit(Type type, Status status, String message) {
        return createAudit(type, status, message, null);
    }


    public static JobAudit createAudit(Type type, Status status, String message, Throwable error) {
        JobAudit jobAudit = new JobAudit(type);

        Anomaly anomaly;
        if (error == null) {
            anomaly = new Anomaly(message);
        }
        else {
            anomaly = new Anomaly(message, error);
        }

        if (Status.ERROR == status) {
            jobAudit.setError(anomaly);
        }
        else if (Status.WARNING == status) {
            jobAudit.setWarning(anomaly);
        }
        return jobAudit;
    }


    public static class Type implements Serializable {
        public static final Type PRE = new Type("PRE");
        public static final Type MID = new Type("MID");
        public static final Type POST = new Type("POST");
        private final String name;


        private Type(String name) {
            this.name = name;
        }


        @Override
        public String toString() {
            return name;
        }


        protected Object readResolve() throws ObjectStreamException {
            if ("PRE".equals(name)) {
                return PRE;
            }
            else if ("MID".equals(name)) {
                return MID;
            }
            else if ("POST".equals(name)) {
                return POST;
            }
            else {
                return this;
            }
        }
    }

    public static class Status implements Serializable {
        public static final Status OK = new Status("OK");
        public static final Status ERROR = new Status("ERROR");
        public static final Status WARNING = new Status("WARNING");
        private final String name;


        private Status(String name) {
            this.name = name;
        }


        @Override
        public String toString() {
            return name;
        }


        protected Object readResolve() throws ObjectStreamException {
            if ("OK".equals(name)) {
                return OK;
            }
            else if ("ERROR".equals(name)) {
                return ERROR;
            }
            else if ("WARNING".equals(name)) {
                return WARNING;
            }
            else {
                return this;
            }
        }
    }

    public static class Anomaly implements Serializable {
        String message;
        private String description;


        public Anomaly(String message) {
            this.message = message;
        }


        public Anomaly(String message, String description) {
            this.message = message;
            this.description = description;
        }


        public Anomaly(String message, Throwable exception) {
            this.message = message;
            description = toString(exception);
        }


        public Anomaly(Throwable exception) {
            message = exception.getLocalizedMessage();
            if (message == null) {
                message = "Erreur technique (" + exception.getClass().getName() + ")";
            }
            description = toString(exception);
        }


        public String getMessage() {
            return message;
        }


        public String getDescription() {
            return description;
        }


        private String toString(Throwable exception) {
            StringWriter result = new StringWriter();
            exception.printStackTrace(new PrintWriter(result));
            return result.toString();
        }
    }
}
