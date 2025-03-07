package com.fidd.cryptor.keys;

public interface Aes256KeyContext extends KeyContext {
    byte[] aes256Key();
    byte[] aes256Iv();

    static Aes256KeyContext of(byte[] aes256Key, byte[] aes256Iv) {
        return new Aes256KeyContext() {
            @Override
            public byte[] aes256Key() {
                return aes256Key;
            }

            @Override
            public byte[] aes256Iv() {
                return aes256Iv;
            }
        };
    }
}
