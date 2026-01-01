package com.fidd.core.subscription.yaml;

import com.fidd.core.subscription.SubscriberList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YamlSubscriberListSerializerTest {
    private YamlSubscriberListSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new YamlSubscriberListSerializer();
    }

    @Test
    void testSerializeEmptySubscriberList() {
        SubscriberList empty = SubscriberList.of();
        byte[] result = serializer.serialize(empty);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testDeserializeEmptySubscriberList() {
        SubscriberList empty = SubscriberList.of();
        byte[] serialized = serializer.serialize(empty);
        SubscriberList deserialized = serializer.deserialize(serialized);

        assertNotNull(deserialized);
        assertEquals(0, deserialized.subscriberList().size());
    }

    @Test
    void testRoundTripSerialization() {
        SubscriberList.Subscriber subscriber1 = SubscriberList.Subscriber.of("Sub1", "X509", new byte[] {1,2,3,4,5,6});
        SubscriberList.Subscriber subscriber2 = SubscriberList.Subscriber.of("Sub2", "PEM", new byte[] {7,8,9,10,11,12});
        SubscriberList original = SubscriberList.of(subscriber1, subscriber2);

        byte[] serialized = serializer.serialize(original);
        SubscriberList deserialized = serializer.deserialize(serialized);

        assertEquals(original.subscriberList().size(), deserialized.subscriberList().size());
        assertEquals(original, deserialized);
    }

    @Test
    void testSerializeReturnsValidUtf8() {
        SubscriberList list = SubscriberList.of();
        byte[] serialized = serializer.serialize(list);
        String yaml = new String(serialized, StandardCharsets.UTF_8);

        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());
    }

    @Test
    void testDeserializeInvalidYamlThrowsException() {
        byte[] invalidYaml = "invalid: [yaml: content".getBytes(StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> serializer.deserialize(invalidYaml));
    }

    @Test
    void testName() {
        assertEquals("YAML", serializer.name());
    }
}