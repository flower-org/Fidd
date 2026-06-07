package com.fidd.core.info;

import com.fidd.core.NamedEntry;

public interface MetadataSectionInfoSerializer extends NamedEntry {
    byte[] serialize(MetadataSectionInfo info);
    MetadataSectionInfo deserialize(byte[] bytes);
}
