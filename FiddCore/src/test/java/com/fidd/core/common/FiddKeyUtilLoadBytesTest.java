package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.flower.crypt.HybridAesEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FiddKeyUtilLoadBytesTest {

    private BaseRepositories baseRepositories;
    private FiddConnector fiddConnector;
    private X509Certificate userCert;
    private PrivateKey privateKey;

    @BeforeEach
    void setup() {
        baseRepositories = mock(BaseRepositories.class);
        fiddConnector = mock(FiddConnector.class);
        userCert = mock(X509Certificate.class);
        privateKey = mock(PrivateKey.class);
    }

    @Test
    void testSuccessfulDecryptionReturnsBytes() throws Exception {
        long messageNumber = 42L;
        long messageLength = 100L;

        when(fiddConnector.getFiddMessageSize(messageNumber)).thenReturn(messageLength);

        String footprint = "footprint";
        try (MockedStatic<FiddKeyLookup> lookupMock = mockStatic(FiddKeyLookup.class)) {
            lookupMock.when(() -> FiddKeyLookup.createLookupFootprint(
                    baseRepositories, userCert, messageNumber, messageLength
            )).thenReturn(footprint);

            byte[] candidate = "candidate1".getBytes(StandardCharsets.UTF_8);
            when(fiddConnector.getFiddKeyCandidates(messageNumber, footprint.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(List.of(candidate));

            byte[] encrypted = "encrypted".getBytes(StandardCharsets.UTF_8);
            when(fiddConnector.getFiddKey(messageNumber, candidate)).thenReturn(encrypted);

            byte[] decrypted = "decrypted".getBytes(StandardCharsets.UTF_8);

            try (MockedStatic<HybridAesEncryptor> decryptMock = mockStatic(HybridAesEncryptor.class)) {
                decryptMock.when(() -> HybridAesEncryptor.decrypt(
                        any(), any(), eq(HybridAesEncryptor.Mode.PUBLIC_KEY_ENCRYPT),
                        eq(privateKey), isNull(), isNull()
                )).thenAnswer(inv -> {
                    inv.<java.io.OutputStream>getArgument(1).write(decrypted);
                    return null;
                });

                byte[] result = FiddKeyUtil.loadFiddKeyBytes(
                        baseRepositories, messageNumber, fiddConnector, userCert, privateKey
                );

                assertArrayEquals(decrypted, result);
            }
        }
    }

    @Test
    void testFailedDecryptionTriesNextCandidateAndReturnsNull() throws Exception {
        long messageNumber = 1L;
        long messageLength = 50L;

        when(fiddConnector.getFiddMessageSize(messageNumber)).thenReturn(messageLength);

        String footprint = "fp";
        try (MockedStatic<FiddKeyLookup> lookupMock = mockStatic(FiddKeyLookup.class)) {
            lookupMock.when(() -> FiddKeyLookup.createLookupFootprint(
                    baseRepositories, userCert, messageNumber, messageLength
            )).thenReturn(footprint);

            byte[] candidate = "bad".getBytes(StandardCharsets.UTF_8);
            when(fiddConnector.getFiddKeyCandidates(messageNumber, footprint.getBytes(StandardCharsets.UTF_8)))
                    .thenReturn(List.of(candidate));

            byte[] encrypted = "encrypted".getBytes(StandardCharsets.UTF_8);
            when(fiddConnector.getFiddKey(messageNumber, candidate)).thenReturn(encrypted);

            try (MockedStatic<HybridAesEncryptor> decryptMock = mockStatic(HybridAesEncryptor.class)) {
                decryptMock.when(() -> HybridAesEncryptor.decrypt(any(), any(), any(), any(), any(), any()))
                        .thenThrow(new RuntimeException("decrypt failed"));

                byte[] result = FiddKeyUtil.loadFiddKeyBytes(
                        baseRepositories, messageNumber, fiddConnector, userCert, privateKey
                );

                assertNull(result);
            }
        }
    }

    @Test
    void testNoCandidatesReturnsNull() throws Exception {
        long messageNumber = 10L;
        long messageLength = 20L;

        when(fiddConnector.getFiddMessageSize(messageNumber)).thenReturn(messageLength);

        try (MockedStatic<FiddKeyLookup> lookupMock = mockStatic(FiddKeyLookup.class)) {
            lookupMock.when(() -> FiddKeyLookup.createLookupFootprint(
                    baseRepositories, userCert, messageNumber, messageLength
            )).thenReturn("fp");

            when(fiddConnector.getFiddKeyCandidates(anyLong(), any()))
                    .thenReturn(List.of());

            byte[] result = FiddKeyUtil.loadFiddKeyBytes(
                    baseRepositories, messageNumber, fiddConnector, userCert, privateKey
            );

            assertNull(result);
        }
    }
}
