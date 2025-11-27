package com.fidd.core.encryption.aes256;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Aes256KeyAndIvTest {

    @Test
    public void testSerializationDeserialization() {
        byte[] aesKey = new byte[32]; // AES-256 key size
        byte[] aesIv = new byte[16];  // AES block size (IV size)

        // Generate random key and IV for testing
        new SecureRandom().nextBytes(aesKey);
        new SecureRandom().nextBytes(aesIv);

        Aes256KeyAndIv original = new Aes256KeyAndIv(aesKey, aesIv);

        // Serialize
        byte[] serializedData = Aes256KeyAndIv.serialize(original);

        // Deserialize
        Aes256KeyAndIv deserialized = Aes256KeyAndIv.deserialize(serializedData);

        // Assertions
        assertArrayEquals(original.aes256Key(), deserialized.aes256Key(), "The keys should be equal after serialization/deserialization.");
        assertArrayEquals(original.aes256Iv(), deserialized.aes256Iv(), "The IVs should be equal after serialization/deserialization.");
    }

    @Test
    public void testEmptySerialization() {
        byte[] emptyKey = new byte[32]; // all zeros
        byte[] emptyIv = new byte[16];  // all zeros

        Aes256KeyAndIv original = new Aes256KeyAndIv(emptyKey, emptyIv);

        // Serialize
        byte[] serializedData = Aes256KeyAndIv.serialize(original);

        // Deserialize
        Aes256KeyAndIv deserialized = Aes256KeyAndIv.deserialize(serializedData);

        // Assertions
        assertArrayEquals(original.aes256Key(), deserialized.aes256Key(), "The keys should be equal after serialization/deserialization.");
        assertArrayEquals(original.aes256Iv(), deserialized.aes256Iv(), "The IVs should be equal after serialization/deserialization.");
    }

    @Test
    public void testFaultyDeserialization() {
        byte[] invalidData = new byte[] { 0, 1 }; // Insufficient data for key and IV

        Exception exception = assertThrows(RuntimeException.class,
                () -> Aes256KeyAndIv.deserialize(invalidData));

        assertEquals("Invalid serialized data length", exception.getMessage());
    }

    @Test
    void testGenerateNewKeyData() {
        EncryptionAlgorithm encryptionAlgorithm = new Aes256CbcEncryptionAlgorithm();
        byte[] keyData = encryptionAlgorithm.generateNewKeyData(new PlainRandomGeneratorType());
        assertNotNull(keyData);
        // Check if keyData can deserialize correctly
        Aes256KeyAndIv serializedKeyAndIv = Aes256KeyAndIv.deserialize(keyData);
        assertNotNull(serializedKeyAndIv);
        assertEquals(32, serializedKeyAndIv.aes256Key().length); // length of AES-256 key
        assertEquals(16, serializedKeyAndIv.aes256Iv().length); // length of IV
    }
}
