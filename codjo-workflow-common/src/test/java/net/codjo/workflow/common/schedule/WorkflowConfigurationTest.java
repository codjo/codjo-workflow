package net.codjo.workflow.common.schedule;

import static net.codjo.test.common.matcher.JUnitMatchers.*;
import org.junit.Test;

public class WorkflowConfigurationTest {
    private static final long FOUR_MINUTES = 240000L;
    private static final long THIRTY_MINUTES = 1800000L;


    @Test
    public void test_defaultValue() throws Exception {
        WorkflowConfiguration configuration = new WorkflowConfiguration();
        assertThat(configuration.getDefaultReplyTimeout(), equalTo(FOUR_MINUTES));
        assertThat(configuration.getDefaultTimeout(), equalTo(THIRTY_MINUTES));
    }
}
