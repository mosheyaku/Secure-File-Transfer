package com.moshe.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Request {
    public RequestHeader header;
    public byte[] payload;

    public Request() {
        header = new RequestHeader();
        payload = null;
    }

    public Request(RequestHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
        if (payload != null) {
            header.payloadSize = payload.length;
        } else {
            header.payloadSize = 0;
        }
    }

    // Serialize whole request to bytes (header + payload)
    public byte[] toBytes() {
        byte[] headerBytes = header.toBytes();
        if (payload == null || payload.length == 0) {
            return headerBytes;
        }
        byte[] full = new byte[headerBytes.length + payload.length];
        System.arraycopy(headerBytes, 0, full, 0, headerBytes.length);
        System.arraycopy(payload, 0, full, headerBytes.length, payload.length);
        return full;
    }
}

