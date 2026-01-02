package com.fidd.core.connection;

import com.fidd.core.connection.yaml.YamlFiddConnectionSerializer;
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

public class YamlFiddConnectionSerializerTest {

    private final YamlFiddConnectionSerializer serializer = new YamlFiddConnectionSerializer();

    @Test
    void testSerializeAndDeserialize_fullFields() throws Exception {
        FiddConnection original = ImmutableFiddConnection.builder()
                .connectorType("github")
                .name("Fidd Blog")
                .url(new URL("https://example.com"))
                .publicKeyFormat("RSA")
                .publicKeyBytes(new byte[]{1, 2, 3})
                .build();

        byte[] yamlBytes = serializer.serialize(original);
        assertNotNull(yamlBytes);

        FiddConnection restored = serializer.deserialize(yamlBytes);

        assertEquals(original.connectorType(), restored.connectorType());
        assertEquals(original.name(), restored.name());
        assertEquals(original.url(), restored.url());
        assertEquals(original.publicKeyFormat(), restored.publicKeyFormat());
        assertArrayEquals(original.publicKeyBytes(), restored.publicKeyBytes());
    }

    @Test
    void testSerializeAndDeserialize_nullOptionalFields() throws Exception {
        FiddConnection original = ImmutableFiddConnection.builder()
                .connectorType("gitlab")
                .name("Null Blog")
                .url(new URL("https://null.com"))
                .build();

        byte[] yamlBytes = serializer.serialize(original);
        String yamlString = new String(yamlBytes, StandardCharsets.UTF_8);

        assertFalse(yamlString.contains("publicKeyFormat"));
        assertFalse(yamlString.contains("publicKeyBytes"));

        FiddConnection restored = serializer.deserialize(yamlBytes);

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
        FiddConnection bad = new FiddConnection() {
            @Override public String name() { return "bad"; }
            @Override public String connectorType() { return "bad"; }
            @Override public URL url() { return null; } // Jackson will choke on null URL
            @Override public String publicKeyFormat() { return null; }
            @Override public byte[] publicKeyBytes() { return null; }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serializer.serialize(bad));

        assertTrue(ex.getMessage().contains("YAML"));
    }
}
