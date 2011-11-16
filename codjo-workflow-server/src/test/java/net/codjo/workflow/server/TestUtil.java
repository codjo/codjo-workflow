package net.codjo.workflow.server;
import net.codjo.workflow.common.message.Arguments;
import net.codjo.workflow.common.message.JobRequest;
import java.util.Date;
import java.util.Map;
/**
 *
 */
public class TestUtil {

    private TestUtil() {
    }


    public static JobRequest createRequest(String type, Map<String, String> arguments) {
        return createRequest(type, null, null, null, new Arguments(arguments));
    }


    public static JobRequest createRequest(String type, String id, String parentId, String name) {
        return createRequest(type, id, parentId, name, null);
    }


    public static JobRequest createRequest(String type,
                                           String id,
                                           String parentId,
                                           String name,
                                           Arguments arguments) {
        JobRequest request = new JobRequest(type);
        request.setId(id);
        request.setParentId(parentId);
        request.setInitiatorLogin(name);
        request.setDate(new Date());
        if (arguments != null) {
            request.setArguments(arguments);
        }
        return request;
    }
}
