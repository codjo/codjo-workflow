package net.codjo.workflow.gui.task;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.workflow.common.organiser.Job;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;
/**
 *
 */
public class JobList extends JPanel {
    private JPanel mainPanel;
    private TaskManagerConfiguration configuration;
    private String jobNamePrefix;

    private JPanel collapseActionPanel;
    private JLabel jobStatusLabel;
    private JLabel jobCountLabel;

    private JLabel collapseListLabel;
    private JXCollapsiblePane jobList;

    private static final ImageIcon EXPAND_ICON =
          new ImageIcon(JobPanel.class.getResource("/images/expand.png"));
    private static final ImageIcon COLLAPSE_ICON =
          new ImageIcon(JobPanel.class.getResource("/images/collapse.png"));


    public JobList(TaskManagerConfiguration configuration,
                   String jobNamePrefix,
                   ListModel jobModel,
                   String jobStatusKey,
                   Icon statusIcon) {

        this.configuration = configuration;
        this.jobNamePrefix = jobNamePrefix;

        jobList.setName(jobNamePrefix);
        jobList.setLayout(new VerticalLayout());

        jobCountLabel.setName(jobNamePrefix + ".jobCount");

        jobStatusLabel.setText(jobStatusKey);
        jobStatusLabel.setIcon(statusIcon);

        initJobList(jobModel);

        collapseActionPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                jobList.setCollapsed(!jobList.isCollapsed());
                collapseListLabel.setIcon(jobList.isCollapsed() ? EXPAND_ICON : COLLAPSE_ICON);
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
        add(new JSeparator(), BorderLayout.SOUTH);

        TranslationNotifier translationNotifier =
              InternationalizationUtil.retrieveTranslationNotifier(configuration.getGuiContext());
        translationNotifier.addInternationalizableComponent(jobStatusLabel, jobStatusKey);
    }


    private void initJobList(ListModel jobModel) {
        for (int i = 0; i < jobModel.getSize(); i++) {
            addJob(i, (Job)jobModel.getElementAt(i), jobList);
        }
        updateJobCount(jobModel.getSize());
        jobModel.addListDataListener(new MyListDataListener(jobModel, jobList));
    }


    private void addJob(int index, Job job, JPanel jobsPanel) {
        JobPanel jobPanel = new JobPanel(configuration, job);
        jobPanel.setComponentPrefix(String.format("%s.%s", jobNamePrefix, index));
        jobsPanel.add(jobPanel, index);
    }


    private void removeJob(int index, JPanel jobsPanel) {
        jobsPanel.remove(index);
    }


    private void updateJob(int index, Job job, JPanel jobsPanel) {
        removeJob(index, jobsPanel);
        addJob(index, job, jobsPanel);
    }


    private void updateJobCount(int jobCount) {
        jobCountLabel.setText(String.format("(%s)", jobCount));
    }


    public JPanel getMainPanel() {
        return mainPanel;
    }


    private class MyListDataListener implements ListDataListener {
        private final JPanel jobsPanel;
        private final ListModel jobListModel;


        private MyListDataListener(ListModel jobListModel, JPanel jobsPanel) {
            this.jobListModel = jobListModel;
            this.jobsPanel = jobsPanel;
        }


        public void intervalAdded(ListDataEvent event) {
            for (int i = event.getIndex0(); i <= event.getIndex1(); i++) {
                addJob(i, (Job)jobListModel.getElementAt(i), jobsPanel);
            }
            updateJobCount(jobListModel.getSize());
            jobsPanel.revalidate();
        }


        public void intervalRemoved(ListDataEvent event) {
            for (int i = event.getIndex1(); i >= event.getIndex0(); i--) {
                removeJob(i, jobsPanel);
            }
            updateJobCount(jobListModel.getSize());
            jobsPanel.revalidate();
        }


        public void contentsChanged(ListDataEvent event) {
            for (int i = event.getIndex0(); i <= event.getIndex1(); i++) {
                updateJob(i, (Job)jobListModel.getElementAt(i), jobsPanel);
            }
            jobsPanel.revalidate();
        }
    }
}
