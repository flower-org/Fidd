package com.fidd.connectors;

import com.fidd.connectors.folder.FolderFiddConnector;

import java.net.URL;

public class DefaultFiddConnectorFactory implements FiddConnectorFactory {
    @Override
    public FiddConnector createConnector(URL source) {
        if ("file".equalsIgnoreCase(source.getProtocol())) {
            return new FolderFiddConnector(source);
        } else {
            throw new IllegalArgumentException("Can't create connector - Unsupported protocol: " + source.getProtocol() + "; for URL: " + source);
        }
    }

    @Override
    public String name() {
        return "Default";
    }
}
