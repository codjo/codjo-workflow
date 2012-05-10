/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
/**
 * Logique de la console.
 */
class ConsoleLogic {
    final ConsoleGui gui;
    private JobEventHandler eventHandler = new EventHandlerLogic();


    ConsoleLogic(GuiContext guiContext, ConsoleGui gui) {
        this.gui = gui;
        gui.init(guiContext);
    }


    public void selectRequest(String requestId) {
        gui.selectRequest(requestId);
    }


    public JobEventHandler getEventHandler() {
        return eventHandler;
    }


    private class EventHandlerLogic extends JobEventHandler {
        @Override
        public boolean receiveError(ProtocolErrorEvent event) {
            gui.receiveError(event.getACLMessage().toFipaACLString());
            return true;
        }


        @Override
        protected void handleRequest(JobRequest request) {
            gui.receiveRequest(request);
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            gui.receiveAudit(audit);
        }
    }


}
