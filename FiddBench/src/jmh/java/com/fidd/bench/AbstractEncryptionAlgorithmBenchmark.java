package com.fidd.bench;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CtrEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * Shared JMH benchmark logic for comparing encryption algorithms.
 *
 * <p>Concrete subclasses define only the benchmark mode and output units: average latency and
 * throughput stay separated without duplicating benchmark bodies.
 */
public abstract class AbstractEncryptionAlgorithmBenchmark {

  @State(Scope.Benchmark)
  public static class EncryptionAlgorithmState {
    @Param({"XOR", "AES_CBC", "AES_CTR", "KUZNECHIK_CBC", "KUZNECHIK_CTR"})
    public String algorithm;

    public EncryptionAlgorithm currentAlgorithm;

    public byte[] keyData;

    @Param({"1024", "65536", "1048576"})
    public int payloadSize;

    public byte[] plainText;
    public byte[] cipherText;

    private static final int DETERMINED_RANDOM_SEED = 100;

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

      keyData = currentAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());

      plainText = new byte[payloadSize];
      Random determinedRandom = new Random(DETERMINED_RANDOM_SEED);
      determinedRandom.nextBytes(plainText);

      cipherText = currentAlgorithm.encrypt(keyData, plainText);
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
  public long streamEncryptCryptoOnly(EncryptionAlgorithmState state) {
    ByteArrayInputStream plainTextStream = new ByteArrayInputStream(state.plainText);
    OutputStream cipherTextStream = OutputStream.nullOutputStream();
    return state.currentAlgorithm.encrypt(
        state.keyData, List.of(plainTextStream), cipherTextStream, null);
  }

  @Benchmark
  public long streamDecryptCryptoOnly(EncryptionAlgorithmState state) {
    ByteArrayInputStream cipherTextStream = new ByteArrayInputStream(state.cipherText);
    OutputStream plainTextStream = OutputStream.nullOutputStream();
    return state.currentAlgorithm.decrypt(state.keyData, cipherTextStream, plainTextStream);
  }
}
