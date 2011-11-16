/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.server.audit;
import net.codjo.agent.AclMessage;
import net.codjo.agent.Agent;
import net.codjo.sql.server.ConnectionPool;
import net.codjo.sql.server.JdbcServiceUtil;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobAudit;
import net.codjo.workflow.common.message.JobRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
/**
 *
 */
public class JdbcAuditDao implements AuditDao {
    private static final String FIND_REQUEST =
          "select ID, PARENT_ID, REQUEST_DATE, INITIATOR_LOGIN, REQUEST_TYPE, ARGUMENT "
          + "from AP_WORKFLOW_LOG "
          + "where REQUEST_TYPE = ? "
          + "  and REQUEST_DATE >= ? and REQUEST_DATE <= ?";
    private static final String INSERT_REQUEST =
          "insert into AP_WORKFLOW_LOG "
          + "(ID, PARENT_ID, REQUEST_DATE, INITIATOR_LOGIN, REQUEST_TYPE, ARGUMENT, DISCRIMINENT)"
          + " values(?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_PRE_AUDIT =
          "update AP_WORKFLOW_LOG "
          + "set PRE_AUDIT_DATE=?,"
          + "    PRE_AUDIT_ARGUMENT=?,"
          + "    PRE_AUDIT_STATUS=?,"
          + "    PRE_AUDIT_ANOMALY_MESSAGE=?,"
          + "    PRE_AUDIT_ANOMALY_TRACE=? "
          + "where ID = ?";
    private static final String UPDATE_POST_AUDIT =
          "update AP_WORKFLOW_LOG "
          + "set POST_AUDIT_DATE=?, "
          + "    POST_AUDIT_ARGUMENT=?, "
          + "    POST_AUDIT_STATUS=?, "
          + "    POST_AUDIT_ANOMALY_MESSAGE=?, "
          + "    POST_AUDIT_ANOMALY_TRACE=? "
          + "where ID = ?";
    private static final String DELETE_AUDIT =
          "delete from AP_WORKFLOW_LOG "
          + "where REQUEST_DATE <= ?";
    private final JdbcServiceUtil jdbc;
    private final DiscriminentStringifier discriminentStringifier;


    public JdbcAuditDao() {
        this(new JdbcServiceUtil());
    }


    public JdbcAuditDao(JdbcServiceUtil jdbc) {
        this(jdbc, new DiscriminentStringifier(Collections.<String, Stringifier>emptyMap()));
    }


    public JdbcAuditDao(DiscriminentStringifier discriminentStringifier) {
        this(new JdbcServiceUtil(), discriminentStringifier);
    }


    public JdbcAuditDao(JdbcServiceUtil jdbc, DiscriminentStringifier discriminentStringifier) {
        this.jdbc = jdbc;
        this.discriminentStringifier = discriminentStringifier;
    }


    public void saveRequest(Agent agent, AclMessage message, final JobRequest request) throws SQLException {
        execute(agent, message, new UpdateRequest() {
            @Override
            public void execute(Connection connection) throws SQLException {
                saveRequest(connection, request);
            }
        });
    }


