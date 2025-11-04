package com.fidd.core.encryption.aes256;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class Aes256CbcEncryptionAlgorithmTest {
    private static Aes256CbcEncryptionAlgorithm encryptionAlgorithm;
    private static byte[] keyData;
    private static final String originalText = "This is a secret message.";

    @BeforeAll
    static void setUp() {
        encryptionAlgorithm = new Aes256CbcEncryptionAlgorithm();
        keyData = encryptionAlgorithm.generateNewKeyData();
    }

    @Test
    void testEncryptDecryptStream() {
        // Encrypt the original text
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        encryptionAlgorithm.encrypt(keyData, inputStream, outputStream);
        byte[] ciphertext = outputStream.toByteArray();

        // Decrypt the ciphertext
        ByteArrayInputStream cipherInputStream = new ByteArrayInputStream(ciphertext);
        ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        encryptionAlgorithm.decrypt(keyData, cipherInputStream, decryptedOutputStream);

        // Convert to String
        String decryptedText = decryptedOutputStream.toString(StandardCharsets.UTF_8);

        // Assert that the decrypted text matches the original text
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testEncryptDecrypt() {
        byte[] plaintext = originalText.getBytes(StandardCharsets.UTF_8);

        // Encrypt the plaintext
        byte[] ciphertext = encryptionAlgorithm.encrypt(keyData, plaintext);

        // Decrypt the ciphertext
        byte[] decryptedTextBytes = encryptionAlgorithm.decrypt(keyData, ciphertext);
        String decryptedText = new String(decryptedTextBytes, StandardCharsets.UTF_8);

        // Assert that the decrypted text matches the original text
        assertEquals(originalText, decryptedText);
    }

    @Test
    void testGenerateNewKeyData() {
        assertNotNull(keyData);
        // Check if keyData can deserialize correctly
        Aes256KeyAndIv serializedKeyAndIv = Aes256KeyAndIv.deserialize(keyData);
        assertNotNull(serializedKeyAndIv);
        assertEquals(32, serializedKeyAndIv.aes256Key().length); // length of AES-256 key
        assertEquals(16, serializedKeyAndIv.aes256Iv().length); // length of IV
    }

    @Test
    void testEncryptWithInvalidKeyData() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> encryptionAlgorithm.encrypt(new byte[0], inputStream, new ByteArrayOutputStream()));
        assertNotNull(thrown);
    }

    @AfterAll
    static void tearDown() {
        encryptionAlgorithm = null;
        keyData = null;
    }
}
