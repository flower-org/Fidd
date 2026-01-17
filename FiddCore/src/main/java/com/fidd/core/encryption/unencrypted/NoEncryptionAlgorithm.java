package com.fidd.core.encryption.unencrypted;

import com.fidd.core.common.SubInputStream;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class NoEncryptionAlgorithm implements RandomAccessEncryptionAlgorithm {
    @Override
    public String name() {
        return EncryptionAlgorithm.UNENCRYPTED;
    }

    // Generate a random key of fixed length
    @Override
    public byte[] generateNewKeyData(RandomGeneratorType random) {
        return new byte[] {};
    }

    @Override
    public byte[] encrypt(byte[] keyData, byte[] plaintext) {
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
        return ciphertext;
    }

    @Override
    public long encrypt(byte[] keyData, List<InputStream> plaintexts, OutputStream ciphertext,
                        @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
        long bytesWritten = 0;
        for (InputStream plaintext : plaintexts) {
            bytesWritten += processStream(-1, plaintext,
                    ciphertext, ciphertextCrcCallbacks);
        }
        return bytesWritten;
    }

    @Override
    public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
        // Same operation
        return processStream(-1, ciphertext, plaintext, null);
    }

    @Override
    public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
        return stream;
    }

    @Override
    public byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long offset, long length) {
        return Arrays.copyOfRange(ciphertext, (int) offset, (int)(offset + length));
    }

    @Override
    public void randomAccessDecrypt(byte[] keyData, long offset, long length, InputStream ciphertextAtOffset, OutputStream plaintext) {
        // For simplicity, just reuse the stream processor
        processStream((int)length, ciphertextAtOffset, plaintext, null);
    }

    @Override
    public InputStream getRandomAccessDecryptedStream(byte[] keyData, long offset, long length, InputStream ciphertextAtOffset) {
        try {
            return new SubInputStream(ciphertextAtOffset, 0, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Helper methods ---
    private long processStream(int length, InputStream in, OutputStream out,
                               @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
        try {
            byte[] buffer = new byte[8192]; // 8 KB buffer
            long total = 0;
            int read;

            while ((read = in.read(buffer, 0,
                    (length != -1) ? Math.min(buffer.length, length - (int) total) : buffer.length)) != -1) {

                out.write(buffer, 0, read);

                if (ciphertextCrcCallbacks != null) {
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    ciphertextCrcCallbacks.forEach(c -> c.write(chunk)); // process chunk
                }

                total += read;
                if (length != -1 && total >= length) {
                    break;
                }
            }

            return total;
        } catch (IOException e) {
            throw new RuntimeException("Stream processing failed", e);
        }
    }
}
