/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.workflow.common.message.JobRequestTemplate;
import java.util.Map;
/**
 *
 */
public class DefaultRequestTemplateFactory implements RequestTemplateFactory {
    private final JobRequestTemplate template;


    public DefaultRequestTemplateFactory(JobRequestTemplate template) {
        this.template = template;
    }


    public JobRequestTemplate createTemplate(final Map wizardState) {
        return template;
    }
}
