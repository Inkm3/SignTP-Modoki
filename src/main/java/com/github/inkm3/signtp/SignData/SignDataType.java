package com.github.inkm3.signtp.SignData;

import org.apache.commons.lang3.SerializationUtils;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class SignDataType implements PersistentDataType<byte[], SerializeTextData> {

    private static final String[] def = new String[] { "", "", "", "" };

    @Override
    public @NotNull Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public @NotNull Class<SerializeTextData> getComplexType() {
        return SerializeTextData.class;
    }

    @Override
    public byte @NotNull [] toPrimitive(@NotNull SerializeTextData complex, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        return SerializationUtils.serialize(complex);
    }

    @Override
    public @NotNull SerializeTextData fromPrimitive(byte @NotNull [] primitive, @NotNull PersistentDataAdapterContext persistentDataAdapterContext) {
        try {
            InputStream i = new ByteArrayInputStream(primitive);
            ObjectInputStream o = new ObjectInputStream(i);
            return (SerializeTextData) o.readObject();
        } catch (IOException | ClassNotFoundException ignore) { }
        return new SerializeTextData(def, def, def);
    }

}

