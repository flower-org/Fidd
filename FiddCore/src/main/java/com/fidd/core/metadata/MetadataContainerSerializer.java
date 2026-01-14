package com.fidd.core.metadata;

import com.fidd.core.NamedEntry;

public interface MetadataContainerSerializer extends NamedEntry {
    interface MetadataContainerAndLength {
        long lengthBytes();
        MetadataContainer metadataContainer();

        static MetadataContainerAndLength of(long lengthBytes, MetadataContainer metadataContainer) {
            return new MetadataContainerAndLength() {
                @Override
                public long lengthBytes() {
                    return lengthBytes;
                }

                @Override
                public MetadataContainer metadataContainer() {
                    return metadataContainer;
                }
            };
        }
    }

    byte[] serialize(MetadataContainer metadata);
    MetadataContainerAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException;
}