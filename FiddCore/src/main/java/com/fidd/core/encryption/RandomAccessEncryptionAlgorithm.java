package com.fidd.core.encryption;

import java.io.InputStream;
import java.io.OutputStream;

public interface RandomAccessEncryptionAlgorithm extends EncryptionAlgorithm {
    default long plaintextPosToCiphertextPos(long plaintextOffset) { return plaintextOffset; }
    default long ciphertextPosToPlaintextPos(long ciphertextOffset) { return ciphertextOffset; }

    byte[] randomAccessDecrypt(byte[] keyData, byte[] ciphertext, long plaintextOffset, long plaintextLength);
    void randomAccessDecrypt(byte[] keyData, long plaintextOffset, long plaintextLength, InputStream ciphertextAtOffset, OutputStream plaintext);

    InputStream getRandomAccessDecryptedStream(byte[] keyData, long plaintextOffset, long plaintextLength, InputStream ciphertextAtOffset);
}
