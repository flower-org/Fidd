package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddkey.FiddKey;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogicalFileUtilTest {

    // ------------------------------------------------------------
    // skipAll tests
    // ------------------------------------------------------------

    @Test
    void skipAll_skipsExactBytes() throws Exception {
        byte[] data = new byte[10];
        ByteArrayInputStream stream = new ByteArrayInputStream(data);

        LogicalFileUtil.skipAll(stream, 5);

        assertEquals(5, stream.readAllBytes().length); // now at index 5
    }

    @Test
    void skipAll_handlesZeroSkipByReadingOneByte() throws Exception {
        InputStream stream = spy(new ByteArrayInputStream(new byte[]{1,2,3}));

        // Force skip() to always return 0
        doReturn(0L).when(stream).skip(anyLong());

        LogicalFileUtil.skipAll(stream, 2);

        // Verify fallback read() was used
        verify(stream, atLeastOnce()).read();
    }

    @Test
    void skipAll_throwsEOFExceptionWhenStreamEndsEarly() {
        InputStream stream = new ByteArrayInputStream(new byte[]{1,2});

        assertThrows(EOFException.class, () ->
                LogicalFileUtil.skipAll(stream, 5)
        );
    }

    // ------------------------------------------------------------
    // getLogicalFileInputStream tests
    // ------------------------------------------------------------

    @Test
    void getLogicalFileInputStream_returnsDecryptedStreamAtCorrectOffset() throws Exception {
        // Mocks
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository encRepo = mock(Repository.class);
        EncryptionAlgorithm algorithm = mock(EncryptionAlgorithm.class);
        FiddConnector connector = mock(FiddConnector.class);

        when(repos.encryptionAlgorithmRepo()).thenReturn(encRepo);
        when(encRepo.get("AES")).thenReturn(algorithm);

        // Fake section
        FiddKey.Section section = mock(FiddKey.Section.class);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(new byte[]{9,9});
        when(section.sectionOffset()).thenReturn(100L);
        when(section.sectionLength()).thenReturn(50L);

        // Fake chunk returned by connector
        InputStream encryptedChunk = new ByteArrayInputStream(new byte[]{10,20,30,40,50});
        when(connector.getFiddMessageChunk(42L, 100L, 50L)).thenReturn(encryptedChunk);

        // Fake decrypted stream
        InputStream decrypted = new ByteArrayInputStream(new byte[]{7,8,9,10});
        when(algorithm.getDecryptedStream(any(), any())).thenReturn(decrypted);

        // Act
        InputStream result = LogicalFileUtil.getLogicalFileInputStream(
                repos, connector, 42L, section, 2L
        );

        // Assert: stream should now be positioned after skipping 2 bytes
        assertEquals(9, result.read());
        assertEquals(10, result.read());

        // Verify correct call order
        InOrder order = inOrder(connector, algorithm);
        order.verify(connector).getFiddMessageChunk(42L, 100L, 50L);
        order.verify(algorithm).getDecryptedStream(new byte[]{9,9}, encryptedChunk);
    }

    @Test
    void getLogicalFileInputStream_usesEmptyKeyDataWhenNull() throws Exception {
        BaseRepositories repos = mock(BaseRepositories.class);
        Repository encRepo = mock(Repository.class);
        EncryptionAlgorithm algorithm = mock(EncryptionAlgorithm.class);
        FiddConnector connector = mock(FiddConnector.class);

        when(repos.encryptionAlgorithmRepo()).thenReturn(encRepo);
        when(encRepo.get("AES")).thenReturn(algorithm);

        FiddKey.Section section = mock(FiddKey.Section.class);
        when(section.encryptionAlgorithm()).thenReturn("AES");
        when(section.encryptionKeyData()).thenReturn(null);
        when(section.sectionOffset()).thenReturn(0L);
        when(section.sectionLength()).thenReturn(1L);

        InputStream encrypted = new ByteArrayInputStream(new byte[]{1});
        when(connector.getFiddMessageChunk(1L, 0L, 1L)).thenReturn(encrypted);

        InputStream decrypted = new ByteArrayInputStream(new byte[]{1});
        when(algorithm.getDecryptedStream(any(), any())).thenReturn(decrypted);

        LogicalFileUtil.getLogicalFileInputStream(repos, connector, 1L, section, 0L);

        verify(algorithm).getDecryptedStream(eq(new byte[0]), any());
    }
}
