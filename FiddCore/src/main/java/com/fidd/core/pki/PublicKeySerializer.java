package com.fidd.core.pki;

import com.fidd.core.NamedEntry;

public interface PublicKeySerializer extends NamedEntry {
    byte[] serialize(PublicKey publicKey);
    PublicKey deserialize(byte[] publicKeyBytes);
}
