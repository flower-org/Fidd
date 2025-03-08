package com.fidd.cryptor.utils;

import com.flower.crypt.PkiUtil;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class WhatsOnMySafeNetToken {
    public static void main(String[] args) throws Exception {
        String libraryPath = "/usr/lib/libeToken.so";
        String pin = "Qwerty123";
        KeyStore keyStore = PkiUtil.loadPKCS11KeyStore(libraryPath, pin);
        PkiUtil.enumerateKeyStore(keyStore, true, true);

        System.out.println(PkiUtil.getKeyAliasesFromKeyStore(keyStore));
        System.out.println(PkiUtil.getCertificateAliasesFromKeyStore(keyStore));

        Certificate cert = PkiUtil.getCertificateFromKeyStore(keyStore, "test-rsa-2048");
        PrivateKey key = (PrivateKey)PkiUtil.getKeyFromKeyStore(keyStore, "test-rsa-2048");

        {
            String myText = "Hello world";

            byte[] encrypted = Cryptor.encryptRSAPublic(myText.getBytes(), cert.getPublicKey());
            byte[] decrypted = Cryptor.decryptRSAPrivate(encrypted, key);

            System.out.println(new String(decrypted));
        }

        {
            String myText = "Hello world";

            byte[] encrypted = Cryptor.encryptRSAPrivate(myText.getBytes(), key);
            byte[] decrypted = Cryptor.decryptRSAPublic(encrypted, cert.getPublicKey());

            System.out.println(new String(decrypted));
        }
    }
}
