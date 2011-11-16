/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
/**
 * Décrit un event suite à une erreur durant le découlement du protocole 'fipa-subscribe'.
 *
 * @see net.codjo.workflow.common.message.JobEvent
 * @see net.codjo.agent.protocol.InitiatorHandler
 */
public class ProtocolErrorEvent {
    private Type type;
    private AclMessage aclMessage;


    public ProtocolErrorEvent(Type type, AclMessage aclMessage) {
        this.type = type;
        this.aclMessage = aclMessage;
    }


    public Type getType() {
        return type;
    }


    public AclMessage getACLMessage() {
        return aclMessage;
    }


    /**
     * Type de l'erreur lié au protocol fipa-subscribe 'Initiator'.
     */
    public static class Type {
        public static final Type REFUSE = new Type("REFUSE");
        public static final Type OUT_OF_SEQUENCE = new Type("OUT_OF_SEQUENCE");
        public static final Type NOT_UNDERSTOOD = new Type("NOT_UNDERSTOOD");
        public static final Type FAILURE = new Type("FAILURE");
        private final String myName;


        private Type(String name) {
            myName = name;
        }


        @Override
        public String toString() {
            return myName;
        }
    }
}
