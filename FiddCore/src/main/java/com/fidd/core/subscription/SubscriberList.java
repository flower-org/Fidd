package com.fidd.core.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableSubscriberList.class)
@JsonDeserialize(as = ImmutableSubscriberList.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface SubscriberList {
    @Value.Immutable
    @JsonSerialize(as = ImmutableSubscriber.class)
    @JsonDeserialize(as = ImmutableSubscriber.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Subscriber {
        @Nullable String subscriberId();
        String publicKeyFormat();
        byte[] publicKeyBytes();

        static String compactLines(String text) {
            List<String> parts = Splitter.onPattern("\\R")
                    .limit(3)
                    .splitToList(text);
            return parts.size() <= 2 ? text : parts.get(0) + "\n" + parts.get(1) + "...";
        }

        /** JavaFX needs "get" methods for PropertyValueFactory */
        @JsonIgnore default String getSubscriberId() { return StringUtils.defaultIfBlank(subscriberId(), ""); }
        /** JavaFX needs "get" methods for PropertyValueFactory */
        @JsonIgnore default String getPublicKeyFormat() { return publicKeyFormat(); }
        /** JavaFX needs "get" methods for PropertyValueFactory */
        @JsonIgnore default String getPublicKey() { return compactLines(new String(publicKeyBytes())); }

        static Subscriber of(@Nullable String subscriberId, String publicKeyFormat, byte[] publicKeyBytes) {
            ImmutableSubscriber.Builder builder = ImmutableSubscriber.builder();
            if (!StringUtils.isBlank(subscriberId)) { builder.subscriberId(subscriberId); }
            return builder.publicKeyFormat(publicKeyFormat)
                    .publicKeyBytes(publicKeyBytes)
                    .build();
        }
    }

    List<Subscriber> subscriberList();

    static SubscriberList of(Subscriber... subscribers) {
        return ImmutableSubscriberList.builder()
                .addSubscriberList(subscribers)
                .build();
    }
}
