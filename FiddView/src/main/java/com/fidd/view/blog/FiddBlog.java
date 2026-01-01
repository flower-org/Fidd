package com.fidd.view.blog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.net.URL;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddBlog.class)
@JsonDeserialize(as = ImmutableFiddBlog.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddBlog {
    String blogName();
    String connectorType();
    URL blogUrl();

    static FiddBlog of(String connectorType, String blogName, URL blogUrl) {
        return ImmutableFiddBlog.builder()
                .connectorType(connectorType)
                .blogName(blogName)
                .blogUrl(blogUrl)
                .build();
    }
}
