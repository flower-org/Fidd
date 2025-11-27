package com.fidd.core.metadata;

import com.fidd.core.NamedEntry;

public interface MetadataSectionSerializer extends NamedEntry {
    interface MetadataSectionAndLength {
        long lengthBytes();
        MetadataSection metadataSection();
    }

    byte[] serialize(MetadataSection metadata);
    MetadataSectionAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException;
}