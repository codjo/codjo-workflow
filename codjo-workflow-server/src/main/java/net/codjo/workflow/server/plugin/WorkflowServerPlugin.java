/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.ContainerFailureException;
import static net.codjo.agent.DFService.createAgentDescription;
import net.codjo.agent.UserId;
import net.codjo.mad.server.handler.AspectLauncher;
import net.codjo.mad.server.plugin.HandlerExecutor.HandlerExecutionMode;
import net.codjo.mad.server.plugin.MadServerPlugin;
import net.codjo.plugin.common.session.SessionListener;
import net.codjo.plugin.common.session.SessionManager;
import net.codjo.plugin.common.session.SessionRefusedException;
import net.codjo.plugin.server.AbstractServerPlugin;
import net.codjo.sql.server.JdbcServiceUtil;
import net.codjo.workflow.common.message.HandlerJobRequest;
import net.codjo.workflow.common.organiser.XmlCodec;
import net.codjo.workflow.common.schedule.ScheduleLeaderAgent;
import net.codjo.workflow.server.api.JobAgent;
import net.codjo.workflow.server.api.JobAgent.MODE;
import net.codjo.workflow.server.api.ResourcesManagerAgent;
import net.codjo.workflow.server.api.ResourcesManagerAgent.AgentFactory;
import net.codjo.workflow.server.aspect.AspectExecutorAgent;
import net.codjo.workflow.server.aspect.AspectLauncherFactory;
import net.codjo.workflow.server.aspect.ExecuteAspectRequest;
import net.codjo.workflow.server.aspect.WorkflowAspectBranchLauncherFactory;
import net.codjo.workflow.server.audit.AuditDao;
import net.codjo.workflow.server.audit.DiscriminentStringifier;
import net.codjo.workflow.server.audit.Stringifier;
import net.codjo.workflow.server.handler.DefaultHandlerContextManager;
import net.codjo.workflow.server.handler.HandlerJobAgent;
import net.codjo.workflow.server.handler.HandlerJobBuilder;
import net.codjo.workflow.server.handler.WorkflowHandlerExecutorFactory;
import net.codjo.workflow.server.leader.JobLeaderAgent;
import net.codjo.workflow.server.leader.JobLeaderSubscribeHandler;
import net.codjo.workflow.server.organiser.DefaultJobFactory;
import net.codjo.workflow.server.organiser.DescriptionJobBuilder;
import net.codjo.workflow.server.organiser.FilteredAuditDao;
import net.codjo.workflow.server.organiser.JobBuilder;
import net.codjo.workflow.server.organiser.OrganiserAgent;
import net.codjo.workflow.server.organiser.RuleEngine;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"OverlyCoupledClass"})
public class WorkflowServerPlugin extends AbstractServerPlugin {
    public static final String WORKFLOW_SCHEDULE_SERVICE = ScheduleLeaderAgent.WORKFLOW_SCHEDULE_SERVICE;
    private final WorkflowServerPluginConfiguration configuration = new WorkflowServerPluginConfiguration();
    private final WorkflowAspectBranchLauncherFactory factory = new WorkflowAspectBranchLauncherFactory();
    private DefaultJobFactory jobFactory = new DefaultJobFactory();
    private RuleEngine ruleEngine = new RuleEngine();
    private DefaultHandlerContextManager handlerContextManager;
    private WorkflowHandlerExecutorFactory handlerExecutorFactory;
    private MadServerPlugin madServerPlugin;
    private SessionManager sessionManager;


    public WorkflowServerPlugin() {
        configuration.registerJobFilter(HandlerJobRequest.HANDLER_JOB_TYPE);
        new ExecuteAspectStringifier().install(this);
        new PurgeAuditStringifier().install(this);
    }


    public WorkflowServerPlugin(final MadServerPlugin madServerPlugin, SessionManager sessionManager) {
        this();
        this.madServerPlugin = madServerPlugin;

        this.sessionManager = sessionManager;

        madServerPlugin.getConfiguration().setAspectBranchLauncherFactory(factory);
        getConfiguration().setAspectLauncherFactory(new AspectLauncherFactory() {
            public AspectLauncher create() {
                return madServerPlugin.getOperations().createAspectLauncher();
            }
        });
    }


    protected WorkflowServerPlugin(DefaultJobFactory jobFactory,
                                   RuleEngine ruleEngine,
                                   WorkflowHandlerExecutorFactory handlerExecutorFactory,
                                   DefaultHandlerContextManager handlerContextManager,
                                   SessionManager sessionManager) {
        this();

        this.jobFactory = jobFactory;
        this.ruleEngine = ruleEngine;
        this.handlerExecutorFactory = handlerExecutorFactory;
        this.handlerContextManager = handlerContextManager;
        this.sessionManager = sessionManager;
    }


    @Override
    public void initContainer(ContainerConfiguration containerConfiguration) throws Exception {
        if (configuration.enableHandlerExecution && madServerPlugin != null) {
            handlerContextManager = new DefaultHandlerContextManager();
            handlerExecutorFactory = new WorkflowHandlerExecutorFactory(handlerContextManager);
            madServerPlugin.getConfiguration().setHandlerExecutorFactory(handlerExecutorFactory,
                                                                         HandlerExecutionMode.ASYNCHRONOUS);
        }
    }


