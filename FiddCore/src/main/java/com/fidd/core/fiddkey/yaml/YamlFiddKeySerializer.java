package com.fidd.core.fiddkey.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.fiddkey.ImmutableFiddKey;

import java.nio.charset.StandardCharsets;

public class YamlFiddKeySerializer implements FiddKeySerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(FiddKey fiddKey) {
        try {
            return YAML_MAPPER.writeValueAsString(fiddKey).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public FiddKey deserialize(byte[] fiddKeyBytes) {
        String yamlString = new String(fiddKeyBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddKey.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
