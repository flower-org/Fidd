package com.fidd.core.fiddkey.yaml;

import com.fidd.core.common.FiddSignature;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.ImmutableFiddKey;
import com.fidd.core.fiddkey.ImmutableSection;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class YamlFiddKeySerializerTest {
    @Test
    public void testSerializeAndDeserialize() {
        FiddKey.Section fiddFileMetadata = ImmutableSection.builder()
                .sectionOffset(100)
                .sectionLength(1000)
                .encryptionAlgorithm("AES-256")
                .encryptionKeyData(new byte[] { 1,2,3,4,5 })
                .crcs(List.of(FiddSignature.of("Adler32", new byte[] { 6,7,8,9,10 })))
                .build();

        List<FiddKey.Section> logicalFiles = List.of(ImmutableSection.builder()
                .sectionOffset(100)
                .sectionLength(1000)
                .encryptionAlgorithm("AES-256")
                .encryptionKeyData(new byte[] { 1,2,3,4,5 })
                .crcs(List.of(FiddSignature.of("Adler32", new byte[] { 6,7,8,9,10 })))
                .build()
        );

        FiddKey fiddKey = ImmutableFiddKey.builder()
                .fiddFileMetadata(fiddFileMetadata)
                .logicalFiles(logicalFiles)
                .build();

        YamlFiddKeySerializer serializer = new YamlFiddKeySerializer();
        byte[] resultBytes = serializer.serialize(fiddKey);

        String yamlString = new String(resultBytes, StandardCharsets.UTF_8);
        System.out.println(yamlString);
        assertNotNull(resultBytes);
        assertFalse(StringUtils.isBlank(yamlString));

        FiddKey resultKey = serializer.deserialize(resultBytes);
        assertNotNull(resultKey);

        assertEquals(fiddKey, resultKey);
    }

    @Test
    public void testDeserializationException() {
        byte[] invalidBytes = "invalid".getBytes(StandardCharsets.UTF_8);

        YamlFiddKeySerializer serializer = new YamlFiddKeySerializer();
        assertThrows(RuntimeException.class, () -> {
            serializer.deserialize(invalidBytes);
        });
    }
}
