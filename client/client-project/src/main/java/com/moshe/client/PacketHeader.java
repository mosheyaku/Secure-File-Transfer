package com.moshe.client;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PacketHeader {

    public static class RequestHeader {

        private byte[] clientId = new byte[16];
        private byte version;
        private int code;
        private long payloadSize;

        public RequestHeader() {}

        public byte[] getClientId() {
            return clientId;
        }

        public void setClientId(String clientIdStr) {
            byte[] bytes = clientIdStr.getBytes(StandardCharsets.UTF_8);
            Arrays.fill(this.clientId, (byte)0);
            System.arraycopy(bytes, 0, this.clientId, 0, Math.min(bytes.length, 16));
        }

        public String getClientIdString() {
            int len = 0;
            for (; len < clientId.length; len++) {
                if (clientId[len] == 0) break;
            }
            return new String(clientId, 0, len, StandardCharsets.UTF_8);
        }

        public byte getVersion() {
            return version;
        }

        public void setVersion(byte version) {
            this.version = version;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public long getPayloadSize() {
            return payloadSize;
        }

        public void setPayloadSize(long payloadSize) {
            this.payloadSize = payloadSize;
        }

        public static RequestHeader fromBytes(byte[] buf) throws IllegalArgumentException {
            if (buf.length < 23) {
                throw new IllegalArgumentException("Buffer too short to parse RequestHeader");
            }

            RequestHeader header = new RequestHeader();

            System.arraycopy(buf, 0, header.clientId, 0, 16);
            header.version = buf[16];

            header.code = ((buf[17] & 0xFF)) | ((buf[18] & 0xFF) << 8);

            header.payloadSize = ((long)(buf[19] & 0xFF)) |
                                ((long)(buf[20] & 0xFF) << 8) |
                                ((long)(buf[21] & 0xFF) << 16) |
                                ((long)(buf[22] & 0xFF) << 24);

            return header;
        }

        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(23);
            buffer.put(clientId, 0, 16);
            buffer.put(version);
            buffer.put((byte)(code & 0xFF));
            buffer.put((byte)((code >> 8) & 0xFF));
            buffer.put((byte)((payloadSize) & 0xFF));
            buffer.put((byte)((payloadSize >> 8) & 0xFF));
            buffer.put((byte)((payloadSize >> 16) & 0xFF));
            buffer.put((byte)((payloadSize >> 24) & 0xFF));
            return buffer.array();
        }
    }

    public static class RespondHeader {
        private byte version;
        private int code;
        private long payloadSize;

        public RespondHeader() {
            this.version = Constants.CLIENT_VERSION;
        }

        public byte getVersion() {
            return version;
        }

        public void setVersion(byte version) {
            this.version = version;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public long getPayloadSize() {
            return payloadSize;
        }

        public void setPayloadSize(long payloadSize) {
            this.payloadSize = payloadSize;
        }

        public static RespondHeader fromBytes(byte[] buf) throws IllegalArgumentException {
            if (buf.length < 7) {
                throw new IllegalArgumentException("Buffer too short to parse RespondHeader");
            }
            RespondHeader header = new RespondHeader();
            header.version = buf[0];
            header.code = ((buf[1] & 0xFF)) | ((buf[2] & 0xFF) << 8);
            header.payloadSize = ((long)(buf[3] & 0xFF)) |
                                 ((long)(buf[4] & 0xFF) << 8) |
                                 ((long)(buf[5] & 0xFF) << 16) |
                                 ((long)(buf[6] & 0xFF) << 24);
            return header;
        }

        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(7);
            buffer.put(version);
            buffer.put((byte)(code & 0xFF));
            buffer.put((byte)((code >> 8) & 0xFF));
            buffer.put((byte)((payloadSize) & 0xFF));
            buffer.put((byte)((payloadSize >> 8) & 0xFF));
            buffer.put((byte)((payloadSize >> 16) & 0xFF));
            buffer.put((byte)((payloadSize >> 24) & 0xFF));
            return buffer.array();
        }
    }
}

