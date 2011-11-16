package net.codjo.workflow.gui.task;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.common.organiser.JobMock;
import net.codjo.workflow.gui.task.TaskManagerListModel.Filter;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import junit.extensions.jfcunit.JFCTestCase;
import org.junit.Test;

public class TaskManagerListModelTest extends JFCTestCase {
    private TaskManagerListModel taskManagerListModel = new TaskManagerListModel(2);
    private LogString log = new LogString();


    public void test_elementAt() throws Exception {
        JobMock job1 = createJob("id01", State.NEW);
        waitForJobReceived(job1);
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(1, taskManagerListModel.getWaitingJobs().getSize());

        JobMock job2 = createJob("id02", State.NEW);
        waitForJobReceived(job2);
        assertEquals(job2, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(1));
        assertEquals(2, taskManagerListModel.getWaitingJobs().getSize());

        JobMock job3 = createJob("id03", State.NEW);
        waitForJobReceived(job3);
        assertEquals(job3, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(job2, taskManagerListModel.getWaitingJobs().getElementAt(1));
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(2));
        assertEquals(3, taskManagerListModel.getWaitingJobs().getSize());

        job2 = createJob("id02", State.WAITING);
        waitForJobReceived(job2);
        assertEquals(job3, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(job2, taskManagerListModel.getWaitingJobs().getElementAt(1));
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(2));
        assertEquals(3, taskManagerListModel.getWaitingJobs().getSize());

        job3 = createJob("id03", State.WAITING);
        waitForJobReceived(job3);
        assertEquals(job3, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(job2, taskManagerListModel.getWaitingJobs().getElementAt(1));
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(2));
        assertEquals(3, taskManagerListModel.getWaitingJobs().getSize());

        job3 = createJob("id03", State.RUNNING);
        waitForJobReceived(job3);
        assertEquals(job3, taskManagerListModel.getRunningJobs().getElementAt(0));
        assertEquals(job2, taskManagerListModel.getWaitingJobs().getElementAt(0));
        assertEquals(job1, taskManagerListModel.getWaitingJobs().getElementAt(1));
        assertEquals(1, taskManagerListModel.getRunningJobs().getSize());
        assertEquals(2, taskManagerListModel.getWaitingJobs().getSize());
    }


    @Test
    public void test_addFilter() throws Exception {
        taskManagerListModel.addFilter(new Filter() {
            public boolean hideRow(Job job) {
                return State.DONE.equals(job.getState());
            }
        });

        JobMock jobMock = createJob("1", State.FAILURE);
        jobMock.setInitiator("Moi");
        waitForJobReceived(jobMock);
        jobMock = createJob("2", State.DONE);
        jobMock.setInitiator("Moi");
        waitForJobReceived(jobMock);
        waitForJobReceived(createJob("3", State.WAITING));
        waitForJobReceived(createJob("4", State.DONE));
        waitForJobReceived(createJob("5", State.RUNNING));

        assertEquals(1, taskManagerListModel.getRunningJobs().getSize());
        assertEquals(1, taskManagerListModel.getWaitingJobs().getSize());
        assertEquals(1, taskManagerListModel.getDoneJobs().getSize());

        taskManagerListModel.addFilter(new Filter() {
            public boolean hideRow(Job job) {
                return !"Moi".equals(job.getInitiator());
            }
        });

        assertEquals(0, taskManagerListModel.getRunningJobs().getSize());
        assertEquals(0, taskManagerListModel.getWaitingJobs().getSize());
        assertEquals(1, taskManagerListModel.getDoneJobs().getSize());
    }


    @Test
    public void test_removeFilter() throws Exception {
        Filter filter = new Filter() {
            public boolean hideRow(Job job) {
                return !"Moi".equals(job.getInitiator());
            }
        };
        taskManagerListModel.addFilter(filter);
        JobMock jobMock = createJob("1", State.DONE);
        jobMock.setInitiator("Moi");
        waitForJobReceived(jobMock);
        waitForJobReceived(createJob("2", State.WAITING));
        waitForJobReceived(createJob("3", State.FAILURE));
        waitForJobReceived(createJob("4", State.RUNNING));

        taskManagerListModel.removeFilter(filter);
        assertEquals(1, taskManagerListModel.getRunningJobs().getSize());
        assertEquals(1, taskManagerListModel.getWaitingJobs().getSize());
        assertEquals(2, taskManagerListModel.getDoneJobs().getSize());
    }


