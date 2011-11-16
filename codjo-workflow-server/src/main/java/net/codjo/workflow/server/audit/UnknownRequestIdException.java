package net.codjo.workflow.server.audit;
import net.codjo.workflow.common.message.JobAudit;
/**
 *
 */
public class UnknownRequestIdException extends Exception {
    public UnknownRequestIdException(JobAudit audit) {
        super(computeMessage(audit));
    }


    static String computeMessage(JobAudit audit) {
        return "Impossible d'enregistrer l'audit " + audit.toString()
               + " car l'identifiant de requête est inconnu.";
    }
}
