package com.fidd.core.fiddfile;

import com.fidd.core.NamedEntry;

public interface FiddFileMetadataSerializer extends NamedEntry {
    byte[] serialize(FiddFileMetadata metadata);
    FiddFileMetadata deserialize(byte[] metadataBytes);
}
