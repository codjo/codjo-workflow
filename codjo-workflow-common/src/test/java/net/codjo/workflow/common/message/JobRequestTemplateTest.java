/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 * Classe de test de {@link JobRequestTemplate}.
 */
public class JobRequestTemplateTest extends TestCase {
    private JobRequest request;


    @Override
    protected void setUp() throws Exception {
        request = new JobRequest();
    }


    public void test_matchType() throws Exception {
        JobRequestTemplate template = JobRequestTemplate.matchType("import");

        request.setType("import");
        assertTrue(template.match(request));

        request.setType("pas-import");
        assertFalse(template.match(request));

        request.setType(null);
        assertFalse(template.match(request));
    }


    public void test_matchInitiator() throws Exception {
        JobRequestTemplate template = JobRequestTemplate.matchInitiator("boris");

        request.setInitiatorLogin("boris");
        assertTrue(template.match(request));

        request.setInitiatorLogin("patricia");
        assertFalse(template.match(request));
    }


    public void test_matchArgument() throws Exception {
        JobRequestTemplate template = JobRequestTemplate.matchArgument("arg0", "val0");

        Arguments arguments = new Arguments();
        arguments.put("arg0", "val0");
        request.setArguments(arguments);
        assertTrue(template.match(request));

        arguments.put("arg0", "bad_val0");
        assertFalse(template.match(request));
    }


    public void test_matchArgument_withoutArgument() {
        JobRequestTemplate template = JobRequestTemplate.matchArgument("arg0", "val0");

        request.setArguments(null);
        assertFalse(template.match(request));
    }


    public void test_matchAnd() throws Exception {
        JobRequestTemplate template =
              JobRequestTemplate.and(JobRequestTemplate.matchInitiator("boris"),
                                     JobRequestTemplate.matchType("import"));

        request.setInitiatorLogin("boris");
        assertFalse(template.match(request));

        request.setType("import");
        assertTrue(template.match(request));
    }


    public void test_matchOr() throws Exception {
        JobRequestTemplate template =
              JobRequestTemplate.or(JobRequestTemplate.matchInitiator("boris"),
                                    JobRequestTemplate.matchType("import"));

        request.setInitiatorLogin("patricia");
        assertFalse(template.match(request));

        request.setInitiatorLogin("boris");
        assertTrue(template.match(request));

        request.setType("import");
        assertTrue(template.match(request));
    }


    public void test_match() throws Exception {
        JobRequestTemplate.MatchExpression expression =
              new JobRequestTemplate.MatchExpression() {
                  public boolean match(JobRequest request) {
                      return null == request.getInitiatorLogin();
                  }
              };

        JobRequestTemplate template = JobRequestTemplate.matchCustom(expression);

        request.setInitiatorLogin("patricia");
        assertFalse(template.match(request));

        request.setInitiatorLogin(null);
        assertTrue(template.match(request));
    }


    public void test_matchAll() throws Exception {
        JobRequestTemplate template = JobRequestTemplate.matchAll();

        request.setInitiatorLogin("patricia");
        assertTrue(template.match(request));

        request.setInitiatorLogin(null);
        assertTrue(template.match(request));
    }
}
