/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
/**
 * Classe d'erreur levée pendant l'exécution d'un job.
 */
public class JobException extends Exception {
    public JobException(String message, Throwable cause) {
        super(message, cause);
    }


    public JobException(String message) {
        super(message);
    }
}
