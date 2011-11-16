package net.codjo.workflow.server.aspect;
import static net.codjo.agent.DFService.createAgentDescription;
import net.codjo.agent.ServiceException;
import net.codjo.mad.server.handler.AspectLauncher;
import net.codjo.security.common.api.User;
import net.codjo.security.server.api.SecurityServiceHelper;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.JdbcServiceUtil;
import net.codjo.workflow.common.message.JobException;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.protocol.JobProtocolParticipant;
import net.codjo.workflow.server.api.JobAgent;
/**
 *
 */
public class AspectExecutorAgent extends JobAgent {

    public AspectExecutorAgent(JdbcServiceUtil jdbcServiceUtil, AspectLauncher aspectLauncher, MODE mode) {
        super(new ExecuteAspectParticipant(jdbcServiceUtil, aspectLauncher),
              createAgentDescription(ExecuteAspectRequest.JOB_ID), mode);
    }


    private static class ExecuteAspectParticipant extends JobProtocolParticipant {
        private final JdbcServiceUtil jdbcUtil;
        private final AspectLauncher aspectLauncher;


        private ExecuteAspectParticipant(JdbcServiceUtil jdbcServiceUtil, AspectLauncher aspectLauncher) {
            this.jdbcUtil = jdbcServiceUtil;
            this.aspectLauncher = aspectLauncher;
        }


        @Override
        protected void executeJob(JobRequest runningRequest) throws JobException {
            ExecuteAspectRequest request = new ExecuteAspectRequest(runningRequest);
            try {
                ConnectionPool connectionPool = jdbcUtil.getConnectionPool(getAgent(), getRequestMessage());
                User user = getSecurityService().getUser(getRequestMessage().decodeUserId());

                aspectLauncher.run(request.getAspectContext(),
                                   request.getAspectBranchId(),
                                   connectionPool,
                                   user);
            }
            catch (Throwable e) {
                throw new JobException(e.getLocalizedMessage()
                                       + ": Erreur lors de l'exécution de l'aspect "
                                       + "'" + request.getAspectBranchId() + "'"
                                       + " en mode fork.",
                                       e);
            }
        }


        private SecurityServiceHelper getSecurityService() throws ServiceException {
            return ((SecurityServiceHelper)getAgent().getHelper(SecurityServiceHelper.NAME));
        }
    }
}
