package com.fidd.view.blog.yaml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fidd.view.blog.FiddBlog;
import com.fidd.view.blog.FiddBlogSerializer;
import com.fidd.view.blog.ImmutableFiddBlog;

import java.nio.charset.StandardCharsets;

public class YamlFiddBlogSerializer implements FiddBlogSerializer {
    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .registerModule(new GuavaModule());

    @Override
    public byte[] serialize(FiddBlog fiddKey) {
        try {
            return YAML_MAPPER.writeValueAsString(fiddKey).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public FiddBlog deserialize(byte[] fiddKeyBytes) {
        String yamlString = new String(fiddKeyBytes, StandardCharsets.UTF_8);
        try {
            return YAML_MAPPER.readValue(yamlString, ImmutableFiddBlog.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error during YAML deserialization", e);
        }
    }

    @Override
    public String name() {
        return "YAML";
    }
}
