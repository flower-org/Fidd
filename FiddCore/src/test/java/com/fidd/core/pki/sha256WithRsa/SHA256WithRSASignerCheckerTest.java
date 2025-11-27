package com.fidd.core.pki.sha256WithRsa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

class SHA256WithRSASignerCheckerTest {

    private SHA256WithRSASignerChecker signerChecker;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        signerChecker = new SHA256WithRSASignerChecker();

        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }

    @Test
    void testSignAndVerifyByteArray_success() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);

        byte[] signature = signerChecker.signData(data, keyPair.getPrivate());
        assertNotNull(signature);

        boolean verified = signerChecker.verifySignature(data, signature, keyPair.getPublic());
        assertTrue(verified);
    }

    @Test
    void testSignAndVerifyInputStream_success() {
        byte[] data = "Stream Data".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

        byte[] signature = signerChecker.signData(inputStream, keyPair.getPrivate());
        assertNotNull(signature);

        ByteArrayInputStream verifyStream = new ByteArrayInputStream(data);
        boolean verified = signerChecker.verifySignature(verifyStream, signature, keyPair.getPublic());
        assertTrue(verified);
    }

    @Test
    void testVerifySignature_failureWithTamperedData() {
        byte[] data = "Original".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signerChecker.signData(data, keyPair.getPrivate());

        byte[] tamperedData = "Tampered".getBytes(StandardCharsets.UTF_8);
        boolean verified = signerChecker.verifySignature(tamperedData, signature, keyPair.getPublic());
        assertFalse(verified);
    }

    @Test
    void testVerifySignature_failureWithTamperedSignature() {
        byte[] data = "Original".getBytes(StandardCharsets.UTF_8);
        byte[] signature = signerChecker.signData(data, keyPair.getPrivate());

        // Modify signature bytes
        signature[0] ^= 0xFF;

        boolean verified = signerChecker.verifySignature(data, signature, keyPair.getPublic());
        assertFalse(verified);
    }

    @Test
    void testName_returnsExpectedAlgorithm() {
        assertEquals("SHA256WithRSA", signerChecker.name());
    }

    @Test
    void testSignData_withNullPrivateKey_throwsRuntimeException() {
        byte[] data = "Invalid".getBytes(StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class, () -> signerChecker.signData(data, null));
    }

    @Test
    void testVerifySignature_withNullPublicKey_throwsRuntimeException() {
        byte[] data = "Invalid".getBytes(StandardCharsets.UTF_8);
        byte[] signature = new byte[0];
        assertThrows(RuntimeException.class, () -> signerChecker.verifySignature(data, signature, null));
    }
}
