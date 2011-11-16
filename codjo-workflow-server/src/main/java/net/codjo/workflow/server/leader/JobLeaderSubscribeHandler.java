/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.leader;
import net.codjo.agent.AclMessage;
import net.codjo.agent.protocol.AbstractSubscribeParticipantHandler;
import net.codjo.agent.protocol.SubscribeParticipant;
import net.codjo.workflow.common.message.JobEvent;

public class JobLeaderSubscribeHandler extends AbstractSubscribeParticipantHandler {

    public void sendNotification(JobEvent jobEvent) {
        AclMessage notification = new AclMessage(AclMessage.Performative.INFORM);
        notification.setContentObject(jobEvent);

        for (SubscribeParticipant.Subscription subscriber : getSubscribers()) {
            subscriber.reply(notification);
        }
    }
}
