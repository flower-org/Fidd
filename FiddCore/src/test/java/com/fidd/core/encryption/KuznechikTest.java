package com.fidd.core.encryption;

import com.fidd.core.encryption.aes256.KuznechikBase;
import com.fidd.core.encryption.aes256.KuznechikCbcEncryptionAlgorithm;
import com.fidd.core.encryption.aes256.KuznechikCtrEcbEncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Testing against base implementation */
public class KuznechikTest {
    public static final String KUZNECHIK = "GOST3412-2015";
    public static final String KUZNECHIK_CTR_NO_PADDING = "GOST3412-2015/CTR/NoPadding";
    public static final String KUZNECHIK_CBC_NO_PADDING = "GOST3412-2015/CBC/PKCS7Padding";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Test
    public void testCtr() throws Exception {
        RandomAccessEncryptionAlgorithm algo = new KuznechikCtrEcbEncryptionAlgorithm();

        byte[] keyBytes = algo.generateNewKeyData(new PlainRandomGeneratorType());
        byte[] plaintext = "StreamingRandomAccessDecryptionTest!WithALittleBitMoreBytesToDo".getBytes();

        KuznechikCtrEcbEncryptionAlgorithm.KeyAndNonce k =
                KuznechikCtrEcbEncryptionAlgorithm.KeyAndNonce.deserialize(keyBytes);
        SecretKeySpec key = new SecretKeySpec(k.key32(), KUZNECHIK);

        Cipher cipher = Cipher.getInstance(KUZNECHIK_CTR_NO_PADDING, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(k.nonce8()));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] decrypted = algo.decrypt(keyBytes, ciphertext);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void testCbc() throws Exception {
        EncryptionAlgorithm algo = new KuznechikCbcEncryptionAlgorithm();

        byte[] keyBytes = algo.generateNewKeyData(new PlainRandomGeneratorType());
        byte[] plaintext = "StreamingRandomAccessDecryptionTest!WithALittleBitMoreBytesToDo".getBytes();

        KuznechikBase.KeyAndIv k = KuznechikBase.KeyAndIv.deserialize(keyBytes);
        SecretKeySpec key = new SecretKeySpec(k.key32(), KUZNECHIK);

        Cipher cipher = Cipher.getInstance(KUZNECHIK_CBC_NO_PADDING, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(k.iv16()));
        byte[] ciphertext = cipher.doFinal(plaintext);

        byte[] decrypted = algo.decrypt(keyBytes, ciphertext);
        assertArrayEquals(plaintext, decrypted);
    }
}
