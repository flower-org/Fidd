package com.fidd.core.pki;

import com.fidd.core.NamedEntry;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface SignerChecker extends NamedEntry {
    byte[] signData(byte[] data, PrivateKey privateKey);
    boolean verifySignature(byte[] data, byte[] sign, PublicKey publicKey);
}
