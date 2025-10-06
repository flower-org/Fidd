package com.fidd.core.encryption;

import java.io.InputStream;
import java.io.OutputStream;

public interface RandomAccessEncryptionAlgorithm {
    byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long offset, int length);
    void randomAccessDecrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext);

    // TODO: use ByteBuffer instead of byte[]?
    // TODO: add file-based methods like?
    // int randomAccessDecrypt(byte[] keyData,
    //                        long offset,
    //                        int length,
    //                        RandomAccessFile ciphertext,
    //                        OutputStream plaintext);
}
