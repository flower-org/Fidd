package com.fidd.core.encryption;

import java.io.InputStream;
import java.io.OutputStream;

public interface RandomAccessEncryptionAlgorithm extends EncryptionAlgorithm {
    default Decryptor getDecryptor(byte[] keyData) {
        return getRandomAccessDecryptor(keyData, 0);
    }

    byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long offset, int length);
    void randomAccessDecrypt(byte[] keyData, long offset, int length, InputStream ciphertextAtOffset, OutputStream plaintext);
    Decryptor getRandomAccessDecryptor(byte[] keyData, long offset);
}
