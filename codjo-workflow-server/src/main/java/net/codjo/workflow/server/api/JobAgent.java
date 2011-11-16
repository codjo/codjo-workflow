/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.api;
import net.codjo.agent.Agent;
import net.codjo.agent.Behaviour;
import net.codjo.agent.DFService;
import net.codjo.agent.DFService.AgentDescription;
import net.codjo.agent.behaviour.OneShotBehaviour;
import net.codjo.agent.behaviour.SequentialBehaviour;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import org.apache.log4j.Logger;
/**
 * Agent capable d'executer un {@link net.codjo.workflow.common.message.JobRequest}.
 */
public class JobAgent extends Agent {
    private MODE mode;
    public enum MODE {
        DELEGATE,
        NOT_DELEGATE
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());
    private JobProtocolParticipant jobProtocolParticipant;
    private AgentDescription agentDescription;


    public JobAgent() {
        this(MODE.NOT_DELEGATE);
    }


    public JobAgent(MODE mode) {
        this(null, null, mode);
    }


    public JobAgent(JobProtocolParticipant jobProtocolParticipant, AgentDescription description) {
        this(jobProtocolParticipant, description, MODE.NOT_DELEGATE);
    }


    public JobAgent(JobProtocolParticipant jobProtocolParticipant, AgentDescription description, MODE mode) {
        this.mode = mode;
        setAgentDescription(description);
        setJobProtocolParticipant(jobProtocolParticipant);
    }


    public JobProtocolParticipant getJobProtocolParticipant() {
        return jobProtocolParticipant;
    }


    public void setJobProtocolParticipant(JobProtocolParticipant jobProtocolParticipant) {
        this.jobProtocolParticipant = jobProtocolParticipant;
    }


    public AgentDescription getAgentDescription() {
        return agentDescription;
    }


    public void setAgentDescription(AgentDescription agentDescription) {
        this.agentDescription = agentDescription;
    }


    @Override
    protected void setup() {
        if (mode == MODE.DELEGATE) {
            prepareSuicide();
        }
        else {
            try {
                DFService.register(this, agentDescription);
            }
            catch (Exception exception) {
                logger.error("Impossible de s'enregistrer auprès du DF " + getClass(), exception);
                die();
            }
        }
        addBehaviour(jobProtocolParticipant);
    }


    @Override
    protected void tearDown() {
        if (mode == MODE.NOT_DELEGATE) {
            try {
                DFService.deregister(this);
            }
            catch (DFService.DFServiceException exception) {
                logger.error("Impossible de s'enlever auprès du DF " + getClass(), exception);
            }
        }
    }


    private void prepareSuicide() {
        Behaviour sequence =
              SequentialBehaviour
                    .wichStartsWith(getJobProtocolParticipant().getExecuteJobBehaviour())
                    .andThen(new SuicideBehaviour());

        getJobProtocolParticipant().setExecuteJobBehaviour(sequence);
    }


    private class SuicideBehaviour extends OneShotBehaviour {

        @Override
        protected void action() {
            die();
        }
    }
}
