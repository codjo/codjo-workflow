package net.codjo.workflow.common.message;
import java.util.Arrays;
/**
 *
 */
public class HandlerJobRequest extends JobRequestWrapper {
    public static final String HANDLER_JOB_TYPE = "handler";
    private static final String XML_CONTENT = "xml-content";
    private static final String HANDLER_IDS = "handler-ids";


    public HandlerJobRequest() {
        this(new JobRequest());
    }


    public HandlerJobRequest(JobRequest request) {
        super(HANDLER_JOB_TYPE, request);
        setHandlerIds();
        request.setLoggable(false);
    }


    public String getXmlContent() {
        return getArgument(XML_CONTENT);
    }


    public void setXmlContent(String xmlContent) {
        setArgument(XML_CONTENT, xmlContent);
    }


    public String[] getHandlerIds() {
        String handlerIds = getArguments().get(HANDLER_IDS);
        if (handlerIds == null || "[]".equals(handlerIds)) {
            return new String[0];
        }
        return removeBlanks(removeBrackets(handlerIds)).split(",");
    }


    public void setHandlerIds(String... handlerIds) {
        setArgument(HANDLER_IDS, Arrays.toString(handlerIds));
    }


    private static String removeBrackets(String string) {
        return string.substring(1, string.length() - 1);
    }


    private static String removeBlanks(String string) {
        return string.replaceAll(" ", "");
    }
}
