package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import com.flower.crypt.Cryptor;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes256CbcEncryptionAlgorithm implements EncryptionAlgorithm {
    @Override
    public String name() {
        return "AES-256-CBC";
    }

    @Override
    public byte[] generateNewKeyData() {
        byte[] aesKey = Cryptor.generateAESKeyRaw();
        byte[] aesIv = Cryptor.generateAESIV();

        Aes256KeyAndIv original = new Aes256KeyAndIv(aesKey, aesIv);

        return Aes256KeyAndIv.serialize(original);
    }

    @Override
    public byte[] encrypt(byte[] keyData, byte[] plaintext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), "AES");
        try {
            return Cryptor.encryptAES(plaintext, secretKeySpec, keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), "AES");
        try {
            return Cryptor.decryptAES(ciphertext, secretKeySpec, keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void encrypt(byte[] keyData, InputStream plaintext, OutputStream ciphertext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), "AES");

        try (plaintext; ciphertext) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = plaintext.read(buffer)) != -1) {
                    byte[] output = cipher.update(buffer, 0, bytesRead);
                    if (output != null) {
                        ciphertext.write(output);
                    }
                }

                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    ciphertext.write(finalOutput);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), "AES");

        try (ciphertext; plaintext) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));

                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = ciphertext.read(buffer)) != -1) {
                    byte[] output = cipher.update(buffer, 0, bytesRead);
                    if (output != null) {
                        plaintext.write(output);
                    }
                }

                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    plaintext.write(finalOutput);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
