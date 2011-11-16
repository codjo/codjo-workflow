package net.codjo.workflow.common.util;
import net.codjo.test.common.fixture.DirectoryFixture;
import net.codjo.util.file.FileUtil;
import net.codjo.workflow.common.message.JobRequest;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class WorkflowLogUtilTest {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private Logger logger = Logger.getLogger(WorkflowLogUtilTest.class);
    private DirectoryFixture directoryFixture = DirectoryFixture.newTemporaryDirectoryFixture();
    private File logFile;
    private FileAppender appender;


    @Test
    public void test_logIfLoggable() throws IOException {
        JobRequest jobRequest = new JobRequest("import");
        jobRequest.setLoggable(true);
        String message = "import info to log";
        WorkflowLogUtil.logIfNeeded(logger, jobRequest, message);
        assertEquals("INFO - import info to log" + NEW_LINE, FileUtil.loadContent(logFile));
    }


    @Test
    public void test_logAtDebugLevelIfNotLoggable() throws Exception {
        JobRequest jobRequest = new JobRequest("handler");
        jobRequest.setLoggable(false);
        String message = "handler info to log";
        WorkflowLogUtil.logIfNeeded(logger, jobRequest, message);
        assertEquals("DEBUG - handler info to log" + NEW_LINE, FileUtil.loadContent(logFile));
    }


    @Before
    public void setUp() throws Exception {
        directoryFixture.doSetUp();
        logFile = new File(directoryFixture, "log.txt");
        appender = new FileAppender(new SimpleLayout(), logFile.getPath());
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
    }


    @After
    public void tearDown() throws Exception {
        logger.removeAppender(appender);
        appender.close();
        directoryFixture.doTearDown();
    }
}
