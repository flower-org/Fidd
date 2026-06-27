package com.fidd.core.info.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.core.info.ImmutableFiddMetadataInfo;
import com.fidd.core.info.FiddMetadataInfo;
import com.fidd.core.info.MetadataSectionInfoSerializer;

import java.nio.charset.StandardCharsets;

public class YamlMetadataSectionInfoSerializer implements MetadataSectionInfoSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(FiddMetadataInfo info) {
        try {
            return YAML_MAPPER.writeValueAsString(info).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML serialization", e);
        }
    }

    @Override
    public FiddMetadataInfo deserialize(byte[] bytes) {
        String yamlString = new String(bytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddMetadataInfo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
