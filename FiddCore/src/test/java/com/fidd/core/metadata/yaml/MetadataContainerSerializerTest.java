package com.fidd.core.metadata.yaml;

import com.fidd.core.metadata.ImmutableMetadataContainer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.core.metadata.blobs.BlobsMetadataContainerSerializer;
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

public class MetadataContainerSerializerTest {
    static Stream<Arguments> serializers() {
        return Stream.of(
                Arguments.of(new BlobsMetadataContainerSerializer())
        );
    }

    @ParameterizedTest
    @MethodSource("serializers")
    public void testSerializeAndDeserialize(MetadataContainerSerializer serializer) throws NotEnoughBytesException {
        MetadataContainer metadataContainer = ImmutableMetadataContainer.builder()
                .metadataFormat("MDF")
                .metadata(new byte[] { 1, 2, 3, 4 })
                .signatureFormat("SGF")
                .signature(new byte[] { 5, 6, 7, 8, 9, 10 })
                .build();

        byte[] resultBytes = serializer.serialize(metadataContainer);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        MetadataContainerSerializer.MetadataContainerAndLength resultMetadataContainer = serializer.deserialize(resultBytes);
        assertNotNull(resultMetadataContainer);

        assertEquals(metadataContainer, resultMetadataContainer.metadataContainer());
    }

    @ParameterizedTest
    @MethodSource("serializers")
    public void testDeserializationException(MetadataContainerSerializer serializer) {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        assertThrows(Exception.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
