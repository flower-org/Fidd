package com.fidd.core.fiddfile.yaml;

import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.ImmutableFiddFileMetadata;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlFiddFileMetadataSerializerTest {
    @Test
    public void testSerializeAndDeserialize() {
        FiddFileMetadata fiddFileMetadata = ImmutableFiddFileMetadata.builder()
                .logicalFileMetadataFormatVersion("FMT")
                .messageNumber(123L)
                .originalMessageNumber(123L)
                .postId("Post123")
                .versionNumber(2)
                .isNewOrSquash(true)
                .isDelete(false)
                .previousMessageNumber(122L)
                .authorsPublicKeyFormat("FMT2")
                .authorsPublicKey(new byte[] {1,2,3,4,5})
                .authorsFiddFileMetadataSignatureFormat("FMT3")
                .build();

        YamlFiddFileMetadataSerializer serializer = new YamlFiddFileMetadataSerializer();
        byte[] resultBytes = serializer.serialize(fiddFileMetadata);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        FiddFileMetadata resultFiddFileMetadata = serializer.deserialize(resultBytes);
        assertNotNull(resultFiddFileMetadata);

        assertEquals(fiddFileMetadata, resultFiddFileMetadata);
    }

    @Test
    public void testDeserializationException() {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        YamlFiddFileMetadataSerializer serializer = new YamlFiddFileMetadataSerializer();
        assertThrows(RuntimeException.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
