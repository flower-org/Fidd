package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.pki.StableTransformForAlgo;
import com.flower.crypt.PkiUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.ProviderNotFoundException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FiddKeyLookupTest {

    BaseRepositories baseRepositories;
    Repository<StableTransformForAlgo> transformRepo;
    X509Certificate cert;
    PublicKey publicKey;
    StableTransformForAlgo transform;

    @BeforeEach
    void setup() {
        baseRepositories = mock(BaseRepositories.class);
        transformRepo = mock(Repository.class);
        cert = mock(X509Certificate.class);
        publicKey = mock(PublicKey.class);
        transform = mock(StableTransformForAlgo.class);

        when(baseRepositories.stableTransformRepo()).thenReturn(transformRepo);
        when(cert.getPublicKey()).thenReturn(publicKey);
    }

    // ------------------------------------------------------------
    // createLookupFootprint
    // ------------------------------------------------------------

    @Test
    void testCreateLookupFootprint_success() throws Exception {
        when(publicKey.getAlgorithm()).thenReturn("RSA");
        when(transformRepo.get("RSA")).thenReturn(transform);
        when(transform.transform()).thenReturn("TRANSFORMED");

        byte[] encrypted = new byte[]{1, 2, 3};

        try (MockedStatic<PkiUtil> mocked = mockStatic(PkiUtil.class);
             MockedStatic<Base36> mocked36 = mockStatic(Base36.class)) {

            mocked.when(() -> PkiUtil.encrypt(any(), eq(publicKey), eq("TRANSFORMED")))
                    .thenReturn(encrypted);

            mocked36.when(() -> Base36.toBase36(encrypted))
                    .thenReturn("ABC123");

            String result = FiddKeyLookup.createLookupFootprint(
                    baseRepositories, cert, 5L, 10L);

            assertEquals("ABC123", result);
        }
    }

    @Test
    void testCreateLookupFootprint_missingTransform() {
        when(publicKey.getAlgorithm()).thenReturn("EC");
        when(transformRepo.get("EC")).thenReturn(null);

        assertThrows(ProviderNotFoundException.class, () ->
                FiddKeyLookup.createLookupFootprint(baseRepositories, cert, 1L, 1L));
    }

    // ------------------------------------------------------------
    // Trie tests
    // ------------------------------------------------------------

    @Test
    void testTrieBasicOperations() {
        FiddKeyLookup.Trie root = new FiddKeyLookup.Trie();

        assertNull(root.get('a'));

        FiddKeyLookup.Trie child = root.create('a');
        assertNotNull(child);
        assertEquals(1, child.getCount());
        assertEquals(child, root.get('a'));

        child.addCount();
        assertEquals(2, child.getCount());
    }

    // ------------------------------------------------------------
    // buildHighAmbiguityPrefixList
    // ------------------------------------------------------------

    @Test
    void testBuildHighAmbiguityPrefixList() {
        List<String> input = List.of("abcd", "abxy", "abcz");

        List<String> result = FiddKeyLookup.buildHighAmbiguityPrefixList(input);

        assertEquals(List.of("a", "ab", "abc"), result);
    }

    // ------------------------------------------------------------
    // buildLowAmbiguityPrefixList
    // ------------------------------------------------------------

    @Test
    void testBuildLowAmbiguityPrefixList() {
        List<String> input = List.of("abcd", "abxy", "abcz");

        List<String> result = FiddKeyLookup.buildLowAmbiguityPrefixList(input);

        assertEquals(List.of("abcd", "abx", "abcz"), result);
    }
}
