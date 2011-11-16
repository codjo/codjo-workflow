package net.codjo.workflow.gui.plugin;
import net.codjo.gui.toolkit.HelpButton;
import net.codjo.gui.toolkit.waiting.WaitingPanel;
import net.codjo.mad.client.request.RequestException;
import net.codjo.mad.gui.framework.GuiContext;
import net.codjo.mad.gui.request.Column;
import net.codjo.mad.gui.request.Preference;
import net.codjo.mad.gui.request.PreferenceFactory;
import net.codjo.mad.gui.request.RequestTable;
import net.codjo.mad.gui.request.RequestToolBar;
import net.codjo.workflow.gui.plugin.TableFilterPanel.FilterType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

class WorkflowLogGui extends JInternalFrame {
    private final WaitingPanel waitingPanel = new WaitingPanel();
    private final GuiContext context;
    private final TableFilterPanel tableFilterPanel;
    private RequestTable workflowLogList;
    private JPanel mainPanel;
    private RequestToolBar toolbar;
    private JPanel filterPanel;
    private HelpButton helpButton;


    WorkflowLogGui(GuiContext context) {
        super("Liste des logs de workflow", true, true, true, true);
        this.context = context;

        initPreference();
        initToolbar();

        mainPanel.setPreferredSize(new Dimension(1000, 600));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        toolbar.setSqlRequetorOrderClause("REQUEST_DATE DESC");
        waitingPanel.setDelayBeforeAnimation(0);
        setGlassPane(waitingPanel);
        helpButton.setHelpUrl(
              "http://wp-confluence/confluence/display/framework/Guide+Utilisateur+IHM+de+agf-workflow");

        tableFilterPanel = new TableFilterPanel(workflowLogList);
        tableFilterPanel.addFilter("Type", "requestType", FilterType.TEXT);
        tableFilterPanel.addFilter("Discriminant", "discriminent", FilterType.TEXT, 100);
        tableFilterPanel.addFilter("Date de début", "requestDate", FilterType.DATE);
        tableFilterPanel.addFilter("Initiateur", "initiatorLogin", FilterType.TEXT);

        String[] defaultStatus = {"ERROR", "WARNING", "OK"};
        tableFilterPanel.addFilter("Statut initial", "preAuditStatus", FilterType.TEXT, defaultStatus);
        tableFilterPanel.addFilter("Statut final", "postAuditStatus", FilterType.TEXT, defaultStatus);
        filterPanel.add(tableFilterPanel);
    }


    public WaitingPanel getWaitingPanel() {
        return waitingPanel;
    }


    public RequestTable getTable() {
        return workflowLogList;
    }


    public RequestToolBar getToolBar() {
        return toolbar;
    }


    public void load() throws RequestException {
        tableFilterPanel.apply();
    }


    private void initToolbar() {
        toolbar.setHasExcelButton(true);
        toolbar.init(context, workflowLogList);
    }


    private void initPreference() {
        String preferenceId = "WorkflowList";

        if (PreferenceFactory.getPreferenceManager() != null
            && PreferenceFactory.containsPreferenceId(preferenceId)) {
            workflowLogList.setPreference(preferenceId);
        }
        else {
            Preference preference = new Preference();
            preference.setId(preferenceId);
            preference.setDetailWindowClass(WorkflowLogDetailLogic.class);
            preference.setSelectAllId("selectAllWorkflowLog");
            preference.setSelectByPkId("selectWorkflowLogById");
            preference.setRequetorId("allWorkflow");
            preference.setColumns(createColumns());
            workflowLogList.setPreference(preference);
        }
    }


    private List<Column> createColumns() {
        List<Column> columns = new ArrayList<Column>();
        columns.add(createColumn("requestType", "Type", 80));
        columns.add(createColumn("discriminent", "Discriminant", 200));
        columns.add(createDateColumn("requestDate", "Date de début", 80));
        columns.add(createDateColumn("postAuditDate", "Date de fin", 80));
        columns.add(createColumn("initiatorLogin", "Initiateur", 70));
        columns.add(createColumn("preAuditStatus", "Statut initial", 50));
        columns.add(createColumn("postAuditStatus", "Statut final", 50));
        return columns;
    }


    private Column createColumn(String fieldName, String label, int preferredSize) {
        return new Column(fieldName, label, 0, Integer.MAX_VALUE, preferredSize);
    }


    private Column createDateColumn(String fieldName, String label, int preferredSize) {
        Column column = createColumn(fieldName, label, preferredSize);
        column.setFormat("Timestamp(yyyy-MM-dd HH:mm:ss)");
        column.setSorter("Date(yyyy-MM-dd HH:mm:ss)");
        return column;
    }
}
