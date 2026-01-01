package com.fidd.view.blog.yaml;

import com.fidd.view.blog.FiddBlog;
import com.fidd.view.blog.ImmutableFiddBlog;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlFiddBlogSerializerTest {

    private final YamlFiddBlogSerializer serializer = new YamlFiddBlogSerializer();

    @Test
    void testSerializeAndDeserialize_fullFields() throws Exception {
        FiddBlog original = ImmutableFiddBlog.builder()
                .connectorType("github")
                .blogName("Fidd Blog")
                .blogUrl(new URL("https://example.com"))
                .publicKeyFormat("RSA")
                .publicKeyBytes(new byte[]{1, 2, 3})
                .build();

        byte[] yamlBytes = serializer.serialize(original);
        assertNotNull(yamlBytes);

        FiddBlog restored = serializer.deserialize(yamlBytes);

        assertEquals(original.connectorType(), restored.connectorType());
        assertEquals(original.blogName(), restored.blogName());
        assertEquals(original.blogUrl(), restored.blogUrl());
        assertEquals(original.publicKeyFormat(), restored.publicKeyFormat());
        assertArrayEquals(original.publicKeyBytes(), restored.publicKeyBytes());
    }

    @Test
    void testSerializeAndDeserialize_nullOptionalFields() throws Exception {
        FiddBlog original = ImmutableFiddBlog.builder()
                .connectorType("gitlab")
                .blogName("Null Blog")
                .blogUrl(new URL("https://null.com"))
                .build();

        byte[] yamlBytes = serializer.serialize(original);
        String yamlString = new String(yamlBytes, StandardCharsets.UTF_8);

        assertFalse(yamlString.contains("publicKeyFormat"));
        assertFalse(yamlString.contains("publicKeyBytes"));

        FiddBlog restored = serializer.deserialize(yamlBytes);

        assertNull(restored.publicKeyFormat());
        assertNull(restored.publicKeyBytes());
    }

    @Test
    void testName() {
        assertEquals("YAML", serializer.name());
    }

    @Test
    void testDeserialize_invalidYamlThrowsRuntimeException() {
        byte[] invalid = "not: valid: yaml: :::".getBytes(StandardCharsets.UTF_8);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serializer.deserialize(invalid));

        assertTrue(ex.getMessage().contains("YAML"));
    }

    @Test
    void testSerialize_throwsRuntimeExceptionOnFailure() {
        // Create a proxy object that Jackson cannot serialize
        FiddBlog bad = new FiddBlog() {
            @Override public String blogName() { return "bad"; }
            @Override public String connectorType() { return "bad"; }
            @Override public URL blogUrl() { return null; } // Jackson will choke on null URL
            @Override public String publicKeyFormat() { return null; }
            @Override public byte[] publicKeyBytes() { return null; }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serializer.serialize(bad));

        assertTrue(ex.getMessage().contains("YAML"));
    }
}
