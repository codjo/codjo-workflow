/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.schedule;
import net.codjo.agent.AgentContainer;
import net.codjo.agent.ContainerFailureException;
import net.codjo.agent.UserId;
import net.codjo.plugin.batch.BatchException;
import net.codjo.plugin.batch.TimeoutBatchException;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobEvent;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.subscribe.JobEventHandler;
import net.codjo.workflow.common.subscribe.ProtocolErrorEvent;
/**
 *
 */
public class ScheduleLauncher {
    private boolean workDone;
    private BatchException commandError;
    private final UserId userId;
    private final String initiatorLogin;
    private JobEventHandler jobEventHandler;
    private ExecuteType executeType = ExecuteType.SYNCHRONOUS;
    private String scheduleLeaderAgentName;
    private WorkflowConfiguration workflowConfiguration = new WorkflowConfiguration();


    public ScheduleLauncher(UserId userId, String initiatorLogin) {
        this.userId = userId;
        this.initiatorLogin = initiatorLogin;
    }


    public ScheduleLauncher(UserId id) {
        this(id, id.getLogin());
    }


    public void executeWorkflow(AgentContainer container, JobRequest request)
          throws ContainerFailureException, BatchException {
        request.setInitiatorLogin(initiatorLogin);
        ScheduleLeaderAgent scheduleLeaderAgent = new ScheduleLeaderAgent(request,
                                                                          new MyJobEventHandler(),
                                                                          userId,
                                                                          workflowConfiguration);

        scheduleLeaderAgentName = scheduleLeaderAgent.createNickName(request.getType());

        switch (executeType) {
            case SYNCHRONOUS:
                ScheduleLeaderAgent.waitUntilFinished(scheduleLeaderAgentName,
                                                      scheduleLeaderAgent,
                                                      container,
                                                      workflowConfiguration);
                if (commandError != null) {
                    throw commandError;
                }
                if (!isWorkDone()) {
                    throw new TimeoutBatchException("workflow", workflowConfiguration.getDefaultTimeout());
                }
                break;
            case ASYNCHRONOUS:
                container.acceptNewAgent(scheduleLeaderAgentName, scheduleLeaderAgent).start();
                break;
        }
    }


    public void setJobEventHandler(JobEventHandler jobEventHandler) {
        this.jobEventHandler = jobEventHandler;
    }


    public ExecuteType getExecuteType() {
        return executeType;
    }


    public void setExecuteType(ExecuteType executeType) {
        this.executeType = executeType;
    }


    public WorkflowConfiguration getWorkflowConfiguration() {
        return workflowConfiguration;
    }


    public void setWorkflowConfiguration(WorkflowConfiguration workflowConfiguration) {
        this.workflowConfiguration = workflowConfiguration;
    }


    /**
     * @deprecated utiliser getWorkflowConfiguration().setDefaultTimeout()
     */
    @Deprecated
    public void setTimeout(long timeout) {
        getWorkflowConfiguration().setDefaultTimeout(timeout);
    }


    String getInitiatorLogin() {
        return initiatorLogin;
    }


    String getScheduleLeaderAgentName() {
        return scheduleLeaderAgentName;
    }


    private boolean isWorkDone() {
        return workDone;
    }


    private class MyJobEventHandler extends JobEventHandler {
        @Override
        protected void handleRequest(JobRequest request) {
            workDone = false;
        }


        @Override
        public boolean receiveError(ProtocolErrorEvent event) {
            if (jobEventHandler != null) {
                jobEventHandler.receiveError(event);
            }
            return super.receiveError(event);
        }


        @Override
        public boolean receive(JobEvent event) {
            if (jobEventHandler != null) {
                jobEventHandler.receive(event);
            }
            return super.receive(event);
        }


        @Override
        protected void handleAudit(JobAudit audit) {
            if (audit.hasError()) {
                commandError = new JobBatchException(audit);
            }

            if (audit.getType() == JobAudit.Type.POST) {
                workDone = true;
            }
        }
    }
    public enum ExecuteType {
        ASYNCHRONOUS,
        SYNCHRONOUS
    }
}
