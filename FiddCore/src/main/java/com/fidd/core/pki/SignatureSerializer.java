package com.fidd.core.pki;

import com.fidd.core.NamedEntry;

public interface SignatureSerializer extends NamedEntry {
    byte[] serialize(Signature signature);
    Signature deserialize(byte[] signatureBytes);
}
