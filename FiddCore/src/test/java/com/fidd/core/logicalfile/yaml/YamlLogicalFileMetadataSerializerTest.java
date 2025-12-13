package com.fidd.core.logicalfile.yaml;

import com.fidd.core.common.FiddSignature;
import com.fidd.core.logicalfile.ImmutableAlternativeLink;
import com.fidd.core.logicalfile.ImmutableLogicalFileMetadata;
import com.fidd.core.logicalfile.ImmutableRegion;
import com.fidd.core.logicalfile.LogicalFileMetadata;
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
                .alternativeLinks(
                    List.of(
                        ImmutableAlternativeLink.builder()
                                .fullUrl("https://mydl.com/dl1")
                                .build(),
                        ImmutableAlternativeLink.builder()
                                .regions(List.of(ImmutableRegion.builder().offset(123).length(456).url("https://mydl.com/dl1.1").build(),
                                        ImmutableRegion.builder().offset(789).length(101112).url("https://mydl.com/dl1.2").build()))
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
