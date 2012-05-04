package net.codjo.workflow.gui.task;
import java.awt.BorderLayout;
import java.awt.Color;
import java.text.SimpleDateFormat;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.border.LineBorder;
import net.codjo.workflow.common.organiser.Job;

import static net.codjo.mad.gui.i18n.InternationalizationUtil.translate;

public class JobPanel extends JPanel {
    public static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM HH:mm:ss");
    public static final Icon DEFAULT_TYPE_ICON =
          new ImageIcon(JobPanel.class.getResource("/images/job.default.png"));
    public static final Icon REJECTED_ICON = new ImageIcon(JobPanel.class.getResource("/images/error.png"));
    public static final Icon WAITING_ICON = new ImageIcon(JobPanel.class.getResource("/images/waiting.gif"));
    public static final Icon RUNNING_ICON = new ImageIcon(JobPanel.class.getResource("/images/running.gif"));
    public static final Icon PAUSED_ICON =
          new ImageIcon(JobPanel.class.getResource("/images/running_paused.gif"));
    public static final Icon DONE_ICON = new ImageIcon(JobPanel.class.getResource("/images/done.png"));
    public static final Icon FAILURE_ICON = new ImageIcon(JobPanel.class.getResource("/images/fail.png"));
    public static final ImageIcon USER_ICON = new ImageIcon(JobPanel.class.getResource("/images/user.png"));

    private final TaskManagerConfiguration configuration;
    private JPanel mainPanel;
    private JLabel statusIconLabel;
    private JLabel dateLabel;
    private JLabel requestTypeLabel;
    private JLabel descriptionLabel;
    private JLabel initiatorLabel;
    private JLabel jobIconLabel;
    private JPanel errorPanel;
    private JTextPane errorLabel;


    public JobPanel(TaskManagerConfiguration configuration, Job job) {
        this.configuration = configuration;

        setLayout(new BorderLayout());
        add(mainPanel);
        setBorder(new LineBorder(Color.GRAY));
        setComponentNames("");

        updateFromJob(job);
    }


    private void setComponentNames(String prefix) {
        dateLabel.setName(prefix + "date");
        requestTypeLabel.setName(prefix + "type");
        descriptionLabel.setName(prefix + "description");
        initiatorLabel.setName(prefix + "initiator");
        jobIconLabel.setName(prefix + "typeIcon");
        statusIconLabel.setName(prefix + "statusIcon");
        errorLabel.setName(prefix + "error");
    }


    public void updateFromJob(Job job) {
        dateLabel.setText(SIMPLE_DATE_FORMAT.format(job.getDate()));
        requestTypeLabel.setText(job.getType());

        String jobDescription = job.getDescription();
        if (jobDescription != null) {
            descriptionLabel.setText(jobDescription);
            descriptionLabel.setToolTipText(jobDescription);
        }

        initiatorLabel.setText(job.getInitiator());
        if (configuration.getUserLogin().equals(job.getInitiator())) {
            initiatorLabel.setIcon(USER_ICON);
        }

        Icon stateIcon = null;
        String tooltipKey = null;
        switch (job.getState()) {
            case DONE:
                stateIcon = DONE_ICON;
                statusIconLabel.setToolTipText(translate("JobPanel.processDone", configuration.getGuiContext()));
                break;
            case FAILURE:
                stateIcon = FAILURE_ICON;
                statusIconLabel.setToolTipText(job.getErrorMessage());
                errorLabel.setText(job.getErrorMessage());
                errorPanel.setVisible(true);
                break;
            case RUNNING:
                stateIcon = RUNNING_ICON;
                statusIconLabel.setToolTipText(translate("JobPanel.processRunning", configuration.getGuiContext()));
                break;
            case REJECTED:
                stateIcon = REJECTED_ICON;
                statusIconLabel.setToolTipText(translate("JobPanel.processRejected", configuration.getGuiContext()));
                break;
            case WAITING:
            case NEW:
                stateIcon = WAITING_ICON;
                statusIconLabel.setToolTipText(translate("JobPanel.processPending", configuration.getGuiContext()));
                break;
        }

        statusIconLabel.setIcon(stateIcon);

        Icon jobIcon = configuration.getJobIcon(job.getType());
        if (jobIcon == null) {
            jobIcon = DEFAULT_TYPE_ICON;
        }
        jobIconLabel.setIcon(jobIcon);
    }


    public void setComponentPrefix(String prefix) {
        setComponentNames(prefix != null && !"".equals(prefix) ? prefix + "." : "");
    }
}
