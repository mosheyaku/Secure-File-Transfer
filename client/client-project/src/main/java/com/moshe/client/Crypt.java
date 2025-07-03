package com.moshe.client;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

public class Crypt {

    // RSA public key encoded in Base64
    private String publicKeyBase64;

    // RSA private key encoded in Base64
    private String privateKeyBase64;

    // AES key encoded in Base64
    private String encodedAesKey;

    // Return the Base64-encoded RSA public key
    public String getPublicKey() {
        return publicKeyBase64;
    }

    // Return the Base64-encoded RSA private key
    public String getPrivateKey() {
        return privateKeyBase64;
    }

    // Set the RSA private key from a Base64 string
    public void setPrivateKey(String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    // Return the Base64-encoded AES key
    public String getEncodedAesKey() {
        return encodedAesKey;
    }

    // Set the AES key from a Base64 string
    public void setEncodedAesKey(String encodedAesKey) {
        this.encodedAesKey = encodedAesKey;
    }

    // Generates a new RSA key pair (public/private)
    public void generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);  // RSA key size (bits)

        KeyPair keyPair = keyGen.generateKeyPair();  // Create key pair

        PublicKey pubKey = keyPair.getPublic();
        PrivateKey privKey = keyPair.getPrivate();

        // Store the keys as Base64 strings
        publicKeyBase64 = Base64.getEncoder().encodeToString(pubKey.getEncoded());
        privateKeyBase64 = Base64.getEncoder().encodeToString(privKey.getEncoded());
    }

    // Decrypts a Base64-encoded string using RSA private key
    public String decryptRSA(String content) throws Exception {
        // Decode the Base64 private key back into raw bytes
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);

        // Reconstruct the private key from bytes using PKCS#8 format
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        // Prepare the RSA cipher for decryption using standard padding
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Decode the input content (encrypted) and decrypt it
        byte[] encryptedBytes = Base64.getDecoder().decode(content);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        // Convert the decrypted bytes into a UTF-8 string
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Encrypts a byte array using AES (CBC mode, PKCS#5 padding)
    public String encryptAES(byte[] content) throws Exception {
        // Decode the Base64 AES key back into bytes
        byte[] aesKey = Base64.getDecoder().decode(encodedAesKey);

        // AES CBC requires an IV (initialization vector); here we use 16 zero-bytes
        byte[] iv = new byte[16];

        // Setup AES key and IV for the cipher
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Initialize cipher for AES encryption
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // Encrypt the content
        byte[] encryptedBytes = cipher.doFinal(content);

        // Return the result encoded as Base64
        return Base64.getEncoder().encodeToString(encryptedBytes);  // ✅ SAFE FOR UTF-8 TRANSFER
    }
}
