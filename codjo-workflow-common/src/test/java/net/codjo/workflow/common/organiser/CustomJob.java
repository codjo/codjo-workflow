package net.codjo.workflow.common.organiser;
/**
 *
 */
public class CustomJob extends Job {
    private String customField;


    public CustomJob(Object id, String type) {
        super(id, type);
    }


    public String getCustomField() {
        return customField;
    }


    public void setCustomField(String customField) {
        this.customField = customField;
    }
}
