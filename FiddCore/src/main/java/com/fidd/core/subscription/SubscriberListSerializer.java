package com.fidd.core.subscription;

import com.fidd.core.NamedEntry;

public interface SubscriberListSerializer extends NamedEntry {
    byte[] serialize(SubscriberList subscriberList);
    SubscriberList deserialize(byte[] subscriberListBytes);
}
