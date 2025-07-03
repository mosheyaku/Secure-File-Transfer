package com.moshe.client;

import com.moshe.client.Payloads.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import com.moshe.client.utils.CRCUtils;

public class Client {

    // Server IP and port
    private String serverIP;
    private int serverPort;

    // Client identity and file information
    private String clientID = "";
    private String clientName = "";
    private String transferFileName = "";
    private boolean registered = false;

    // Networking fields
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    // Cryptographic handler (RSA + AES)
    private Crypt crypt = new Crypt();

    // Constants used across methods
    private static final int MAX_RETRY_COUNT = Constants.MAX_RETRY_COUNT;
    private static final int CLIENT_VERSION = Constants.CLIENT_VERSION;

    // Constructor: initializes client with default values
    public Client() {
        this.serverPort = 0;
        this.registered = false;
        this.clientID = "";
    }

    // Check if the client is registered
    public boolean isRegistered() {
        return registered;
    }

    // Reads server and client settings from local files
    public boolean readSetting() {
        // Read server IP, port, client name, and filename from transfer.info
        try (BufferedReader br = new BufferedReader(new FileReader(Constants.TRANSFER_FILE))) {
            String line = br.readLine();
            if (line == null) return false;
            int pos = line.indexOf(":");
            if (pos < 0) return false;
            serverIP = line.substring(0, pos);
            serverPort = Integer.parseInt(line.substring(pos + 1));

            clientName = br.readLine();
            transferFileName = br.readLine();

        } catch (IOException e) {
            log("READ SETTINGS", "Error reading settings file: " + e.getMessage());
            return false;
        }

        // Try to read clientID and name from me.info (if exists)
        registered = false;
        try (BufferedReader br = new BufferedReader(new FileReader(Constants.ME_FILE))) {
            String nameLine = br.readLine();
            String idLine = br.readLine();
            if (nameLine != null && idLine != null) {
                clientName = nameLine;
                clientID = idLine;
                registered = true;
            }
        } catch (IOException ignored) {
            // It’s fine if the file doesn't exist
        }

        // If registered, read the private key from file
        if (registered) {
            try (BufferedReader br = new BufferedReader(new FileReader(Constants.PRIVATE_KEY_FILE))) {
                String privateKey = br.readLine();
                if (privateKey == null) return false;
                crypt.setPrivateKey(privateKey);
            } catch (IOException e) {
                log("READ SETTINGS", "Error reading private key file: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    // Establishes connection with the server
    public boolean connectToServer() {
        try {
            socket = new Socket(serverIP, serverPort);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            log("CONNECTION", "Success.");
            return true;
        } catch (IOException e) {
            log("CONNECTION", "Fail: " + e.getMessage());
            return false;
        }
    }

    // Helper method to read exactly `length` bytes from input stream
    private byte[] readExactly(int length) throws IOException {
        byte[] buffer = new byte[length];
        int totalRead = 0;
        while (totalRead < length) {
            int bytesRead = input.read(buffer, totalRead, length - totalRead);
            if (bytesRead == -1) {
                throw new IOException("Stream closed before reading fully");
            }
            totalRead += bytesRead;
        }
        return buffer;
    }

    // Sends request to server (header + optional payload)
    private void sendRequest(Request request) throws IOException {
        byte[] headerBytes = request.header.toBytes();
        output.write(headerBytes);

        if (request.payload != null && request.payload.length > 0) {
            output.write(request.payload);
        }
        output.flush();
    }

    // This method reads a response from the input stream, parses the header and payload,
    // and returns a Respond object containing both.
    private Respond receiveRespond() throws IOException {
        byte[] headerBuf = readExactly(7);
        RespondHeader header = RespondHeader.fromBytes(headerBuf);

        byte[] payload = new byte[0];

        if (header.payloadSize > 0) {
            payload = readExactly(header.payloadSize);
        }

        return new Respond(header, payload);
    }

    // Builds a fixed-length payload from string (zero-padded)
    private byte[] buildFixedSizePayload(String str, int size) {
        byte[] payload = new byte[size];
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(strBytes.length, size);
        System.arraycopy(strBytes, 0, payload, 0, len);
        return payload;
    }

    // Builds name payload (255 bytes)
    private byte[] buildRequestNamePayload() {
        return buildFixedSizePayload(clientName, 255);
    }

    // Builds file name payload (255 bytes)
    private byte[] buildRequestFileNamePayload() {
        return buildFixedSizePayload(transferFileName, 255);
    }

    // Builds share key payload: [name (255) + public key (variable length)]
    private byte[] buildRequestShareKeyPayload() {
        byte[] nameBytes = buildFixedSizePayload(clientName, 255);
        byte[] pubKeyBytes = crypt.getPublicKey().getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[255 + pubKeyBytes.length];
        System.arraycopy(nameBytes, 0, payload, 0, 255);
        System.arraycopy(pubKeyBytes, 0, payload, 255, pubKeyBytes.length);
        return payload;
    }

    // Encrypts and builds file transfer payload
    private byte[] buildRequestSendFilePayload() throws Exception {
        File file = new File(transferFileName);
        long fileSize = file.length();
        if (fileSize > Integer.MAX_VALUE) throw new IOException("File too large");

        // Read the file content into byte array
        byte[] fileContent = new byte[(int) fileSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            int read = fis.read(fileContent);
            if (read != fileSize) throw new IOException("Failed to read entire file");
        }

        // Encrypt file content using AES, result is base64 string
        String encryptedContent = crypt.encryptAES(fileContent);

        // Use US_ASCII to safely convert base64 string to bytes
        byte[] encryptedBytes = encryptedContent.getBytes(StandardCharsets.US_ASCII);

        // Build final payload: [4B fileSize][255B fileName][N B encryptedContent]
        int payloadSize = 4 + 255 + encryptedBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(payloadSize);
        buffer.putInt((int) fileSize);
        buffer.put(buildFixedSizePayload(transferFileName, 255));
        buffer.put(encryptedBytes);

        return buffer.array();
    }

    // Parses client ID from server's response
    private RespondClientIDPayload parseClientIDPayload(byte[] payload) {
        String clientId = new String(payload, StandardCharsets.UTF_8).trim();
        return new RespondClientIDPayload(clientId);
    }

    // Parses shared AES key response and decrypts the AES key
    private RespondShareKeyPayload parseShareKeyPayload(byte[] payload) throws Exception {
        if (payload.length < 16) throw new IllegalArgumentException("Payload too short");

        byte[] clientIdBytes = Arrays.copyOfRange(payload, 0, 16);
        String clientId = new String(clientIdBytes, StandardCharsets.UTF_8).trim();

        byte[] encryptedKeyBytes = Arrays.copyOfRange(payload, 16, payload.length);
        String encryptedAesKey = new String(encryptedKeyBytes, StandardCharsets.UTF_8).trim();

        String decodedAesKey = crypt.decryptRSA(encryptedAesKey);

        return new RespondShareKeyPayload(clientId, decodedAesKey);
    }

    // Parses file acceptance response from server
    private RespondFileAcceptPayload parseFileAcceptPayload(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);

        byte[] clientIdBytes = new byte[16];
        buffer.get(clientIdBytes);
        String clientId = new String(clientIdBytes, StandardCharsets.UTF_8).trim();

        int contentSize = buffer.getInt();

        byte[] fileNameBytes = new byte[255];
        buffer.get(fileNameBytes);
        String fileName = new String(fileNameBytes, StandardCharsets.UTF_8).trim();

        int checksum = buffer.getInt();
        String checksumHex = String.format("%08x", checksum);

        return new RespondFileAcceptPayload(clientId, contentSize, fileName, checksumHex);
    }

    // Sends registration request and receives client ID from server
    public boolean registerClient() throws Exception {
        log("REGISTER", "Starting registration...");
        RequestHeader header = new RequestHeader();
        header.setClientID(clientID);
        header.version = CLIENT_VERSION;
        header.code = Constants.REQUEST_REGISTER;

        byte[] payload = buildRequestNamePayload();
        header.payloadSize = payload.length;

        Request request = new Request(header, payload);
        sendRequest(request);
        Respond respond = receiveRespond();

        if (respond.header.code != Constants.RESPOND_REGISTER_SUCCESS || respond.payload.length == 0) {
            log("REGISTER", "Fail: Unexpected server response or empty payload.");
            return false;
        }
        // Save assigned client ID
        RespondClientIDPayload respPayload = parseClientIDPayload(respond.payload);
        clientID = respPayload.getClientId();

        // Generate RSA key pair
        crypt.generateRSAKeyPair();

        // Save client ID and name
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(Constants.ME_FILE))) {
            bw.write(clientName);
            bw.newLine();
            bw.write(clientID);
            bw.newLine();
        }

        // Save private key to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(Constants.PRIVATE_KEY_FILE))) {
            bw.write(crypt.getPrivateKey());
            bw.newLine();
        }

        log("REGISTER", "Success.");
        registered = true;
        return true;
    }

