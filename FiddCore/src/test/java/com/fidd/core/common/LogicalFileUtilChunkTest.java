package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.RandomAccessEncryptionAlgorithm;
import com.fidd.core.fiddkey.FiddKey;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogicalFileUtilChunkTest {

    @Test
    void getLogicalFileInputStreamChunk_throwsIfAlgorithmNotRandomAccess() {
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository encRepo = mock(Repository.class);
        EncryptionAlgorithm nonRandom = mock(EncryptionAlgorithm.class);
        FiddConnector connector = mock(FiddConnector.class);
        FiddKey.Section section = mock(FiddKey.Section.class);

        when(repos.encryptionAlgorithmRepo()).thenReturn(encRepo);
        when(encRepo.get("AES")).thenReturn(nonRandom);
        when(section.encryptionAlgorithm()).thenReturn("AES");

        assertThrows(InvalidAlgorithmParameterException.class, () ->
                LogicalFileUtil.getLogicalFileInputStreamChunk(
                        repos, connector, 1L, section, 0L, 0L, 10L
                )
        );
    }

    @Test
    void getLogicalFileInputStreamChunk_callsRandomAccessDecryptCorrectly() throws Exception {
        // Mocks
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository encRepo = mock(Repository.class);
        RandomAccessEncryptionAlgorithm algorithm = mock(RandomAccessEncryptionAlgorithm.class);
        FiddConnector connector = mock(FiddConnector.class);
        FiddKey.Section section = mock(FiddKey.Section.class);

        when(repos.encryptionAlgorithmRepo()).thenReturn(encRepo);
        when(encRepo.get("AES")).thenReturn(algorithm);

        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(new byte[]{1,2,3});
        when(section.sectionOffset()).thenReturn(100L);
        when(section.sectionLength()).thenReturn(500L);

        // Fake ciphertext offset mapping
        when(algorithm.ciphertextPosToPlaintextPos(200L)).thenReturn(100L);
        when(algorithm.plaintextPosToCiphertextPos(200L)).thenReturn(900L);
        when(algorithm.plaintextLengthToCiphertextLength(20L)).thenReturn(500L);

        // Fake chunk returned by connector
        InputStream encryptedChunk = new ByteArrayInputStream(new byte[]{9,9,9});
        when(connector.getFiddMessageChunk(77L, 100L + 50L + 900, 500L))
                .thenReturn(encryptedChunk);

        // Fake decrypted stream
        InputStream decrypted = new ByteArrayInputStream(new byte[]{42});
        when(algorithm.getRandomAccessDecryptedStream(
                any(), anyLong(), anyLong(), any()
        )).thenReturn(decrypted);

        // Act
        InputStream result = LogicalFileUtil.getLogicalFileInputStreamChunk(
                repos, connector, 77L, section,
                50L,     // fileOffset
                200L,    // dataOffset
                20L      // dataLength
        );

        // Assert: returned stream is exactly the decrypted one
        assertEquals(42, result.read());

        // Verify correct parameters
        verify(algorithm).getRandomAccessDecryptedStream(
                eq(new byte[]{1,2,3}),
                eq(200L),     // mapped ciphertext offset
                eq(20L),      // dataLength
                eq(encryptedChunk)
        );
    }

    @Test
    void getLogicalFileInputStreamChunk_usesEmptyKeyDataWhenNull() throws Exception {
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository encRepo = mock(Repository.class);
        RandomAccessEncryptionAlgorithm algorithm = mock(RandomAccessEncryptionAlgorithm.class);
        FiddConnector connector = mock(FiddConnector.class);
        FiddKey.Section section = mock(FiddKey.Section.class);

        when(repos.encryptionAlgorithmRepo()).thenReturn(encRepo);
        when(encRepo.get("AES")).thenReturn(algorithm);

        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(null);
        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(10L);

        when(algorithm.ciphertextPosToPlaintextPos(0L)).thenReturn(0L);
        when(algorithm.plaintextPosToCiphertextPos(0L)).thenReturn(0L);
        when(algorithm.plaintextLengthToCiphertextLength(1L)).thenReturn(10L);

        InputStream encrypted = new ByteArrayInputStream(new byte[]{1});
        when(connector.getFiddMessageChunk(5L, 0L, 10L)).thenReturn(encrypted);

        InputStream decrypted = new ByteArrayInputStream(new byte[]{2});
        when(algorithm.getRandomAccessDecryptedStream(any(), anyLong(), anyLong(), any()))
                .thenReturn(decrypted);

        LogicalFileUtil.getLogicalFileInputStreamChunk(
                repos, connector, 5L, section,
                0L, 0L, 1L
        );

        verify(algorithm).getRandomAccessDecryptedStream(
                eq(new byte[0]), // empty key
                eq(0L),
                eq(1L),
                eq(encrypted)
        );
    }
}
