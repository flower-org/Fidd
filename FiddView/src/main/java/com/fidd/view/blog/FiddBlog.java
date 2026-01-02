package com.fidd.view.blog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.net.URL;

import static com.fidd.core.subscription.SubscriberList.Subscriber.compactLines;

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

    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default String getBlogName() { return blogName(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default String getConnectorType() { return connectorType(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default URL getBlogUrl() { return blogUrl(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default @Nullable String getPublicKeyFormat() { return publicKeyFormat(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default @Nullable String getPublicKey() { return publicKeyBytes() == null ? null : compactLines(new String(publicKeyBytes())); }

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
