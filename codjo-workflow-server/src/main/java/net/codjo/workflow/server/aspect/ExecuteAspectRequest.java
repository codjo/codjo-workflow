package net.codjo.workflow.server.aspect;
import net.codjo.aspect.AspectContext;
import net.codjo.mad.server.handler.XmlCodec;
import net.codjo.mad.server.handler.aspect.AspectBranchId;
import net.codjo.workflow.common.message.JobRequest;
import net.codjo.workflow.common.message.JobRequestWrapper;
/**
 *
 */
public class ExecuteAspectRequest extends JobRequestWrapper {
    public static final String JOB_ID = "execute-aspect";
    private static final String ASPECT_BRANCH_ID = "aspect-branch-id";
    private static final String ASPECT_CONTEXT = "aspect-context";


    public ExecuteAspectRequest(JobRequest request) {
        super(JOB_ID, request);
    }


    public ExecuteAspectRequest() {
        super(JOB_ID, new JobRequest());
    }


    public void setAspectBranchId(AspectBranchId branchId) {
        setArgument(ASPECT_BRANCH_ID, XmlCodec.toXml(branchId));
    }


    public AspectBranchId getAspectBranchId() {
        return XmlCodec.extractAspectBranchId(getArgument(ASPECT_BRANCH_ID));
    }


    public void setAspectContext(AspectContext context) {
        setArgument(ASPECT_CONTEXT, XmlCodec.toXml(context));
    }


    public AspectContext getAspectContext() {
        return XmlCodec.extractAspectContext(getArgument(ASPECT_CONTEXT));
    }
}
