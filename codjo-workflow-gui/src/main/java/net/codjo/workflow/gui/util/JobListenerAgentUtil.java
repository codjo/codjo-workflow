/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.util;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.AgentController;
import net.codjo.agent.BadControllerException;
import net.codjo.agent.ContainerFailureException;
import net.codjo.mad.gui.base.GuiPlugin;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.JobListenerAgent;
import java.beans.PropertyVetoException;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
/**
 *
 */
public class JobListenerAgentUtil {
    private JobListenerAgentUtil() {
    }


    public static AgentController createGuiAgent(GuiContext guiContext,
                                                 JInternalFrame internalFrame, JobEventHandler eventHandler)
          throws ContainerFailureException {
        AgentController listenerAgent = startListenerAgent(getAgentContainer(guiContext), eventHandler);

        internalFrame.addInternalFrameListener(new CloseListener(listenerAgent));

        displayGui(guiContext, internalFrame);

        return listenerAgent;
    }


    public static AgentController startListenerAgent(AgentContainer container, JobEventHandler eventHandler)
          throws ContainerFailureException {
        JobListenerAgent jobListenerAgent =
              new JobListenerAgent(SwingWrapper.wrapp(eventHandler));

        final AgentController listenerAgentController =
              container.acceptNewAgent(jobListenerAgent.createNickName(), jobListenerAgent);
        listenerAgentController.start();
        return listenerAgentController;
    }


    private static AgentContainer getAgentContainer(GuiContext guiContext) {
        return ((AgentContainer)guiContext.getProperty(GuiPlugin.AGENT_CONTAINER_KEY));
    }


    private static void displayGui(GuiContext guiContext, JInternalFrame gui) {
        guiContext.getDesktopPane().add(gui);
        gui.pack();
        gui.setVisible(true);
        try {
            gui.setSelected(true);
        }
        catch (PropertyVetoException ex) {
            ;
        }
    }


    private static class CloseListener extends InternalFrameAdapter {
        private AgentController controller;


        CloseListener(AgentController listenerAgentController) {
            this.controller = listenerAgentController;
        }


        @Override
        public void internalFrameClosed(InternalFrameEvent event) {
            handleClose(event);
        }


        @Override
        public void internalFrameClosing(InternalFrameEvent event) {
            handleClose(event);
        }


        private void handleClose(InternalFrameEvent event) {
            if (controller == null) {
                return;
            }
            event.getInternalFrame().removeInternalFrameListener(this);
            try {
                controller.kill();
            }
            catch (BadControllerException e) {
                ;
            }
            controller = null;
        }
    }
}
