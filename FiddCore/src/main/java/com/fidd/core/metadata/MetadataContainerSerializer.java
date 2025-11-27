package com.fidd.core.metadata;

import com.fidd.core.NamedEntry;

public interface MetadataContainerSerializer extends NamedEntry {
    interface MetadataContainerAndLength {
        long lengthBytes();
        MetadataContainer metadataContainer();
    }

    byte[] serialize(MetadataContainer metadata);
    MetadataContainerAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException;
}