/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.util.Map;
/**
 *
 */
public interface VtomCaller {
    void call(Map wizardState) throws CommandFile.ExecuteException;
}
