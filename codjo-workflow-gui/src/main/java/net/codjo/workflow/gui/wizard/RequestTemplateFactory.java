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
public interface RequestTemplateFactory {
    JobRequestTemplate createTemplate(final Map wizardState);
}
