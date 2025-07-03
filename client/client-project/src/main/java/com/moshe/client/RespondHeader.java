package com.moshe.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RespondHeader {
    public byte version;
    public int code;
    public int payloadSize;

    public RespondHeader() {
        version = 0;
        code = 0;
        payloadSize = 0;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put(version);
        buffer.putShort((short) code);
        buffer.putInt(payloadSize);
        return buffer.array();
    }

    public static RespondHeader fromBytes(byte[] data) {
        if (data.length < 7) {
            throw new IllegalArgumentException("Invalid RespondHeader byte array length");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        RespondHeader header = new RespondHeader();
        header.version = buffer.get();
        header.code = Short.toUnsignedInt(buffer.getShort());
        header.payloadSize = buffer.getInt();

        return header;
    }
}
