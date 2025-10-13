package com.fidd.core.fiddkey;

import com.fidd.core.NamedEntry;

public interface FiddKeySerializer extends NamedEntry {
    byte[] serialize(FiddKey fiddKey);
    FiddKey deserialize(byte[] fiddKeyBytes);
}
