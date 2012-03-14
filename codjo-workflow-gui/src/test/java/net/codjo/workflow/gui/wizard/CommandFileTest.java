/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import net.codjo.test.common.Directory.NotDeletedException;
import net.codjo.test.common.fixture.DirectoryFixture;
import net.codjo.util.file.FileUtil;

import static net.codjo.test.common.PathUtil.findTargetDirectory;
/**
 * Classe de test de {@link net.codjo.workflow.gui.wizard.CommandFile}.
 */
public class CommandFileTest extends TestCase {
    private static final String UNKNOWN_COMMAND =
          "'commande' n'est pas reconnu en tant que commande interne\n"
          + "ou externe, un programme ex‚cutable ou un fichier de commandes.\n";
    private DirectoryFixture fixture =
          new DirectoryFixture(findTargetDirectory(CommandFileTest.class) + "/CommandFileTestTEMPO");
    private File file = new File(fixture, "mycmd.cmd");
    private CommandFile commandFile;


    public void test_execute_withoutArgument() throws Exception {
        createCommandFile("echo gogo");

        commandFile.execute();

        assertMessages("", "gogo");
    }


    public void test_execute_withoutArgumentWithError() throws Exception {
        createCommandFile("commande inexistant");

        try {
            commandFile.execute();
            fail();
        }
        catch (CommandFile.ExecuteException ex) {
            assertEquals("Erreur lors de l'execution de " + file, ex.getMessage());
            assertTrue(ex.getExitValue() != 0);
            assertEquals(commandFile.getProcessMessage(), ex.getProcessMessage());
        }

        assertMessages(UNKNOWN_COMMAND, UNKNOWN_COMMAND);
    }


    public void test_execute_withArguments() throws Exception {
        String[] arguments = new String[]{"argument1", "argument2"};
        createCommandFile("echo %1 %2");

        commandFile.execute(arguments);

        assertMessages("", "argument1 argument2");
    }


    public void test_defaultTimeout() throws Exception {
        createCommandFile("");
        assertEquals(-1, commandFile.getTimeout());
        commandFile.setTimeout(20);
        assertEquals(20, commandFile.getTimeout());
    }


    public void test_defaultWorkingDirectory() throws Exception {
        createCommandFile("");
        assertEquals(null, commandFile.getWorkingDirectory());
        commandFile.setWorkingDirectory(file.getParentFile());
        assertEquals(file.getParentFile(), commandFile.getWorkingDirectory());
    }


    public void test_workingDirectory() throws Exception {
        createCommandFile("chdir");

        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        commandFile.setWorkingDirectory(tempDir);

        commandFile.execute();

        assertMessages("", tempDir.getPath());
    }


    public void test_execute_timeout() throws Exception {
        createCommandFile("pause");

        commandFile.setTimeout(100);

        try {
            commandFile.execute();
            fail();
        }
        catch (CommandFile.ExecuteException ex) {
            assertEquals("Erreur lors de l'execution de " + file, ex.getMessage());
            assertTrue(ex.getExitValue() != 0);
        }

        assertMessages("", "Appuyez sur une touche pour continuer...");
    }


    public void test_constructor() throws Exception {
        try {
            commandFile = new CommandFile(null);
            fail();
        }
        catch (IllegalArgumentException exception) {
            assertEquals("Fichier vide ou inexistant " + null, exception.getMessage());
        }

        try {
            commandFile = new CommandFile(new File("do/not/exist.cmd"));
            fail();
        }
        catch (IllegalArgumentException exception) {
            assertEquals("Fichier vide ou inexistant do\\not\\exist.cmd", exception.getMessage());
        }
    }


    @Override
    protected void setUp() throws Exception {
        fixture.doSetUp();
    }


    @Override
    protected void tearDown() throws Exception {
        try {
            fixture.doTearDown();
        }
        catch (NotDeletedException e) {
            // For spike purpose : Retry a new 10 times to delete the folder...
            fixture.doTearDown();
        }
    }


    private void assertMessages(String errorMessages, String processMessage) {
        assertEquals(errorMessages, commandFile.getErrorMessage());
        assertNotNull(commandFile.getProcessMessage());
        assertTrue(commandFile.getProcessMessage().contains(processMessage));
    }


    private void createCommandFile(String fileContent)
          throws IOException {
        FileUtil.saveContent(file, fileContent);
        commandFile = new CommandFile(file);
    }
}
