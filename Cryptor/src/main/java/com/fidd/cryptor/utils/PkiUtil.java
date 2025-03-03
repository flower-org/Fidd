package com.fidd.cryptor.utils;

import com.google.common.io.Resources;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class PkiUtil {
    public static TrustManagerFactory getSystemTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore)null);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            return trustManagerFactory;
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public static TrustManagerFactory getTrustManagerForCertificateResource(String resourceName) {
        return getTrustManagerForCertificateStream(getStreamFromResource(resourceName));
    }

    public static InputStream getStreamFromResource(String resourceName) {
        try {
            URL resource = Resources.getResource(resourceName);
            return resource.openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TrustManagerFactory getTrustManagerFromPKCS11(String libraryPath, String pin) {
        KeyStore pkcs11Store = loadPKCS11KeyStore(libraryPath, pin);
        return getTrustManagerForKeyStore(pkcs11Store);
    }

    public static X509Certificate getCertificateFromResource(String resourceName) {
        return getCertificateFromStream(getStreamFromResource(resourceName));
    }

    public static X509Certificate getCertificateFromStream(InputStream certificateStream) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(certificateStream);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate getCertificateFromString(String certStr) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certStr.getBytes()));
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyStore loadTrustStore(X509Certificate cert) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("trustedCert", cert);
            return keyStore;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyStore loadTrustStore(InputStream certificateStream) {
        X509Certificate cert = getCertificateFromStream(certificateStream);
        return loadTrustStore(cert);
    }

    public static KeyStore loadTrustStore(String resourceName) {
        X509Certificate cert = getCertificateFromStream(getStreamFromResource(resourceName));
        return loadTrustStore(cert);
    }

    public static KeyStore loadTrustStore(File file) throws FileNotFoundException {
        X509Certificate cert = getCertificateFromStream(new FileInputStream(file));
        return loadTrustStore(cert);
    }

    public static TrustManagerFactory getTrustManagerForCertificateStream(InputStream certificateStream) {
        KeyStore keyStore = loadTrustStore(certificateStream);
        return getTrustManagerForKeyStore(keyStore);
    }

    public static TrustManagerFactory getTrustManagerForKeyStore(KeyStore keyStore) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            X509TrustManager trustManager = null;
            for (TrustManager tm : trustManagers) {
                if (tm instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tm;
                    break;
                }
            }

            if (trustManager == null) {
                throw new IllegalStateException("Cert not loaded - X509TrustManager not found");
            }

            return trustManagerFactory;
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------

    public static KeyManagerFactory getKeyManagerFromResources(String certResourceName, String keyResourceName, String keyPassword) {
        InputStream cerStream = getStreamFromResource(certResourceName);
        InputStream keyStream = getStreamFromResource(keyResourceName);
        return getKeyManagerFromPem(cerStream, keyStream, keyPassword);
    }

    public static KeyManagerFactory getKeyManagerFromPem(InputStream certificateStream, InputStream keyStream, String keyPassword) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certificateStream);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PemReader pemReader = new PemReader(new InputStreamReader(keyStream));
            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(content);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("myCert", privateKey, keyPassword.toCharArray(), new Certificate[]{cert});

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyPassword.toCharArray());

            return keyManagerFactory;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException |
                 InvalidKeySpecException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyManagerFactory getKeyManagerFromCertAndPrivateKey(X509Certificate cert, PrivateKey privateKey) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("myCert", privateKey, null, new Certificate[]{cert});

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, null);

            return keyManagerFactory;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static Provider loadPKCS11Provider(String libraryPath) {
        // Has to start with `--` to indicate inline config
        String config = String.format("--name = SmartCard\nlibrary = %s\n", libraryPath);

        Provider pkcs11Provider = Security.getProvider("SunPKCS11");
        pkcs11Provider = pkcs11Provider.configure(config);
        Security.addProvider(pkcs11Provider);

        return pkcs11Provider;
    }

    public static KeyStore loadPKCS11KeyStore(String libraryPath, String pin) {
        Provider pkcs11Provider = loadPKCS11Provider(libraryPath);
        return loadPKCS11KeyStore(pkcs11Provider, pin);
    }

    public static KeyStore loadPKCS11KeyStore(Provider pkcs11Provider, String pin) {
        try {
            KeyStore pkcs11Store = KeyStore.getInstance("PKCS11", pkcs11Provider);
            pkcs11Store.load(null, pin.toCharArray());

            return pkcs11Store;
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyManagerFactory getKeyManagerFromPKCS11(String libraryPath, String pin) {
        try {
            KeyStore pkcs11Store = loadPKCS11KeyStore(libraryPath, pin);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(pkcs11Store, null);

            return keyManagerFactory;
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static Key getKeyFromKeyStore(KeyStore keyStore, String alias) {
        try {
            return keyStore.getKey(alias, null);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static Certificate getCertificateFromKeyStore(KeyStore keyStore, String alias) {
        try {
            return keyStore.getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getKeyAliasesFromKeyStore(KeyStore keyStore) {
        List<String> keyAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    keyAliases.add(alias);
                }
            }
            return keyAliases;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getCertificateAliasesFromKeyStore(KeyStore keyStore) {
        List<String> certificateAliases = new ArrayList<>();
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias) || keyStore.isCertificateEntry(alias)) {
                    certificateAliases.add(alias);
                }
            }
            return certificateAliases;
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static void enumerateKeyStore(KeyStore keyStore, boolean outputKeysInPem, boolean outputCertsInPem) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("Alias: " + alias);

                // Check if entry is a key entry
                if (keyStore.isKeyEntry(alias)) {
                    Key key = keyStore.getKey(alias, null);
                    System.out.println("Key Entry: " + key.getAlgorithm());
                    if (outputKeysInPem) {
                        System.out.println(getKeyAsPem(key));
                    }
                }

                // Check if the entry is a certificate entry
                if (keyStore.isKeyEntry(alias) || keyStore.isCertificateEntry(alias)) {
                    Certificate cert = keyStore.getCertificate(alias);
                    System.out.println("Certificate Entry: " + cert.getType());
                    if (outputCertsInPem) {
                        System.out.println(getCertificateAsPem(cert));
                    }
                }
            }
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException |
                 CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCertificateAsPem(Certificate cert) throws CertificateEncodingException {
        return cert.getEncoded() == null ? "Can't form PEM for certificate - Access Denied"
                :
               "-----BEGIN CERTIFICATE-----\n" +
               Base64.getMimeEncoder().encodeToString(cert.getEncoded()) +
               "\n" +
               "-----END CERTIFICATE-----\n";
    }

    public static String getKeyAsPem(Key key) {
        return key.getEncoded() == null ? "Can't form PEM for key - Access Denied"
                :
               "-----BEGIN PRIVATE KEY-----\n" +
               Base64.getMimeEncoder().encodeToString(key.getEncoded()) +
               "\n" +
               "-----END PRIVATE KEY-----\n";
    }

    public static String printSessionCertificates(SSLSession session) {
        StringBuilder builder = new StringBuilder();
        builder.append("Local certificates:\n");
        Certificate[] localCerts = session.getLocalCertificates();
        if (localCerts != null) {
            for (Certificate certificate : localCerts) {
                builder.append(certificate).append("\n");
            }
        }
        try {
            Certificate[] peerCerts = session.getPeerCertificates();
            builder.append("Peer certificates:\n");
            if (peerCerts != null) {
                for (Certificate certificate : peerCerts) {
                    builder.append(certificate).append("\n");
                }
            }
        } catch (SSLPeerUnverifiedException e) {
            // One-way SSL
        }

        return builder.toString();
    }

    public static void saveCertificateToPemFile(X509Certificate certificate, File saveToFile) throws IOException {
        JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(saveToFile));
        pemWriter.writeObject(certificate);
    }

    /** @return true if cer1 was signed by caCer */
    public static boolean verifyCertificateSignature(X509Certificate cer1, X509Certificate caCer) {
        PublicKey caPublicKey = caCer.getPublicKey();
        try {
            cer1.verify(caPublicKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static KeyPair generateRsa2048KeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, X500Principal subject) throws CertificateException {
        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + (365L *24*60*60*1000));

        BigInteger serialNumber = new BigInteger(160, new SecureRandom());
        X509v3CertificateBuilder certificateBuilder =
                new JcaX509v3CertificateBuilder(subject, serialNumber, startDate, endDate, subject, keyPair.getPublic());

        final ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        } catch (OperatorCreationException e) {
            throw new CertificateException(e);
        }
        return new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(signer));
    }

    public static String signData(String dataString, PrivateKey privateKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(privateKey);
        signature.update(dataString.getBytes());
        byte[] signedData = signature.sign();
        return Base64.getEncoder().encodeToString(signedData);
    }

    public static boolean verifySignature(String dataString, String sign, PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initVerify(publicKey);
        signature.update(dataString.getBytes());
        byte[] decodedSignature = Base64.getDecoder().decode(sign);
        return signature.verify(decodedSignature);
    }

    public static String encrypt(String plaintext, PublicKey publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedText, PrivateKey privateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }

    public static String encrypt(String plaintext, PrivateKey privateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedText, PublicKey publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes);
    }
}
