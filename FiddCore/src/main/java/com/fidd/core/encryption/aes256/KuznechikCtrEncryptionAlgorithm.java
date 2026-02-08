package com.fidd.core.encryption.aes256;

import com.fidd.core.common.SubInputStream;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import com.flower.crypt.Cryptor;
import java.io.*;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KuznechikCtrEncryptionAlgorithm extends Aes256Base
    implements RandomAccessEncryptionAlgorithm {

  public static final String KUZNECHIK = "GOST3412-2015";
  public static final String KUZNECHIK_CTR_NO_PADDING = "GOST3412-2015/CTR/NoPadding";
  private static final int BLOCK_SIZE = 16;
  private static final int FAST_FORWARD_BUF_SIZE = 8192;

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  /** keyData layout: 32 bytes key + 8 bytes IV (nonce) */
  private record KuznechikKeyAndIv(byte[] key32, byte[] iv8) {

    byte[] serialize() {
      byte[] out = new byte[40];
      System.arraycopy(key32, 0, out, 0, 32);
      System.arraycopy(iv8, 0, out, 32, 8);
      return out;
    }

    static KuznechikKeyAndIv deserialize(byte[] data) {
      if (data.length != 40) {
        throw new IllegalArgumentException("Invalid serialized data length");
      }
      return new KuznechikKeyAndIv(
          Arrays.copyOfRange(data, 0, 32), Arrays.copyOfRange(data, 32, 40));
    }
  }

  @Override
  String keySpec() {
    return KUZNECHIK;
  }

  @Override
  String transform() {
    return KUZNECHIK_CTR_NO_PADDING;
  }

  @Override
  public String name() {
    return "KUZNECHIK-CTR";
  }

  @Override
  public byte[] generateNewKeyData(RandomGeneratorType random) {
    byte[] key32 = Cryptor.generateAESKeyRaw(random.generator());
    byte[] iv8 = new byte[8];
    random.generator().nextBytes(iv8);
    return new KuznechikKeyAndIv(key32, iv8).serialize();
  }

  @Override
  public byte[] encrypt(byte[] keyData, byte[] plaintext) {
    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);
    try {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ki.iv8));
      return cipher.doFinal(plaintext);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);
    try {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ki.iv8));
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
    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);

    long total = 0;
    if (plaintexts.isEmpty()) return 0;

    try (ciphertext) {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ki.iv8));

      byte[] buf = new byte[1024];
      for (InputStream in : plaintexts) {
        int r;
        while ((r = in.read(buf)) != -1) {
          byte[] out = cipher.update(buf, 0, r);
          if (out != null && out.length > 0) {
            ciphertext.write(out);
            if (ciphertextCrcCallbacks != null) ciphertextCrcCallbacks.forEach(c -> c.write(out));
            total += out.length;
          }
        }
      }

      byte[] fin = cipher.doFinal();
      if (fin != null && fin.length > 0) {
        ciphertext.write(fin);
        if (ciphertextCrcCallbacks != null) ciphertextCrcCallbacks.forEach(c -> c.write(fin));
        total += fin.length;
      }
      return total;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long decrypt(
      byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);

    long total = 0;
    try (ciphertext;
        plaintext) {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ki.iv8));

      byte[] buf = new byte[1024];
      int r;
      while ((r = ciphertext.read(buf)) != -1) {
        byte[] out = cipher.update(buf, 0, r);
        if (out != null && out.length > 0) {
          plaintext.write(out);
          total += out.length;
        }
      }
      byte[] fin = cipher.doFinal();
      if (fin != null && fin.length > 0) {
        plaintext.write(fin);
        total += fin.length;
      }
      return total;
    } catch (Exception e) {
      if (!allowPartial) throw new RuntimeException(e);
      return total;
    }
  }

  @Override
  public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);
    try {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ki.iv8));
      return new CipherInputStream(stream, cipher);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Random-access decryption for CTR mode.
   *
   * <p>We position cipher state to {@code plaintextOffset} in two stages: 1) fast-forward by full
   * blocks, 2) if offset is inside a block, decrypt the first partial block manually.
   *
   * <p>The manual first-block step is intentional: for BC Kuznechik/CTR a short {@code
   * cipher.update(...)} skip can be provider-buffering sensitive and may produce a shifted
   * keystream for the first bytes of stream mode.
   *
   * <p>Why manual XOR for the first part: {@code ciphertextAtOffset} already starts exactly at the
   * requested offset (not at block boundary). After full-block fast-forward the cipher is aligned
   * to the beginning of {@code startBlock}, so for the first returned bytes we must apply keystream
   * bytes {@code [offsetInBlock..15]} of that block. Those bytes are part of the result and are
   * returned to caller, then stream processing continues from the next block boundary.
   */
  @Override
  public InputStream getRandomAccessDecryptedStream(
      byte[] keyData, long plaintextOffset, long plaintextLength, InputStream ciphertextAtOffset) {

    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) return InputStream.nullInputStream();

    KuznechikKeyAndIv ki = KuznechikKeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(ki.key32, KUZNECHIK);

    final long startBlock = plaintextOffset / BLOCK_SIZE;
    final int offsetInBlock = (int) (plaintextOffset % BLOCK_SIZE);

    try {
      Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ki.iv8));

      long bytesToSkip = startBlock * BLOCK_SIZE;
      if (bytesToSkip > 0) {
        // CTR: feeding zeroes advances the internal counter/keystream without using ciphertext.
        byte[] zeros = new byte[FAST_FORWARD_BUF_SIZE];
        byte[] trash = new byte[FAST_FORWARD_BUF_SIZE];

        while (bytesToSkip > 0) {
          int chunk = (int) Math.min(zeros.length, bytesToSkip);
          cipher.update(zeros, 0, chunk, trash, 0);
          bytesToSkip -= chunk;
        }
      }

      if (offsetInBlock == 0) {
        CipherInputStream cis = new CipherInputStream(ciphertextAtOffset, cipher);
        return new SubInputStream(cis, 0, plaintextLength);
      }

      // Materialize one full keystream block and take only its tail for the non-aligned start.
      byte[] ksBlock = new byte[BLOCK_SIZE];
      byte[] zerosBlock = new byte[BLOCK_SIZE];
      cipher.update(zerosBlock, 0, BLOCK_SIZE, ksBlock, 0);

      int firstPartLen = (int) Math.min(plaintextLength, BLOCK_SIZE - offsetInBlock);
      byte[] ctFirst = ciphertextAtOffset.readNBytes(firstPartLen);
      if (ctFirst.length != firstPartLen) {
        throw new EOFException("Unexpected end of ciphertext stream");
      }

      byte[] ptFirst = new byte[firstPartLen];
      for (int i = 0; i < firstPartLen; i++) {
        ptFirst[i] = (byte) (ctFirst[i] ^ ksBlock[offsetInBlock + i]);
      }

      if (plaintextLength == firstPartLen) {
        return new ByteArrayInputStream(ptFirst);
      }

      CipherInputStream cis = new CipherInputStream(ciphertextAtOffset, cipher);
      InputStream seq = new SequenceInputStream(new ByteArrayInputStream(ptFirst), cis);
      return new SubInputStream(seq, 0, plaintextLength);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] randomAccessDecrypt(
      byte[] keyData, byte[] ciphertext, long plaintextOffset, long plaintextLength) {
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) return new byte[0];

    long cOff = plaintextPosToCiphertextPos(plaintextOffset);
    long cLen = plaintextLengthToCiphertextLength(plaintextLength);

    if (cOff + cLen > ciphertext.length) {
      throw new IllegalArgumentException("Requested range is out of ciphertext bounds");
    }

    try (InputStream ctAtOff = new ByteArrayInputStream(ciphertext, (int) cOff, (int) cLen);
        InputStream pt =
            getRandomAccessDecryptedStream(keyData, plaintextOffset, plaintextLength, ctAtOff)) {
      return pt.readAllBytes();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void randomAccessDecrypt(
      byte[] keyData,
      long plaintextOffset,
      long plaintextLength,
      InputStream ciphertextAtOffset,
      OutputStream plaintext) {
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) return;

    try (InputStream in =
        getRandomAccessDecryptedStream(
            keyData, plaintextOffset, plaintextLength, ciphertextAtOffset)) {
      byte[] buf = new byte[8192];
      int r;
      while ((r = in.read(buf)) != -1) {
        plaintext.write(buf, 0, r);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
