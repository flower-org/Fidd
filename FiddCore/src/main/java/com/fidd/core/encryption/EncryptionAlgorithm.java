package com.fidd.core.encryption;

import com.fidd.core.NamedEntry;
import com.fidd.core.random.RandomGeneratorType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface EncryptionAlgorithm extends NamedEntry {
    String UNENCRYPTED = "UNENCRYPTED";

    interface CrcCallback {
        void write(byte[] b);
        void write(int b);
        byte[] getCrc();
    }

    byte[] generateNewKeyData(RandomGeneratorType random);

    byte[] encrypt(byte[] keyData, byte[] plaintext);
    byte[] decrypt(byte[] keyData, byte[] ciphertext);

    /** @return Bytes written to output stream ciphertext */
    long encrypt(byte[] keyData, List<InputStream> plaintext, OutputStream ciphertext, CrcCallback ciphertextCrcCallback);
    /** @return Bytes written to output stream plaintext */
    long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext);
}
