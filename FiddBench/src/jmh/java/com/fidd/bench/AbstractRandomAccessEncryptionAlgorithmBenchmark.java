package com.fidd.bench;

import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public abstract class AbstractRandomAccessEncryptionAlgorithmBenchmark {

  @State(Scope.Benchmark)
  public static class RandomAccessEncryptionState {
    @Param({"XOR", "AES_CTR", "KUZNECHIK_CTR"})
    public String algorithm;

    public RandomAccessEncryptionAlgorithm currentAlgorithm;

    public byte[] keyData;

    @Param({"1024", "65536", "1048576"})
    public int payloadSize;

    @Param({"5", "15", "32"})
    public long offset;

    @Param({"3", "15", "16"})
    public long length;

    public byte[] plainText;
    public byte[] cipherText;

    private static final int DETERMINED_RANDOM_SEED = 100;

    @Setup(Level.Trial)
    public void setup() {
      currentAlgorithm =
          switch (algorithm) {
            case "XOR" -> new XorEncryptionAlgorithm();
            case "AES_CTR" -> new Aes256CtrEncryptionAlgorithm();
            case "KUZNECHIK_CTR" -> new KuznechikCtrEcbEncryptionAlgorithm();
            default -> throw new IllegalArgumentException("Unknown algorithm name: " + algorithm);
          };

      keyData = currentAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());

      plainText = new byte[payloadSize];
      Random determinedRandom = new Random(DETERMINED_RANDOM_SEED);
      determinedRandom.nextBytes(plainText);

      cipherText = currentAlgorithm.encrypt(keyData, plainText);

      long cOffset = currentAlgorithm.plaintextPosToCiphertextPos(offset);
      long cLength = currentAlgorithm.plaintextLengthToCiphertextLength(length);
      long availableCipherBytes = cipherText.length - cOffset;
      if (cLength > availableCipherBytes) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid params: payloadSize=%d, offset=%d, length=%d, offset+length=%d",
                payloadSize, offset, length, offset + length));
      }
    }
  }

  @Benchmark
  public byte[] randomAccessDecryptBenchmark(RandomAccessEncryptionState state) {
    return state.currentAlgorithm.randomAccessDecrypt(
        state.keyData, state.cipherText, state.offset, state.length);
  }

  @Benchmark
  public long randomAccessDecryptBenchmarkVoid(RandomAccessEncryptionState state) {
    long cOffset = state.currentAlgorithm.plaintextPosToCiphertextPos(state.offset);
    long cLength = state.currentAlgorithm.plaintextLengthToCiphertextLength(state.length);
    ByteArrayInputStream cipherTextStream =
        new ByteArrayInputStream(state.cipherText, (int) cOffset, (int) cLength);
    ByteArrayOutputStream plainTextStream = new ByteArrayOutputStream();
    state.currentAlgorithm.randomAccessDecrypt(
        state.keyData, state.offset, state.length, cipherTextStream, plainTextStream);

    return plainTextStream.size();
  }

  @Benchmark
  public byte[] getRandomAccessDecryptedStreamBenchmark(RandomAccessEncryptionState state) {
    long cOffset = state.currentAlgorithm.plaintextPosToCiphertextPos(state.offset);
    long cLength = state.currentAlgorithm.plaintextLengthToCiphertextLength(state.length);
    ByteArrayInputStream cipherTextStream =
        new ByteArrayInputStream(state.cipherText, (int) cOffset, (int) cLength);
    try (InputStream resultStream =
        state.currentAlgorithm.getRandomAccessDecryptedStream(
            state.keyData, state.offset, state.length, cipherTextStream)) {
      return resultStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
