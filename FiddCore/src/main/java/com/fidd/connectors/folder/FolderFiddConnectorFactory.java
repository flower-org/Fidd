package com.fidd.connectors.folder;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.FiddConnectorFactory;

import java.io.File;
import java.net.URL;

import static com.fidd.connectors.folder.FolderFiddConstants.FOLDER_FIDD_CONNECTOR_FACTORY_NAME;

public class FolderFiddConnectorFactory implements FiddConnectorFactory {
    @Override
    public FiddConnector createConnector(URL source) {
        try {
            File folder = new File(source.toURI());
            return new FolderFiddConnector(folder.toPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return FOLDER_FIDD_CONNECTOR_FACTORY_NAME;
    }
}
