package net.codjo.workflow.gui.task;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import junit.framework.Assert;
import net.codjo.i18n.common.Language;
import net.codjo.mad.gui.i18n.InternationalizationUtil;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import net.codjo.workflow.gui.WorkflowGuiContext;
import org.junit.Before;
import org.junit.Test;
import org.uispec4j.Window;
/**
 *
 */
public class TaskManagerLogicTest {
    private TaskManagerConfiguration taskManagerConfiguration;


    @Before
    public void setUp() throws Exception {
        taskManagerConfiguration = createTaskManagerConfiguration();
        taskManagerConfiguration.setUserLogin("MyLogin");
    }


    @Test
    public void test_clearDoneJobs() throws Exception {
        TaskManagerListModel listModel = new TaskManagerListModel(5);
        listModel.jobReceived(createJob(1, State.DONE));
        listModel.jobReceived(createJob(2, State.FAILURE));
        listModel.jobReceived(createJob(3, State.DONE));
        TaskManagerGui managerGui = new TaskManagerLogic(taskManagerConfiguration, listModel).getGui();

        new Window(managerGui).getPanel("actionPanel").getButton("clearDone").click();

        Assert.assertEquals(0, listModel.getDoneJobs().getSize());
    }


    @Test
    public void test_filterMyJobs() throws Exception {
        TaskManagerListModel listModel = new TaskManagerListModel(5);
        Job job = createJob(1, State.RUNNING);
        job.setInitiator("MyLogin");
        listModel.jobReceived(job);
        job = createJob(2, State.WAITING);
        job.setInitiator("Toi");
        listModel.jobReceived(job);
        TaskManagerGui managerGui = new TaskManagerLogic(taskManagerConfiguration, listModel).getGui();

        new Window(managerGui).getPanel("actionPanel").getToggleButton("filterInitiator").click();

        Assert.assertEquals(0, listModel.getWaitingJobs().getSize());
        Assert.assertEquals(1, listModel.getRunningJobs().getSize());
    }


    @Test
    public void test_filterDoneJobs() throws Exception {
        TaskManagerListModel listModel = new TaskManagerListModel(5);
        listModel.jobReceived(createJob(1, State.DONE));
        listModel.jobReceived(createJob(2, State.FAILURE));
        listModel.jobReceived(createJob(3, State.FAILURE));
        listModel.jobReceived(createJob(4, State.DONE));
        TaskManagerGui managerGui = new TaskManagerLogic(taskManagerConfiguration, listModel).getGui();

        new Window(managerGui).getPanel("actionPanel").getToggleButton("filterDone").click();
        Assert.assertEquals(2, listModel.getDoneJobs().getSize());

        new Window(managerGui).getPanel("actionPanel").getToggleButton("filterDone").click();
        Assert.assertEquals(4, listModel.getDoneJobs().getSize());
    }


    private static TaskManagerConfiguration createTaskManagerConfiguration() {
        TaskManagerConfiguration configuration = new TaskManagerConfiguration();
        configuration.setGuiContext(new WorkflowGuiContext());
        return configuration;
    }


    private static Job createJob(int jobIndex, State state) {
        JobMock job1 = JobMock.create(String.format("jobID%s", jobIndex),
                                      String.format("Job %s", jobIndex),
                                      state);
        job1.setDate(new Date());
        job1.setInitiator("user_tr");
        job1.setDescription("description");
        return job1;
    }

    

    public static void main(String[] args) throws ParseException {
        System.setProperty("sun.awt.noerasebackground", "true");

        TaskManagerListModel taskManagerModel = new TaskManagerListModel(5);

        TaskManagerConfiguration configuration = TaskManagerLogicTest.createTaskManagerConfiguration();
        InternationalizationUtil.retrieveTranslationNotifier(configuration.getGuiContext()).setLanguage(Language.EN);
        configuration.setUserLogin("user_tr");
        TaskManagerGui gui = new TaskManagerLogic(configuration, taskManagerModel).getGui();
        gui.setLocationRelativeTo(null);
        gui.setVisible(true);

        Job job;
        Calendar cal = GregorianCalendar.getInstance();

        job = createJob(0, State.DONE);
        job.setDate(cal.getTime());
        job.setInitiator("plop");
        taskManagerModel.jobReceived(job);

        cal.add(Calendar.MINUTE, 10);
        job = createJob(1, State.FAILURE);
        job.setErrorMessage("NullPointerException\nimpossible de trouver la base de données...");
        job.setDate(cal.getTime());
        taskManagerModel.jobReceived(job);

        cal.add(Calendar.SECOND, 10);
        job = createJob(2, State.RUNNING);
        job.setDate(cal.getTime());
        job.setDescription("description");
        taskManagerModel.jobReceived(job);

        cal.add(Calendar.HOUR, 1);
        job = createJob(3, State.WAITING);
        job.setDate(cal.getTime());
        job.setInitiator("plop");
        taskManagerModel.jobReceived(job);

        cal.add(Calendar.MINUTE, 10);
        job = createJob(4, State.WAITING);
        job.setDate(cal.getTime());
        taskManagerModel.jobReceived(job);
    }
}
