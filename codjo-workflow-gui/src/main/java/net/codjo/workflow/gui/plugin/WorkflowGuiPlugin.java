package net.codjo.workflow.gui.plugin;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerConfiguration;
import net.codjo.agent.util.IdUtil;
import net.codjo.i18n.common.Language;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.mad.gui.base.ButtonBuilder;
import net.codjo.mad.gui.base.GuiConfiguration;
import net.codjo.mad.gui.i18n.AbstractInternationalizableGuiPlugin;
import net.codjo.workflow.common.message.PurgeAuditJobRequest;
import net.codjo.workflow.gui.task.TaskManagerAction;
import net.codjo.workflow.gui.task.TaskManagerAgent;
import net.codjo.workflow.gui.task.TaskManagerConfiguration;
import net.codjo.workflow.gui.task.TaskManagerListModel;

public final class WorkflowGuiPlugin extends AbstractInternationalizableGuiPlugin {
    public static final String LOGIN_PARAMETER = "login";
    private final WorkflowGuiPluginConfiguration configuration = new WorkflowGuiPluginConfiguration();
    private TaskManagerListModel taskManagerListModel;
    private String login;


    public WorkflowGuiPlugin() {
        configuration.setTaskManagerJobIcon(PurgeAuditJobRequest.PURGE_AUDIT_JOB_TYPE,
                                            new ImageIcon(getClass().getResource("/images/job.purge-audit.gif")));
    }


    @Override
    protected void registerLanguageBundles(TranslationManager translationManager) {
        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.FR);
        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.EN);
    }


    public WorkflowGuiPluginConfiguration getConfiguration() {
        return configuration;
    }


    @Override
    public void initContainer(ContainerConfiguration containerConfiguration) throws Exception {
        login = containerConfiguration.getParameter(LOGIN_PARAMETER);
    }


    @Override
    public void start(AgentContainer agentContainer) throws Exception {
        taskManagerListModel = new TaskManagerListModel(10);
        String agentName = String.format("taskManager-%s%s", login, IdUtil.createUniqueId(this));
        agentContainer.acceptNewAgent(agentName, new TaskManagerAgent(taskManagerListModel)).start();
    }


    @Override
    public void initGui(GuiConfiguration guiConfiguration) throws Exception {
        super.initGui(guiConfiguration);
        guiConfiguration.registerAction(this,
                                        "ConsoleAction",
                                        new ConsoleAction(guiConfiguration.getGuiContext()));

        configuration.taskManagerConfiguration.setUserLogin(login);
        configuration.taskManagerConfiguration.setGuiContext(guiConfiguration.getGuiContext());
        guiConfiguration.addToStatusBar(
              new ButtonBuilder(guiConfiguration.getGuiContext(),
                                "taskManager",
                                new TaskManagerAction(configuration.taskManagerConfiguration, taskManagerListModel),
                                null,
                                "TaskManagerAction.tooltip"));
    }


    public class WorkflowGuiPluginConfiguration {
        private final TaskManagerConfiguration taskManagerConfiguration = new TaskManagerConfiguration();


        public void setTaskManagerJobIcon(String requestType, Icon icon) {
            taskManagerConfiguration.setJobIcon(requestType, icon);
        }
    }
}
