package com.fidd.core.fiddfile;

import com.fidd.core.NamedEntry;

public interface FiddFileMetadataSignatureSerializer extends NamedEntry {
    byte[] serialize(FiddFileMetadataSignature metadataSignature);
    FiddFileMetadataSignature deserialize(byte[] metadataSignatureBytes);
}
