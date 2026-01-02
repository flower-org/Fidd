package com.fidd.connectors;

import com.fidd.core.NamedEntry;

import java.net.URL;

public interface FiddConnectorFactory extends NamedEntry {
    FiddConnector createConnector(URL source);
}
