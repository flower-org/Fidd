package com.fidd.core.metadata;

import com.fidd.core.NamedEntry;

public interface MetadataSectionSerializer extends NamedEntry {
    byte[] serialize(MetadataSection metadata);
    MetadataSection deserialize(byte[] metadataBytes);
}