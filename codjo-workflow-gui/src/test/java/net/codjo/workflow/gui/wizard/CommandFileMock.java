/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.test.common.LogString;
import java.io.File;
import java.util.Arrays;
/**
 *
 */
public class CommandFileMock extends CommandFile {
    private final LogString log;
    private ExecuteException mockFailure;


    public CommandFileMock(LogString log) {
        super(new File("./pom.xml"));
        this.log = log;
    }


    public void setTimeout(int timeout) {
        log.call("setTimeout", Integer.toString(timeout));
    }


    public void execute() throws ExecuteException {
        log.call("execute");
        if (mockFailure != null) {
            throw mockFailure;
        }
    }


    public void execute(String[] arguments) throws ExecuteException {
        log.call("execute", Arrays.asList(arguments));
        if (mockFailure != null) {
            throw mockFailure;
        }
    }


    public void mockExecuteFailure(ExecuteException exception) {
        this.mockFailure = exception;
    }
}
