package com.fidd.core.encryption;

import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RandomAccessEncryptionAlgorithmTest {
    private final RandomAccessEncryptionAlgorithm algo = new XorEncryptionAlgorithm();

    @Test
    void testName() {
        assertEquals("XOR", algo.name(), "Algorithm name should be XOR");
    }

    @Test
    void testRandomAccessDecryptByteArray() {
        byte[] key = "fixed-test-key-1234567890123456".getBytes(); // 32 bytes
        byte[] plaintext = "HelloRandomAccessDecryptionWorld!".getBytes();

        // Encrypt full plaintext
        byte[] ciphertext = algo.encrypt(key, plaintext);

        // Decrypt slice [offset=5, length=10]
        int offset = 5;
        int length = 10;
        byte[] decryptedSlice = algo.randomAccessDecrypt(key, ciphertext, offset, length);

        // Expected slice of original plaintext
        byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

        assertArrayEquals(expectedSlice, decryptedSlice,
                "Decrypted slice should equal the corresponding plaintext slice");
    }

    @Test
    void testRandomAccessDecryptStream() {
        byte[] key = "fixed-test-key-1234567890123456".getBytes(); // 32 bytes
        byte[] plaintext = "StreamingRandomAccessDecryptionTest!".getBytes();

        // Encrypt full plaintext
        byte[] ciphertext = algo.encrypt(key, plaintext);

        // Offset and length
        int offset = 8;
        int length = 12;

        // Provide ciphertext slice via InputStream
        ByteArrayInputStream ciphertextStream =
                new ByteArrayInputStream(Arrays.copyOfRange(ciphertext, offset, offset + length));
        ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();

        // Decrypt slice
        algo.randomAccessDecrypt(key, offset, length, ciphertextStream, plaintextOut);

        byte[] decryptedSlice = plaintextOut.toByteArray();
        byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

        assertArrayEquals(expectedSlice, decryptedSlice,
                "Stream-based random access decryption should match plaintext slice");
    }

    @Test
    void testRandomAccessDecryptFullRoundTrip() {
        byte[] key = "another-fixed-test-key-1234567890".getBytes(); // 32 bytes
        byte[] plaintext = "FullRoundTripVerification".getBytes();

        // Encrypt
        byte[] ciphertext = algo.encrypt(key, plaintext);

        // Decrypt entire ciphertext via randomAccessDecrypt
        byte[] decrypted = algo.randomAccessDecrypt(key, ciphertext, 0, plaintext.length);

        assertArrayEquals(plaintext, decrypted,
                "Full randomAccessDecrypt should recover original plaintext");
    }
}
