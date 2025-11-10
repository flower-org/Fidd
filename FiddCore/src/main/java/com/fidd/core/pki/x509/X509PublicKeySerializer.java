package com.fidd.core.pki.x509;

import com.fidd.core.pki.PublicKeySerializer;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class X509PublicKeySerializer implements PublicKeySerializer {
    @Override
    public byte[] serialize(X509Certificate publicKey) {
        try {
            return publicKey.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public X509Certificate deserialize(byte[] publicKeyBytes) {
        CertificateFactory factory;
        try {
            factory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(publicKeyBytes);
            return (X509Certificate) factory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "X509";
    }
}
