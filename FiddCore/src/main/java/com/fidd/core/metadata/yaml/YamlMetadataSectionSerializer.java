package com.fidd.core.metadata.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.metadata.ImmutableMetadataSection;
import com.fidd.core.metadata.MetadataSection;
import com.fidd.core.metadata.MetadataSectionSerializer;

import java.nio.charset.StandardCharsets;

public class YamlMetadataSectionSerializer implements MetadataSectionSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(MetadataSection metadata) {
        try {
            return YAML_MAPPER.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public MetadataSection deserialize(byte[] metadataBytes) {
        String yamlString = new String(metadataBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableMetadataSection.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() { return "YAML"; }
}
