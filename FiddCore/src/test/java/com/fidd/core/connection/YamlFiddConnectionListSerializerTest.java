package com.fidd.core.connection;

import com.fidd.core.connection.yaml.YamlFiddConnectionListSerializer;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YamlFiddConnectionListSerializerTest {

    private final YamlFiddConnectionListSerializer serializer =
            new YamlFiddConnectionListSerializer();

    @Test
    void testSerializeProducesYaml() throws Exception {
        FiddConnection conn = FiddConnection.of(
                "rss",
                "MyBlog",
                new URL("https://example.com"),
                null,
                null
        );
        FiddConnectionList list = FiddConnectionList.of(conn);

        byte[] yamlBytes = serializer.serialize(list);
        String yaml = new String(yamlBytes, StandardCharsets.UTF_8);

        assertNotNull(yaml);
        assertTrue(yaml.contains("blogName: \"MyBlog\""));
        assertTrue(yaml.contains("connectorType: \"rss\""));
        assertTrue(yaml.contains("blogUrl: \"https://example.com\""));
    }

    @Test
    void testDeserializeParsesYaml() throws Exception {
        String yaml = ""
                + "fiddConnectionList:\n"
                + "  - blogName: \"MyBlog\"\n"
                + "    connectorType: \"rss\"\n"
                + "    blogUrl: \"https://example.com\"\n";

        FiddConnectionList list = serializer.deserialize(yaml.getBytes(StandardCharsets.UTF_8));

        assertNotNull(list);
        assertEquals(1, list.fiddConnectionList().size());
        assertEquals("MyBlog", list.fiddConnectionList().get(0).blogName());
        assertEquals("rss", list.fiddConnectionList().get(0).connectorType());
        assertEquals(new URL("https://example.com"), list.fiddConnectionList().get(0).blogUrl());
    }

    @Test
    void testRoundTripSerialization() throws Exception {
        FiddConnection conn = FiddConnection.of(
                "rss",
                "RoundTripBlog",
                new URL("https://roundtrip.com"),
                "PEM",
                "abc123".getBytes(StandardCharsets.UTF_8)
        );
        FiddConnectionList original = FiddConnectionList.of(conn);

        byte[] yaml = serializer.serialize(original);
        FiddConnectionList restored = serializer.deserialize(yaml);

        assertEquals(original.fiddConnectionList().size(), restored.fiddConnectionList().size());
        assertEquals(original.fiddConnectionList().get(0).blogName(),
                restored.fiddConnectionList().get(0).blogName());
        assertEquals(original.fiddConnectionList().get(0).connectorType(),
                restored.fiddConnectionList().get(0).connectorType());
        assertEquals(original.fiddConnectionList().get(0).blogUrl(),
                restored.fiddConnectionList().get(0).blogUrl());
        assertEquals("PEM", restored.fiddConnectionList().get(0).publicKeyFormat());
        assertArrayEquals("abc123".getBytes(StandardCharsets.UTF_8),
                restored.fiddConnectionList().get(0).publicKeyBytes());
    }

    @Test
    void testDeserializeInvalidYamlThrows() {
        byte[] invalidYaml = "not: [valid: yaml".getBytes(StandardCharsets.UTF_8);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serializer.deserialize(invalidYaml));

        assertTrue(ex.getMessage().contains("Error during YAML deserialization"));
    }

    @Test
    void testSerializeThrowsOnInvalidObject() {
        // Create a broken list by bypassing builder (edge case)
        FiddConnectionList broken = new FiddConnectionList() {
            @Override
            public List<FiddConnection> fiddConnectionList() {
                throw new RuntimeException("boom");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> serializer.serialize(broken));

        assertTrue(ex.getMessage().contains("Error during YAML serialization"));
    }

    @Test
    void testName() {
        assertEquals("YAML", serializer.name());
    }
}