    // Sends public RSA key and receives AES key encrypted with it
    public boolean shareKey() throws Exception {
        log("SHARE KEY", "Sending public key...");
        RequestHeader header = new RequestHeader();
        header.setClientID(clientID);
        header.version = CLIENT_VERSION;
        header.code = Constants.REQUEST_SENDING_PUBKEY;

        byte[] payload = buildRequestShareKeyPayload();
        header.payloadSize = payload.length;

        Request request = new Request(header, payload);
        sendRequest(request);

        Respond respond = receiveRespond();
        if (respond.header.code != Constants.RESPOND_SENDING_ENCKEY || respond.payload.length == 0) {
            log("SHARE KEY", "Fail: Unexpected server response or empty payload.");
            return false;
        }

        // Set AES key
        RespondShareKeyPayload respPayload = parseShareKeyPayload(respond.payload);
        crypt.setEncodedAesKey(respPayload.getEncodedAesKey());

        log("SHARE KEY", "Success.");
        return true;
    }

    // Sends login request and receives AES key again (same as in shareKey)
    public boolean login() throws Exception {
        log("LOGIN", "Attempting to login...");
        RequestHeader header = new RequestHeader();
        header.setClientID(clientID);
        header.version = CLIENT_VERSION;
        header.code = Constants.REQUEST_LOGIN;

        byte[] payload = buildRequestNamePayload();
        header.payloadSize = payload.length;

        Request request = new Request(header, payload);
        sendRequest(request);

        Respond respond = receiveRespond();
        if (respond.header.code != Constants.RESPOND_LOGIN_CONFIRMED || respond.payload.length == 0) {
            log("LOGIN", "Fail: Unexpected server response or empty payload.");
            return false;
        }

        RespondShareKeyPayload respPayload = parseShareKeyPayload(respond.payload);
        crypt.setEncodedAesKey(respPayload.getEncodedAesKey());

        log("LOGIN", "Success.");
        return true;
    }

