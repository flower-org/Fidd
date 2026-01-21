package com.fidd.core.encryption.aes256;

public class Aes256CbcEncryptionAlgorithm extends Aes256Base {
    public static final String AES = "AES";
    public static final String AES_CBC_PKCS_5_PADDING = "AES/CBC/PKCS5Padding";

    @Override public String keySpec() { return AES; }
    @Override public String transform() { return AES_CBC_PKCS_5_PADDING; }

    @Override
    public String name() {
        return "AES-256-CBC";
    }
}
