package com.fidd.core.common;

import com.fidd.core.common.FiddKeyLookup.Trie;
import com.flower.crypt.PkiUtil;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FiddKeyLookupTest {

    @Test
    void testCreateLookupFootprint() throws Exception {
        X509Certificate cert = mock(X509Certificate.class);
        PublicKey key = mock(PublicKey.class);

        when(cert.getPublicKey()).thenReturn(key);

        byte[] encrypted = new byte[]{1, 2, 3};
        try (MockedStatic<PkiUtil> pki = mockStatic(PkiUtil.class);
             MockedStatic<Base36> base36 = mockStatic(Base36.class)) {

            pki.when(() -> PkiUtil.encrypt(any(byte[].class), eq(key))).thenReturn(encrypted);
            base36.when(() -> Base36.toBase36(encrypted)).thenReturn("ABC123");

            String result = FiddKeyLookup.createLookupFootprint(cert, 10L, 20L);

            assertEquals("ABC123", result);
        }
    }

    @Test
    void testCreateLookupFootprints() throws Exception {
        X509Certificate cert1 = mock(X509Certificate.class);
        X509Certificate cert2 = mock(X509Certificate.class);
        PublicKey key = mock(PublicKey.class);

        when(cert1.getPublicKey()).thenReturn(key);
        when(cert2.getPublicKey()).thenReturn(key);

        byte[] encrypted = new byte[]{9, 9};
        try (MockedStatic<PkiUtil> pki = mockStatic(PkiUtil.class);
             MockedStatic<Base36> base36 = mockStatic(Base36.class)) {

            pki.when(() -> PkiUtil.encrypt(any(byte[].class), eq(key))).thenReturn(encrypted);
            base36.when(() -> Base36.toBase36(encrypted)).thenReturn("ZZ");

            List<String> results = FiddKeyLookup.createLookupFootprints(
                    List.of(cert1, cert2), 1L, 2L);

            assertEquals(List.of("ZZ", "ZZ"), results);
        }
    }

    @Test
    void testTrieBasicBehavior() {
        Trie root = new Trie();

        assertEquals(0, root.getCount());
        Trie a = root.create('a');
        assertEquals(1, a.getCount());
        assertNull(root.get('b'));
    }

    @Test
    void testBuildHighAmbiguityPrefixList() {
        List<String> footprints = List.of("abcd", "abxy", "abzz");

        List<String> result = FiddKeyLookup.buildHighAmbiguityPrefixList(footprints);

        // High ambiguity: stops when a new branch is created
        assertEquals(List.of("a", "ab", "abz"), result);
    }

    @Test
    void testBuildLowAmbiguityPrefixList() {
        List<String> footprints = List.of("abcd", "abxy", "abzz");

        List<String> result = FiddKeyLookup.buildLowAmbiguityPrefixList(footprints);

        // Low ambiguity: shortest prefix that uniquely identifies each
        assertEquals(List.of("abc", "abx", "abz"), result);
    }
}
