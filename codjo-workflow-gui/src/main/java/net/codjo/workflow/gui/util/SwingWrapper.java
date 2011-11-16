/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.util;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;
/**
 * Classe utilitaire permettant de se synchroniser sur le thread Swing.
 */
public final class SwingWrapper {
    private static final Logger LOG = Logger.getLogger(SwingWrapper.class.getName());


    private SwingWrapper() {
    }


    public static JobEventHandler wrapp(JobEventHandler handler) {
        return new JobEventHandlerWrapper(handler);
    }


    private static class JobEventHandlerWrapper extends JobEventHandler {
        private JobEventHandler handler;


        JobEventHandlerWrapper(JobEventHandler handler) {
            this.handler = handler;
        }


        @Override
        public boolean receiveError(final ProtocolErrorEvent event) {
            ReceiveErrorCommand command = new ReceiveErrorCommand(handler, event);
            command.invokeInSwingThread();
            return command.getResult();
        }


        @Override
        public boolean receive(JobEvent event) {
            ReceiveCommand command = new ReceiveCommand(handler, event);
            command.invokeInSwingThread();
            return command.getResult();
        }
    }

    private static class ReceiveErrorCommand implements Runnable {
        private JobEventHandler handler;
        private final ProtocolErrorEvent event;
        private boolean result;


        ReceiveErrorCommand(JobEventHandler handler, ProtocolErrorEvent event) {
            this.handler = handler;
            this.event = event;
        }


        public void run() {
            result = handler.receiveError(event);
        }


        public boolean getResult() {
            return result;
        }


        public void invokeInSwingThread() {
            try {
                SwingUtilities.invokeAndWait(this);
            }
            catch (InterruptedException e) {
                // Pas grave
            }
            catch (InvocationTargetException e) {
                LOG.error("ReceiveErrorCommand", e);
            }
        }
    }

    private static class ReceiveCommand implements Runnable {
        private JobEventHandler handler;
        private final JobEvent event;
        private boolean result;


        ReceiveCommand(JobEventHandler handler, JobEvent event) {
            this.handler = handler;
            this.event = event;
        }


        public void run() {
            result = handler.receive(event);
        }


        public boolean getResult() {
            return result;
        }


        public void invokeInSwingThread() {
            try {
                SwingUtilities.invokeAndWait(this);
            }
            catch (InterruptedException e) {
                // Pas grave
            }
            catch (InvocationTargetException e) {
                LOG.error("ReceiveCommand", e);
            }
        }
    }
}
