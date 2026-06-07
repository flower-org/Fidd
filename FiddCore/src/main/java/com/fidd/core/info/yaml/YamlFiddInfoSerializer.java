package com.fidd.core.info.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fidd.core.info.FiddInfo;
import com.fidd.core.info.FiddInfoSerializer;
import com.fidd.core.info.ImmutableFiddInfo;

import java.nio.charset.StandardCharsets;

public class YamlFiddInfoSerializer implements FiddInfoSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule())
            .registerModule(new JavaTimeModule());

    @Override
    public byte[] serialize(FiddInfo info) {
        try {
            return YAML_MAPPER.writeValueAsString(info).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML serialization", e);
        }
    }

    @Override
    public FiddInfo deserialize(byte[] bytes) {
        String yamlString = new String(bytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
