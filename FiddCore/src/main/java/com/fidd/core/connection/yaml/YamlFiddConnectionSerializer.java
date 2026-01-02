package com.fidd.core.connection.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.connection.FiddConnection;
import com.fidd.core.connection.FiddConnectionSerializer;
import com.fidd.core.connection.ImmutableFiddConnection;

import java.nio.charset.StandardCharsets;

public class YamlFiddConnectionSerializer implements FiddConnectionSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(FiddConnection fiddKey) {
        try {
            return YAML_MAPPER.writeValueAsString(fiddKey).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public FiddConnection deserialize(byte[] fiddKeyBytes) {
        String yamlString = new String(fiddKeyBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddConnection.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
