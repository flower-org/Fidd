package com.fidd.core.encryption.xor;

import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;

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
    public byte[] generateNewKeyData() {
        byte[] key = new byte[32]; // 256-bit key
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (Math.random() * 256);
        }
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
    public long encrypt(byte[] keyData, List<InputStream> plaintexts, OutputStream ciphertext) {
        long bytesWritten = 0;
        for (InputStream plaintext : plaintexts) {
            bytesWritten += processStream(keyData, (int)(bytesWritten % keyData.length), -1, plaintext, ciphertext);
        }
        return bytesWritten;
    }

    @Override
    public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext) {
        // Same operation
        return processStream(keyData, 0, -1, ciphertext, plaintext);
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
        processStream(keyData, (int)(offset % keyData.length), length, ciphertextAtOffset, plaintext);
    }

    // --- Helper methods ---
    private byte[] xorWithKey(byte[] data, byte[] key, int keyOffset) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ key[(keyOffset + i) % key.length]);
        }
        return result;
    }

    private long processStream(byte[] keyData, int keyOffset, int length, InputStream in, OutputStream out) {
        try {
            int b;
            int i = 0;
            while ((b = in.read()) != -1) {
                out.write(b ^ keyData[(keyOffset + i) % keyData.length]);
                i++;
                if (length != -1 && i >= length) {
                    break;
                }
            }

            return i;
        } catch (IOException e) {
            throw new RuntimeException("Stream processing failed", e);
        }
    }
}
