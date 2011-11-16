package net.codjo.workflow.server.organiser;
import net.codjo.agent.UserId;
import static net.codjo.agent.UserId.createId;
import net.codjo.workflow.common.organiser.Job;
import net.codjo.workflow.server.TestUtil;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class DescriptionJobBuilderTest {
    private static final UserId USER_ID = createId("titi", "toto");
    private DiscriminentStringifierMock discriminentStringifierMock = new DiscriminentStringifierMock();
    private DescriptionJobBuilder descriptionJobBuilder;


    @Before
    public void setUp() {
        descriptionJobBuilder = new DescriptionJobBuilder(discriminentStringifierMock);
    }


    @Test
    public void test_createJob() throws Exception {
        discriminentStringifierMock.mock("ma description");

        Job job = descriptionJobBuilder.createJob(TestUtil.createRequest("import",
                                                                         Collections.<String, String>emptyMap()),
                                                  USER_ID);

        assertEquals("import", job.getType());
        assertEquals("ma description", job.getDescription());
    }
}   
