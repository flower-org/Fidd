package com.fidd.core.encryption;

import java.io.InputStream;
import java.io.OutputStream;

public interface RandomAccessEncryptionAlgorithm extends EncryptionAlgorithm {
    default long plaintextPosToCiphertextPos(long plaintextOffset) { return plaintextOffset; }

    byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long plaintextOffset, long plaintextLength);
    void randomAccessDecrypt(byte[] keyData, long plaintextOffset, long plaintextLength, InputStream ciphertextAtOffset, OutputStream plaintext);

    InputStream getRandomAccessDecryptedStream(byte[] keyData, long plaintextOffset, long plaintextLength, InputStream stream);
}
