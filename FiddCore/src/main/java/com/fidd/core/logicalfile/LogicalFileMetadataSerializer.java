package com.fidd.core.logicalfile;

import com.fidd.core.NamedEntry;

public interface LogicalFileMetadataSerializer extends NamedEntry {
    byte[] serialize(LogicalFileMetadata metadata);
    LogicalFileMetadata deserialize(byte[] metadataBytes);
}
