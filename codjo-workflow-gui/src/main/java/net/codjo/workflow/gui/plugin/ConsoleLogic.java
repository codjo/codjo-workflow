/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.plugin;
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
    private final ConsoleGui gui;
    private JobEventHandler eventHandler = new EventHandlerLogic();


    ConsoleLogic(ConsoleGui gui) {
        this.gui = gui;
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


    public static void main(String[] args) {
        JFrame frame = new JFrame("Test ConsoleLogic");
        ConsoleLogic logic = new ConsoleLogic(new ConsoleGui());
        frame.setContentPane(logic.gui.getContentPane());
        frame.pack();
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });

        JobRequest request = new JobRequest();
        request.setId("job-1");
        request.setType("import");
        logic.getEventHandler().receive(new JobEvent(request));

        request = new JobRequest();
        request.setId("job-2");
        request.setType("export");
        logic.getEventHandler().receive(new JobEvent(request));

        JobAudit audit = new JobAudit(JobAudit.Type.PRE);
        audit.setRequestId("job-1");
        logic.getEventHandler().receive(new JobEvent(audit));

        audit = new JobAudit(JobAudit.Type.MID);
        audit.setRequestId("job-1");
        logic.getEventHandler().receive(new JobEvent(audit));

        audit = new JobAudit(JobAudit.Type.MID);
        audit.setRequestId("job-1");
        audit.setErrorMessage("error message");
        logic.getEventHandler().receive(new JobEvent(audit));

        audit = new JobAudit(JobAudit.Type.POST);
        audit.setRequestId("job-1");
        logic.getEventHandler().receive(new JobEvent(audit));
    }
}
