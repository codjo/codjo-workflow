/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import org.apache.log4j.Logger;
/**
 * Executes un fichier de commande.
 */
public class CommandFile {
    private static final Logger LOGGER = Logger.getLogger(CommandFile.class);
    private String errorMessage;
    private String processMessage;
    private final File cmdFile;
    private int timeout = -1;
    private File workingDirectory;
    private Thread timeoutThread;


    public CommandFile(File file) {
        this.cmdFile = file;
        if (cmdFile == null || !cmdFile.exists()) {
            throw new IllegalArgumentException("Fichier vide ou inexistant " + cmdFile);
        }
    }


    public int getTimeout() {
        return timeout;
    }


    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }


    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }


    public File getWorkingDirectory() {
        return workingDirectory;
    }


    public void execute() throws ExecuteException {
        execute(null);
    }


    public void execute(String[] arguments) throws ExecuteException {
        setErrorMessage(null);
        setProcessMessage(null);

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Execution de (timeout=" + timeout + "): " + cmdFile + " "
                             + (arguments == null ? "" : Arrays.asList(arguments).toString()));
            }

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(createCommandLine(arguments), null, workingDirectory);

            if (timeout > 0) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("CREATE KILLER " + cmdFile);
                }
                createTimeoutKiller(cmdFile.getName(), proc);
            }

            StreamReader errorReader = new StreamReader(proc.getErrorStream());
            StreamReader outputReader = new StreamReader(proc.getInputStream());

            errorReader.start();
            outputReader.start();

            proc.waitFor();

            outputReader.waitReadFinished();
            errorReader.waitReadFinished();

            setErrorMessage(errorReader.getMessage());
            setProcessMessage("[ERROR]\n"
                              + ((getErrorMessage() != null) ? getErrorMessage() : "") + "[OUTPUT]\n"
                              + outputReader.getMessage());
            if (proc.exitValue() != 0) {
                throw new ExecuteException("Erreur lors de l'execution de " + cmdFile,
                                           getProcessMessage(),
                                           proc.exitValue());
            }
        }
        catch (IOException exception) {
            throw new ExecuteException(exception, getProcessMessage(), -1);
        }
        catch (InterruptedException exception) {
            throw new ExecuteException(exception, getProcessMessage(), -2);
        }
        finally {
            if (timeoutThread != null) {
                timeoutThread.interrupt();
                timeoutThread = null;
            }
        }
    }


    private String[] createCommandLine(String[] arguments) {
        if (arguments == null) {
            return new String[]{cmdFile.getPath()};
        }
        String[] commandLine = new String[arguments.length + 1];
        commandLine[0] = cmdFile.getPath();
        System.arraycopy(arguments, 0, commandLine, 1, arguments.length);
        return commandLine;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public String getProcessMessage() {
        return processMessage;
    }


    private void setErrorMessage(String newErrorMessage) {
        if (newErrorMessage == null) {
            newErrorMessage = "";
        }
        errorMessage = newErrorMessage;
    }


    private void setProcessMessage(String newProcessMessage) {
        processMessage = newProcessMessage;
    }


    private void createTimeoutKiller(final String processName, final Process process) {
        timeoutThread = new TimeoutThread(processName, process);
        timeoutThread.start();
    }


    private static class StreamReader extends Thread {
        private StringBuffer message = new StringBuffer();
        private InputStream is;
        private final Object lock = new Object();
        private boolean finished = false;


        StreamReader(InputStream is) {
            this.is = is;
        }


        public String getMessage() {
            return message.toString();
        }


        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                while (line != null) {
                    message.append(line).append("\n");
                    LOGGER.debug("line read > " + line + "<");
                    line = br.readLine();
                }
            }
            catch (IOException ioe) {
                message.append(ioe.toString());
                LOGGER.error("Error during execution " + message, ioe);
            }

            synchronized (lock) {
                finished = true;
                lock.notifyAll();
            }
        }


        private void waitReadFinished() {
            synchronized (lock) {
                while (!finished) {
                    try {
                        lock.wait();
                    }
                    catch (InterruptedException e) {
                        LOGGER.warn("", e);
                    }
                }
            }
        }
    }

    public static class ExecuteException extends Exception {
        private final String processMessage;
        private final int exitValue;


        public ExecuteException(String message, String processMessage, int exitValue) {
            super(message);
            this.processMessage = processMessage;
            this.exitValue = exitValue;
        }


        public ExecuteException(Exception exception, String processMessage, int exitValue) {
            super(exception);
            this.processMessage = processMessage;
            this.exitValue = exitValue;
        }


        public int getExitValue() {
            return exitValue;
        }


        public String getProcessMessage() {
            return processMessage;
        }
    }

    private class TimeoutThread extends Thread {
        private final String processName;
        private final Process process;


        TimeoutThread(String processName, Process process) {
            this.processName = processName;
            this.process = process;
        }


        public synchronized void run() {
            try {
                Thread.sleep(timeout);
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process (" + processName + ")timeout tombé : Verification du process");
                    }
                    process.exitValue();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process (" + processName + ")timeout tombé : Process déja terminé");
                    }
                }
                catch (IllegalThreadStateException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process (" + processName + ")timeout tombé : Destruction du process");
                    }
                    process.destroy();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Process (" + processName + ")timeout tombé : Process détruit");
                    }
                }
            }
            catch (InterruptedException e) {
                ;
            }
            catch (Throwable e) {
                LOGGER.error("Process (" + processName + ")timeout internal failure", e);
            }
        }
    }
}
