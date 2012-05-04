package net.codjo.workflow.gui.task;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import java.awt.Container;
import java.util.Date;
import javax.swing.DefaultListModel;
import junit.framework.TestCase;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.junit.Test;
import org.uispec4j.Panel;
import org.uispec4j.assertion.UISpecAssert;
/**
 *
 */
public class JobListTest extends TestCase {

    @Test
    public void test_model() throws Exception {
        DefaultListModel listModel = new DefaultListModel();

        TaskManagerConfiguration configuration = new TaskManagerConfiguration();
        configuration.setGuiContext(new WorkflowGuiContext());
        configuration.setUserLogin("MyLogin");
        JobList jobList = new JobList(configuration,
                                      "testPanel",
                                      listModel,
                                      "jobList",
                                      null);

        Container jobsPanel;

        jobsPanel = new Panel(jobList.getMainPanel()).getPanel("testPanel").getAwtContainer();
        assertJobPanel(listModel, jobsPanel);
    }


    private void assertJobPanel(DefaultListModel listModel, Container jobsPanel) {
        listModel.addElement(createJob("import"));
        listModel.addElement(createJob("segmentation"));
        listModel.addElement(createJob("broadcast"));

        jobsPanel = (Container)jobsPanel.getComponent(0);
        jobsPanel = (Container)jobsPanel.getComponent(0);
        assertEquals(3, jobsPanel.getComponentCount());

        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(0)).containsLabel("import"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(1)).containsLabel("segmentation"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(2)).containsLabel("broadcast"));

        listModel.addElement(createJob("customType"));

        assertEquals(4, jobsPanel.getComponentCount());
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(3)).containsLabel("customType"));

        listModel.remove(1);

        assertEquals(3, jobsPanel.getComponentCount());
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(0)).containsLabel("import"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(1)).containsLabel("broadcast"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(2)).containsLabel("customType"));

        listModel.set(2, createJob("control"));

        assertEquals(3, jobsPanel.getComponentCount());
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(0)).containsLabel("import"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(1)).containsLabel("broadcast"));
        UISpecAssert.assertTrue(new Panel((Container)jobsPanel.getComponent(2)).containsLabel("control"));

        listModel.remove(0);
        listModel.remove(0);
        listModel.remove(0);

        assertEquals(0, jobsPanel.getComponentCount());
    }


    private Job createJob(String type) {
        JobMock job = JobMock.create(type, State.NEW);
        job.setDate(new Date());
        return job;
    }
}