    public void saveAudit(Agent agent, AclMessage message, final JobAudit audit)
          throws SQLException, UnknownRequestIdException {
        UpdateRequest updateRequest = new UpdateRequest() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try {
                    saveAudit(connection, audit);
                }
                catch (UnknownRequestIdException e) {
                    setException(e);
                }
            }
        };
        execute(agent, message, updateRequest);
        if (updateRequest.getException() instanceof UnknownRequestIdException) {
            throw (UnknownRequestIdException)updateRequest.getException();
        }
    }


    public List<JobRequest> findRequest(Agent agent, AclMessage message,
                                        final String requestType, final Date beginDate, final Date endDate)
          throws SQLException {
        return execute(agent, message, new QueryRequest<List<JobRequest>>() {
            public List<JobRequest> execute(Connection connection) throws SQLException {
                return findRequest(connection, requestType, beginDate, endDate);
            }
        });
    }


    public void deleteAudit(Agent agent, AclMessage message, final Date date) throws SQLException {
        execute(agent, message, new UpdateRequest() {
            @Override
            public void execute(Connection connection) throws SQLException {
                deleteAudit(connection, date);
            }
        });
    }


    private <T> T execute(Agent agent, AclMessage message, QueryRequest<T> queryRequest)
          throws SQLException {
        ConnectionPool connectionPool = jdbc.getConnectionPool(agent, message);
        Connection connection = connectionPool.getConnection();
        try {
            return queryRequest.execute(connection);
        }
        finally {
            connectionPool.releaseConnection(connection);
        }
    }


    private void execute(Agent agent, AclMessage message, final UpdateRequest updateRequest)
          throws SQLException {
        execute(agent, message, new QueryRequest<Object>() {
            public Object execute(Connection connection) throws SQLException {
                updateRequest.execute(connection);
                return null;
            }
        });
    }


    private List<JobRequest> findRequest(Connection connection,
                                         String requestType,
                                         Date beginDate,
                                         Date endDate) throws SQLException {
        List<JobRequest> result = new ArrayList<JobRequest>();

        PreparedStatement statement = connection.prepareStatement(FIND_REQUEST);
        try {
            statement.setString(1, requestType);
            statement.setTimestamp(2, toTimestamp(beginDate));
            statement.setTimestamp(3, toTimestamp(endDate));

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                JobRequest jobRequest = new JobRequest(rs.getString("REQUEST_TYPE"));
                jobRequest.setId(rs.getString("ID"));
                jobRequest.setParentId(rs.getString("PARENT_ID"));
                jobRequest.setInitiatorLogin(rs.getString("INITIATOR_LOGIN"));
                jobRequest.setDate(rs.getTimestamp("REQUEST_DATE"));

                Arguments arguments = new Arguments();
                arguments.decode(rs.getString("ARGUMENT"));
                jobRequest.setArguments(arguments);

                result.add(jobRequest);
            }
        }
        finally {
            statement.close();
        }

        return result;
    }


    private void saveRequest(Connection connection, JobRequest request) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(INSERT_REQUEST);
        try {
            statement.setString(1, request.getId());
            statement.setString(2, request.getParentId());
            statement.setTimestamp(3, toTimestamp(request.getDate()));
            statement.setString(4, request.getInitiatorLogin());
            statement.setString(5, request.getType());
            statement.setString(6, encodeArguments(request.getArguments()));
            statement.setString(7, discriminentStringifier.getDiscriminent(request));

            statement.executeUpdate();
        }
        finally {
            statement.close();
        }
    }


    private void saveAudit(Connection connection, JobAudit audit)
          throws SQLException, UnknownRequestIdException {
        PreparedStatement statement = connection.prepareStatement(getUpdateQuery(audit.getType()));
        try {
            statement.setTimestamp(1, toTimestamp(audit.getDate()));
            statement.setString(2, encodeArguments(audit.getArguments()));
            JobAudit.Status status = audit.getStatus();
            statement.setString(3, status.toString());
            if (status == JobAudit.Status.ERROR) {
                fillAnomalyInfo(statement, audit.getError());
            }
            else if (status == JobAudit.Status.WARNING) {
                fillAnomalyInfo(statement, audit.getWarning());
            }
            else {
                statement.setString(4, null);
                statement.setString(5, null);
            }
            statement.setString(6, audit.getRequestId());

            int rowCount = statement.executeUpdate();
            if (rowCount == 0) {
                throw new UnknownRequestIdException(audit);
            }
        }
        finally {
            statement.close();
        }
    }


    private void deleteAudit(Connection connection, Date date) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(DELETE_AUDIT);
        try {
            statement.setTimestamp(1, toTimestamp(date));
            statement.executeUpdate();
        }
        finally {
            statement.close();
        }
    }


    private String getUpdateQuery(JobAudit.Type type) {
        return (type == JobAudit.Type.PRE) ? UPDATE_PRE_AUDIT : UPDATE_POST_AUDIT;
    }


    private void fillAnomalyInfo(PreparedStatement statement, JobAudit.Anomaly anomaly) throws SQLException {
        statement.setString(4, anomaly.getMessage());
        statement.setString(5, anomaly.getDescription());
    }


    private Timestamp toTimestamp(Date date) {
        return new Timestamp(date.getTime());
    }


    private String encodeArguments(Arguments arguments) {
        if (arguments == null) {
            return null;
        }
        return arguments.encode().trim();
    }


    static interface QueryRequest<T> {
        T execute(Connection connection) throws SQLException;
    }

    abstract class UpdateRequest {
        Exception exception;


        public Exception getException() {
            return exception;
        }


        public void setException(Exception exception) {
            this.exception = exception;
        }


        abstract void execute(Connection connection) throws SQLException;
    }
}
