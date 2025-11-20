package com.fidd.core.metadata.yaml;

import com.fidd.core.metadata.ImmutableMetadataSection;
import com.fidd.core.metadata.MetadataSection;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlMetadataSectionSerializerTest {
    @Test
    public void testSerializeAndDeserialize() {
        MetadataSection metadataSection = ImmutableMetadataSection.builder()
                .metadataFormat("MDF")
                .metadata(new byte[] { 1, 2, 3, 4 })
                .signatureFormat("SGF")
                .signature(new byte[] { 5, 6, 7, 8, 9, 10 })
                .build();

        YamlMetadataSectionSerializer serializer = new YamlMetadataSectionSerializer();
        byte[] resultBytes = serializer.serialize(metadataSection);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        MetadataSection resultMetadataSection = serializer.deserialize(resultBytes);
        assertNotNull(resultMetadataSection);

        assertEquals(metadataSection, resultMetadataSection);
    }

    @Test
    public void testDeserializationException() {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        YamlMetadataSectionSerializer serializer = new YamlMetadataSectionSerializer();
        assertThrows(RuntimeException.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
