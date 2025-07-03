package com.moshe.client;

public class Respond {
    public RespondHeader header;
    public byte[] payload;

    public Respond() {
        header = new RespondHeader();
        payload = null;
    }

    public Respond(RespondHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public static Respond fromBytes(byte[] headerBytes, byte[] payloadBytes) {
        RespondHeader header = RespondHeader.fromBytes(headerBytes);
        return new Respond(header, payloadBytes);
    }
}

