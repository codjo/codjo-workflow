package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 * Classe de test de {@link Arguments}.
 */
public class ArgumentsTest extends TestCase {
    public void test_toMap() throws Exception {
        Arguments arguments = new Arguments();
        arguments.put("key1", "value1");
        arguments.put("key2", "value2");
        assertEquals("{key1=value1, key2=value2}", arguments.toMap().toString());
    }
}
