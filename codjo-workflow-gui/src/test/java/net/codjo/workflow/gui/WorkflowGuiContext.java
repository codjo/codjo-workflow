package net.codjo.workflow.gui;
import net.codjo.i18n.common.Language;
import net.codjo.i18n.common.TranslationManager;
import net.codjo.i18n.gui.TranslationNotifier;
import net.codjo.mad.gui.MadGuiContext;
import net.codjo.mad.gui.framework.DefaultGuiContext;
import net.codjo.mad.gui.i18n.InternationalizationUtil;

public class WorkflowGuiContext extends DefaultGuiContext {
    public WorkflowGuiContext() {
        MadGuiContext context = new MadGuiContext();

        TranslationManager translationManager = InternationalizationUtil.retrieveTranslationManager(context);
        TranslationNotifier translationNotifier
              = InternationalizationUtil.retrieveTranslationNotifier(context);

        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.FR);
        translationManager.addBundle("net.codjo.workflow.gui.i18n", Language.EN);

        putProperty(TranslationManager.TRANSLATION_MANAGER_PROPERTY, translationManager);
        putProperty(TranslationNotifier.TRANSLATION_NOTIFIER_PROPERTY, translationNotifier);
    }
}