    // Encrypts and sends file to server
    public boolean sendFile() throws Exception {
        log("SEND FILE", "Sending file...");
        RequestHeader header = new RequestHeader();
        header.setClientID(clientID);
        header.version = CLIENT_VERSION;
        header.code = Constants.REQUEST_SENDING_FILE;

        byte[] payload = buildRequestSendFilePayload();
        header.payloadSize = payload.length;

        Request request = new Request(header, payload);
        sendRequest(request);

        log("SEND FILE", "Success.");
        return true;
    }

    // Verifies CRC returned from server and responds accordingly
    public boolean checkAccept(int retries) throws Exception {
        Respond respond = receiveRespond();
        if (respond.header.code != Constants.RESPOND_FILE_ACCEPTED || respond.payload.length == 0) {
            log("CHECK ACCEPT", "Fail. Internal server error.");
            return false;
        }

        RespondFileAcceptPayload respPayload = parseFileAcceptPayload(respond.payload);
        String fileCRC = CRCUtils.calculateFileCRC32(transferFileName);

        RequestHeader header = new RequestHeader();
        header.setClientID(clientID);
        header.version = CLIENT_VERSION;

        // Compare CRC from server and local file
        if (respPayload.getCheckSumHex().equalsIgnoreCase(fileCRC)) {
            log("CHECK ACCEPT", "Success.");
            header.code = Constants.REQUEST_VALID_CRC;
        } else {
            log("CHECK ACCEPT", "Fail. CRC mismatch.");
            header.code = (retries < MAX_RETRY_COUNT)
                    ? Constants.REQUEST_INVALID_CRC
                    : Constants.REQUEST_LAST_INVALID_CRC;
        }

        byte[] payload = buildRequestFileNamePayload();
        header.payloadSize = payload.length;

        Request request = new Request(header, payload);
        sendRequest(request);

        return respPayload.getCheckSumHex().equalsIgnoreCase(fileCRC);
    }

    // Final confirmation after CRC is accepted by server
    public boolean confirmCRC(int retries) throws IOException {
        Respond respond = receiveRespond();
        if (respond.header.code != Constants.RESPOND_MESSAGE_CONFIRMED || respond.payload.length == 0) {
            log("CONFIRM", "Fail. Internal server error.");
            return false;
        }
        if (retries > MAX_RETRY_COUNT)
            log("CONFIRM", "Fail.");
        else
            log("CONFIRM", "Success. File transfer completed.");
        return true;
    }

    // Helper logging method
    private void log(String header, String content) {
        System.out.println("[" + header + "] : " + content);
    }
}
