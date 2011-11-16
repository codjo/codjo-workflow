package net.codjo.workflow.common.util;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 *
 */
public class RequestUtil {
    private RequestUtil() {
    }


    public static String generateConversationId(long currentTime, Object identityInstance) {
        StringBuilder conversationId = new StringBuilder(26);
        conversationId.append("C-");
        try {
            byte[] address = InetAddress.getLocalHost().getAddress();
            conversationId.append(Integer.toString(address[2], 36));
            conversationId.append(Integer.toString(address[3], 36));
        }
        catch (UnknownHostException e) {
            conversationId.append(Integer.toString((int)(Math.random() * 1000)));
        }
        conversationId.append(Integer.toString(System.identityHashCode(identityInstance), 36));
        conversationId.append(Long.toString(currentTime, 36));

        return conversationId.toString();
    }
}
