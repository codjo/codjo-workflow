package net.codjo.workflow.gui.plugin;
import net.codjo.agent.AgentContainer;
import net.codjo.mad.gui.framework.AbstractAction;
import net.codjo.mad.gui.framework.GuiContext;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JInternalFrame;
/**
 *
 */
class WorkflowLogAction extends AbstractAction {
    private AgentContainer agentContainer;


    protected WorkflowLogAction(GuiContext ctxt, AgentContainer agentContainer) {
        super(ctxt, "Liste des logs", "Affiche la liste des logs de workflow");
        this.agentContainer = agentContainer;
        putValue(SMALL_ICON, loadActionIcon("console.png"));
    }


    @Override
    protected JInternalFrame buildFrame(GuiContext ctxt) throws Exception {
        return new WorkflowLogLogic(ctxt, agentContainer).getGui();
    }


    private Icon loadActionIcon(String fileName) {
        URL resource = getClass().getResource(fileName);
        if (resource != null) {
            return new ImageIcon(resource);
        }
        else {
            return null;
        }
    }
}
