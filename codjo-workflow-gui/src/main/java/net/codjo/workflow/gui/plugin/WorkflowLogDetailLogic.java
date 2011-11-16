package net.codjo.workflow.gui.plugin;
import net.codjo.mad.client.request.RequestException;
import net.codjo.mad.gui.request.DetailDataSource;
import javax.swing.JInternalFrame;
/**
 *
 */
public class WorkflowLogDetailLogic {
    private WorkflowLogDetailGui gui;


    public WorkflowLogDetailLogic(DetailDataSource dataSource) throws RequestException {
        gui = new WorkflowLogDetailGui(dataSource);
        dataSource.load();
    }


    public JInternalFrame getGui() {
        return gui;
    }
}
