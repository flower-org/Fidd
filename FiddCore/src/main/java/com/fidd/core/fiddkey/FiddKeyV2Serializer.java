package com.fidd.core.fiddkey;

import com.fidd.core.NamedEntry;

public interface FiddKeyV2Serializer extends NamedEntry {
    byte[] serialize(FiddKeyV2 fiddKeyV2);
    FiddKeyV2 deserialize(byte[] fiddKeyV2Bytes);
}
