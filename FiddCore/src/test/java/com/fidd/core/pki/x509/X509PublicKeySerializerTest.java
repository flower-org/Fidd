package com.fidd.core.pki.x509;

import com.flower.crypt.PkiUtil;
import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class X509PublicKeySerializerTest {
    @Test
    public void testSerializeAndDeserialize() throws Exception {
        // Create a self-signed X509 certificate (simplified for example purposes)
        X509Certificate certificate = generateSelfSignedCertificate();

        X509PublicKeySerializer serializer = new X509PublicKeySerializer();

        // Serialize the certificate
        byte[] serializedCert = serializer.serialize(certificate);

        // Deserialize the certificate
        X509Certificate deserializedCert = serializer.deserialize(serializedCert);

        // Check if the original and deserialized certificates are equivalent
        assertArrayEquals(certificate.getEncoded(), deserializedCert.getEncoded());
        assertEquals(certificate.getSubjectDN(), deserializedCert.getSubjectDN());
        assertEquals(certificate.getIssuerDN(), deserializedCert.getIssuerDN());
    }

    private X509Certificate generateSelfSignedCertificate() throws Exception {
        KeyPair keyPair = PkiUtil.generateRsa2048KeyPair();
        X500Principal subject = new X500Principal("CN=Self-Generated Certificate");
        return PkiUtil.generateSelfSignedCertificate(keyPair, subject);
    }
}
