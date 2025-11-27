package com.fidd.core.metadata.blobs;

import com.fidd.core.metadata.ImmutableMetadataSection;
import com.fidd.core.metadata.MetadataSection;
import com.fidd.core.metadata.MetadataSectionSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.pack.BlobsPacker;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class BlobsMetadataSectionSerializer implements MetadataSectionSerializer {
    @Override
    public byte[] serialize(MetadataSection metadataSection) {
        byte[] metadataFormat = metadataSection.metadataFormat().getBytes(StandardCharsets.UTF_8);
        byte[] metadata = metadataSection.metadata();

        byte[] signatureFormat;
        if (metadataSection.signatureFormat() != null) {
            signatureFormat = metadataSection.signatureFormat().getBytes(StandardCharsets.UTF_8);
        } else { signatureFormat = new byte[] {}; }

        byte[] signature;
        if (metadataSection.signature() != null) {
            signature = metadataSection.signature();
        } else { signature = new byte[] {}; }

        return BlobsPacker.packBlobs(signature, metadata, signatureFormat, metadataFormat);
    }

    @Override
    public MetadataSectionAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException {
        final Pair<Long, List<byte[]>> unpacked = BlobsPacker.unpackBlobs(metadataBytes);

        final long lengthBytes = unpacked.getLeft();
        final List<byte[]> parts = unpacked.getRight();

        byte[] signature = parts.get(0);
        byte[] metadata = parts.get(1);
        byte[] signatureFormat = parts.get(2);
        byte[] metadataFormat = parts.get(3);

        ImmutableMetadataSection.Builder builder = ImmutableMetadataSection.builder()
                .metadataFormat(new String(metadataFormat, StandardCharsets.UTF_8))
                .metadata(metadata);

        if (signatureFormat.length > 0) {
            builder.signatureFormat(new String(signatureFormat, StandardCharsets.UTF_8));
        }
        if (signature.length > 0) {
            builder.signature(signature);
        }

        MetadataSection metadataSection = builder.build();
        return new MetadataSectionAndLength() {
            @Override
            public long lengthBytes() {
                return lengthBytes;
            }

            @Override
            public MetadataSection metadataSection() {
                return metadataSection;
            }
        };
    }

    @Override
    public String name() { return "BLOBS"; }
}
