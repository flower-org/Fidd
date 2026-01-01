package com.fidd.view.blog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.net.URL;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddBlog.class)
@JsonDeserialize(as = ImmutableFiddBlog.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddBlog {
    String blogName();
    String connectorType();
    URL blogUrl();
    @Nullable String publicKeyFormat();
    @Nullable byte[] publicKeyBytes();

    static FiddBlog of(String connectorType, String blogName, URL blogUrl,
                       @Nullable String publicKeyFormat, @Nullable byte[] publicKeyBytes
    ) {
        ImmutableFiddBlog.Builder builder = ImmutableFiddBlog.builder()
                .connectorType(connectorType)
                .blogName(blogName)
                .blogUrl(blogUrl);

        if (publicKeyFormat != null) { builder.publicKeyFormat(publicKeyFormat); }
        if (publicKeyBytes != null) { builder.publicKeyBytes(publicKeyBytes); }

        return builder.build();
    }
}
