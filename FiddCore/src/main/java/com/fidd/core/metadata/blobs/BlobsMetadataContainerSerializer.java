package com.fidd.core.metadata.blobs;

import com.fidd.core.metadata.ImmutableMetadataContainer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.pack.BlobsPacker;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class BlobsMetadataContainerSerializer implements MetadataContainerSerializer {
    @Override
    public byte[] serialize(MetadataContainer metadataContainer) {
        byte[] metadataFormat = metadataContainer.metadataFormat().getBytes(StandardCharsets.UTF_8);
        byte[] metadata = metadataContainer.metadata();

        byte[] signatureFormat;
        if (metadataContainer.signatureFormat() != null) {
            signatureFormat = metadataContainer.signatureFormat().getBytes(StandardCharsets.UTF_8);
        } else { signatureFormat = new byte[] {}; }

        byte[] signature;
        if (metadataContainer.signature() != null) {
            signature = metadataContainer.signature();
        } else { signature = new byte[] {}; }

        return BlobsPacker.packBlobs(signature, metadata, signatureFormat, metadataFormat);
    }

    @Override
    public MetadataContainerAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException {
        final Pair<Long, List<byte[]>> unpacked = BlobsPacker.unpackBlobs(metadataBytes);

        final long lengthBytes = unpacked.getLeft();
        final List<byte[]> parts = unpacked.getRight();

        byte[] signature = parts.get(0);
        byte[] metadata = parts.get(1);
        byte[] signatureFormat = parts.get(2);
        byte[] metadataFormat = parts.get(3);

        ImmutableMetadataContainer.Builder builder = ImmutableMetadataContainer.builder()
                .metadataFormat(new String(metadataFormat, StandardCharsets.UTF_8))
                .metadata(metadata);

        if (signatureFormat.length > 0) {
            builder.signatureFormat(new String(signatureFormat, StandardCharsets.UTF_8));
        }
        if (signature.length > 0) {
            builder.signature(signature);
        }

        MetadataContainer metadataContainer = builder.build();
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

    @Override
    public String name() { return "BLOBS"; }
}
