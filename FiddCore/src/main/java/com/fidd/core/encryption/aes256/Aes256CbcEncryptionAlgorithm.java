package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.List;

import com.flower.crypt.Cryptor;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes256CbcEncryptionAlgorithm implements EncryptionAlgorithm {
    public static final String AES = "AES";
    public static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";

    @Override
    public String name() {
        return "AES-256-CBC";
    }

    @Override
    public byte[] generateNewKeyData() {
        byte[] aesKey = Cryptor.generateAESKeyRaw();
        byte[] aesIv = Cryptor.generateAESIV();

        // Generate random key and IV
        new SecureRandom().nextBytes(aesKey);
        new SecureRandom().nextBytes(aesIv);

        Aes256KeyAndIv original = new Aes256KeyAndIv(aesKey, aesIv);

        return Aes256KeyAndIv.serialize(original);
    }

    @Override
    public byte[] encrypt(byte[] keyData, byte[] plaintext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), AES);
        try {
            return Cryptor.encryptAES(plaintext, secretKeySpec, keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), AES);
        try {
            return Cryptor.decryptAES(ciphertext, secretKeySpec, keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long encrypt(byte[] keyData, List<InputStream> plaintexts, OutputStream ciphertext,
                        @Nullable CrcCallback ciphertextCrcCallback) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), AES);
        long totalBytesWritten = 0;

        if (!plaintexts.isEmpty()) {
            try {
                Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));
                for (InputStream plaintext : plaintexts) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = plaintext.read(buffer)) != -1) {
                        byte[] output = cipher.update(buffer, 0, bytesRead);
                        if (output != null) {
                            ciphertext.write(output);
                            if (ciphertextCrcCallback != null) { ciphertextCrcCallback.write(output); }
                            totalBytesWritten += output.length;
                        }
                    }
                }
                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    ciphertext.write(finalOutput);
                    if (ciphertextCrcCallback != null) { ciphertextCrcCallback.write(finalOutput); }
                    totalBytesWritten += finalOutput.length;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return totalBytesWritten;
    }

    @Override
    public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), AES);
        long totalBytesWritten = 0;

        try (ciphertext; plaintext) {
            try {
                Cipher cipher = Cipher.getInstance(AES_CBC_PKCS_5_PADDING);
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = ciphertext.read(buffer)) != -1) {
                    byte[] output = cipher.update(buffer, 0, bytesRead);
                    if (output != null) {
                        plaintext.write(output);
                        totalBytesWritten += output.length;
                    }
                }

                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    plaintext.write(finalOutput);
                    totalBytesWritten += finalOutput.length;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return totalBytesWritten;
    }
}
