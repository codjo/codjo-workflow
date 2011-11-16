package net.codjo.workflow.server.organiser;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 */
public class RuleEngineMock extends RuleEngine {
    private final LogString log;
    private final List<Job> ruleJobs = new ArrayList<Job>();
    private final List<String> rejectedJobs = new ArrayList<String>();
    private final List<String> runningJobs = new ArrayList<String>();
    private final List<String> errorJobs = new ArrayList<String>();
    private final Map<String, List<String>> rejectOnInsert = new HashMap<String, List<String>>();
    private final Map<String, List<String>> triggerOnRetract = new HashMap<String, List<String>>();


    public RuleEngineMock(LogString log) {
        this.log = log;
    }


    @Override
    public void addRulesFile(URL rulesUrl) {
        log.call("addRulesFile", rulesUrl);
        super.addRulesFile(rulesUrl);
    }


    @Override
    public void start() throws Exception {
        log.call("start");
    }


    @Override
    public void insert(Job job) {
        String jobType = job.getType();
        ruleJobs.add(job);
        if (runningJobs.contains(jobType)) {
            job.setState(State.RUNNING);
        } else if (rejectedJobs.contains(jobType)) {
            job.setState(State.REJECTED);
        } else {
            job.setState(State.WAITING);
        }

        List<String> targetJobs = rejectOnInsert.get(job.getType());
        if (targetJobs != null) {
            for (String targetJob : targetJobs) {
                rejectedJobs.add(targetJob);
            }

            for (Job ruleJob : ruleJobs) {
                if (targetJobs.contains(ruleJob.getType())) {
                    ruleJob.setState(State.NEW);
                    ruleJob.setState(State.REJECTED);
                }
            }
        }

        log.call("insert", jobType);
        if (errorJobs.contains(jobType)) {
            throw new RuntimeException(String.format("exception with %s job !!!", jobType));
        }
    }


    @Override
    public void retract(Job job) {
        String jobType = job.getType();
        ruleJobs.remove(job);

        List<String> targetJobs = triggerOnRetract.get(job.getType());
        if (targetJobs != null) {
            for (String targetJob : targetJobs) {
                runningJobs.add(targetJob);
            }

            for (Job ruleJob : ruleJobs) {
                if (targetJobs.contains(ruleJob.getType())) {
                    ruleJob.setState(State.RUNNING);
                }
            }
        }

        log.call("retract", job.getType());
        if (errorJobs.contains(jobType)) {
            throw new RuntimeException(String.format("exception with job %s", jobType));
        }
    }


    @Override
    public List<Job> getAllJobs() {
        log.call("getAllJobs");
        return ruleJobs;
    }


    @Override
    public List<Job> getRunningJobs() {
        List<Job> jobs = new ArrayList<Job>();
        for (Job runningJob : ruleJobs) {
            if (runningJobs.contains(runningJob.getType())) {
                jobs.add(runningJob);
            }
        }
        return jobs;
    }


    @Override
    public List<Job> getRejectedJobs() {
        List<Job> jobs = new ArrayList<Job>();
        for (Job job : ruleJobs) {
            if (rejectedJobs.contains(job.getType())) {
                jobs.add(job);
            }
        }
        return jobs;
    }


    public void mockWillBeRunning(String jobType) {
        runningJobs.add(jobType);
    }


    public void mockWillBeRejected(String jobType) {
        rejectedJobs.add(jobType);
    }


    public void mockWillThrowException(String jobType) {
        errorJobs.add(jobType);
    }


    /**
     * ATTENTION! les jobs qui vont être rejetés doivent être en WAITING au moment
     * ou le job "trigger" est inseré.
     */
    public void insertJobWillReject(String trigger, String... targetJobs) {
        rejectOnInsert.put(trigger, Arrays.asList(targetJobs));
    }

    public void retractJobWillTrigger(String trigger, String... targetJobs) {
        triggerOnRetract.put(trigger, Arrays.asList(targetJobs));
    }
}
