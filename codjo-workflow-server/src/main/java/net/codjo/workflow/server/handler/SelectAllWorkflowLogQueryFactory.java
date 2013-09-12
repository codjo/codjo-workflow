package net.codjo.workflow.server.handler;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import net.codjo.database.api.query.PreparedQuery;
import net.codjo.mad.server.handler.HandlerException;
import net.codjo.mad.server.handler.sql.QueryBuilder;
import net.codjo.mad.server.handler.sql.QueryParameterFiller;
import net.codjo.mad.server.handler.sql.SqlHandler;
/**
 *
 */
public class SelectAllWorkflowLogQueryFactory implements QueryBuilder, QueryParameterFiller {
    private int indexParameter = 1;


    public String buildQuery(Map<String, String> args, SqlHandler sqlHandler) throws HandlerException {
        StringBuilder query = new StringBuilder()
              .append("select ")
              .append("ID, ")
              .append("REQUEST_TYPE, ")
              .append("REQUEST_DATE, ")
              .append("POST_AUDIT_DATE, ")
              .append("INITIATOR_LOGIN, ")
              .append("DISCRIMINENT, ")
              .append("PRE_AUDIT_STATUS, ")
              .append("POST_AUDIT_STATUS ")
              .append("from AP_WORKFLOW_LOG ");
        boolean conditionAlreadyAdded = addCondition(query, args, "requestType", false);
        conditionAlreadyAdded = addDateCondition(query, args, "requestDate", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(query, args, "initiatorLogin", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(query, args, "discriminent", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(query, args, "preAuditStatus", conditionAlreadyAdded);
        addCondition(query, args, "postAuditStatus", conditionAlreadyAdded);
        query.append("order by REQUEST_DATE DESC");
        return query.toString();
    }


    public void fillQuery(PreparedQuery statement, Map<String, String> arguments) throws SQLException {
        setConditionParameter(statement, arguments, "requestType");
        setDateConditionParameter(statement, arguments, "requestDate");
        setConditionParameter(statement, arguments, "initiatorLogin");
        setConditionParameter(statement, arguments, "discriminent");
        setConditionParameter(statement, arguments, "preAuditStatus");
        setConditionParameter(statement, arguments, "postAuditStatus");
    }


    protected boolean addCondition(StringBuilder script,
                                   Map<String, String> args,
                                   String key,
                                   boolean conditionAlreadyAdded) {
        String value = args.get(key);
        if (value != null && !"null".equals(value)) {
            if (conditionAlreadyAdded) {
                script.append("and ");
            }
            else {
                script.append("where ");
            }
            script.append(toSqlName(key)).append(" = ? ");
            return true;
        }
        return conditionAlreadyAdded;
    }


    protected boolean addDateCondition(StringBuilder script,
                                       Map<String, String> args,
                                       String key,
                                       boolean conditionAlreadyAdded) {
        String value = args.get(key);
        if (value != null && !"null".equals(value)) {
            if (conditionAlreadyAdded) {
                script.append("and ");
            }
            else {
                script.append("where ");
            }
            script.append(toSqlName(key)).append(" >= ? and ").append(toSqlName(key)).append(" < ? ");
            return true;
        }
        return conditionAlreadyAdded;
    }


    protected void setConditionParameter(PreparedQuery statement,
                                         Map<String, String> args,
                                         String key) throws SQLException {
        String value = args.get(key);
        if (value != null && !"null".equals(value)) {
            statement.setString(indexParameter++, value);
        }
    }


    protected void setDateConditionParameter(PreparedQuery statement,
                                             Map<String, String> args,
                                             String key) throws SQLException {
        String value = args.get(key);
        if (value != null && !"null".equals(value)) {
            Date requestDate = Date.valueOf(value);
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(requestDate);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date requestDatePlusOne = new Date(calendar.getTime().getTime());

            statement.setDate(indexParameter++, requestDate);
            statement.setDate(indexParameter++, requestDatePlusOne);
        }
    }


    private static String toSqlName(String propertyName) {
        StringBuilder buffer = new StringBuilder();

        if (isAlreadyInSQLSyntax(propertyName)) {
            return propertyName;
        }

        for (int i = 0; i < propertyName.length(); i++) {
            if (Character.isUpperCase(propertyName.charAt(i))) {
                buffer.append('_');
            }
            buffer.append(propertyName.charAt(i));
        }

        return buffer.toString().toUpperCase();
    }


    private static boolean isAlreadyInSQLSyntax(String propertyName) {
        for (int i = 0; i < propertyName.length(); i++) {
            char currentChar = propertyName.charAt(i);
            if (!Character.isUpperCase(currentChar) && currentChar != '_' && !Character
                  .isDigit(currentChar)) {
                return false;
            }
        }
        return true;
    }
}
