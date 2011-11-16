/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
/**
 * Classe permettant de définir un modèle de requête {@link JobRequest}.
 */
public class JobRequestTemplate {
    private MatchExpression matchExpression;


    public JobRequestTemplate(MatchExpression matchExpression) {
        this.matchExpression = matchExpression;
    }


    public boolean match(JobRequest request) {
        return matchExpression.match(request);
    }


    public static JobRequestTemplate matchType(String type) {
        return new JobRequestTemplate(new TypeExpression(type));
    }


    public static JobRequestTemplate matchInitiator(String initiator) {
        return new JobRequestTemplate(new InitiatorExpression(initiator));
    }


    public static JobRequestTemplate matchArgument(String key, String value) {
        return new JobRequestTemplate(new ArgumentExpression(key, value));
    }


    public static JobRequestTemplate and(JobRequestTemplate left, JobRequestTemplate right) {
        return new JobRequestTemplate(new AndExpression(left, right));
    }


    public static JobRequestTemplate or(JobRequestTemplate left, JobRequestTemplate right) {
        return new JobRequestTemplate(new JobRequestTemplate.OrExpression(left, right));
    }


    public static JobRequestTemplate matchCustom(MatchExpression expression) {
        return new JobRequestTemplate(expression);
    }


    public static JobRequestTemplate matchAll() {
        return new JobRequestTemplate(new TrueExpression());
    }


    public static interface MatchExpression {
        boolean match(JobRequest request);
    }

    private static class TypeExpression implements MatchExpression {
        private final String type;


        TypeExpression(String type) {
            this.type = type;
        }


        public boolean match(JobRequest request) {
            return type.equals(request.getType());
        }
    }

    private static class InitiatorExpression implements MatchExpression {
        private final String initiator;


        InitiatorExpression(String initiator) {
            this.initiator = initiator;
        }


        public boolean match(JobRequest request) {
            return initiator.equals(request.getInitiatorLogin());
        }
    }

    private static class ArgumentExpression implements MatchExpression {
        private final String key;
        private final String value;


        ArgumentExpression(String key, String value) {
            this.key = key;
            this.value = value;
        }


        public boolean match(JobRequest request) {
            return request.getArguments() != null
                   && value.equals(request.getArguments().get(key));
        }
    }

    private static class AndExpression implements MatchExpression {
        private final JobRequestTemplate left;
        private final JobRequestTemplate right;


        AndExpression(JobRequestTemplate left, JobRequestTemplate right) {
            this.left = left;
            this.right = right;
        }


        public boolean match(JobRequest request) {
            return left.match(request) && right.match(request);
        }
    }

    private static class OrExpression implements MatchExpression {
        private final JobRequestTemplate left;
        private final JobRequestTemplate right;


        OrExpression(JobRequestTemplate left, JobRequestTemplate right) {
            this.left = left;
            this.right = right;
        }


        public boolean match(JobRequest request) {
            return left.match(request) || right.match(request);
        }
    }

    private static class TrueExpression implements MatchExpression {
        public boolean match(JobRequest request) {
            return true;
        }
    }
}