    @Test
    public void test_applyFilter() throws Exception {
        taskManagerListModel.getWaitingJobs()
              .addListDataListener(new MyListDataListener(new LogString("waitingJob", log)));

        waitForJobReceived(createJob("id01", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)");

        waitForJobReceived(createJob("id02", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)", "waitingJob.contentsChanged(1, 1)");

        waitForJobReceived(createJob("id03", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)", "waitingJob.contentsChanged(1, 2)");

        Filter filter = new Filter() {
            public boolean hideRow(Job job) {
                return "id02".equals(job.getId());
            }
        };
        taskManagerListModel.addFilter(filter);

        log.assertAndClear("waitingJob.intervalRemoved(1, 1)", "waitingJob.contentsChanged(1, 1)");

        taskManagerListModel.removeFilter(filter);

        log.assertAndClear("waitingJob.intervalAdded(1, 1)", "waitingJob.contentsChanged(2, 2)");
    }


    public void test_maxDoneJobs() throws Exception {
        JobMock job1 = createJob("id01", State.DONE);
        waitForJobReceived(job1);
        JobMock job02 = createJob("id02", State.RUNNING);
        waitForJobReceived(job02);
        JobMock job03 = createJob("id03", State.DONE);
        waitForJobReceived(job03);
        JobMock job04 = createJob("id04", State.FAILURE);
        waitForJobReceived(job04);

        assertEquals(1, taskManagerListModel.getRunningJobs().getSize());
        assertEquals(job02, taskManagerListModel.getRunningJobs().getElementAt(0));
        assertEquals(2, taskManagerListModel.getDoneJobs().getSize());
        assertEquals(job04, taskManagerListModel.getDoneJobs().getElementAt(0));
        assertEquals(job03, taskManagerListModel.getDoneJobs().getElementAt(1));
    }


    public void test_jobReceived() throws Exception {
        taskManagerListModel.getWaitingJobs()
              .addListDataListener(new MyListDataListener(new LogString("waitingJob", log)));
        taskManagerListModel.getRunningJobs()
              .addListDataListener(new MyListDataListener(new LogString("runningJob", log)));
        taskManagerListModel.getDoneJobs()
              .addListDataListener(new MyListDataListener(new LogString("doneJob", log)));

        waitForJobReceived(createJob("id01", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)");

        waitForJobReceived(createJob("id02", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)", "waitingJob.contentsChanged(1, 1)");

        waitForJobReceived(createJob("id03", State.NEW));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)", "waitingJob.contentsChanged(1, 2)");

        waitForJobReceived(createJob("id04", State.WAITING));
        log.assertAndClear("waitingJob.intervalAdded(0, 0)", "waitingJob.contentsChanged(1, 3)");

        waitForJobReceived(createJob("id05", State.RUNNING));
        log.assertAndClear("runningJob.intervalAdded(0, 0)");

        waitForJobReceived(createJob("id01", State.WAITING));
        log.assertAndClear("waitingJob.contentsChanged(3, 3)");

        waitForJobReceived(createJob("id01", State.RUNNING));
        log.assertAndClear("waitingJob.intervalRemoved(3, 3)",
                           "runningJob.intervalAdded(0, 0)",
                           "runningJob.contentsChanged(1, 1)");

        waitForJobReceived(createJob("id02", State.DONE));
        log.assertAndClear("waitingJob.intervalRemoved(2, 2)",
                           "doneJob.intervalAdded(0, 0)");
//9
        waitForJobReceived(createJob("id03", State.DONE));
        log.assertAndClear("waitingJob.intervalRemoved(1, 1)",
                           "doneJob.intervalAdded(0, 0)",
                           "doneJob.contentsChanged(1, 1)");

        waitForJobReceived(createJob("id04", State.DONE));
        log.assertAndClear("waitingJob.intervalRemoved(0, 0)",
                           "doneJob.intervalAdded(0, 0)",
                           "doneJob.contentsChanged(1, 2)",
                           "doneJob.intervalRemoved(2, 2)");

        waitForJobReceived(createJob("id01", State.DONE));
        log.assertAndClear("runningJob.intervalRemoved(0, 0)",
                           "runningJob.contentsChanged(0, 0)",
                           "doneJob.intervalAdded(0, 0)",
                           "doneJob.contentsChanged(1, 2)",
                           "doneJob.intervalRemoved(2, 2)");
    }


    private void waitForJobReceived(JobMock job) {
        taskManagerListModel.jobReceived(job);
        awtSleep();
    }


    private JobMock createJob(String id, State state) {
        return JobMock.create(id, "mock", state);
    }


    private static class MyListDataListener implements ListDataListener {
        private LogString log;


        private MyListDataListener(LogString log) {
            this.log = log;
        }


        public void intervalAdded(ListDataEvent e) {
            log.call("intervalAdded", e.getIndex0(), e.getIndex1());
        }


        public void intervalRemoved(ListDataEvent e) {
            log.call("intervalRemoved", e.getIndex0(), e.getIndex1());
        }


        public void contentsChanged(ListDataEvent e) {
            log.call("contentsChanged", e.getIndex0(), e.getIndex1());
        }
    }
}
