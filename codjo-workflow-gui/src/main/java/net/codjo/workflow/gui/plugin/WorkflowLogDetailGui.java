package net.codjo.workflow.gui.plugin;
import net.codjo.mad.gui.request.DetailDataSource;
import net.codjo.mad.gui.request.wrapper.GuiWrapper;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
/**
 *
 */
class WorkflowLogDetailGui extends JInternalFrame {
    private JTextField id;
    private JTextField parentId;
    private JTextField requestDate;
    private JTextField initiatorLogin;
    private JTextField requestType;
    private JTextField discriminent;
    private JTextArea argument;
    private JTextArea preArguments;
    private JTextField preDate;
    private JTextField preStatus;
    private JTextField preAnomalyMessage;
    private JTextArea preAnomalyTrace;
    private JTextField postDate;
    private JTextField postStatus;
    private JTextArea postArguments;
    private JTextField postAnomalyMessage;
    private JTextArea postAnomalyTrace;
    private JPanel mainPanel;
    private JSplitPane horizontalSplit;


    WorkflowLogDetailGui(DetailDataSource dataSource) {
        super("Détails du log", true, true, true, true);
        declareFields(dataSource);
        mainPanel.setPreferredSize(new Dimension(800, 600));
        horizontalSplit.setDividerLocation((800 - horizontalSplit.getDividerSize()) / 2);
        setLayout(new BorderLayout());
        add(mainPanel);
    }


    private void declareFields(DetailDataSource dataSource) {
        dataSource.declare("id", id);
        dataSource.declare("parentId", parentId);
        dataSource.declare("requestDate", requestDate);
        dataSource.declare("initiatorLogin", initiatorLogin);
        dataSource.declare("requestType", requestType);
        dataSource.declare("discriminent", discriminent);
        dataSource.declare("argument", argument);
        dataSource.declare("preAuditArgument", preArguments);
        dataSource.declare("preAuditDate", preDate);
        dataSource.declare("preAuditStatus", preStatus);
        dataSource.declare("preAuditAnomalyMessage", preAnomalyMessage);
        dataSource.declare("preAuditAnomalyTrace", preAnomalyTrace);
        dataSource.declare("postAuditDate", postDate);
        dataSource.declare("postAuditStatus", postStatus);
        dataSource.declare("postAuditArgument", postArguments);
        dataSource.declare("postAuditAnomalyMessage", postAnomalyMessage);
        dataSource.declare("postAuditAnomalyTrace", postAnomalyTrace);

        Map declaredFields = dataSource.getDeclaredFields();
        for (Object field : declaredFields.values()) {
            GuiWrapper wrapper = (GuiWrapper)field;
            JTextComponent textComponent = (JTextComponent)wrapper.getGuiComponent();
            textComponent.setEditable(false);
            textComponent.setBackground(null);
        }
    }
}
