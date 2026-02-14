package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

abstract class KuznechikBase implements EncryptionAlgorithm {
  private static final String PROVIDER = "BC";
  static final String KUZNECHIK = "GOST3412-2015";
  private static final int KEY_SIZE = 32;
  private static final int IV_SIZE = 16;
  private static final int BUFFER_SIZE = 1024;

  static {
    if (Security.getProvider(PROVIDER) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  abstract String transform();

  private record KeyAndIv(byte[] key32, byte[] iv16) {
    static byte[] serialize(KeyAndIv keyAndIv) {
      byte[] serialized = new byte[KEY_SIZE + IV_SIZE];
      System.arraycopy(keyAndIv.key32, 0, serialized, 0, KEY_SIZE);
      System.arraycopy(keyAndIv.iv16, 0, serialized, KEY_SIZE, IV_SIZE);
      return serialized;
    }

    static KeyAndIv deserialize(byte[] serializedKeyAndIv) {
      if (serializedKeyAndIv.length != KEY_SIZE + IV_SIZE) {
        throw new IllegalArgumentException("Invalid serialized data length");
      }
      byte[] key = Arrays.copyOfRange(serializedKeyAndIv, 0, KEY_SIZE);
      byte[] iv = Arrays.copyOfRange(serializedKeyAndIv, KEY_SIZE, KEY_SIZE + IV_SIZE);
      return new KeyAndIv(key, iv);
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
    KeyAndIv keyAndIv = KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.key32(), KUZNECHIK);
    try {
      Cipher cipher = Cipher.getInstance(transform(), PROVIDER);
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(keyAndIv.iv16()));
      return cipher.doFinal(plaintext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
    KeyAndIv keyAndIv = KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.key32(), KUZNECHIK);
    try {
      Cipher cipher = Cipher.getInstance(transform(), PROVIDER);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(keyAndIv.iv16()));
      return cipher.doFinal(ciphertext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
      ByteArrayOutputStream plaintextAll = new ByteArrayOutputStream();
      for (InputStream plaintext : plaintexts) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = plaintext.read(buffer)) != -1) {
          plaintextAll.write(buffer, 0, read);
        }
      }

      byte[] encrypted = encrypt(keyData, plaintextAll.toByteArray());
      if (encrypted.length > 0) {
        ciphertext.write(encrypted);
        if (ciphertextCrcCallbacks != null) {
          ciphertextCrcCallbacks.forEach(c -> c.write(encrypted));
        }
      }
      return encrypted.length;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long decrypt(
      byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
    try (ciphertext;
        plaintext) {
      byte[] ciphertextBytes = ciphertext.readAllBytes();
      byte[] plaintextBytes;
      try {
        plaintextBytes = decrypt(keyData, ciphertextBytes);
      } catch (RuntimeException e) {
        if (!allowPartial) {
          throw e;
        }
        return 0;
      }
      plaintext.write(plaintextBytes);
      return plaintextBytes.length;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
    try (stream) {
      byte[] decrypted = decrypt(keyData, stream.readAllBytes());
      return new ByteArrayInputStream(decrypted);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
