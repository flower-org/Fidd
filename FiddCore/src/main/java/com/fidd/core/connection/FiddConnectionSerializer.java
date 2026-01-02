package com.fidd.core.connection;

import com.fidd.core.NamedEntry;

public interface FiddConnectionSerializer extends NamedEntry {
    byte[] serialize(FiddConnection fiddKey);
    FiddConnection deserialize(byte[] fiddKeyBytes);
}
