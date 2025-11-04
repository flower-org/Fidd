package com.fidd.core.encryption.aes256;

import java.util.Arrays;

/**
 * @param aes256Key 32 bytes: AES-256 key size
 * @param aes256Iv  16 bytes: AES block size (IV size)
 */
public record Aes256KeyAndIv(byte[] aes256Key, byte[] aes256Iv) {

    // Serialize the key and IV into a single byte array
    public static byte[] serialize(Aes256KeyAndIv keyAndIv) {
        byte[] serialized = new byte[keyAndIv.aes256Key.length + keyAndIv.aes256Iv.length];
        System.arraycopy(keyAndIv.aes256Key, 0, serialized, 0, keyAndIv.aes256Key.length);
        System.arraycopy(keyAndIv.aes256Iv, 0, serialized, keyAndIv.aes256Key.length, keyAndIv.aes256Iv.length);
        return serialized;
    }

    // Deserialize a byte array back into an Aes256KeyAndIv object
    public static Aes256KeyAndIv deserialize(byte[] serializedKeyAndIv) {
        if (serializedKeyAndIv.length != 48) { // 32 bytes for key + 16 bytes for IV
            throw new IllegalArgumentException("Invalid serialized data length");
        }

        byte[] key = Arrays.copyOfRange(serializedKeyAndIv, 0, 32); // Get first 32 bytes
        byte[] iv = Arrays.copyOfRange(serializedKeyAndIv, 32, 48);  // Get last 16 bytes
        return new Aes256KeyAndIv(key, iv);
    }
}
