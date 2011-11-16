package net.codjo.workflow.server.organiser;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.common.organiser.Job.State;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.drools.FactHandle;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.rule.Package;

public class RuleEngine {
    private final List<Job> jobs = new ArrayList<Job>();
    private final List<URL> rulesUrls = new ArrayList<URL>();
    private WorkingMemory workingMemory;


    public RuleEngine() {
    }


    public List<URL> getRuleFiles() {
        return rulesUrls;
    }


    public void addRulesFile(File rulesFile) {
        try {
            addRulesFile(rulesFile.toURL());
        }
        catch (MalformedURLException e) {
            throw new RuntimeException("Impossible de trouver l'url correspondant au fichier", e);
        }
    }


    public void addRulesFile(URL rulesUrl) {
        rulesUrls.add(rulesUrl);
    }


    public void checkRuleFiles() throws Exception {
        createRuleBase();
    }


    public void start() throws Exception {
        RuleBase rules = createRuleBase();
        workingMemory = rules.newStatefulSession();
    }


    public void insert(Job job) {
        checkStarted();
        if (State.NEW != job.getState()) {
            throw new IllegalArgumentException("Le job inséré doit avoir le statut NEW");
        }

        workingMemory.insert(job);
        workingMemory.fireAllRules();

        updateWorkingMemory();

        jobs.add(job);
    }


    public void retract(Job job) {
        checkStarted();

        workingMemory.retract(workingMemory.getFactHandle(job));
        workingMemory.fireAllRules();

        jobs.remove(job);

        updateWorkingMemory();
    }


    public List<Job> getAllJobs() {
        return getJobs(null);
    }


    public List<Job> getRunningJobs() {
        return getJobs(State.RUNNING);
    }


    public List<Job> getRejectedJobs() {
        return getJobs(State.REJECTED);
    }


    public List<Job> getJobs(State status) {
        checkStarted();

        List<Job> someJobs = new ArrayList<Job>();
        for (Iterator iterator = workingMemory.iterateObjects(); iterator.hasNext();) {
            @SuppressWarnings("unchecked")
            Job job = (Job)iterator.next();
            if (status == null || status == job.getState()) {
                someJobs.add(job);
            }
        }
        return someJobs;
    }


    private List<Job> getOrderedWaitingJobs() {
        List<Job> waitingJobs = new ArrayList<Job>();
        for (Job job : jobs) {
            if (job.getState() == State.WAITING) {
                waitingJobs.add(job);
            }
        }
        return waitingJobs;
    }


    private void updateWorkingMemory() {
        for (Job waitingJob : getOrderedWaitingJobs()) {
            waitingJob.setState(State.NEW);
            FactHandle factHandle = workingMemory.getFactHandle(waitingJob);
            workingMemory.update(factHandle, waitingJob);
            workingMemory.fireAllRules();
        }
    }


    private void checkStarted() {
        if (workingMemory == null) {
            throw new RuntimeException("Le moteur d'inférence doit être démarré");
        }
    }


    private RuleBase createRuleBase() throws Exception {
        Properties properties = new Properties();
        properties.put("drools.dialect.java.compiler", "JANINO");
        PackageBuilderConfiguration conf = new PackageBuilderConfiguration(properties);

        PackageBuilder builder = new PackageBuilder(conf);
        RuleBase ruleBase = RuleBaseFactory.newRuleBase();

        if (rulesUrls.isEmpty()) {
            URL resource = getClass().getResource("/META-INF/workflow.drl");
            if (resource == null) {
                resource = OrganiserAgent.class.getResource("defaultRules.drl");
            }
            rulesUrls.add(resource);
        }

        for (URL rulesUrl : rulesUrls) {
            try {
                builder.addPackageFromDrl(new InputStreamReader(rulesUrl.openStream()));
                Package pkg = builder.getPackage();
                ruleBase.addPackage(pkg);
            }
            catch (Exception e) {
                throw new Exception(String.format("Erreur lors du chargement du fichier de règles '%s' !!!",
                                                  extractFileName(rulesUrl)), e);
            }
        }

        return ruleBase;
    }


    private String extractFileName(URL rulesUrl) {
        String path = rulesUrl.getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
