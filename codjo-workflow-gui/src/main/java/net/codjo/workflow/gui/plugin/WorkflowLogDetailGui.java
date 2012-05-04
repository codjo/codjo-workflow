package net.codjo.workflow.gui.plugin;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import net.codjo.i18n.gui.InternationalizableContainer;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.mad.gui.request.DetailDataSource;
import net.codjo.mad.gui.request.wrapper.GuiWrapper;
/**
 *
 */
class WorkflowLogDetailGui extends JInternalFrame implements InternationalizableContainer {
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
    private JPanel requestPanel;
    private JPanel initialStatePanel;
    private JPanel finalStatePanel;

    private JLabel typeLabel;
    private JLabel idLabel;
    private JLabel idParentLabel;
    private JLabel requestDateLabel;
    private JLabel initiatorLabel;
    private JLabel discriminentLabel;
    private JLabel requestArgumentLabel;
    private JLabel initialStateDateLabel;
    private JLabel initialStateLabel;
    private JLabel initialStateArgumentLabel;
    private JLabel initialStateMessageLabel;
    private JLabel initialStateDescriptionLabel;
    private JLabel finalStateDateLabel;
    private JLabel finalStateLabel;
    private JLabel finalStateArgumentLabel;
    private JLabel finalStateDescriptionLabel;
    private JLabel finalStateMessageLabel;


    WorkflowLogDetailGui(DetailDataSource dataSource) {
        super("Détails du log", true, true, true, true);
        declareFields(dataSource);
        mainPanel.setPreferredSize(new Dimension(800, 600));
        horizontalSplit.setDividerLocation((800 - horizontalSplit.getDividerSize()) / 2);
        setLayout(new BorderLayout());
        add(mainPanel);

        TranslationNotifier translationNotifier = InternationalizationUtil.retrieveTranslationNotifier(dataSource.getGuiContext());
        translationNotifier.addInternationalizableContainer(this);
    }


    public void addInternationalizableComponents(TranslationNotifier translationNotifier) {
        translationNotifier.addInternationalizableComponent(this, "WorkflowLogDetailGui.title");
        translationNotifier.addInternationalizableComponent(requestPanel, "WorkflowLogDetailGui.requestPanel.title");
        translationNotifier.addInternationalizableComponent(initialStatePanel, "WorkflowLogDetailGui.initialStatePanel.title");
        translationNotifier.addInternationalizableComponent(finalStatePanel, "WorkflowLogDetailGui.finalStatePanel.title");

        translationNotifier.addInternationalizableComponent(typeLabel, "WorkflowLogDetailGui.typeLabel");
        translationNotifier.addInternationalizableComponent(idLabel, "WorkflowLogDetailGui.idLabel");
        translationNotifier.addInternationalizableComponent(idParentLabel, "WorkflowLogDetailGui.idParentLabel");

        translationNotifier.addInternationalizableComponent(requestDateLabel, "WorkflowLogDetailGui.requestDateLabel");
        translationNotifier.addInternationalizableComponent(initiatorLabel, "WorkflowLogDetailGui.initiatorLabel");

        translationNotifier.addInternationalizableComponent(discriminentLabel, "WorkflowLogDetailGui.discriminentLabel");
        translationNotifier.addInternationalizableComponent(requestArgumentLabel, "WorkflowLogDetailGui.requestArgumentLabel");

        translationNotifier.addInternationalizableComponent(initialStateDateLabel, "WorkflowLogDetailGui.initialStateDateLabel");
        translationNotifier.addInternationalizableComponent(initialStateArgumentLabel, "WorkflowLogDetailGui.initialStateArgumentLabel");
        translationNotifier.addInternationalizableComponent(initialStateLabel, "WorkflowLogDetailGui.initialStateLabel");
        translationNotifier.addInternationalizableComponent(initialStateMessageLabel, "WorkflowLogDetailGui.initialStateMessageLabel");
        translationNotifier.addInternationalizableComponent(initialStateDescriptionLabel, "WorkflowLogDetailGui.initialStateDescriptionLabel");

        translationNotifier.addInternationalizableComponent(finalStateDateLabel, "WorkflowLogDetailGui.finalStateDateLabel");
        translationNotifier.addInternationalizableComponent(finalStateArgumentLabel, "WorkflowLogDetailGui.finalStateArgumentLabel");
        translationNotifier.addInternationalizableComponent(finalStateLabel, "WorkflowLogDetailGui.finalStateLabel");
        translationNotifier.addInternationalizableComponent(finalStateMessageLabel, "WorkflowLogDetailGui.finalStateMessageLabel");
        translationNotifier.addInternationalizableComponent(finalStateDescriptionLabel, "WorkflowLogDetailGui.finalStateDescriptionLabel");
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
