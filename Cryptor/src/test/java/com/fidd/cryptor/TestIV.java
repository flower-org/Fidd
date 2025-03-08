package com.fidd.cryptor;

import com.fidd.cryptor.utils.Cryptor;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.fidd.cryptor.utils.PkiUtil.AES;
import static com.fidd.cryptor.utils.PkiUtil.AES_CBC;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestIV {
    private static final String ALGORITHM = AES;
    private static final int IV_LENGTH_BYTES = 16;

    @Test
    public void testThatAesWithoutIvIsNotAesWithZeroIv() throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] key = Cryptor.generateAESKeyRaw();
        byte[] ivB = new byte[IV_LENGTH_BYTES];
        byte[] data = Cryptor.generateAESKeyRaw();

        SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(ivB);

        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] encr1 = cipher.doFinal(data);

        Cipher cipher2 = Cipher.getInstance(AES_CBC);
        cipher2.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encr2 = cipher2.doFinal(data);

        assertFalse(Arrays.equals(encr1, encr2));
    }
}
