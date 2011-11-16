package net.codjo.workflow.gui.plugin;
import net.codjo.mad.gui.base.AbstractGuiPlugin;
import net.codjo.mad.gui.base.GuiConfiguration;
import net.codjo.mad.gui.i18n.AbstractInternationalizableGuiPlugin;
import net.codjo.plugin.common.ApplicationCore;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.i18n.common.Language;
/**
 * IHM des logs.
 */
public final class WorkflowAuditGuiPlugin extends AbstractInternationalizableGuiPlugin {
    private ApplicationCore applicationCore;


    public WorkflowAuditGuiPlugin(ApplicationCore applicationCore) {
        this.applicationCore = applicationCore;
    }


    @Override
    protected void registerLanguageBundles(TranslationManager translationManager) {
        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.FR);
        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.EN);
    }


    @Override
    public void initGui(GuiConfiguration guiConfiguration) throws Exception {
        super.initGui(guiConfiguration);
        guiConfiguration.registerAction(this, "WorkflowLogAction",
                                        new WorkflowLogAction(guiConfiguration.getGuiContext(),
                                                              applicationCore.getAgentContainer()));
    }
}
