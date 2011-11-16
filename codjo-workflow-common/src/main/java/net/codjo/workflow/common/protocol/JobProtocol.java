/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.protocol;
/**
 * Protocole 'workflow-job-protocol'. Ce protocole définit l'interaction entre l'agent 'Initiator' demandant
 * l'execution d'une requete  et un agent 'Participant' executant ladite requête.
 *
 * <p> Déroulement du protocole :
 *
 * <ul>
 *
 * <li> 'Initiator' envoie un message 'request' à 'Participant'. </li>
 *
 * <li> 'Participant' lui répond par un message 'inform' comprenant un {@link
 * net.codjo.workflow.common.message.JobAudit} de type PRE. NB : Si l'audit comporte une erreur, le protocole
 * s'arrête (l'agent 'Participant' n'accepte pas le travail). </li>
 *
 * <li> 'Participant' lui envoie (de manière optionnel) des messages 'inform'  comprenant un audit de type MID
 * pour informer  de l'avancement de la requête. NB : Si l'audit comporte une erreur, le protocole continue.
 * </li>
 *
 *
 * <li> 'Participant' termine le protocol en envoyant un message 'inform' comprenant un audit de type POST. NB
 * : Si l'audit comporte une erreur, le protocole se termine et tout le travail effectué est annulé. </li>
 *
 *
 * </ul>
 *
 * </p>
 *
 * @see JobProtocolInitiator
 * @see JobProtocolParticipant
 */
public interface JobProtocol {
    public static final String ID = "workflow-job-protocol";
}
