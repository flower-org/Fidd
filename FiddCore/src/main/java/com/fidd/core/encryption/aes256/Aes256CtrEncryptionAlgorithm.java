package com.fidd.core.encryption.aes256;

import com.fidd.core.common.SubInputStream;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Aes256CtrEncryptionAlgorithm extends Aes256Base implements RandomAccessEncryptionAlgorithm
{
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
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) {
      return new byte[0];
    }

    if (plaintextOffset > Integer.MAX_VALUE || plaintextLength > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("offset/length too large for in-memory ciphertext");
    }

    int offset = (int) plaintextOffset;
    int length = (int) plaintextLength;

    if ((long) offset + (long) length > ciphertext.length) {
      throw new IllegalArgumentException("Requested range is out of ciphertext bounds");
    }

    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);

    try {
      return randomAccessDecryptBytes(key, keyAndIv.aes256Iv(), ciphertext, offset, length);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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

    try (InputStream in = getRandomAccessDecryptedStream(keyData, plaintextOffset, plaintextLength, ciphertextAtOffset)) {
      byte[] buf = new byte[1024];
      long remaining = plaintextLength;

      while (remaining > 0) {
        int toRead = (int) Math.min(buf.length, remaining);
        int r = in.read(buf, 0, toRead);
        if (r == -1) {
          throw new IOException("Unexpected end of ciphertext stream");
        }
        plaintext.write(buf, 0, r);
        remaining -= r;
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

  private static byte[] randomAccessDecryptBytes(SecretKeySpec key,
                                                 byte[] iv,
                                                 byte[] ciphertext,
                                                 long offset,
                                                 int length) throws Exception {
    byte[] result = new byte[length];

    long startBlock = offset / BLOCK_SIZE;
    int startOffsetInBlock = (int) (offset % BLOCK_SIZE);

    byte[] ivForBlock = addToIv128(iv, startBlock);

    Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivForBlock)); // CTR: keystream

    byte[] zeros = new byte[BLOCK_SIZE];
    byte[] keystreamBlock = new byte[BLOCK_SIZE];

    int remaining = length;
    int outPos = 0;
    long ctPos = offset;
    int blockInnerOffset = startOffsetInBlock;

    while (remaining > 0) {
      // 0 XOR keystream = keystream
      cipher.update(zeros, 0, BLOCK_SIZE, keystreamBlock, 0);

      int bytesInThisBlock = Math.min(BLOCK_SIZE - blockInnerOffset, remaining);

      for (int i = 0; i < bytesInThisBlock; i++) {
        int cipherIndex = (int) ctPos + i;
        result[outPos + i] = (byte) (
          (ciphertext[cipherIndex] & 0xFF) ^
            (keystreamBlock[blockInnerOffset + i] & 0xFF)
        );
      }

      remaining -= bytesInThisBlock;
      outPos += bytesInThisBlock;
      ctPos += bytesInThisBlock;
      blockInnerOffset = 0;
    }

    return result;
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
