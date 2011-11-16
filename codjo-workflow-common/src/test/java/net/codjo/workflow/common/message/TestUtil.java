/*
 * Team : AGF AM / OSI / SI / BO
 *
 * Copyright (c) 2001 AGF Asset Management.
 */
package net.codjo.workflow.common.message;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
/**
 * Classe utilitaire pour les objets serializables.
 */
public class TestUtil {
    private TestUtil() {
    }


    public static <T extends Serializable> T serialize(T object) throws IOException, ClassNotFoundException {
        //noinspection unchecked
        return (T)toObject(toByteArray(object));
    }


    public static Serializable toObject(byte[] serializedRequest)
          throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(serializedRequest);
        ObjectInputStream streamInput = new ObjectInputStream(in);

        return (Serializable)streamInput.readObject();
    }


    public static byte[] toByteArray(Serializable request)
          throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(request);
        return out.toByteArray();
    }
}
