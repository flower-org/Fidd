package com.fidd.core.logicalfile.yaml;

import com.fidd.core.logicalfile.ImmutableLogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
                .authorsFileSignatureFormat("FMT")
                .authorsFileSignature(new byte[] {2,3,4,5,6,7})
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
