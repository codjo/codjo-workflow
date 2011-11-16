package net.codjo.workflow.gui.task;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import net.codjo.workflow.gui.task.TaskManagerAgent.Callback;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

public class TaskManagerListModel implements Callback {
    private final int maxFinishedJobs;
    private final Map<Object, JobListModel> idToState = new HashMap<Object, JobListModel>();

    private final JobListModel waitingJobs = new JobListModel();
    private final JobListModel runningJobs = new JobListModel();
    private final JobListModel doneJobs = new MaxJobListModel();


    public TaskManagerListModel(int maxFinishedJobs) {
        this.maxFinishedJobs = maxFinishedJobs;
    }


    public void jobReceived(final Job job) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                doUpdateModel(job);
            }
        });
    }


    public void addFilter(Filter filter) {
        runningJobs.addFilter(filter);
        waitingJobs.addFilter(filter);
        doneJobs.addFilter(filter);
    }


    public void removeFilter(Filter filter) {
        runningJobs.removeFilter(filter);
        waitingJobs.removeFilter(filter);
        doneJobs.removeFilter(filter);
    }


    public void clearDone() {
        Iterator<Entry<Object, JobListModel>> entryIterator = idToState.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Entry entry = entryIterator.next();
            if (entry.getValue().equals(doneJobs)) {
                entryIterator.remove();
            }
        }
        doneJobs.clear();
    }


    private void doUpdateModel(Job job) {
        JobListModel destinationModel = getStatusModel(job.getState());
        if (idToState.containsKey(job.getId()) && destinationModel.equals(idToState.get(job.getId()))) {
            idToState.get(job.getId()).updateElement(job);
        }
        else {

            if (idToState.containsKey(job.getId()) && !destinationModel.equals(idToState.get(job.getId()))) {
                idToState.get(job.getId()).removeElement(job.getId());
            }

            idToState.put(job.getId(), destinationModel);
            destinationModel.addElement(job);
        }
    }


    private JobListModel getStatusModel(State jobState) {
        switch (jobState) {
            case WAITING:
            case NEW:
                return waitingJobs;
            case RUNNING:
                return runningJobs;
            case REJECTED:
            case DONE:
            case FAILURE:
                return doneJobs;
            default:
                return null;
        }
    }


    public ListModel getWaitingJobs() {
        return waitingJobs;
    }


    public ListModel getRunningJobs() {
        return runningJobs;
    }


    public ListModel getDoneJobs() {
        return doneJobs;
    }


    private class JobListModel extends AbstractListModel {
        protected final List<Object> jobsId = new ArrayList<Object>();
        protected final List<Object> filteredJobsId = new ArrayList<Object>();
        protected final Map<Object, Job> jobs = new HashMap<Object, Job>();

        private final List<Filter> filters = new ArrayList<Filter>();


        public int getSize() {
            return filteredJobsId.size();
        }


        public Object getElementAt(int index) {
            return jobs.get(filteredJobsId.get(index));
        }


        public void addElement(Job job) {
            Object jobId = job.getId();
            jobs.put(jobId, job);
            jobsId.add(0, jobId);
            if (passFilter(job)) {
                doAddElement(0, jobId);
            }
        }


        public void removeElement(Object id) {
            int index = filteredJobsId.indexOf(id);
            jobs.remove(id);
            jobsId.remove(id);
            doRemoveElement(id, index);
        }


        public void updateElement(Job job) {
            jobs.put(job.getId(), job);
            int index = filteredJobsId.indexOf(job.getId());
            fireContentsChanged(this, index, index);
        }


        public void addFilter(Filter filter) {
            filters.add(filter);
            applyFilters();
        }


        public void removeFilter(Filter filter) {
            filters.remove(filter);
            applyFilters();
        }


        private void applyFilters() {
            for (Object id : jobsId) {
                Job job = jobs.get(id);

                if (!passFilter(job) && filteredJobsId.contains(id)) {
                    doRemoveElement(id, filteredJobsId.indexOf(id));
                }
                else if (passFilter(job) && !filteredJobsId.contains(id)) {
                    doAddElement(jobsId.indexOf(id), id);
                }
            }
        }


        private void clear() {
            int size = filteredJobsId.size();
            filteredJobsId.clear();
            jobsId.clear();
            jobs.clear();
            if (size > 0) {
                fireIntervalRemoved(this, 0, size - 1);
            }
        }


        private void doAddElement(int index, Object id) {
            filteredJobsId.add(index, id);

            fireIntervalAdded(this, index, index);
            if (filteredJobsId.size() - 1 >= index + 1) {
                fireContentsChanged(this, index + 1, filteredJobsId.size() - 1);
            }
        }


        private void doRemoveElement(Object id, int index) {
            filteredJobsId.remove(id);

            fireIntervalRemoved(this, index, index);
            if (filteredJobsId.size() - 1 >= index) {
                fireContentsChanged(this, index, filteredJobsId.size() - 1);
            }
        }


        private boolean passFilter(Job job) {
            for (Filter filter : filters) {
                if (filter.hideRow(job)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class MaxJobListModel extends JobListModel {

        @Override
        public void addElement(Job obj) {
            super.addElement(obj);
            if (getSize() > maxFinishedJobs) {
                Object id = jobsId.get(jobsId.size() - 1);
                removeElement(id);
                idToState.remove(id);
            }
        }
    }

    public interface Filter {
        public boolean hideRow(Job job);
    }
}
