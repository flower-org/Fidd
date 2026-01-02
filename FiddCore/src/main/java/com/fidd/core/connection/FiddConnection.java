package com.fidd.core.connection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.net.URL;

import static com.fidd.core.subscription.SubscriberList.Subscriber.compactLines;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddConnection.class)
@JsonDeserialize(as = ImmutableFiddConnection.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddConnection {
    String name();
    String connectorType();
    URL url();
    @Nullable String publicKeyFormat();
    @Nullable byte[] publicKeyBytes();

    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default String getName() { return name(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default String getConnectorType() { return connectorType(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default URL getUrl() { return url(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default @Nullable String getPublicKeyFormat() { return publicKeyFormat(); }
    /** JavaFX needs "get" methods for PropertyValueFactory */
    @JsonIgnore default @Nullable String getPublicKey() { return publicKeyBytes() == null ? null : compactLines(new String(publicKeyBytes())); }

    static FiddConnection of(String connectorType, String name, URL url,
                             @Nullable String publicKeyFormat, @Nullable byte[] publicKeyBytes
    ) {
        ImmutableFiddConnection.Builder builder = ImmutableFiddConnection.builder()
                .connectorType(connectorType)
                .name(name)
                .url(url);

        if (publicKeyFormat != null) { builder.publicKeyFormat(publicKeyFormat); }
        if (publicKeyBytes != null) { builder.publicKeyBytes(publicKeyBytes); }

        return builder.build();
    }
}
