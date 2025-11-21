package com.fidd.core.metadata.yaml;

import com.fidd.core.metadata.ImmutableMetadataSection;
import com.fidd.core.metadata.MetadataSection;
import com.fidd.core.metadata.MetadataSectionSerializer;
import com.fidd.core.metadata.blobs.BlobsMetadataSectionSerializer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MetadataSectionSerializerTest {
    static Stream<Arguments> serializers() {
        return Stream.of(
                Arguments.of(new YamlMetadataSectionSerializer()),
                Arguments.of(new BlobsMetadataSectionSerializer())
        );
    }

    @ParameterizedTest
    @MethodSource("serializers")
    public void testSerializeAndDeserialize(MetadataSectionSerializer serializer) {
        MetadataSection metadataSection = ImmutableMetadataSection.builder()
                .metadataFormat("MDF")
                .metadata(new byte[] { 1, 2, 3, 4 })
                .signatureFormat("SGF")
                .signature(new byte[] { 5, 6, 7, 8, 9, 10 })
                .build();

        byte[] resultBytes = serializer.serialize(metadataSection);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        MetadataSection resultMetadataSection = serializer.deserialize(resultBytes);
        assertNotNull(resultMetadataSection);

        assertEquals(metadataSection, resultMetadataSection);
    }

    @ParameterizedTest
    @MethodSource("serializers")
    public void testDeserializationException(MetadataSectionSerializer serializer) {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
