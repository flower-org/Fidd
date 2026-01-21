package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import com.flower.crypt.Cryptor;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Aes256CtrEncryptionAlgorithm implements RandomAccessEncryptionAlgorithm
{
  public static final String AES = "AES";
  public static final String AES_CTR_NO_PADDING = "AES/CTR/NoPadding";
  private static final int BLOCK_SIZE = 16;

  @Override
  public String name()
  {
    return "AES-256-CTR";
  }

  @Override
  public byte[] generateNewKeyData(RandomGeneratorType random)
  {
    byte[] aesKey = Cryptor.generateAESKeyRaw(random.generator());
    byte[] aesIv = Cryptor.generateAESIV(random.generator());
    return Aes256KeyAndIv.serialize(new Aes256KeyAndIv(aesKey, aesIv));
  }

  @Override
  public byte[] encrypt(byte[] keyData, byte[] plaintext)
  {
    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);
    try
    {
      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(keyAndIv.aes256Iv()));
      return cipher.doFinal(plaintext);
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] decrypt(byte[] keyData, byte[] ciphertext)
  {
    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);
    try
    {
      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(keyAndIv.aes256Iv()));
      return cipher.doFinal(ciphertext);
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long encrypt(byte[] keyData,
                      List<InputStream> plaintexts,
                      OutputStream ciphertext,
                      @Nullable List<CrcCallback> ciphertextCrcCallbacks)
  {
    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);

    long totalBytesWritten = 0;
    if (plaintexts.isEmpty()) return 0;

    try
    {
      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(keyAndIv.aes256Iv()));

      for (InputStream plaintext : plaintexts)
      {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = plaintext.read(buffer)) != -1)
        {
          byte[] output = cipher.update(buffer, 0, bytesRead);
          if (output != null && output.length > 0)
          {
            ciphertext.write(output);
            if (ciphertextCrcCallbacks != null) ciphertextCrcCallbacks.forEach(c -> c.write(output));
            totalBytesWritten += output.length;
          }
        }
      }

      byte[] finalOutput = cipher.doFinal();
      if (finalOutput != null && finalOutput.length > 0)
      {
        ciphertext.write(finalOutput);
        if (ciphertextCrcCallbacks != null) ciphertextCrcCallbacks.forEach(c -> c.write(finalOutput));
        totalBytesWritten += finalOutput.length;
      }

      return totalBytesWritten;
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial)
  {
    Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
    SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);

    long totalBytesWritten = 0;

    try (ciphertext; plaintext)
    {
      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(keyAndIv.aes256Iv()));

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = ciphertext.read(buffer)) != -1)
      {
        byte[] output = cipher.update(buffer, 0, bytesRead);
        if (output != null && output.length > 0)
        {
          plaintext.write(output);
          totalBytesWritten += output.length;
        }
      }

      byte[] finalOutput = cipher.doFinal();
      if (finalOutput != null && finalOutput.length > 0)
      {
        plaintext.write(finalOutput);
        totalBytesWritten += finalOutput.length;
      }
    } catch (Exception e)
    {
      if (!allowPartial) {
        throw new RuntimeException(e);
      }
    }

    return totalBytesWritten;
  }

  @Override
  public InputStream getDecryptedStream(byte[] keyData, InputStream stream)
  {
    try
    {
      Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
      SecretKeySpec key = new SecretKeySpec(keyAndIv.aes256Key(), AES);

      Cipher cipher = Cipher.getInstance(AES_CTR_NO_PADDING);
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(keyAndIv.aes256Iv()));

      return new CipherInputStream(stream, cipher);
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] randomAccessDecrypt(byte[] keyData,
                                    byte[] ciphertext,
                                    long plaintextOffset,
                                    long plaintextLength)
  {
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
                                  OutputStream plaintext)
  {
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
                                                    InputStream ciphertextAtOffset)
  {
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

      return new InputStream() {
        private long remaining = plaintextLength;

        @Override
        public int read() throws IOException {
          if (remaining <= 0) return -1;
          int b = cis.read();
          if (b == -1) return -1;
          remaining--;
          return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
          if (remaining <= 0) return -1;
          int toRead = (int) Math.min(len, remaining);
          int r = cis.read(b, off, toRead);
          if (r == -1) return -1;
          remaining -= r;
          return r;
        }

        @Override
        public void close() throws IOException {
          cis.close();
        }
      };
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private static byte[] randomAccessDecryptBytes(SecretKeySpec key,
                                                 byte[] iv,
                                                 byte[] ciphertext,
                                                 long offset,
                                                 int length) throws Exception
  {
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

    while (remaining > 0)
    {
      // 0 XOR keystream = keystream
      cipher.update(zeros, 0, BLOCK_SIZE, keystreamBlock, 0);

      int bytesInThisBlock = Math.min(BLOCK_SIZE - blockInnerOffset, remaining);

      for (int i = 0; i < bytesInThisBlock; i++)
      {
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

  private static byte[] addToIv128(byte[] iv, long blockIndex)
  {
    if (iv.length != 16) throw new IllegalArgumentException("IV must be 16 bytes");
    if (blockIndex < 0) throw new IllegalArgumentException("blockIndex must be non-negative");

    byte[] out = iv.clone();
    long carry = blockIndex;

    for (int i = 15; i >= 0 && carry != 0; i--)
    {
      long sum = (out[i] & 0xFFL) + (carry & 0xFFL);
      out[i] = (byte) sum;
      carry = (carry >>> 8) + (sum >>> 8);
    }

    if (carry != 0) throw new IllegalArgumentException("Counter overflow");
    return out;
  }
}
