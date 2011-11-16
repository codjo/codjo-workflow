package net.codjo.workflow.gui.task;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import java.util.Date;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import org.uispec4j.Panel;
import org.uispec4j.assertion.UISpecAssert;

public class JobPanelTest {
    private TaskManagerConfiguration taskManagerConfiguration = new TaskManagerConfiguration();
    private Job job = createJob("import");
    private JobPanel jobPanel;
    private Panel panel;


    @Before
    public void setUp() throws Exception {
        job.setDate(new Date());
        taskManagerConfiguration.setUserLogin("UserLogin");
        jobPanel = new JobPanel(taskManagerConfiguration, job);
        panel = new Panel(jobPanel);
    }


    @Test
    public void test_updateJob() throws Exception {
        job.setDate(JobPanel.SIMPLE_DATE_FORMAT.parse("01/01 12:21:01"));
        job.setDescription("ma description");
        job.setInitiator("Moi");
        job.setState(State.WAITING);

        jobPanel.updateFromJob(job);

        UISpecAssert.assertTrue(panel.getTextBox("date").textEquals("01/01 12:21:01"));
        UISpecAssert.assertTrue(panel.getTextBox("type").textEquals("import"));
        UISpecAssert.assertTrue(panel.getTextBox("typeIcon").iconEquals(JobPanel.DEFAULT_TYPE_ICON));
        UISpecAssert.assertTrue(panel.getTextBox("description").textEquals("ma description"));
        UISpecAssert.assertTrue(panel.getTextBox("initiator").textEquals("Moi"));
        UISpecAssert.assertTrue(panel.getTextBox("statusIcon").iconEquals(JobPanel.WAITING_ICON));
    }


    @Test
    public void test_myJob() throws Exception {
        taskManagerConfiguration.setUserLogin("MyLogin");
        job.setInitiator("MyLogin");
        UISpecAssert.assertTrue(panel.getTextBox("initiator").iconEquals(null));

        jobPanel.updateFromJob(job);
        UISpecAssert.assertTrue(panel.getTextBox("initiator").iconEquals(JobPanel.USER_ICON));
    }


    @Test
    public void test_statusIcon() throws Exception {
        assertStatusIcon(JobPanel.WAITING_ICON, State.NEW);
        assertStatusIcon(JobPanel.REJECTED_ICON, State.REJECTED);
        assertStatusIcon(JobPanel.WAITING_ICON, State.WAITING);
        assertStatusIcon(JobPanel.RUNNING_ICON, State.RUNNING);
        assertStatusIcon(JobPanel.DONE_ICON, State.DONE);
        assertStatusIcon(JobPanel.FAILURE_ICON, State.FAILURE);

        for (State state : State.values()) {
            JobMock jobMock = JobMock.create("import", state);
            jobMock.setDate(new Date());

            jobPanel.updateFromJob(jobMock);

            assertNotNull(((JLabel)panel.getTextBox("statusIcon").getAwtComponent()).getIcon());
        }
    }


    @Test
    public void test_setComponentPrefix() throws Exception {
        assertNotNull(panel.findSwingComponent(JLabel.class, "date"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "type"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "typeIcon"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "description"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "initiator"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "statusIcon"));

        jobPanel.setComponentPrefix(null);

        assertNotNull(panel.findSwingComponent(JLabel.class, "date"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "type"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "typeIcon"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "description"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "initiator"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "statusIcon"));

        jobPanel.setComponentPrefix("");

        assertNotNull(panel.findSwingComponent(JLabel.class, "date"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "type"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "typeIcon"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "description"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "initiator"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "statusIcon"));

        jobPanel.setComponentPrefix("prefix");

        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.date"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.type"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.typeIcon"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.description"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.initiator"));
        assertNotNull(panel.findSwingComponent(JLabel.class, "prefix.statusIcon"));
    }


    @Test
    public void test_descriptionToolTip() throws Exception {
        job.setDescription("une super description");

        jobPanel.updateFromJob(job);

        UISpecAssert.assertTrue(panel.getTextBox("description").textEquals("une super description"));
        assertEquals("une super description",
                     ((JLabel)panel.getTextBox("description").getAwtComponent()).getToolTipText());
    }


    @Test
    public void test_errorMessageToolTip() throws Exception {
        JobMock jobMock = JobMock.create("import", State.FAILURE);
        jobMock.setDate(new Date());
        jobMock.setErrorMessage("message d'erreur");

        jobPanel.updateFromJob(jobMock);

        assertEquals("message d'erreur",
                     ((JLabel)panel.getTextBox("statusIcon").getAwtComponent()).getToolTipText());
        assertEquals("message d'erreur", ((JTextPane)panel.getTextBox("error").getAwtComponent()).getText());
    }


    @Test
    public void test_jobIcon() throws Exception {
        ImageIcon importIcon = new ImageIcon();
        taskManagerConfiguration.setJobIcon("import", importIcon);
        ImageIcon broadcastIcon = new ImageIcon();
        taskManagerConfiguration.setJobIcon("broadcast", broadcastIcon);

        UISpecAssert.assertTrue(panel.getTextBox("typeIcon").iconEquals(JobPanel.DEFAULT_TYPE_ICON));

        jobPanel.updateFromJob(createJob("import"));
        UISpecAssert.assertTrue(panel.getTextBox("typeIcon").iconEquals(importIcon));

        jobPanel.updateFromJob(createJob("broadcast"));
        UISpecAssert.assertTrue(panel.getTextBox("typeIcon").iconEquals(broadcastIcon));

        jobPanel.updateFromJob(createJob("unknown"));
        UISpecAssert.assertTrue(panel.getTextBox("typeIcon").iconEquals(JobPanel.DEFAULT_TYPE_ICON));
    }


    private void assertStatusIcon(Icon expectedIcon, State jobState) {
        JobMock jobMock = JobMock.create("import", jobState);
        jobMock.setDate(new Date());
        jobPanel.updateFromJob(jobMock);

        UISpecAssert.assertTrue(panel.getTextBox("statusIcon").iconEquals(expectedIcon));
    }


    private JobMock createJob(String type) {
        JobMock jobMock = JobMock.create(type, State.NEW);
        jobMock.setDate(new Date());
        return jobMock;
    }
}
