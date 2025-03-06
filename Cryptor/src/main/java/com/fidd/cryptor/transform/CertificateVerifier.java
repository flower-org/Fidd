package com.fidd.cryptor.transform;

import java.security.cert.X509Certificate;

public interface CertificateVerifier {
    boolean verifyCertificateSignature(X509Certificate certificate);
}
