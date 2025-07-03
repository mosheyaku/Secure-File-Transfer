package com.moshe.client;

public class Constants {
    public static final int REQUEST_REGISTER = 1025;
    public static final int REQUEST_SENDING_PUBKEY = 1026;
    public static final int REQUEST_LOGIN = 1027;
    public static final int REQUEST_SENDING_FILE = 1028;
    public static final int REQUEST_VALID_CRC = 1029;
    public static final int REQUEST_INVALID_CRC = 1030;
    public static final int REQUEST_LAST_INVALID_CRC = 1031;

    public static final int RESPOND_REGISTER_SUCCESS = 2100;
    public static final int RESPOND_REGISTER_FAIL = 2101;
    public static final int RESPOND_SENDING_ENCKEY = 2102;
    public static final int RESPOND_FILE_ACCEPTED = 2103;
    public static final int RESPOND_MESSAGE_CONFIRMED = 2104;
    public static final int RESPOND_LOGIN_CONFIRMED = 2105;
    public static final int RESPOND_LOGIN_REJECTED = 2106;

    public static final int CLIENT_VERSION = 3;
    public static final int MAX_RETRY_COUNT = 3;
    public static final String TRANSFER_FILE = "transfer.info";
    public static final String ME_FILE = "me.info";
    public static final String PRIVATE_KEY_FILE = "priv.key";
}

