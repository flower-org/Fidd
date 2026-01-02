package com.fidd.core.connection.yaml;

import com.fidd.core.NamedEntry;
import com.fidd.core.connection.FiddConnectionList;

public interface FiddConnectionListSerializer extends NamedEntry {
    byte[] serialize(FiddConnectionList fiddConnectionList);
    FiddConnectionList deserialize(byte[] fiddConnectionListBytes);
}
