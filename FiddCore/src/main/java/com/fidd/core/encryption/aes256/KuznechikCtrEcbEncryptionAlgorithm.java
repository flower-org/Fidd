package com.fidd.core.encryption.aes256;

import com.fidd.core.common.SubInputStream;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import com.flower.crypt.Cryptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.bouncycastle.crypto.engines.GOST3412_2015Engine;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Manual CTR implementation on top of Kuznechik ECB block primitive.
 *
 * <p>Counter block layout is fixed as {@code nonce8 || counter64be}. This gives explicit counter
 * control and O(1) random-access positioning by block index.
 */
public class KuznechikCtrEcbEncryptionAlgorithm implements RandomAccessEncryptionAlgorithm {
  public static final String KUZNECHIK = "GOST3412-2015";
  public static final String KUZNECHIK_ECB_NO_PADDING = "GOST3412-2015/ECB/NoPadding";
  private static final int BLOCK_SIZE = 16;
  private static final int NONCE_SIZE = 8;
  private static final int BUFFER_SIZE = 8192;

  @Override
  public String name() {
    return "KUZNECHIK-CTR-ECB";
  }

  private record KeyAndNonce(byte[] key32, byte[] nonce8) {
    byte[] serialize() {
      byte[] out = new byte[40];
      System.arraycopy(key32, 0, out, 0, 32);
      System.arraycopy(nonce8, 0, out, 32, 8);
      return out;
    }

