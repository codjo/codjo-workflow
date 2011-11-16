package net.codjo.workflow.server.handler;
import net.codjo.mad.server.handler.HandlerException;
import net.codjo.mad.server.handler.sql.QueryBuilder;
import net.codjo.mad.server.handler.sql.SqlHandler;
import java.util.Map;

public class SelectAllWorkflowLogQueryFactory implements QueryBuilder {

    public String buildQuery(Map<String, String> args, SqlHandler sqlHandler) throws HandlerException {
        StringBuilder script = new StringBuilder()
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
        boolean conditionAlreadyAdded = addCondition(script, args, "requestType", false);
        conditionAlreadyAdded = addDateCondition(script, args, "requestDate", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(script, args, "initiatorLogin", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(script, args, "discriminent", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(script, args, "preAuditStatus", conditionAlreadyAdded);
        conditionAlreadyAdded = addCondition(script, args, "postAuditStatus", conditionAlreadyAdded);
        if (conditionAlreadyAdded) {
            script.append(" ");
        }
        script.append("order by REQUEST_DATE DESC");

        return script.toString();
    }


    public static String toSqlName(String propertyName) {
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


    protected boolean addCondition(StringBuilder script,
                                   Map<String, String> args,
                                   String key,
                                   boolean conditionAlreadyAdded) {
        String value = args.get(key);
        if (value != null && !"null".equals(value)) {
            if (conditionAlreadyAdded) {
                script.append(" and ");
            }
            else {
                script.append("where ");
            }
            script.append(toSqlName(key)).append(" = '").append(value).append("'");
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
                script.append(" and ");
            }
            else {
                script.append("where ");
            }
            script.append("convert(datetime, (convert(char(12), ")
                  .append(toSqlName(key))
                  .append(", 112)))")
                  .append(" = '")
                  .append(value)
                  .append("'");
            return true;
        }
        return conditionAlreadyAdded;
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
