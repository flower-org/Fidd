package com.fidd.core.connection.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.connection.FiddConnectionList;
import com.fidd.core.connection.ImmutableFiddConnectionList;

import java.nio.charset.StandardCharsets;

public class YamlFiddConnectionListSerializer implements FiddConnectionListSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(FiddConnectionList fiddConnectionList) {
        try {
            return YAML_MAPPER.writeValueAsString(fiddConnectionList).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML serialization", e);
        }
    }

    @Override
    public FiddConnectionList deserialize(byte[] fiddConnectionListBytes) {
        String yamlString = new String(fiddConnectionListBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddConnectionList.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
