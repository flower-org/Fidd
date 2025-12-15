package com.fidd.core.logicalfile.yaml;

import com.fidd.core.common.FiddSignature;
import com.fidd.core.common.ProgressiveCrc;
import com.fidd.core.logicalfile.ImmutableExternalResource;
import com.fidd.core.logicalfile.ImmutableLogicalFileMetadata;
import com.fidd.core.logicalfile.ImmutableFileRegion;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.logicalfile.ResourceDescriptorType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlLogicalFileMetadataSerializerTest {
    @Test
    public void testSerializeAndDeserialize() {
        LogicalFileMetadata logicalFileMetadata = ImmutableLogicalFileMetadata.builder()
                .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                .filePath("/what/not_sure.zip")
                .createdAt(1234L)
                .updatedAt(2345L)
                .authorsFileSignatures(List.of(FiddSignature.of("FMT", new byte[] {2,3,4,5,6,7})))
                .progressiveCrcs(List.of(ProgressiveCrc.of("CRC32", new byte[] {2,3,4,5,6,7,2,3,4,5,6,7,2,3,4,5,6,7,2,3,4,5,6,7}, 10241024L)))
                .externalLinks(
                    List.of(
                        ImmutableExternalResource.builder()
                                .fileRegions(List.of(
                                        ImmutableFileRegion.builder()
                                            .offset(0L)
                                            .length(1000000L)
                                            .regionFileName("a7ed428b-deb2-442c-83d2-8b7dc7fce5b0")
                                            .resourceDescriptorType(ResourceDescriptorType.URL)
                                            .resourceDescriptor("https://mydl.com/dl1".getBytes(StandardCharsets.UTF_8))
                                            .build()
                                ))
                                .build(),
                        ImmutableExternalResource.builder()
                                .fileRegions(List.of(
                                        ImmutableFileRegion.builder()
                                                .offset(0L)
                                                .length(500000L)
                                                .regionFileName("6436c8df-42cf-4ba1-a691-ad5e906b62bd")
                                                .resourceDescriptorType(ResourceDescriptorType.URL)
                                                .resourceDescriptor("https://mydl.com/dl1.1".getBytes(StandardCharsets.UTF_8))
                                                .build(),
                                        ImmutableFileRegion.builder()
                                                .offset(500000L)
                                                .length(1000000L)
                                                .regionFileName("9dc24d4c-77a3-4314-b6ed-c59d0f7f7e66")
                                                .resourceDescriptorType(ResourceDescriptorType.URL)
                                                .resourceDescriptor("https://mydl.com/dl1.2".getBytes(StandardCharsets.UTF_8))
                                                .build()
                                ))
                                .build()
                    )
                )
                .build();

        YamlLogicalFileMetadataSerializer serializer = new YamlLogicalFileMetadataSerializer();
        byte[] resultBytes = serializer.serialize(logicalFileMetadata);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        LogicalFileMetadata resultLogicalFileMetadata = serializer.deserialize(resultBytes);
        assertNotNull(resultLogicalFileMetadata);

        assertEquals(logicalFileMetadata, resultLogicalFileMetadata);
    }

    @Test
    public void testDeserializationException() {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        YamlLogicalFileMetadataSerializer serializer = new YamlLogicalFileMetadataSerializer();
        assertThrows(RuntimeException.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
