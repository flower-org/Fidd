package com.fidd.core.encryption;

import com.fidd.core.encryption.unencrypted.NoEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class RandomAccessEncryptionAlgorithmTest {
    static Stream<Arguments> randomAccessEncryptionAlgorithms() {
        return Stream.of(
                Arguments.of(new XorEncryptionAlgorithm()),
                Arguments.of(new NoEncryptionAlgorithm())
        );
    }

    @ParameterizedTest
    @MethodSource("randomAccessEncryptionAlgorithms")
    void testRandomAccessDecryptByteArray(RandomAccessEncryptionAlgorithm algo) {
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

    @ParameterizedTest
    @MethodSource("randomAccessEncryptionAlgorithms")
    void testRandomAccessDecryptStream(RandomAccessEncryptionAlgorithm algo) {
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


    @ParameterizedTest
    @MethodSource("randomAccessEncryptionAlgorithms")
    void testRandomAccessDecryptInputStream(RandomAccessEncryptionAlgorithm algo) throws IOException {
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
        // Decrypt slice
        InputStream plaintextOut = algo.getRandomAccessDecryptedStream(key, offset, length, ciphertextStream);

        byte[] decryptedSlice = plaintextOut.readAllBytes();
        byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

        assertArrayEquals(expectedSlice, decryptedSlice,
                "Stream-based random access decryption should match plaintext slice");
    }

    @ParameterizedTest
    @MethodSource("randomAccessEncryptionAlgorithms")
    void testRandomAccessDecryptFullRoundTrip(RandomAccessEncryptionAlgorithm algo) {
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
