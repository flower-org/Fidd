package com.fidd.core.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.common.Base36;
import org.immutables.value.Value;

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
        String publicKeyFormat();
        byte[] publicKeyBytes();

        @JsonIgnore
        default String getPublicKeyFormat() {
            return publicKeyFormat();
        }
        @JsonIgnore
        default String getPublicKeyBase36() {
            return Base36.toBase36(publicKeyBytes());
        }

        static Subscriber of(String publicKeyFormat, byte[] publicKeyBytes) {
            return ImmutableSubscriber.builder()
                    .publicKeyFormat(publicKeyFormat)
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
