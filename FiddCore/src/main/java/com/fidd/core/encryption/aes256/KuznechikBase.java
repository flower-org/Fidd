package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public abstract class KuznechikBase implements EncryptionAlgorithm {
  private static final int KEY_SIZE = 32;
  private static final int IV_SIZE = 16;
  private static final int BUFFER_SIZE = 8192;

  abstract PaddedBufferedBlockCipher newCipher();

  public record KeyAndIv(byte[] key32, byte[] iv16) {
    public static byte[] serialize(KeyAndIv keyAndIv) {
      byte[] serialized = new byte[KEY_SIZE + IV_SIZE];
      System.arraycopy(keyAndIv.key32, 0, serialized, 0, KEY_SIZE);
      System.arraycopy(keyAndIv.iv16, 0, serialized, KEY_SIZE, IV_SIZE);
      return serialized;
    }

    public static KeyAndIv deserialize(byte[] serializedKeyAndIv) {
      if (serializedKeyAndIv.length != KEY_SIZE + IV_SIZE) {
        throw new IllegalArgumentException("Invalid serialized data length");
      }
      byte[] key = Arrays.copyOfRange(serializedKeyAndIv, 0, KEY_SIZE);
      byte[] iv = Arrays.copyOfRange(serializedKeyAndIv, KEY_SIZE, KEY_SIZE + IV_SIZE);
      return new KeyAndIv(key, iv);
    }
  }

  private long writeChunk(
      OutputStream out, byte[] buffer, int len, @Nullable List<CrcCallback> crcCallbacks)
      throws IOException {
    if (len <= 0) {
      return 0;
    }
    out.write(buffer, 0, len);
    if (crcCallbacks != null) {
      byte[] outSlice = Arrays.copyOf(buffer, len);
      crcCallbacks.forEach(c -> c.write(outSlice));
    }
    return len;
  }

  private PaddedBufferedBlockCipher initCipher(boolean forEncryption, byte[] keyData) {
    KeyAndIv keyAndIv = KeyAndIv.deserialize(keyData);
    PaddedBufferedBlockCipher cipher = newCipher();
    cipher.init(
        forEncryption, new ParametersWithIV(new KeyParameter(keyAndIv.key32()), keyAndIv.iv16()));
    return cipher;
  }

  private byte[] processBytes(boolean forEncryption, byte[] keyData, byte[] input) {
    PaddedBufferedBlockCipher cipher = initCipher(forEncryption, keyData);
    byte[] out = new byte[cipher.getOutputSize(input.length)];
    try {
      int produced = cipher.processBytes(input, 0, input.length, out, 0);
      produced += cipher.doFinal(out, produced);
      return Arrays.copyOf(out, produced);
    } catch (InvalidCipherTextException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] generateNewKeyData(RandomGeneratorType random) {
    byte[] key32 = new byte[KEY_SIZE];
    byte[] iv16 = new byte[IV_SIZE];
    random.generator().nextBytes(key32);
    random.generator().nextBytes(iv16);
    return KeyAndIv.serialize(new KeyAndIv(key32, iv16));
  }

  @Override
  public byte[] encrypt(byte[] keyData, byte[] plaintext) {
    return processBytes(true, keyData, plaintext);
  }

  @Override
  public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
    return processBytes(false, keyData, ciphertext);
  }

  @Override
  public long encrypt(
      byte[] keyData,
      List<InputStream> plaintexts,
      OutputStream ciphertext,
      @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
    if (plaintexts.isEmpty()) {
      return 0;
    }

    try {
      PaddedBufferedBlockCipher cipher = initCipher(true, keyData);
      byte[] outBuf = new byte[cipher.getOutputSize(BUFFER_SIZE)];
      long total = 0;

      for (InputStream plaintext : plaintexts) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = plaintext.read(buffer)) != -1) {
          int produced = cipher.processBytes(buffer, 0, read, outBuf, 0);
          total += writeChunk(ciphertext, outBuf, produced, ciphertextCrcCallbacks);
        }
      }

      int fin = cipher.doFinal(outBuf, 0);
      total += writeChunk(ciphertext, outBuf, fin, ciphertextCrcCallbacks);
      return total;
    } catch (InvalidCipherTextException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long decrypt(
      byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
    try (ciphertext;
        plaintext) {
      PaddedBufferedBlockCipher cipher = initCipher(false, keyData);
      byte[] inBuf = new byte[BUFFER_SIZE];
      byte[] outBuf = new byte[cipher.getOutputSize(BUFFER_SIZE)];
      long total = 0;

      int read;
      while ((read = ciphertext.read(inBuf)) != -1) {
        int produced = cipher.processBytes(inBuf, 0, read, outBuf, 0);
        total += writeChunk(plaintext, outBuf, produced, null);
      }

      try {
        int fin = cipher.doFinal(outBuf, 0);
        total += writeChunk(plaintext, outBuf, fin, null);
      } catch (InvalidCipherTextException e) {
        if (!allowPartial) {
          throw new RuntimeException(e);
        }
      }
      return total;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
    return new CipherInputStream(stream, initCipher(false, keyData));
  }
}
