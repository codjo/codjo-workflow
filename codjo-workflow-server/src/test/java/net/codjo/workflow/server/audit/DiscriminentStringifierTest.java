package net.codjo.workflow.server.audit;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import static net.codjo.workflow.server.TestUtil.createRequest;
import java.util.Collections;
import java.util.HashMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
/**
 *
 */
public class DiscriminentStringifierTest {
    private HashMap<String, Stringifier> stringifiers = new HashMap<String, Stringifier>();
    private DiscriminentStringifier discriminentStringifier = new DiscriminentStringifier(stringifiers);


    @Test
    public void test_getDiscriminent() throws Exception {
        stringifiers.put("import", new Stringifier() {
            public String toString(JobRequest jobRequest) {
                return "file=toto.txt";
            }
        });
        stringifiers.put("segmentation", new Stringifier() {
            public String toString(JobRequest jobRequest) {
                return "axe=titi";
            }
        });

        assertEquals("file=myfile.txt",
                     discriminentStringifier.getDiscriminent(createRequest("broadcast",
                                                                           "request-126",
                                                                           "request-124",
                                                                           "crego",
                                                                           new Arguments("file",
                                                                                         "myfile.txt"))));

        assertEquals("file=toto.txt",
                     discriminentStringifier.getDiscriminent(createRequest("import",
                                                                           "request-125",
                                                                           "request-124",
                                                                           "crego",
                                                                           new Arguments(
                                                                                 "comment",
                                                                                 "nothing"))));
    }


    @Test
    public void test_noArguments() throws Exception {
        assertEquals("",
                     discriminentStringifier.getDiscriminent(createRequest("patati",
                                                                           Collections.<String, String>emptyMap())));
    }
}
