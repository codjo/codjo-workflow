/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.io.Serializable;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
/**
 *
 */
public class WizardUtil {
    private WizardUtil() {
    }


    public static Map<String, Serializable> createBroadcastState(String fileName, String date) {
        Map<String, Serializable> wizardState = new HashMap<String, Serializable>();
        wizardState.put(WizardConstants.BROADCAST_FILE_NAME, fileName);
        wizardState.put(WizardConstants.BROADCAST_DATE, Date.valueOf(date));
        return wizardState;
    }


    public static Map<String, String> createImportState(String fileName, String fileType, String inbox) {
        Map<String, String> wizardState = new HashMap<String, String>();
        wizardState.put(WizardConstants.IMPORT_FILE_PATH, fileName);
        wizardState.put(WizardConstants.IMPORT_TYPE, fileType);
        wizardState.put(WizardConstants.IMPORT_INBOX, inbox);
        return wizardState;
    }
}
