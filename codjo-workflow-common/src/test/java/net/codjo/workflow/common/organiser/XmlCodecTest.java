package net.codjo.workflow.common.organiser;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.organiser.Job.State;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static net.codjo.test.common.XmlUtil.assertEquivalent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
/**
 *
 */
public class XmlCodecTest {
    private XmlCodec xmlCodec = new XmlCodec();
    private List<Job> jobs = new ArrayList<Job>();


    @Before
    public void setUp() {
        CustomJob jobMock1 = new CustomJob("id1", "import");
        jobMock1.setDescription("job1 description");
        jobMock1.setInitiator("job1 initiator");
        jobMock1.setUserId("job1 userId");
        jobMock1.setState(State.WAITING);
        jobMock1.setTable("job1 table");
        jobMock1.setCustomField("job1 customField");
        jobMock1.getArgs().put("arg1", "value1");
        jobMock1.getArgs().put("arg2", "value2");

        CustomJob jobMock2 = new CustomJob("id2", "segmentation");

        jobs.add(jobMock1);
        jobs.add(jobMock2);
    }


    @Test
    public void test_jobsToXml() throws Exception {
        String expected = "<jobs>"
                          + "  <net.codjo.workflow.common.organiser.CustomJob type=\"import\" state=\"WAITING\" table=\"job1 table\" initiator=\"job1 initiator\">"
                          + "        <userId>job1 userId</userId>"
                          + "        <customField>job1 customField</customField>"
                          + "        <id class=\"string\">id1</id>"
                          + "        <args class=\"tree-map\">"
                          + "            <no-comparator/>"
                          + "            <entry>"
                          + "               <string>arg1</string>"
                          + "               <string>value1</string>"
                          + "            </entry>"
                          + "            <entry>"
                          + "               <string>arg2</string>"
                          + "               <string>value2</string>"
                          + "            </entry>"
                          + "        </args>"
                          + "        <description>job1 description</description>"
                          + "  </net.codjo.workflow.common.organiser.CustomJob>"
                          + "  <net.codjo.workflow.common.organiser.CustomJob type=\"segmentation\" state=\"NEW\">"
                          + "        <id class=\"string\">id2</id>"
                          + "     <args class=\"tree-map\">"
                          + "       <no-comparator/>"
                          + "     </args>"
                          + "  </net.codjo.workflow.common.organiser.CustomJob>"
                          + "</jobs>";

        assertEquivalent(expected, xmlCodec.jobsToXml(jobs));
    }


    @Test
    public void test_xmlToJobs() throws Exception {
        List<Job> actualJobs = xmlCodec.xmlToJobs(xmlCodec.jobsToXml(jobs));

        Job job1 = actualJobs.get(0);
        assertNotSame(jobs.get(0), job1);
        assertEquals("import", job1.getType());
        assertEquals("{arg1=value1, arg2=value2}", job1.getArgs().toString());
        assertEquals("job1 description", job1.getDescription());
        assertEquals("job1 initiator", job1.getInitiator());
        assertEquals("job1 userId", job1.getUserId());
        assertEquals(Job.State.WAITING, job1.getState());
        assertEquals("job1 table", job1.getTable());

        Job job2 = actualJobs.get(1);
        assertNotSame(jobs.get(1), job2);
        assertEquals("segmentation", job2.getType());
        assertEquals("{}", job2.getArgs().toString());
        assertNull(job2.getDescription());
        assertNull(job2.getInitiator());
        assertEquals(Job.State.NEW, job2.getState());
        assertNull(job2.getTable());
    }


    @Test
    public void test_jobEventToXml() throws Exception {
        String expected = "<net.codjo.workflow.common.organiser.JobEventMock>"
                          + "  <request>"
                          + "    <id>importId</id>"
                          + "    <date>2010-01-01 00:00:00.0 CET</date>"
                          + "    <loggable>true</loggable>"
                          + "  </request>"
                          + "</net.codjo.workflow.common.organiser.JobEventMock>";

        String actual = xmlCodec.jobEventToXml(new JobEventMock("importId", "2010-01-01"));
        assertEquivalent(expected, actual);
    }


    @Test
    public void test_xmlToJobEvent() throws Exception {
        JobEventMock jobEvent = new JobEventMock("importId", "2010-01-01");
        JobEvent actualJobEvent = xmlCodec.xmlToJobEvent(xmlCodec.jobEventToXml(jobEvent));

        assertNotSame(jobEvent, actualJobEvent);
        assertEquals(jobEvent.getRequest().getId(), actualJobEvent.getRequest().getId());
        assertEquals(jobEvent.getRequest().getDate(), actualJobEvent.getRequest().getDate());
    }
}
