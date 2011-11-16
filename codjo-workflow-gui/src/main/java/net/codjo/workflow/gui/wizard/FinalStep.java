/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.gui.toolkit.wizard.StepPanel;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequestTemplate;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
/**
 *
 */
public class FinalStep extends StepPanel {
    private final VtomCaller vtomCaller;
    private final WizardSummaryGui summary;
    private final JobGui vtomGui;
    private final JobGuiData[] jobGuiDatas;
    private EventHandlerWrapper eventHandler = new EventHandlerWrapper();


    public FinalStep(String title, VtomCaller vtomCaller, WizardSummaryGui summary,
                     JobGui vtomGui, JobGuiData[] list) {
        this.vtomCaller = vtomCaller;
        this.summary = summary;
        this.vtomGui = vtomGui;
        this.jobGuiDatas = list;

        setName(title);
        buildGui();
    }


    @Override
    public void start(final Map wizardState) {
        summary.display(wizardState);
        vtomGui.displayStart();

        JobGuiData firstJobData = jobGuiDatas[0];

        JobEventHandlerForGui current =
              new JobEventHandlerForGui(firstJobData, wizardState);
        eventHandler.setAdaptee(current);

        for (int i = 1; i < jobGuiDatas.length; i++) {
            JobGuiData jobGuiData = jobGuiDatas[i];
            JobEventHandlerForGui next =
                  new JobEventHandlerForGui(jobGuiData, wizardState);
            current.next(next);
            current = next;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    vtomCaller.call(wizardState);
                }
                catch (CommandFile.ExecuteException exception) {
                    JobAudit jobAudit = new JobAudit();
                    jobAudit.setError(new JobAudit.Anomaly(exception.getLocalizedMessage(),
                                                           "[message] " + exception.getMessage()
                                                           + "; [process] "
                                                           + exception.getProcessMessage()));
                    vtomGui.displayStop(jobAudit);
                }
            }
        }).start();
    }


    public JobEventHandler getEventHandler() {
        return eventHandler;
    }


    private void buildGui() {
        BoxLayout boxLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        setLayout(boxLayout);

        add(summary.getGui());
        add(Box.createRigidArea(new Dimension(4, 4)));
        add(initPanelSize(vtomGui));

        for (JobGuiData jobGuiData : jobGuiDatas) {
            add(Box.createRigidArea(new Dimension(8, 8)));
            add(initPanelSize(jobGuiData.getGui()));
        }

        add(Box.createVerticalBox());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }


    private JComponent initPanelSize(JobGui jobGui) {
        JComponent jobUI = jobGui.getGui();
        jobUI.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        return jobUI;
    }


    public static interface JobGui {
        JComponent getGui();


        void displayStart();


        void displayMidAudit(JobAudit audit);


        void displayStop(JobAudit jobResult);
    }

    public static interface WizardSummaryGui {
        JComponent getGui();


        void display(Map requestState);
    }

    public static class JobGuiData {
        private JobGui gui;
        private RequestTemplateFactory factory;


        public JobGuiData(JobGui gui, RequestTemplateFactory factory) {
            this.gui = gui;
            this.factory = factory;
        }


        public JobGui getGui() {
            return gui;
        }


        public JobRequestTemplate createTemplate(Map wizardState) {
            return factory.createTemplate(wizardState);
        }
    }

    private class EventHandlerWrapper extends JobEventHandler {
        JobEventHandler adaptee;


        public void setAdaptee(JobEventHandler adaptee) {
            this.adaptee = adaptee;
        }


        @Override
        public boolean receive(JobEvent event) {
            if (adaptee == null) {
                return false;
            }
            boolean received = adaptee.receive(event);
            if (received && event.isRequest()) {
                vtomGui.displayStop(new JobAudit());
            }
            return received;
        }
    }

    private class JobEventHandlerForGui extends JobEventHandler {
        private final JobGui myGui;


        JobEventHandlerForGui(JobRequestTemplate template, JobGui myGui) {
            super(template);
            this.myGui = myGui;
        }


        JobEventHandlerForGui(JobGuiData jobGuiData, Map wizardState) {
            this(jobGuiData.createTemplate(wizardState), jobGuiData.getGui());
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            if (JobAudit.Type.PRE == audit.getType()) {
                if (audit.hasError()) {
                    myGui.displayStop(audit);
                }
                else {
                    myGui.displayStart();
                }
            }
            else if (JobAudit.Type.MID == audit.getType()) {
                myGui.displayMidAudit(audit);
            }
            else if (JobAudit.Type.POST == audit.getType()) {
                myGui.displayStop(audit);
            }
        }
    }
}
