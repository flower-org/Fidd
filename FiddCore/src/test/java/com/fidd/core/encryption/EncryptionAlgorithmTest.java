package com.fidd.core.encryption;

import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.unencrypted.NoEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionAlgorithmTest {
    static Stream<Arguments> encryptionAlgorithms() {
        return Stream.of(
                Arguments.of(new Aes256CbcEncryptionAlgorithm()),
          Arguments.of(new Aes256CtrEncryptionAlgorithm()),
                Arguments.of(new XorEncryptionAlgorithm()),
                Arguments.of(new NoEncryptionAlgorithm())
        );
    }

    private static final String originalText = "This is a secret message.";

    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testEncryptDecryptStream(EncryptionAlgorithm encryptionAlgorithm) {
        byte[] keyData = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        // Encrypt the original text
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        encryptionAlgorithm.encrypt(keyData, List.of(inputStream), outputStream, null);
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


    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testEncryptDecryptMultiStream(EncryptionAlgorithm encryptionAlgorithm) {
        byte[] keyData = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        // Encrypt the original text
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        encryptionAlgorithm.encrypt(keyData, List.of(inputStream, inputStream2), outputStream, null);
        byte[] ciphertext = outputStream.toByteArray();

        // Decrypt the ciphertext
        ByteArrayInputStream cipherInputStream = new ByteArrayInputStream(ciphertext);
        ByteArrayOutputStream decryptedOutputStream = new ByteArrayOutputStream();

        encryptionAlgorithm.decrypt(keyData, cipherInputStream, decryptedOutputStream);

        // Convert to String
        String decryptedText = decryptedOutputStream.toString(StandardCharsets.UTF_8);

        // Assert that the decrypted text matches the original text
        assertEquals(originalText+originalText, decryptedText);
    }

    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testEncryptDecrypt(EncryptionAlgorithm encryptionAlgorithm) {
        byte[] keyData = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        byte[] plaintext = originalText.getBytes(StandardCharsets.UTF_8);

        // Encrypt the plaintext
        byte[] ciphertext = encryptionAlgorithm.encrypt(keyData, plaintext);

        // Decrypt the ciphertext
        byte[] decryptedTextBytes = encryptionAlgorithm.decrypt(keyData, ciphertext);
        String decryptedText = new String(decryptedTextBytes, StandardCharsets.UTF_8);

        // Assert that the decrypted text matches the original text
        assertEquals(originalText, decryptedText);
    }

    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testEncryptWithInvalidKeyData(EncryptionAlgorithm encryptionAlgorithm) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
        if (!encryptionAlgorithm.name().equals(EncryptionAlgorithm.UNENCRYPTED)) {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> encryptionAlgorithm.encrypt(new byte[0], List.of(inputStream), new ByteArrayOutputStream(), null));
            assertNotNull(thrown);
        }
    }

    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testEncryptDecryptSymmetry(EncryptionAlgorithm encryptionAlgorithm) {
        byte[] key = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        byte[] plaintext = "Hello, World!".getBytes();

        byte[] ciphertext = encryptionAlgorithm.encrypt(key, plaintext);
        byte[] decrypted = encryptionAlgorithm.decrypt(key, ciphertext);

        assertArrayEquals(plaintext, decrypted, "Decrypted text should match original plaintext");
    }

    @ParameterizedTest
    @MethodSource("encryptionAlgorithms")
    void testStreamEncryptDecrypt(EncryptionAlgorithm encryptionAlgorithm) {
        byte[] key = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        byte[] plaintext = "Stream test data".getBytes();

        ByteArrayInputStream in = new ByteArrayInputStream(plaintext);
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        encryptionAlgorithm.encrypt(key, List.of(in), encryptedOut, null);

        byte[] ciphertext = encryptedOut.toByteArray();

        ByteArrayInputStream cipherIn = new ByteArrayInputStream(ciphertext);
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        encryptionAlgorithm.decrypt(key, cipherIn, decryptedOut);

        assertArrayEquals(plaintext, decryptedOut.toByteArray(), "Stream decryption should restore original data");
    }

  @Test
  void testCtrRandomAccessDecryptInsideSingleBlock()
  {
    Aes256CtrEncryptionAlgorithm ctr = new Aes256CtrEncryptionAlgorithm();
    byte[] keyData = ctr.generateNewKeyData(new PlainRandomGeneratorType());

    byte[] plaintext = "Hello AES CTR random access test".getBytes(StandardCharsets.US_ASCII);
    byte[] ciphertext = ctr.encrypt(keyData, plaintext);

    int offset = 6;   // "AES ..."
    int length = 7;   // "AES CTR"
    byte[] slice = ctr.randomAccessDecrypt(keyData, ciphertext, offset, length);

    assertArrayEquals(Arrays.copyOfRange(plaintext, offset, offset + length), slice);
    assertEquals("AES CTR", new String(slice, StandardCharsets.US_ASCII));
  }

  @Test
  void testCtrRandomAccessDecryptCrossesBlockBoundary()
  {
    Aes256CtrEncryptionAlgorithm ctr = new Aes256CtrEncryptionAlgorithm();
    byte[] keyData = ctr.generateNewKeyData(new PlainRandomGeneratorType());

    // 32 bytes
    byte[] plaintext = "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    byte[] ciphertext = ctr.encrypt(keyData, plaintext);

    int offset = 15; // last byte of block 0
    int length = 4;  // byte 15-18
    byte[] slice = ctr.randomAccessDecrypt(keyData, ciphertext, offset, length);

    assertArrayEquals(Arrays.copyOfRange(plaintext, offset, offset + length), slice);
    assertEquals("F012", new String(slice, StandardCharsets.US_ASCII));
  }

  @Test
  void testCtrRandomAccessDecryptTail()
  {
    Aes256CtrEncryptionAlgorithm ctr = new Aes256CtrEncryptionAlgorithm();
    byte[] keyData = ctr.generateNewKeyData(new PlainRandomGeneratorType());

    byte[] plaintext = "Hello AES CTR random access test".getBytes(StandardCharsets.US_ASCII);
    byte[] ciphertext = ctr.encrypt(keyData, plaintext);

    int offset = 17; // "access test" starts here
    int length = plaintext.length - offset;
    byte[] slice = ctr.randomAccessDecrypt(keyData, ciphertext, offset, length);

    assertArrayEquals(Arrays.copyOfRange(plaintext, offset, plaintext.length), slice);
  }
}
