package com.fidd.core.pki;

import com.fidd.core.NamedEntry;

public interface StableTransformForAlgo extends NamedEntry {
    String transform();

    static StableTransformForAlgo of(String algo, String transform) {
        return new StableTransformForAlgo() {
            @Override public String transform() { return transform; }
            @Override public String name() { return algo; }
        };
    }
}
