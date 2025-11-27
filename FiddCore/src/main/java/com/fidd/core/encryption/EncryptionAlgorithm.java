package com.fidd.core.encryption;

import com.fidd.core.NamedEntry;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface EncryptionAlgorithm extends NamedEntry {
    byte[] generateNewKeyData();

    byte[] encrypt(byte[] keyData, byte[] plaintext);
    byte[] decrypt(byte[] keyData, byte[] ciphertext);

    /** @return Bytes written to output stream ciphertext */
    long encrypt(byte[] keyData, List<InputStream> plaintext, OutputStream ciphertext);
    /** @return Bytes written to output stream plaintext */
    long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext);

    // TODO: use ByteBuffer instead of byte[]?
    // TODO: add file-based methods like?
    // int decrypt(byte[] keyData,
    //             RandomAccessFile ciphertext,
    //             OutputStream plaintext);
}
