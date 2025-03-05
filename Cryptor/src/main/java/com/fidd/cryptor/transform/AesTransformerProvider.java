package com.fidd.cryptor.transform;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AesTransformerProvider implements TransformerProvider {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private final SecretKeySpec secretKey;
    private final IvParameterSpec iv;

    public AesTransformerProvider(byte[] key, byte[] iv) {
        this.secretKey = new SecretKeySpec(key, ALGORITHM);
        this.iv = new IvParameterSpec(iv);
    }

    @Nullable
    @Override
    public Transformer getEncryptTransformer() {
        return data -> {
            try {
                Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
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
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
                return cipher.doFinal(encryptedData);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable @Override public Transformer getSignTransformer() { return null; }

    @Nullable @Override public SignatureChecker getSignatureChecker() { return null; }
}