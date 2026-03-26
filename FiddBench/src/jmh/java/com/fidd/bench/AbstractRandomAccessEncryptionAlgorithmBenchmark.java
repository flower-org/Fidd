package com.fidd.bench;

import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
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
import org.openjdk.jmh.infra.Blackhole;

public abstract class AbstractRandomAccessEncryptionAlgorithmBenchmark {

  @State(Scope.Thread)
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
    public long cipherTextOffset;
    public long cipherTextLength;
    public ByteArrayInputStream cipherTextStream;
    public ByteArrayOutputStream plainTextStream;
    public byte[] readBuffer;

    private static final int DETERMINISTIC_RANDOM_SEED = 100;
    private static final long DETERMINISTIC_KEY_SEED = 200L;

    @Setup(Level.Trial)
    public void setup() {
      currentAlgorithm =
          switch (algorithm) {
            case "XOR" -> new XorEncryptionAlgorithm();
            case "AES_CTR" -> new Aes256CtrEncryptionAlgorithm();
            case "KUZNECHIK_CTR" -> new KuznechikCtrEcbEncryptionAlgorithm();
            default -> throw new IllegalArgumentException("Unknown algorithm name: " + algorithm);
          };

      keyData =
          currentAlgorithm.generateNewKeyData(
              new DeterministicRandomGeneratorType(DETERMINISTIC_KEY_SEED));

      plainText = new byte[payloadSize];
      Random determinedRandom = new Random(DETERMINISTIC_RANDOM_SEED);
      determinedRandom.nextBytes(plainText);

      cipherText = currentAlgorithm.encrypt(keyData, plainText);

      cipherTextOffset = currentAlgorithm.plaintextPosToCiphertextPos(offset);
      cipherTextLength = currentAlgorithm.plaintextLengthToCiphertextLength(length);
      long availableCipherBytes = cipherText.length - cipherTextOffset;
      if (cipherTextLength > availableCipherBytes) {
        throw new IllegalArgumentException(
            String.format(
                "Invalid params: payloadSize=%d, offset=%d, length=%d, offset+length=%d",
                payloadSize, offset, length, offset + length));
      }

      if (cipherTextOffset > Integer.MAX_VALUE || cipherTextLength > Integer.MAX_VALUE) {
        throw new IllegalArgumentException(
            String.format(
                "Benchmark ByteArrayInputStream requires int-sized offsets: cipherTextOffset=%d, cipherTextLength=%d",
                cipherTextOffset, cipherTextLength));
      }

      cipherTextStream =
          new ByteArrayInputStream(cipherText, (int) cipherTextOffset, (int) cipherTextLength);
      plainTextStream = new ByteArrayOutputStream((int) length);
      readBuffer = new byte[(int) length];
    }

    @Setup(Level.Invocation)
    public void resetStreams() {
      cipherTextStream.reset();
      plainTextStream.reset();
    }
  }

  @Benchmark
  public byte[] randomAccessDecryptBenchmark(RandomAccessEncryptionState state) {
    return state.currentAlgorithm.randomAccessDecrypt(
        state.keyData, state.cipherText, state.offset, state.length);
  }

  @Benchmark
  public long randomAccessDecryptBenchmarkVoid(RandomAccessEncryptionState state) {
    state.currentAlgorithm.randomAccessDecrypt(
        state.keyData, state.offset, state.length, state.cipherTextStream, state.plainTextStream);

    return state.plainTextStream.size();
  }

  @Benchmark
  public void getRandomAccessDecryptedStreamBenchmark(
      RandomAccessEncryptionState state, Blackhole blackhole) {
    try (InputStream resultStream =
        state.currentAlgorithm.getRandomAccessDecryptedStream(
            state.keyData, state.offset, state.length, state.cipherTextStream)) {
      byte[] buffer = state.readBuffer;
      long checksum = 0L;
      int read;
      while ((read = resultStream.read(buffer)) != -1) {
        for (int i = 0; i < read; i++) {
          checksum = checksum * 31 + (buffer[i] & 0xFFL);
        }
      }
      blackhole.consume(checksum);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
