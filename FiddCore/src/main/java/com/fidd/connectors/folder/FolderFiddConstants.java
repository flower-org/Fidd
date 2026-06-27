package com.fidd.connectors.folder;

public interface FolderFiddConstants {
    String FIDD_INFO_FILE_NAME = "fidd.info";
    String FIDD_META_FILE_NAME = "fidd.meta";
    String FIDD_KEY_FILE_NAME = "fidd.key";
    String FIDD_MESSAGE_FILE_NAME = "fidd.message";
    String ENCRYPTED_FIDD_KEY_SUBFOLDER = "keys";
    String DEFAULT_FIDD_SIGNATURE_EXT = ".sign";
    String ENCRYPTED_EXT = ".crypt";
    String ENCRYPTED_FIDD_KEY_FILE_EXT = "." + FIDD_KEY_FILE_NAME + ENCRYPTED_EXT;
    String FOLDER_FIDD_CONNECTOR_FACTORY_NAME = "FOLDER";
}
