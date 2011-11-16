/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import junit.framework.TestCase;
/**
 * Classe de test de {@link Arguments}.
 */
public class ArgumentTest extends TestCase {
    private Arguments arguments;


    public void test_serializable() throws Exception {
        arguments.put("arg0", "valéo");
        arguments.put("arg1", "val1");

        byte[] serializedArgument = TestUtil.toByteArray(arguments);

        Arguments deserializedArguments =
              (Arguments)TestUtil.toObject(serializedArgument);
        assertNotNull(deserializedArguments);
    }


    @Override
    protected void setUp() throws Exception {
        arguments = new Arguments();
    }


    public void test_constructeur() throws Exception {
        arguments = new Arguments("arg0", "val0");
        assertEquals("val0", arguments.get("arg0"));
    }


    public void test_setArgument() throws Exception {
        arguments.put("arg0", "val0");
        assertEquals("val0", arguments.get("arg0"));

        assertEquals(null, arguments.get("unknownArg"));
    }


    public void test_decode() throws Exception {
        String encodedArgument = "arg0 = val0\n" + "arg1 = val1\n";

        arguments.decode(encodedArgument);

        assertEquals("val0", arguments.get("arg0"));
        assertEquals("val1", arguments.get("arg1"));
    }


    public void test_encode() throws Exception {
        String expectedArgument = "arg0=valéo\narg1=val1\n";

        arguments.put("arg0", "valéo");
        arguments.put("arg1", "val1");

        assertEquals(expectedArgument, arguments.encode());
    }
}
