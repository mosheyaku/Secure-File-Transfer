package com.moshe.client;

import com.moshe.client.utils.Logger;

public class Main {
    public static void main(String[] args) {
        Client client = new Client();
        System.out.println("Current dir: " + System.getProperty("user.dir"));

        try {
            Logger.log("MAIN", "Reading settings...");
            if (!client.readSetting()) {
                Logger.log("MAIN", "Failed to read settings.");
                return;
            }

            Logger.log("MAIN", "Connecting to server...");
            if (!client.connectToServer()) {
                Logger.log("MAIN", "Failed to connect to server.");
                return;
            }

            Logger.log("MAIN", "Checking registration...");
            boolean success;
            if (!client.isRegistered()) {
                Logger.log("MAIN", "Registering and sharing key...");
                success = client.registerClient() && client.shareKey();
            } else {
                Logger.log("MAIN", "Logging in...");
                success = client.login();
            }

            if (!success) {
                Logger.log("MAIN", "Login or registration failed.");
                return;
            }

            Logger.log("MAIN", "Starting file transfer...");
            int retryCount = 0;
            while (retryCount < Constants.MAX_RETRY_COUNT) {
                Logger.log("MAIN", "Attempt #" + (retryCount + 1));
                retryCount++;

                Logger.log("MAIN", "Sending file...");
                if (!client.sendFile()) {
                    Logger.log("MAIN", "sendFile() failed. Retrying...");
                    continue;
                }

                Logger.log("MAIN", "Checking if server accepted the file...");
                if (!client.checkAccept(retryCount)) {
                    Logger.log("MAIN", "checkAccept() failed. Retrying...");
                    continue;
                }

                Logger.log("MAIN", "Confirming CRC...");
                if (!client.confirmCRC(retryCount)) {
                    Logger.log("MAIN", "confirmCRC() failed. Retrying...");
                    continue;
                }

                Logger.log("MAIN", "File transfer completed successfully.");
                break;
            }

        } catch (Exception e) {
            Logger.log("EXCEPTION", "Unhandled exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

