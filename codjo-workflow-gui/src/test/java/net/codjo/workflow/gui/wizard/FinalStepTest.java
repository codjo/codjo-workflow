/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.gui.wizard;
import net.codjo.agent.test.Semaphore;
import net.codjo.test.common.LogString;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.JobRequestTemplate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import org.uispec4j.Panel;
import org.uispec4j.TextBox;
import org.uispec4j.UISpecTestCase;
/**
 *
 */
@SuppressWarnings({"unchecked"})
public class FinalStepTest extends UISpecTestCase {
    private static final String DATE_ARGUMENT = "my.date";
    public static final String UNSTARTED = "unstarted";
    private static final String RUNNING = "running";
    public static final String FINISHED = "stop";
    private static final String JOB_TYPE = "my.type";
    private static final String JOB_TYPE_2 = "my.type.2";
    private static final String TITLE = "Let's go...";
    private LogString log = new LogString();
    private Semaphore semaphore = new Semaphore();
    private FinalStep step;
    private Panel panel;
    private VtomCallerMock vtomCallerMock;


    public void test_name() throws Exception {
        assertEquals(TITLE, step.getName());
    }


    public void test_vtomIsCalled() throws Exception {
        Map state = createState("2006-05-26");

        step.start(state);

        semaphore.acquire();

        log.assertContent("vtom.call({my.date=2006-05-26})");
    }


    public void test_getEventHandler() throws Exception {
        assertNotNull(step.getEventHandler());
        assertSame(step.getEventHandler(), step.getEventHandler());
    }


    public void test_displaySummary() throws Exception {
        Map state = createState("2006-05-26");

        step.start(state);

        assertEquals(state.toString(), panel.getTextBox("summary").getText());
    }


    public void test_guiUpdate() throws Exception {
        Map state = createState("2006-05-26");

        assertEquals(UNSTARTED, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());

        step.start(state);

        assertEquals(RUNNING, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());

        step.getEventHandler().receive(createRequest("id", JOB_TYPE, state));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());

        step.getEventHandler().receive(createAudit("id", JobAudit.Type.PRE));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(RUNNING, panel.getTextBox("myJobGui").getText());

