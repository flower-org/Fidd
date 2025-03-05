package com.fidd.cryptor.transform;

public interface SignatureChecker {
    boolean checkSignature(byte[] text, byte[] signature);
}
