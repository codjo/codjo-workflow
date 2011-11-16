/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.subscribe;
import net.codjo.agent.AclMessage;
import junit.framework.TestCase;
/**
 * Classe de test de {@link ProtocolErrorEvent}.
 */
public class ProtocolErrorEventTest extends TestCase {
    public void test_constructeur() throws Exception {
        AclMessage aclMessage = new AclMessage(AclMessage.Performative.AGREE);

        ProtocolErrorEvent event =
              new ProtocolErrorEvent(ProtocolErrorEvent.Type.FAILURE, aclMessage);

        assertEquals(ProtocolErrorEvent.Type.FAILURE, event.getType());
        assertSame(aclMessage, event.getACLMessage());
    }
}