    static KeyAndNonce deserialize(byte[] data) {
      if (data.length != 40) {
        throw new IllegalArgumentException("Invalid serialized data length");
      }
      return new KeyAndNonce(Arrays.copyOfRange(data, 0, 32), Arrays.copyOfRange(data, 32, 40));
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      KeyAndNonce that = (KeyAndNonce) o;
      return Objects.deepEquals(key32, that.key32) && Objects.deepEquals(nonce8, that.nonce8);
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(key32), Arrays.hashCode(nonce8));
    }
  }

  private static final class CtrKeystream {
    private final GOST3412_2015Engine ecb;
    private final byte[] counterBlock = new byte[BLOCK_SIZE];
    private final byte[] ksBlock = new byte[BLOCK_SIZE];

    private long counter;
    private int ksPos = BLOCK_SIZE;

    private CtrKeystream(GOST3412_2015Engine ecb, byte[] nonce8, long plaintextOffset) {
      this.ecb = ecb;
      System.arraycopy(nonce8, 0, counterBlock, 0, NONCE_SIZE);

      long startBlock = plaintextOffset / BLOCK_SIZE;
      int offsetInBlock = (int) (plaintextOffset % BLOCK_SIZE);

      counter = startBlock;
      if (offsetInBlock > 0) {
        refill();
        ksPos = offsetInBlock;
      }
    }

    private byte nextByte() {
      if (ksPos == BLOCK_SIZE) {
        refill();
      }
      return ksBlock[ksPos++];
    }

    private void refill() {
      encodeCounter64Be(counterBlock, counter);
      ecb.processBlock(counterBlock, 0, ksBlock, 0);
      ksPos = 0;
      counter++;
    }

    private static void encodeCounter64Be(byte[] counterBlock, long counter) {
      for (int i = 0; i < NONCE_SIZE; i++) {
        counterBlock[NONCE_SIZE + i] = (byte) (counter >>> (56 - (i * 8)));
      }
    }
  }

  private static final class XorCtrInputStream extends InputStream {
    private final InputStream src;
    private final CtrKeystream keystream;

    private XorCtrInputStream(InputStream src, CtrKeystream keystream) {
      this.src = src;
      this.keystream = keystream;
    }

    @Override
    public int read() throws IOException {
      int b = src.read();
      if (b == -1) {
        return -1;
      }
      return (b ^ (keystream.nextByte() & 0xFF)) & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int r = src.read(b, off, len);
      if (r <= 0) {
        return r;
      }
      for (int i = 0; i < r; i++) {
        b[off + i] = (byte) (b[off + i] ^ keystream.nextByte());
      }
      return r;
    }

    @Override
    public void close() throws IOException {
      src.close();
    }
  }

  private static GOST3412_2015Engine newEcbEncryptEngine(byte[] key32) {
    GOST3412_2015Engine ecb = new GOST3412_2015Engine();
    ecb.init(true, new KeyParameter(key32));
    return ecb;
  }

  private static void xorInto(
      byte[] in, int inOff, byte[] out, int outOff, int len, CtrKeystream keystream) {
    for (int i = 0; i < len; i++) {
      out[outOff + i] = (byte) (in[inOff + i] ^ keystream.nextByte());
    }
  }

  private byte[] xorAtOffset(byte[] keyData, byte[] input, long plaintextOffset) {
    if (plaintextOffset < 0) {
      throw new IllegalArgumentException("offset must be non-negative");
    }
    if (input.length == 0) {
      return input;
    }

    KeyAndNonce keyAndNonce = KeyAndNonce.deserialize(keyData);
    byte[] out = new byte[input.length];
    try {
      GOST3412_2015Engine ecb = newEcbEncryptEngine(keyAndNonce.key32);
      CtrKeystream keystream = new CtrKeystream(ecb, keyAndNonce.nonce8, plaintextOffset);
      xorInto(input, 0, out, 0, input.length, keystream);
      return out;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public byte[] generateNewKeyData(RandomGeneratorType random) {
    byte[] key32 = Cryptor.generateAESKeyRaw(random.generator());
    byte[] nonce8 = new byte[NONCE_SIZE];
    random.generator().nextBytes(nonce8);
    return new KeyAndNonce(key32, nonce8).serialize();
  }

  @Override
  public byte[] encrypt(byte[] keyData, byte[] plaintext) {
    return xorAtOffset(keyData, plaintext, 0);
  }

  @Override
  public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
    return xorAtOffset(keyData, ciphertext, 0);
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

    KeyAndNonce keyAndNonce = KeyAndNonce.deserialize(keyData);
    try (ciphertext) {
      GOST3412_2015Engine ecb = newEcbEncryptEngine(keyAndNonce.key32);
      CtrKeystream keystream = new CtrKeystream(ecb, keyAndNonce.nonce8, 0);

      long total = 0;
      byte[] inBuf = new byte[BUFFER_SIZE];
      byte[] outBuf = new byte[BUFFER_SIZE];

      for (InputStream in : plaintexts) {
        int r;
        while ((r = in.read(inBuf)) != -1) {
          xorInto(inBuf, 0, outBuf, 0, r, keystream);
          ciphertext.write(outBuf, 0, r);
          if (ciphertextCrcCallbacks != null) {
            byte[] outSlice = Arrays.copyOf(outBuf, r);
            ciphertextCrcCallbacks.forEach(c -> c.write(outSlice));
          }
          total += r;
        }
      }
      return total;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long decrypt(
      byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
    KeyAndNonce keyAndNonce = KeyAndNonce.deserialize(keyData);
    long total = 0;

    try (ciphertext;
        plaintext) {
      GOST3412_2015Engine ecb = newEcbEncryptEngine(keyAndNonce.key32);
      CtrKeystream keystream = new CtrKeystream(ecb, keyAndNonce.nonce8, 0);

      byte[] inBuf = new byte[BUFFER_SIZE];
      byte[] outBuf = new byte[BUFFER_SIZE];

      int r;
      while ((r = ciphertext.read(inBuf)) != -1) {
        xorInto(inBuf, 0, outBuf, 0, r, keystream);
        plaintext.write(outBuf, 0, r);
        total += r;
      }
      return total;
    } catch (Exception e) {
      if (!allowPartial) {
        throw new RuntimeException(e);
      }
      return total;
    }
  }

  @Override
  public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
    KeyAndNonce keyAndNonce = KeyAndNonce.deserialize(keyData);
    try {
      GOST3412_2015Engine ecb = newEcbEncryptEngine(keyAndNonce.key32);
      return new XorCtrInputStream(stream, new CtrKeystream(ecb, keyAndNonce.nonce8, 0));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getRandomAccessDecryptedStream(
      byte[] keyData, long plaintextOffset, long plaintextLength, InputStream ciphertextAtOffset) {
    if (plaintextOffset < 0 || plaintextLength < 0) {
      throw new IllegalArgumentException("offset/length must be non-negative");
    }
    if (plaintextLength == 0) {
      return InputStream.nullInputStream();
    }

    KeyAndNonce keyAndNonce = KeyAndNonce.deserialize(keyData);
    try {
      GOST3412_2015Engine ecb = newEcbEncryptEngine(keyAndNonce.key32);
      InputStream pt =
          new XorCtrInputStream(
              ciphertextAtOffset, new CtrKeystream(ecb, keyAndNonce.nonce8, plaintextOffset));
      return new SubInputStream(pt, 0, plaintextLength);
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
    if (plaintextLength == 0) {
      return new byte[0];
    }

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
    if (plaintextLength == 0) {
      return;
    }

    try (InputStream in =
        getRandomAccessDecryptedStream(
            keyData, plaintextOffset, plaintextLength, ciphertextAtOffset)) {
      byte[] buf = new byte[BUFFER_SIZE];
      int r;
      while ((r = in.read(buf)) != -1) {
        plaintext.write(buf, 0, r);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
