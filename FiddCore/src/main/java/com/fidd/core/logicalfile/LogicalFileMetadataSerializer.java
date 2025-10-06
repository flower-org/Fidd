package com.fidd.core.logicalfile;

import com.fidd.core.NamedEntry;
import com.fidd.core.fiddkey.FiddKey;

public interface LogicalFileMetadataSerializer extends NamedEntry {
    byte[] serialize(FiddKey metadata);
    FiddKey deserialize(byte[] metadataBytes);
}
