package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.random.RandomGeneratorType;
import com.flower.crypt.Cryptor;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

abstract class Aes256Base implements EncryptionAlgorithm {
    abstract String keySpec();
    abstract String transform();

    @Override
    public byte[] generateNewKeyData(RandomGeneratorType random) {
        byte[] aesKey = Cryptor.generateAESKeyRaw(random.generator());
        byte[] aesIv = Cryptor.generateAESIV(random.generator());
        return Aes256KeyAndIv.serialize(new Aes256KeyAndIv(aesKey, aesIv));
    }

    @Override
    public byte[] encrypt(byte[] keyData, byte[] plaintext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), keySpec());
        try {
            return Cryptor.encryptAES(plaintext, secretKeySpec, transform(), keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] keyData, byte[] ciphertext) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), keySpec());
        try {
            return Cryptor.decryptAES(ciphertext, secretKeySpec, transform(), keyAndIv.aes256Iv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long encrypt(byte[] keyData, List<InputStream> plaintexts, OutputStream ciphertext,
                        @Nullable List<CrcCallback> ciphertextCrcCallbacks) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), keySpec());
        long totalBytesWritten = 0;

        if (!plaintexts.isEmpty()) {
            try {
                Cipher cipher = Cipher.getInstance(transform());
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));
                for (InputStream plaintext : plaintexts) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = plaintext.read(buffer)) != -1) {
                        byte[] output = cipher.update(buffer, 0, bytesRead);
                        if (output != null) {
                            ciphertext.write(output);
                            if (ciphertextCrcCallbacks != null) { ciphertextCrcCallbacks.forEach(c -> c.write(output)); }
                            totalBytesWritten += output.length;
                        }
                    }
                }
                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    ciphertext.write(finalOutput);
                    if (ciphertextCrcCallbacks != null) { ciphertextCrcCallbacks.forEach(c -> c.write(finalOutput)); }
                    totalBytesWritten += finalOutput.length;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return totalBytesWritten;
    }

    @Override
    public long decrypt(byte[] keyData, InputStream ciphertext, OutputStream plaintext, boolean allowPartial) {
        Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), keySpec());
        long totalBytesWritten = 0;

        try (ciphertext; plaintext) {
            Cipher cipher;
            try {
                cipher = Cipher.getInstance(transform());
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try {
                byte[] finalOutput = cipher.doFinal();
                if (finalOutput != null) {
                    plaintext.write(finalOutput);
                    totalBytesWritten += finalOutput.length;
                }
            } catch (Exception e) {
                if (!allowPartial) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return totalBytesWritten;
    }

    @Override
    public InputStream getDecryptedStream(byte[] keyData, InputStream stream) {
        try {
            Aes256KeyAndIv keyAndIv = Aes256KeyAndIv.deserialize(keyData);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyAndIv.aes256Key(), keySpec());

            final Cipher cipher = Cipher.getInstance(transform());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(keyAndIv.aes256Iv()));

            // Wrap the input stream with decryption
            return new CipherInputStream(stream, cipher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
