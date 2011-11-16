/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
/**
 * Encapsule les arguments de {@link JobRequest} ou {@link JobAudit}.
 */
public class Arguments implements Serializable {
    private Map<String, String> properties = new TreeMap<String, String>();


    public Arguments() {
    }


    public Arguments(String key, String value) {
        put(key, value);
    }


    public Arguments(Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }


    public void put(String key, String value) {
        properties.put(key, value);
    }


    public String get(String key) {
        return properties.get(key);
    }


    public void decode(String encodedArgument) {
        loadProperty(properties, encodedArgument);
    }


    private static int findEndOfProperty(String file, int fromIndex) {
        int idx = file.indexOf('\n', fromIndex);
        if (idx == -1) {
            return -1;
        }

        if (idx == 0 || '\\' != file.charAt(idx - 1)) {
            return idx;
        }

        return findEndOfProperty(file, idx + 1);
    }


    private static void loadProperty(Map<String, String> props, String file) {
        if (file.length() == 0) {
            return;
        }
        int eolIdx = findEndOfProperty(file, 0);
        if (eolIdx == -1) {
            eolIdx = file.length();
        }
        int equalIdx = file.indexOf('=');

        String name;
        String value = "";
        if (equalIdx != -1 && eolIdx > equalIdx) {
            name = file.substring(0, equalIdx).trim();
            value = file.substring(equalIdx + 1, eolIdx).trim();
        }
        else {
            name = file.substring(0, eolIdx).trim();
        }
        if (name.length() > 0) {
            props.put(name, value);
        }
        loadProperty(props, file.substring(Math.min(eolIdx + 1, file.length())));
    }


    public String encode() {
        StringBuilder buffer = new StringBuilder();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            buffer.append(entry.getKey()).append("=").append(entry.getValue());
            buffer.append("\n");
        }

        return buffer.toString();
    }


    @Override
    public String toString() {
        return "Arguments" + properties;
    }


    public Map<String, String> toMap() {
        return properties;
    }
}
