package com.fidd.core.encryption.aes256;

import com.fidd.core.common.SubInputStream;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Aes256CtrEncryptionAlgorithm extends Aes256Base implements RandomAccessEncryptionAlgorithm {
  public static final String AES = "AES";
  public static final String AES_CTR_NO_PADDING = "AES/CTR/NoPadding";
  private static final int BLOCK_SIZE = 16;

  @Override public String keySpec() { return AES; }
  @Override public String transform() { return AES_CTR_NO_PADDING; }

  @Override
  public String name()
  {
    return "AES-256-CTR";
  }

  @Override
  public byte[] randomAccessDecrypt(byte[] keyData,
                                    byte[] ciphertext,
                                    long plaintextOffset,
                                    long plaintextLength) {
    InputStream inputStream = new ByteArrayInputStream(ciphertext,
            (int)plaintextPosToCiphertextPos(plaintextOffset),
            (int)plaintextLengthToCiphertextLength(plaintextLength));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    randomAccessDecrypt(keyData, plaintextOffset, plaintextLength, inputStream, outputStream);
    return outputStream.toByteArray();
  }

  @Override
  public void randomAccessDecrypt(byte[] keyData,
                                  long plaintextOffset,
                                  long plaintextLength,
                                  InputStream ciphertextAtOffset,
                                  OutputStream plaintext) {
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) return;

    InputStream decryptedStream = getRandomAccessDecryptedStream(keyData, plaintextOffset, plaintextLength, ciphertextAtOffset);
    try (InputStream in = new SubInputStream(decryptedStream, 0, plaintextLength);
         decryptedStream) {
      byte[] buf = new byte[AES_BUFFER_SIZE];

      while (true) {
        int r = in.read(buf);
        if (r == -1) { break; }
        plaintext.write(buf, 0, r);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getRandomAccessDecryptedStream(byte[] keyData,
                                                    long plaintextOffset,
                                                    long plaintextLength,
                                                    InputStream ciphertextAtOffset) {
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) {
      return InputStream.nullInputStream();
    }

    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);

    final long startBlock = plaintextOffset / BLOCK_SIZE;
    final int startOffsetInBlock = (int) (plaintextOffset % BLOCK_SIZE);

    try {
      byte[] ivForBlock = addToIv128(keyAndIv.aes256Iv(), startBlock);

      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivForBlock));

      if (startOffsetInBlock > 0) {
        byte[] skip = new byte[startOffsetInBlock];
        cipher.update(skip);
      }

      CipherInputStream cis = new CipherInputStream(ciphertextAtOffset, cipher);
      return new SubInputStream(cis, 0, plaintextLength);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] addToIv128(byte[] iv, long blockIndex) {
    if (iv.length != 16) throw new IllegalArgumentException("IV must be 16 bytes");
    if (blockIndex < 0) throw new IllegalArgumentException("blockIndex must be non-negative");

    byte[] out = iv.clone();
    long carry = blockIndex;

    for (int i = 15; i >= 0 && carry != 0; i--) {
      long sum = (out[i] & 0xFFL) + (carry & 0xFFL);
      out[i] = (byte) sum;
      carry = (carry >>> 8) + (sum >>> 8);
    }

    if (carry != 0) throw new IllegalArgumentException("Counter overflow");
    return out;
  }
}
