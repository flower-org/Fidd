package com.fidd.bench;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Shared JMH benchmark logic for comparing encryption algorithms.
 *
 * <p>Concrete subclasses define only the benchmark mode and output units: average latency and
 * throughput stay separated without duplicating benchmark bodies.
 */
public abstract class AbstractEncryptionAlgorithmBenchmark {

  private static final class ChecksumOutputStream extends OutputStream {
    private long checksum;

    void reset() {
      checksum = 0L;
    }

    long checksum() {
      return checksum;
    }

    @Override
    public void write(int b) {
      checksum = checksum * 31 + (b & 0xFFL);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      for (int i = off; i < off + len; i++) {
        checksum = checksum * 31 + (b[i] & 0xFFL);
      }
    }
  }

  @State(Scope.Thread)
  public static class EncryptionAlgorithmState {
    @Param({"XOR", "AES_CBC", "AES_CTR", "KUZNECHIK_CBC", "KUZNECHIK_CTR"})
    public String algorithm;

    public EncryptionAlgorithm currentAlgorithm;

    public byte[] keyData;

    @Param({"1024", "65536", "1048576"})
    public int payloadSize;

    public byte[] plainText;
    public byte[] cipherText;
    public ByteArrayInputStream plainTextStream;
    public ByteArrayInputStream cipherTextStream;
    public List<InputStream> plainTextStreams;
    public ChecksumOutputStream checksumOutputStream;

    private static final int DETERMINISTIC_RANDOM_SEED = 100;
    private static final long DETERMINISTIC_KEY_SEED = 200L;

    @Setup(Level.Trial)
    public void setup() {
      currentAlgorithm =
          switch (algorithm) {
            case "XOR" -> new XorEncryptionAlgorithm();
            case "AES_CBC" -> new Aes256CbcEncryptionAlgorithm();
            case "AES_CTR" -> new Aes256CtrEncryptionAlgorithm();
            case "KUZNECHIK_CBC" -> new KuznechikCbcEncryptionAlgorithm();
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
      plainTextStream = new ByteArrayInputStream(plainText);
      cipherTextStream = new ByteArrayInputStream(cipherText);
      plainTextStreams = List.of(plainTextStream);
      checksumOutputStream = new ChecksumOutputStream();
    }

    @Setup(Level.Invocation)
    public void resetStreams() {
      plainTextStream.reset();
      cipherTextStream.reset();
      checksumOutputStream.reset();
    }
  }

  @Benchmark
  public byte[] basicEncrypt(EncryptionAlgorithmState state) {
    return state.currentAlgorithm.encrypt(state.keyData, state.plainText);
  }

  @Benchmark
  public byte[] basicDecrypt(EncryptionAlgorithmState state) {
    return state.currentAlgorithm.decrypt(state.keyData, state.cipherText);
  }

  @Benchmark
  public long streamEncryptCryptoOnly(EncryptionAlgorithmState state, Blackhole blackhole) {
    long written =
        state.currentAlgorithm.encrypt(
            state.keyData, state.plainTextStreams, state.checksumOutputStream, null);
    blackhole.consume(state.checksumOutputStream.checksum());
    return written;
  }

  @Benchmark
  public long streamDecryptCryptoOnly(EncryptionAlgorithmState state, Blackhole blackhole) {
    long written =
        state.currentAlgorithm.decrypt(
            state.keyData, state.cipherTextStream, state.checksumOutputStream);
    blackhole.consume(state.checksumOutputStream.checksum());
    return written;
  }
}
