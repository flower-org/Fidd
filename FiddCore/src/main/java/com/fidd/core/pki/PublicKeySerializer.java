package com.fidd.core.pki;

import com.fidd.core.NamedEntry;

import java.security.cert.X509Certificate;

public interface PublicKeySerializer extends NamedEntry {
    byte[] serialize(X509Certificate publicKey);
    X509Certificate deserialize(byte[] publicKeyBytes);
}
