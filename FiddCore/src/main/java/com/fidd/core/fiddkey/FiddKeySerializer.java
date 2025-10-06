package com.fidd.core.fiddkey;

import com.fidd.core.NamedEntry;

public interface FiddKeySerializer extends NamedEntry {
    byte[] serialize(FiddKey metadata);
    FiddKey deserialize(byte[] metadataBytes);
}
