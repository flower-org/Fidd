package com.fidd.cryptor.transform;

import com.flower.crypt.PkiUtil;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.flower.crypt.PkiUtil.AES;
import static com.flower.crypt.PkiUtil.AES_CBC;

public class AesTransformerProvider implements TransformerProvider {
    private static final String ALGORITHM = AES;
    private static final String TRANSFORMATION = AES_CBC;
    private static final IvParameterSpec DEFAULT_IV = new IvParameterSpec(new byte[16]);

    private final SecretKeySpec secretKey;
    @Nullable private final IvParameterSpec iv;

    public AesTransformerProvider(byte[] key, @Nullable byte[] iv) {
        this.secretKey = new SecretKeySpec(key, ALGORITHM);
        this.iv = iv == null ? null : new IvParameterSpec(iv);
    }

    @Nullable
    @Override
    public Transformer getEncryptTransformer() {
        return data -> {
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv == null ? DEFAULT_IV : iv);
                return cipher.doFinal(data);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable
    @Override
    public Transformer getDecryptTransformer() {
        return encryptedData -> {
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv == null ? DEFAULT_IV : iv);
                return cipher.doFinal(encryptedData);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable
    @Override
    public FileTransformer getEncryptFileTransformer() {
        return (inputFile, outputFile) -> {
            try {
                try (FileInputStream fis = new FileInputStream(inputFile);
                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                    PkiUtil.encryptFile(secretKey, iv, fis, fos, (int)inputFile.length());
                }
            } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                     | InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable
    @Override
    public FileTransformer getDecryptFileTransformer() {
        return (inputFile, outputFile) -> {
            try {
                try (FileInputStream fis = new FileInputStream(inputFile);
                     FileOutputStream fos = new FileOutputStream(outputFile)) {
                    PkiUtil.decryptFile(secretKey, iv, fis, fos, (int)inputFile.length());
                }
            } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
                     | InvalidAlgorithmParameterException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        };
    }

    // -------------- BELOW FUNCTIONS UNSUPPORTED BY AES --------------

    @Nullable @Override public Transformer getSignTransformer() { return null; }

    @Nullable @Override public SignatureChecker getSignatureChecker() { return null; }

    @Nullable @Override public FileToByteTransformer getSignFileTransformer() { return null; }

    @Nullable @Override public FileSignatureChecker getFileSignatureChecker() { return null; }

    @Nullable @Override public CsrSigner getCsrSigner() { return null; }

    @Nullable @Override public CertificateVerifier getCertificateVerifier() { return null; }
}