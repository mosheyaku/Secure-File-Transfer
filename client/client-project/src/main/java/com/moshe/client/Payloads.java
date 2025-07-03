package com.moshe.client;

public class Payloads {

    public static class RespondClientIDPayload {
        private final String clientId;

        public RespondClientIDPayload(String clientId) {
            this.clientId = clientId;
        }

        public String getClientId() {
            return clientId;
        }
    }

    public static class RespondShareKeyPayload {
        private final String clientId;
        private final String encodedAesKey;

        public RespondShareKeyPayload(String clientId, String encodedAesKey) {
            this.clientId = clientId;
            this.encodedAesKey = encodedAesKey;
        }

        public String getClientId() {
            return clientId;
        }

        public String getEncodedAesKey() {
            return encodedAesKey;
        }
    }

    public static class RespondFileAcceptPayload {
        private final String clientId;
        private final int contentSize;
        private final String transferFileName;
        private final String checkSumHex;

        public RespondFileAcceptPayload(String clientId, int contentSize, String transferFileName, String checkSumHex) {
            this.clientId = clientId;
            this.contentSize = contentSize;
            this.transferFileName = transferFileName;
            this.checkSumHex = checkSumHex;
        }

        public String getClientId() {
            return clientId;
        }

        public int getContentSize() {
            return contentSize;
        }

        public String getTransferFileName() {
            return transferFileName;
        }

        public String getCheckSumHex() {
            return checkSumHex;
        }
    }
}

