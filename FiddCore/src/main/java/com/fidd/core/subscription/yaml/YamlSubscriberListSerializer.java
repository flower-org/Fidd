package com.fidd.core.subscription.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.subscription.ImmutableSubscriberList;
import com.fidd.core.subscription.SubscriberList;
import com.fidd.core.subscription.SubscriberListSerializer;

import java.nio.charset.StandardCharsets;

public class YamlSubscriberListSerializer implements SubscriberListSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(SubscriberList subscriberList) {
        try {
            return YAML_MAPPER.writeValueAsString(subscriberList).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML serialization", e);
        }
    }

    @Override
    public SubscriberList deserialize(byte[] subscriberListBytes) {
        String yamlString = new String(subscriberListBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableSubscriberList.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
