package com.fidd.core.encryption;

import java.io.InputStream;
import java.io.OutputStream;

public interface EncryptionAlgorithm {
    byte[] generateNewKeyData();

    byte[] encrypt(byte[] keyData, byte[] plaintext);
    byte[] decrypt(byte[] keyData, byte[] ciphertext);

    void encrypt(byte[] keyData, InputStream plaintext, OutputStream ciphertext);
    void decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext);

    // TODO: use ByteBuffer instead of byte[]?
    // TODO: add file-based methods like?
    // int decrypt(byte[] keyData,
    //             RandomAccessFile ciphertext,
    //             OutputStream plaintext);
}
