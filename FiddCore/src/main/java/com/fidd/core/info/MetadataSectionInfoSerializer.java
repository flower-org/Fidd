package com.fidd.core.info;

import com.fidd.core.NamedEntry;

public interface MetadataSectionInfoSerializer extends NamedEntry {
    byte[] serialize(FiddMetadataInfo info);
    FiddMetadataInfo deserialize(byte[] bytes);
}
