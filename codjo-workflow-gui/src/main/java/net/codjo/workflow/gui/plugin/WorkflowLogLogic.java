package net.codjo.workflow.gui.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.gui.toolkit.util.ErrorDialog;
import net.codjo.mad.client.request.FieldsList;
import net.codjo.mad.client.request.RequestException;
import net.codjo.mad.client.request.Result;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.request.RequestToolBar;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.DateUtil;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.schedule.ScheduleLauncher;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
                                                              "Veuillez saisir le nombre de mois à conserver",
                                                              "Préparation de la purge",
                                                              JOptionPane.INFORMATION_MESSAGE);
            if (period != null && !"".equals(period)) {
                try {
                    int rowCount = countRows(period);
                    if (rowCount == 0) {
                        JOptionPane.showMessageDialog(gui,
                                                      "Il n'y a aucune ligne à supprimer pour cette période.");
                    }
                    else {
                        int confirm = JOptionPane.showConfirmDialog(gui,
                                                                    "Vous êtes sur le point de supprimer "
                                                                    + rowCount
                                                                    + " lignes.\n"
                                                                    + "Souhaitez-vous continuer ?",
                                                                    "Confirmer la purge",
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
                            });
                        }
                    }
                }
                catch (Exception e) {
                    showError(e);
                }
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