    @Override
    public void start(AgentContainer agentContainer) throws Exception {
        if (configuration.enableHandlerExecution && handlerExecutorFactory != null) {
            activateHandlerExecution(agentContainer, configuration.getJobFilter());
        }

        configuration.registerJobBuilder(
              new DescriptionJobBuilder(new DiscriminentStringifier(configuration.getDiscriminentStringifiers())),
              Integer.MIN_VALUE);
        configuration.registerJobBuilder(new HandlerJobBuilder());

        factory.setAgentContainer(agentContainer);

        JobLeaderSubscribeHandler jobLeaderSubscribeHandler = new JobLeaderSubscribeHandler();
        JobLeaderAgent jobLeaderAgent = new JobLeaderAgent(configuration.getAuditDao(),
                                                           jobLeaderSubscribeHandler);
        agentContainer.acceptNewAgent("job-leader-agent", jobLeaderAgent).start();

        ruleEngine.checkRuleFiles();
        OrganiserAgent organiserAgent = new OrganiserAgent(ruleEngine,
                                                           jobFactory,
                                                           configuration.getJobFilter(),
                                                           new XmlCodec(),
                                                           10,
                                                           jobLeaderSubscribeHandler);
        agentContainer.acceptNewAgent("organiser-agent", organiserAgent).start();

        if (getConfiguration().getAspectLauncherFactory() != null) {
            activateForkAspectEngine(agentContainer);
        }
    }


    public WorkflowServerPluginConfiguration getConfiguration() {
        return configuration;
    }


    private void activateHandlerExecution(AgentContainer agentContainer, List<String> filter)
          throws ContainerFailureException {
        sessionManager.addListener(new SessionListener() {
            public void handleSessionStart(UserId userId) throws SessionRefusedException {
            }


            public void handleSessionStop(UserId userId) {
                handlerContextManager.cleanUserContext(userId);
            }
        });

        handlerExecutorFactory.setAgentContainer(agentContainer);
        agentContainer.acceptNewAgent("handler-drh-agent", new ResourcesManagerAgent(new AgentFactory() {
            public JobAgent create() throws Exception {
                return new HandlerJobAgent(handlerContextManager, MODE.DELEGATE);
            }
        }, HandlerJobAgent.HANDLER_AGENT_DESCRIPTION)).start();

        configuration.setAuditDao(new FilteredAuditDao(configuration.getAuditDao(), filter));
    }


    private void activateForkAspectEngine(AgentContainer agentContainer) throws ContainerFailureException {
        agentContainer
              .acceptNewAgent("aspect-drh-agent",
                              new ResourcesManagerAgent(new AspectExecutorAgentFactory(),
                                                        createAgentDescription(ExecuteAspectRequest.JOB_ID)))
              .start();

        for (int i = 1; i <= 2; i++) {
            agentContainer
                  .acceptNewAgent("aspect-executor-agent-" + i,
                                  createAspectExecutorAgent(MODE.NOT_DELEGATE))
                  .start();
        }
    }


    private AspectExecutorAgent createAspectExecutorAgent(MODE mode) {
        return new AspectExecutorAgent(new JdbcServiceUtil(),
                                       configuration.getAspectLauncherFactory().create(),
                                       mode);
    }


    private class AspectExecutorAgentFactory implements AgentFactory {
        public JobAgent create() throws Exception {
            return createAspectExecutorAgent(MODE.DELEGATE);
        }
    }

    public class WorkflowServerPluginConfiguration {
        private final Map<String, Stringifier> discriminentStringifiers = new HashMap<String, Stringifier>();
        private List<String> jobFilter = new ArrayList<String>();
        private AuditDao auditDao = AuditDao.NULL;
        private AspectLauncherFactory aspectLauncherFactory;
        private boolean enableHandlerExecution = true;


        public List<String> getJobFilter() {
            return jobFilter;
        }


        public AuditDao getAuditDao() {
            return auditDao;
        }


        public void setAuditDao(AuditDao auditDao) {
            this.auditDao = auditDao;
        }


        public AspectLauncherFactory getAspectLauncherFactory() {
            return aspectLauncherFactory;
        }


        public void setAspectLauncherFactory(AspectLauncherFactory aspectLauncherFactory) {
            this.aspectLauncherFactory = aspectLauncherFactory;
        }


        public void registerJobBuilder(JobBuilder jobBuilder) {
            registerJobBuilder(jobBuilder, 0);
        }


        public void registerJobBuilder(JobBuilder jobBuilder, int priority) {
            WorkflowServerPlugin.this.jobFactory.register(jobBuilder, priority);
        }


        public void registerJobFilter(String newFilter) {
            jobFilter.add(newFilter);
        }


        public void addRulesFile(File ruleFile) {
            WorkflowServerPlugin.this.ruleEngine.addRulesFile(ruleFile);
        }


        public void addRulesFile(URL ruleFileUrl) {
            WorkflowServerPlugin.this.ruleEngine.addRulesFile(ruleFileUrl);
        }


        public Map<String, Stringifier> getDiscriminentStringifiers() {
            return Collections.unmodifiableMap(discriminentStringifiers);
        }


        public void setDiscriminentStringifier(String requestType, Stringifier stringifier) {
            discriminentStringifiers.put(requestType, stringifier);
        }


        public void enableHandlerExecution(boolean enabled) {
            this.enableHandlerExecution = enabled;
        }
    }
}
