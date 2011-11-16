package net.codjo.workflow.common.message;
import net.codjo.agent.AclMessage;
/**
 *
 */
public class JobContractResultOntology {
    private JobContractResultOntology() {
    }


    public static void accept(AclMessage cfpResult) {
        cfpResult.setContent("i'm booked for you");
    }


    public static void acceptAndDelegate(AclMessage cfpResult, String nickName) {
        cfpResult.setContent("delegate-to:" + nickName);
    }


    public static String extractDelegate(AclMessage cfpResult) {
        return cfpResult.getContent().substring("delegate-to:".length());
    }


    public static boolean isDelegate(AclMessage cfpResult) {
        return cfpResult.getContent().startsWith("delegate-to:");
    }
}
