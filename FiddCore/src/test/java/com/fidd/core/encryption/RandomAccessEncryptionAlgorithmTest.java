package com.fidd.core.encryption;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.encryption.unencrypted.NoEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RandomAccessEncryptionAlgorithmTest {
  private static byte[] deterministicPayload(int size) {
    byte[] out = new byte[size];
    for (int i = 0; i < size; i++) {
      out[i] = (byte) ((31 * i + 7) & 0xFF);
    }
    return out;
  }

  static Stream<Arguments> randomAccessEncryptionAlgorithms() {
    return Stream.of(
        Arguments.of(new Aes256CtrEncryptionAlgorithm()),
        Arguments.of(new KuznechikCtrEcbEncryptionAlgorithm()),
        Arguments.of(new XorEncryptionAlgorithm()),
        Arguments.of(new NoEncryptionAlgorithm()));
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptByteArray(RandomAccessEncryptionAlgorithm algo) {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
    byte[] plaintext = "HelloRandomAccessDecryptionWorld!".getBytes();

    // Encrypt full plaintext
    byte[] ciphertext = algo.encrypt(key, plaintext);

    // Decrypt slice [offset=5, length=10]
    int offset = 5;
    int length = 10;
    byte[] decryptedSlice = algo.randomAccessDecrypt(key, ciphertext, offset, length);

    // Expected slice of original plaintext
    byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

    assertArrayEquals(
        expectedSlice,
        decryptedSlice,
        "Decrypted slice should equal the corresponding plaintext slice");
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptStreamWithOffsetMoreThanOneBlock(
      RandomAccessEncryptionAlgorithm algo) {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
    byte[] plaintext = "StreamingRandomAccessDecryptionTest!WithALittleBitMoreBytesToDo".getBytes();

    // Encrypt full plaintext
    byte[] ciphertext = algo.encrypt(key, plaintext);

    // Offset and length
    int offset = 17;
    int length = 12;

    // Provide ciphertext slice via InputStream
    ByteArrayInputStream ciphertextStream =
        new ByteArrayInputStream(Arrays.copyOfRange(ciphertext, offset, offset + length));
    ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();

    // Decrypt slice
    algo.randomAccessDecrypt(key, offset, length, ciphertextStream, plaintextOut);

    byte[] decryptedSlice = plaintextOut.toByteArray();
    byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

    assertArrayEquals(
        expectedSlice,
        decryptedSlice,
        "Stream-based random access decryption should match plaintext slice");
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptInputStream(RandomAccessEncryptionAlgorithm algo) throws IOException {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
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
    InputStream plaintextOut =
        algo.getRandomAccessDecryptedStream(key, offset, length, ciphertextStream);

    byte[] decryptedSlice = plaintextOut.readAllBytes();
    byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

    assertArrayEquals(
        expectedSlice,
        decryptedSlice,
        "Stream-based random access decryption should match plaintext slice");
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptFullRoundTrip(RandomAccessEncryptionAlgorithm algo) {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
    byte[] plaintext = "FullRoundTripVerification".getBytes();

    // Encrypt
    byte[] ciphertext = algo.encrypt(key, plaintext);

    // Decrypt entire ciphertext via randomAccessDecrypt
    byte[] decrypted = algo.randomAccessDecrypt(key, ciphertext, 0, plaintext.length);

    assertArrayEquals(
        plaintext, decrypted, "Full randomAccessDecrypt should recover original plaintext");
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptByteArrayLargeOffset(RandomAccessEncryptionAlgorithm algo) {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
    byte[] plaintext = deterministicPayload(256 * 1024);
    byte[] ciphertext = algo.encrypt(key, plaintext);

    int offset = 64 * 1024 + 37;
    int length = 1021;

    byte[] decryptedSlice = algo.randomAccessDecrypt(key, ciphertext, offset, length);
    byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

    assertArrayEquals(
        expectedSlice,
        decryptedSlice,
        "Byte-array random access decryption should match plaintext at large offset");
  }

  @ParameterizedTest
  @MethodSource("randomAccessEncryptionAlgorithms")
  void testRandomAccessDecryptInputStreamLargeOffset(RandomAccessEncryptionAlgorithm algo)
      throws IOException {
    byte[] key = algo.generateNewKeyData(new PlainRandomGeneratorType());
    byte[] plaintext = deterministicPayload(256 * 1024);
    byte[] ciphertext = algo.encrypt(key, plaintext);

    int offset = 96 * 1024 + 11;
    int length = 2049;

    ByteArrayInputStream ciphertextStream =
        new ByteArrayInputStream(Arrays.copyOfRange(ciphertext, offset, offset + length));
    InputStream plaintextOut =
        algo.getRandomAccessDecryptedStream(key, offset, length, ciphertextStream);

    byte[] decryptedSlice = plaintextOut.readAllBytes();
    byte[] expectedSlice = Arrays.copyOfRange(plaintext, offset, offset + length);

    assertArrayEquals(
        expectedSlice,
        decryptedSlice,
        "InputStream random access decryption should match plaintext at large offset");
  }
}
