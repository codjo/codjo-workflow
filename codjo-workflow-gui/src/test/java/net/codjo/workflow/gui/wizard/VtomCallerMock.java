/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.agent.test.Semaphore;
import net.codjo.test.common.LogString;
import java.util.Map;
/**
 *
 */
class VtomCallerMock implements VtomCaller {
    private CommandFile.ExecuteException mockCallFailure;
    private LogString log = new LogString();
    private Semaphore semaphore = new Semaphore();


    VtomCallerMock() {
    }


    VtomCallerMock(LogString log, Semaphore semaphore) {
        this.semaphore = semaphore;
        this.log = log;
    }


    public void call(Map wizardState) throws CommandFile.ExecuteException {
        log.call("vtom.call", wizardState);
        semaphore.release();
        if (mockCallFailure != null) {
            throw mockCallFailure;
        }
    }


    public void mockCallFailure(CommandFile.ExecuteException exception) {
        this.mockCallFailure = exception;
    }
}
