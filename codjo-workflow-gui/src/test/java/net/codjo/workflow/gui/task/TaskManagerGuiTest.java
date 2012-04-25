package net.codjo.workflow.gui.task;
import java.util.Date;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.junit.Test;
import org.uispec4j.Panel;
import org.uispec4j.Window;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
/**
 *
 */
public class TaskManagerGuiTest {

    @Test
    public void test_alwaysOnTop() throws Exception {
        assertTrue(new TaskManagerGui(createTaskManagerConfiguration(),
                                      new DefaultListModel(),
                                      new DefaultListModel(),
                                      new DefaultListModel()).isAlwaysOnTop());
    }


    @Test
    public void test_componentPrefix() throws Exception {
        DefaultListModel waitingJobsModel = new DefaultListModel();
        waitingJobsModel.addElement(createJob("importWaiting"));
        waitingJobsModel.addElement(createJob("segmentationWaiting"));

        DefaultListModel runningJobsModel = new DefaultListModel();
        runningJobsModel.addElement(createJob("importRunning"));
        runningJobsModel.addElement(createJob("segmentationRunning"));

        DefaultListModel doneJobsModel = new DefaultListModel();
        doneJobsModel.addElement(createJob("segmentationDone"));
        doneJobsModel.addElement(createJob("segmentationDone"));

        TaskManagerConfiguration configuration = createTaskManagerConfiguration();
        configuration.setUserLogin("MyLogin");
        TaskManagerGui taskManagerGui = new TaskManagerGui(configuration,
                                                           waitingJobsModel,
                                                           runningJobsModel,
                                                           doneJobsModel);

        Panel jobsPanel = new Window(taskManagerGui).getPanel("jobs");

        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "waiting.0.date"));
        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "waiting.1.date"));
        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "running.0.date"));
        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "running.1.date"));
        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "done.0.date"));
        assertNotNull(jobsPanel.findSwingComponent(JLabel.class, "done.1.date"));
    }


    @Test
    public void test_addToolBarButton() throws Exception {
        TaskManagerGui managerGui = new TaskManagerGui(createTaskManagerConfiguration(),
                                                       new TaskManagerListModel(5));

        JButton button = new JButton("mon bouton");
        button.setName("myButton");
        managerGui.addToolBarButton(button, null);
        new Window(managerGui).getPanel("actionPanel").getButton("myButton");
    }


    private TaskManagerConfiguration createTaskManagerConfiguration() {
        TaskManagerConfiguration configuration = new TaskManagerConfiguration();
        configuration.setGuiContext(new WorkflowGuiContext());
        return configuration;
    }


    private Job createJob(String type) {
        Job job = JobMock.create(type, State.NEW);
        job.setDate(new Date());
        return job;
    }
}
