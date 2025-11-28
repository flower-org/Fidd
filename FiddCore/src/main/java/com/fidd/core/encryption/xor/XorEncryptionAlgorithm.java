package com.fidd.core.encryption.xor;

import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class XorEncryptionAlgorithm implements RandomAccessEncryptionAlgorithm {
    @Override
    public String name() {
        return "XOR";
    }

    // Generate a random key of fixed length
    @Override
    public byte[] generateNewKeyData(RandomGeneratorType random) {
        byte[] key = new byte[32]; // 256-bit key
        random.generator().nextBytes(key);
        return key;
    }

    @Override
    public byte[] encrypt(byte[] keyData, byte[] plaintext) {
        return xorWithKey(plaintext, keyData, 0);
    }

    @Override
    public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
        // XOR is symmetric: same operation
        return xorWithKey(ciphertext, keyData, 0);
    }

    @Override
    public long encrypt(byte[] keyData, List<InputStream> plaintexts, OutputStream ciphertext,
                        @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
        long bytesWritten = 0;
        for (InputStream plaintext : plaintexts) {
            bytesWritten += processStream(keyData, (int)(bytesWritten % keyData.length), -1, plaintext,
                    ciphertext, ciphertextCrcCallbacks);
        }
        return bytesWritten;
    }

    @Override
    public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext) {
        // Same operation
        return processStream(keyData, 0, -1, ciphertext, plaintext, null);
    }

    @Override
    public byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long offset, int length) {
        // Decrypt only a slice of the ciphertext
        byte[] slice = Arrays.copyOfRange(ciphertext, (int) offset, (int) offset + length);
        return xorWithKey(slice, keyData, (int)(offset % keyData.length));
    }

    @Override
    public void randomAccessDecrypt(byte[] keyData, long offset, int length, InputStream ciphertextAtOffset, OutputStream plaintext) {
        // For simplicity, just reuse the stream processor
        processStream(keyData, (int)(offset % keyData.length), length, ciphertextAtOffset, plaintext, null);
    }

    // --- Helper methods ---
    private byte[] xorWithKey(byte[] data, byte[] key, int keyOffset) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[(keyOffset + i) % key.length]);
        }
        return result;
    }

    private long processStream(byte[] keyData, int keyOffset, int length, InputStream in, OutputStream out,
                               @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
        try {
            byte[] buffer = new byte[8192]; // 8 KB buffer
            long total = 0;
            int read;

            while ((read = in.read(buffer, 0,
                    (length != -1) ? Math.min(buffer.length, length - (int) total) : buffer.length)) != -1) {

                // XOR transform in-place
                for (int j = 0; j < read; j++) {
                    buffer[j] = (byte) (buffer[j] ^ keyData[(keyOffset + (int) total + j) % keyData.length]);
                }

                // Write transformed bytes
                out.write(buffer, 0, read);

                // CRC callback
                if (ciphertextCrcCallbacks != null) {
                    // If callback only supports write(byte[]):
                    byte[] chunk = Arrays.copyOf(buffer, read);
                    ciphertextCrcCallbacks.forEach(c -> c.write(chunk));
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
