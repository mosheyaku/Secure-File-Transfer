package com.moshe.client.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;

public class CRCUtils {

    public static String calculateFileCRC32(String filePath) throws IOException {
        CRC32 crc = new CRC32();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }
        return String.format("%08x", crc.getValue());
    }

    public static String calculateCRC32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return String.format("%08x", crc.getValue());
    }
}

