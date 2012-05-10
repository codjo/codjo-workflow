package net.codjo.workflow.gui.plugin;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.codjo.agent.AgentContainer;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.client.request.FieldsList;
import net.codjo.mad.client.request.RequestException;
import net.codjo.mad.client.request.Result;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.request.RequestToolBar;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.DateUtil;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.schedule.ScheduleLauncher;

import static net.codjo.mad.gui.i18n.InternationalizationUtil.retrieveTranslationNotifier;
import static net.codjo.mad.gui.i18n.InternationalizationUtil.translate;

class WorkflowLogLogic {
    private WorkflowLogGui gui;


    WorkflowLogLogic(GuiContext context, AgentContainer agentContainer) throws RequestException {
        gui = new WorkflowLogGui(context);
        addPurgeAuditAction(context, agentContainer);
        gui.load();
    }


    public WorkflowLogGui getGui() {
        return gui;
    }


    private void addPurgeAuditAction(GuiContext context, AgentContainer agentContainer) {
        Action action = new PurgeAuditAction(context, agentContainer);
        RequestToolBar toolBar = getGui().getToolBar();
        toolBar.addSeparator();
        JButton button = toolBar.add(action);
        button.setName(getGui().getTable().getName() + ".PurgeAuditAction");
        button.setText("Purge");

        TranslationNotifier notifier = retrieveTranslationNotifier(context);
        notifier.addInternationalizableComponent(button, "WorkflowList.purgeButton", null);
    }


    private class PurgeAuditAction extends AbstractAction {
        private GuiContext context;
        private AgentContainer agentContainer;


        PurgeAuditAction(GuiContext context, AgentContainer agentContainer) {
            this.context = context;
            this.agentContainer = agentContainer;
        }


        public void actionPerformed(ActionEvent event) {
            final String period = JOptionPane.showInputDialog(gui,
                                                              translate("WorkflowList.purgeButton.inputMessage",
                                                                        context),
                                                              translate("WorkflowList.purgeButton.inputMessage.title",
                                                                        context),
                                                              JOptionPane.INFORMATION_MESSAGE);
            if (period == null || "".equals(period)) {
                return;
            }
            try {
                int rowCount = countRows(period);
                if (rowCount == 0) {
                    JOptionPane.showMessageDialog(gui, translate("WorkflowList.purgeButton.noLineMessage", context));
                }
                else {
                    int confirm =
                          JOptionPane.showConfirmDialog(gui,
                                                        rowCount + " " +
                                                        translate("WorkflowList.purgeButton.confirmationMessage",
                                                                  context),
                                                        translate("WorkflowList.purgeButton.confirmationMessage.title",
                                                                  context),
                                                        JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        gui.getWaitingPanel().exec(new Runnable() {
                                                       public void run() {
                                                           purgeAudit(period);
                                                       }
                                                   }, new Runnable() {
                            public void run() {
                                reloadGui();
                            }
                        }
                        );
                    }
                }
            }
            catch (Exception e) {
                showError(e);
            }
        }


        private int countRows(String period) throws RequestException {
            Result result = context.getSender()
                  .executeSqlHandler("countWorkflowLog",
                                     new FieldsList("requestDate",
                                                    DateUtil.computeStringDateFromPeriod(period)));
            return Integer.valueOf(result.getFirstRow().getFieldValue("count"));
        }


        private void purgeAudit(String period) {
            try {
                new ScheduleLauncher(context.getUser().getId())
                      .executeWorkflow(agentContainer,
                                       new JobRequest("purge-audit", new Arguments("period", period)));
            }
            catch (Exception e) {
                showError(e);
            }
        }


        private void reloadGui() {
            runInThreadSwing(new Runnable() {
                public void run() {
                    try {
                        gui.load();
                    }
                    catch (RequestException e) {
                        showError(e);
                    }
                }
            });
        }


        private void showError(final Exception e) {
            runInThreadSwing(new Runnable() {
                public void run() {
                    ErrorDialog.show(gui, e.getMessage(), e);
                }
            });
        }


        private void runInThreadSwing(Runnable runnable) {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            }
            else {
                SwingUtilities.invokeLater(runnable);
            }
        }
    }
}
