package com.fidd.core.metadata.blobs;

import com.fidd.core.common.FiddSignature;
import com.fidd.core.metadata.ImmutableMetadataContainer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.pack.BlobsPacker;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BlobsMetadataContainerSerializer implements MetadataContainerSerializer {
    @Override
    public byte[] serialize(MetadataContainer metadataContainer) {
        List<byte[]> blobs = new ArrayList<>();

        if (metadataContainer.signatures() != null) {
            for (FiddSignature fiddSignature : metadataContainer.signatures()) {
                byte[] signatureFormat = fiddSignature.format().getBytes(StandardCharsets.UTF_8);
                byte[] signature = fiddSignature.bytes();
                blobs.add(signatureFormat);
                blobs.add(signature);
            }
        }

        byte[] metadataFormat = metadataContainer.metadataFormat().getBytes(StandardCharsets.UTF_8);
        byte[] metadata = metadataContainer.metadata();
        blobs.add(metadataFormat);
        blobs.add(metadata);

        return BlobsPacker.packBlobs(blobs);
    }

    @Override
    public MetadataContainerAndLength deserialize(byte[] metadataBytes) throws NotEnoughBytesException {
        final Pair<Long, List<byte[]>> unpacked = BlobsPacker.unpackBlobs(metadataBytes);

        final long lengthBytes = unpacked.getLeft();
        final List<byte[]> parts = unpacked.getRight();

        byte[] metadataFormat = parts.get(parts.size()-2);
        byte[] metadata = parts.get(parts.size()-1);

        List<FiddSignature> signatures = new ArrayList<>();
        for (int i = 0; i < parts.size()-2; i+=2) {
            byte[] signatureFormat = parts.get(i);
            byte[] signature = parts.get(i + 1);
            signatures.add(FiddSignature.of(new String(signatureFormat, StandardCharsets.UTF_8), signature));
        }

        MetadataContainer metadataContainer = ImmutableMetadataContainer.builder()
                .metadataFormat(new String(metadataFormat, StandardCharsets.UTF_8))
                .metadata(metadata)
                .signatures(signatures)
                .build();

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
