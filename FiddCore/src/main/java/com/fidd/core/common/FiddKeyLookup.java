package com.fidd.core.common;

import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.pki.StableTransformForAlgo;
import com.flower.crypt.PkiUtil;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.ProviderNotFoundException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddKeyLookup {
    static final Repository<StableTransformForAlgo> STABLE_TRANSFORM_REPO = new DefaultBaseRepositories().stableTransformRepo();

    public static String createLookupFootprint(X509Certificate subscriberCert, long messageNumber, long messageLength) throws Exception {
        String footprint = Long.toString(messageLength) + Long.toString(messageNumber);
        byte[] footprintBytes = footprint.getBytes(StandardCharsets.UTF_8);

        PublicKey publicKey = subscriberCert.getPublicKey();
        StableTransformForAlgo transform = STABLE_TRANSFORM_REPO.get(publicKey.getAlgorithm());
        if (transform == null) {
            throw new ProviderNotFoundException("StableTransform not defined for algorithm " + publicKey.getAlgorithm());
        }
        byte[] lookupSignatureBytes = PkiUtil.encrypt(footprintBytes, publicKey, transform.transform());
        return Base36.toBase36(lookupSignatureBytes);
    }

    public static List<String> createLookupFootprints(List<X509Certificate> subscriberCerts, long messageNumber, long messageLength) throws Exception {
        List<String> signatures = new java.util.ArrayList<>();
        for (X509Certificate subscriberCert : subscriberCerts) {
            signatures.add(createLookupFootprint(subscriberCert, messageNumber, messageLength));
        }
        return signatures;
    }

    static class Trie {
        int count = 0;
        final Map<Character, Trie> map;

        public Trie() { this.map = new HashMap<>(); }
        public @Nullable Trie get(Character c) { return map.get(c); }
        public Trie create(Character c) {
            Trie t = new Trie();
            t.addCount();
            map.put(c, t);
            return t;
        }
        public int addCount() { return ++count; }
        public int getCount() { return count; }
    }

    /** High ambiguity: stops when a new branch is created */
    public static List<String> buildHighAmbiguityPrefixList(List<String> footprints) {
        List<String> result = new ArrayList<>();

        Trie root = new Trie();
        // Fill the trie while finding unique prefixes, in a greedy way
        for (String footprint : footprints) {
            StringBuilder prefix = new StringBuilder();
            Trie t = root;
            for (char c : footprint.toCharArray()) {
                prefix.append(c);
                Trie next = t.get(c);
                if (next == null) {
                    t.create(c);
                    break;
                } else {
                    next.addCount();
                    t = next;
                }
            }
            result.add(prefix.toString());
        }

        return result;
    }

    /** Low ambiguity: shortest prefix that uniquely identifies each */
    public static List<String> buildLowAmbiguityPrefixList(List<String> footprints) {
        List<String> result = new ArrayList<>();

        Trie root = new Trie();
        // Fill the trie
        for (String footprint : footprints) {
            Trie t = root;
            for (char c : footprint.toCharArray()) {
                Trie next = t.get(c);
                if (next == null) {
                    next = t.create(c);
                } else {
                    next.addCount();
                }
                t = next;
            }
        }

        // Find unique prefixes
        for (String footprint : footprints) {
            StringBuilder prefix = new StringBuilder();
            Trie t = root;
            for (char c : footprint.toCharArray()) {
                prefix.append(c);
                Trie next = t.get(c);
                if (checkNotNull(next).getCount() == 1) {
                    break;
                }
                next.addCount();
                t = next;
            }
            result.add(prefix.toString());
        }

        return result;
    }
}
