package com.fidd.core.info;

import com.fidd.core.NamedEntry;

public interface FiddInfoSerializer extends NamedEntry {
    byte[] serialize(FiddInfo info);
    FiddInfo deserialize(byte[] bytes);
}
