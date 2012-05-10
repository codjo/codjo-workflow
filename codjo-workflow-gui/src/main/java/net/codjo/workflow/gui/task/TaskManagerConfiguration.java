package net.codjo.workflow.gui.task;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import net.codjo.mad.gui.framework.GuiContext;
/**
 *
 */
public class TaskManagerConfiguration {
    private String userLogin;
    private final Map<String, Icon> jobTypeToIcon = new HashMap<String, Icon>();
    private GuiContext guiContext;


    public Icon getJobIcon(String jobType) {
        return jobTypeToIcon.get(jobType);
    }


    public void setJobIcon(String jobType, Icon icon) {
        jobTypeToIcon.put(jobType, icon);
    }


    public String getUserLogin() {
        return userLogin;
    }


    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }


    public void setGuiContext(GuiContext guiContext) {
        this.guiContext = guiContext;
    }


    public GuiContext getGuiContext() {
        return guiContext;
    }
}
