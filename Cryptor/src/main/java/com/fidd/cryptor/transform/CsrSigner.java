package com.fidd.cryptor.transform;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;

public interface CsrSigner {
    X509Certificate signCsr(PKCS10CertificationRequest csr);
}
