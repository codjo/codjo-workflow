package net.codjo.workflow.server.handler;
import net.codjo.agent.UserId;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
/**
 *
 */
public class DefaultHandlerContextManagerTest {
    private DefaultHandlerContextManager handlerContextManager = new DefaultHandlerContextManager();
    private UserId user1 = UserId.createId("user1", "password");
    private UserId user2 = UserId.createId("user2", "password");
    private HandlerExecutorCommandMock command1 = new HandlerExecutorCommandMock();
    private HandlerExecutorCommandMock command2 = new HandlerExecutorCommandMock();
    private HandlerExecutorCommandMock command3 = new HandlerExecutorCommandMock();


    @Before
    public void setUp() {
        handlerContextManager.setHandlerExecutorCommand(user1, "request1", command1);
        handlerContextManager.setHandlerExecutorCommand(user1, "request2", command2);
        handlerContextManager.setHandlerExecutorCommand(user2, "request3", command3);
    }


    @Test
    public void test_nominal() throws Exception {
        assertSame(command1, handlerContextManager.getHandlerExecutorCommand(user1, "request1"));
        assertSame(command2, handlerContextManager.getHandlerExecutorCommand(user1, "request2"));
        assertSame(command3, handlerContextManager.getHandlerExecutorCommand(user2, "request3"));
    }


    @Test
    public void test_cleanUserProcessor() throws Exception {
        handlerContextManager.cleanUserContext(user1);

        try {
            handlerContextManager.getHandlerExecutorCommand(user1, "request1");
            fail();
        }
        catch (Exception e) {
            ;
        }

        assertSame(command3, handlerContextManager.getHandlerExecutorCommand(user2, "request3"));
    }
}


