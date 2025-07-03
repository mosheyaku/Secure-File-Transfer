package com.moshe.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class RequestHeader {
    public byte[] clientId = new byte[16];
    public byte version;
    public int code;
    public int payloadSize;

    public RequestHeader() {
        Arrays.fill(clientId, (byte) 0);
        version = 0;
        code = 0;
        payloadSize = 0;
    }

    public void setClientID(String id) {
        Arrays.fill(clientId, (byte) 0);
        byte[] idBytes = id.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(idBytes.length, 16);
        System.arraycopy(idBytes, 0, clientId, 0, len);
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(23);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(clientId);
        buffer.put(version);
        buffer.putShort((short) code);
        buffer.putInt(payloadSize);
        return buffer.array();
    }

    public static RequestHeader fromBytes(byte[] data) {
        if (data.length < 23) {
            throw new IllegalArgumentException("Invalid RequestHeader byte array length");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        RequestHeader header = new RequestHeader();
        buffer.get(header.clientId);
        header.version = buffer.get();
        header.code = Short.toUnsignedInt(buffer.getShort());
        header.payloadSize = buffer.getInt();
        return header;
    }

    public String getClientIDString() {
        return new String(clientId, StandardCharsets.UTF_8).trim();
    }
}