        step.getEventHandler().receive(createAudit("id", JobAudit.Type.POST));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(FINISHED, panel.getTextBox("myJobGui").getText());
    }


    public void test_guiUpdate_multipleSteps() throws Exception {
        doSetup(new FinalStep.JobGuiData[]{
              new FinalStep.JobGuiData(new JobGuiMock("myJobGui"),
                                       new RequestTemplateFactoryMock()),
              new FinalStep.JobGuiData(new JobGuiMock("myJobGui2"),
                                       new DefaultRequestTemplateFactory(JobRequestTemplate.matchAll()))
        });

        Map state = createState("2006-05-26");

        assertEquals(UNSTARTED, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui2").getText());

        step.start(state);

        assertEquals(RUNNING, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui2").getText());

        step.getEventHandler().receive(createRequest("id", JOB_TYPE, state));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui2").getText());

        step.getEventHandler().receive(createAudit("id", JobAudit.Type.PRE));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(RUNNING, panel.getTextBox("myJobGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui2").getText());

        step.getEventHandler().receive(createAudit("id", JobAudit.Type.POST));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(FINISHED, panel.getTextBox("myJobGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui2").getText());

        step.getEventHandler().receive(createRequest("id2", "id", JOB_TYPE_2, null));
        step.getEventHandler().receive(createAudit("id2", JobAudit.Type.PRE));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(FINISHED, panel.getTextBox("myJobGui").getText());
        assertEquals(RUNNING, panel.getTextBox("myJobGui2").getText());

        step.getEventHandler().receive(createAudit("id2", JobAudit.Type.POST));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(FINISHED, panel.getTextBox("myJobGui").getText());
        assertEquals(FINISHED, panel.getTextBox("myJobGui2").getText());
    }


    public void test_guiUpdate_dateWithTime() throws Exception {
        Map state = new HashMap();
        state.put(DATE_ARGUMENT, java.sql.Timestamp.valueOf("2006-05-26 10:25:12"));

        step.start(state);
        step.getEventHandler().receive(createRequest("id", JOB_TYPE, state));
        step.getEventHandler().receive(createAudit("id", JobAudit.Type.PRE));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(RUNNING, panel.getTextBox("myJobGui").getText());
    }


    public void test_requestFilter() throws Exception {
        Map state = createState("2006-05-26");

        step.start(state);

        step.getEventHandler().receive(createRequest("id", "import"));
        step.getEventHandler().receive(createAudit("id", JobAudit.Type.PRE));

        assertEquals(RUNNING, panel.getTextBox("vtomGui").getText());
        assertEquals(UNSTARTED, panel.getTextBox("myJobGui").getText());
    }


    public void test_guiUpdate_postError() throws Exception {
        Map state = createState("2006-05-26");

        step.start(state);

        step.getEventHandler().receive(createRequest("id", JOB_TYPE, state));
        step.getEventHandler().receive(createAudit("id", JobAudit.Type.PRE));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(RUNNING, panel.getTextBox("myJobGui").getText());

        step.getEventHandler().receive(createErrorAudit("id", JobAudit.Type.POST,
                                                        "errorMessage"));

        assertEquals(FINISHED + " : errorMessage", panel.getTextBox("myJobGui").getText());
    }


    public void test_guiUpdate_preError() throws Exception {
        Map state = createState("2006-05-26");

        step.start(state);

        step.getEventHandler().receive(createRequest("id", JOB_TYPE, state));
        step.getEventHandler().receive(createErrorAudit("id", JobAudit.Type.PRE,
                                                        "errorMessage"));

        assertEquals(FINISHED, panel.getTextBox("vtomGui").getText());
        assertEquals(FINISHED + " : errorMessage", panel.getTextBox("myJobGui").getText());
    }


    public void test_vtomFailure() throws Exception {
        Map state = createState("2006-05-26");

        vtomCallerMock.mockCallFailure(new CommandFile.ExecuteException("message",
                                                                        "error process", -1));

        step.start(state);

        TextBox vtomGui = panel.getTextBox("vtomGui");
        assertTrue(vtomGui.textEquals(FINISHED + " : message"));
    }


    @Override
    protected void setUp() throws Exception {
        doSetup(new FinalStep.JobGuiData[]{
              new FinalStep.JobGuiData(new JobGuiMock("myJobGui"),
                                       new RequestTemplateFactoryMock())
        });
    }


    private void doSetup(FinalStep.JobGuiData[] datas) {
        FinalStep.WizardSummaryGui summary = new WizardSummaryGuiMock();
        FinalStep.JobGui vtom = new JobGuiMock("vtomGui");
        vtomCallerMock = new VtomCallerMock(log, semaphore);
        step = new FinalStep(TITLE, vtomCallerMock, summary, vtom, datas);
        panel = new Panel(step);
    }


    private JobEvent createRequest(String id, String type) {
        return createRequest(id, type, null);
    }


    private JobEvent createRequest(String id, String type, Map state) {
        return createRequest(id, null, type, state);
    }


    private JobEvent createRequest(String id, String prevId, String type, Map state) {
        JobRequest request = new JobRequest(type);
        request.setId(id);
        request.setParentId(prevId);
        request.setInitiatorLogin(System.getProperty("user.name"));
        if (state != null) {
            Arguments arguments =
                  new Arguments(DATE_ARGUMENT,
                                removeHoursToDate((Date)state.get(DATE_ARGUMENT)));
            request.setArguments(arguments);
        }
        return new JobEvent(request);
    }


    private String removeHoursToDate(Date dateWithHour) {
        return new SimpleDateFormat("yyyy-MM-dd").format(dateWithHour);
    }


    private JobEvent createAudit(String id, JobAudit.Type type) {
        JobAudit request = new JobAudit(type);
        request.setRequestId(id);
        return new JobEvent(request);
    }


    private JobEvent createErrorAudit(String id, JobAudit.Type type, String error) {
        JobAudit audit = new JobAudit(type);
        audit.setRequestId(id);
        audit.setErrorMessage(error);
        return new JobEvent(audit);
    }


    private Map createState(String dateString) {
        Map map = new HashMap();
        map.put(DATE_ARGUMENT, java.sql.Date.valueOf(dateString));
        return map;
    }


    private static class WizardSummaryGuiMock extends JTextArea
          implements FinalStep.WizardSummaryGui {
        WizardSummaryGuiMock() {
            setName("summary");
        }


        public JComponent getGui() {
            return this;
        }


        public void display(Map requestState) {
            setText(requestState.toString());
        }
    }

    private static class JobGuiMock extends JTextArea implements FinalStep.JobGui {
        JobGuiMock() {
            this("");
        }


        JobGuiMock(String name) {
            setName(name);
            setText(UNSTARTED);
        }


        public JComponent getGui() {
            return this;
        }


        public void displayStart() {
            setText(RUNNING);
        }


        public void displayMidAudit(JobAudit audit) {
            setText("displayMidAudit" + audit.getType());
        }


        public void displayStop(JobAudit jobResult) {
            if (jobResult.hasError()) {
                setText(FINISHED + " : " + jobResult.getErrorMessage());
            }
            else {
                setText(FINISHED);
            }
        }
    }

    private class RequestTemplateFactoryMock implements RequestTemplateFactory {
        public JobRequestTemplate createTemplate(final Map wizardState) {
            String expected = removeHoursToDate((Date)wizardState.get(DATE_ARGUMENT));
            return JobRequestTemplate.matchArgument(DATE_ARGUMENT, expected);
        }
    }
}
