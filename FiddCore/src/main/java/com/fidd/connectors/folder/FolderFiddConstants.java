package com.fidd.connectors.folder;

public interface FolderFiddConstants {
    String KEY_FILE_NAME = "fidd.key";
    String MESSAGE_FILE_NAME = "fidd.message";
    String ENCRYPTED_KEY_SUBFOLDER = "keys";
    String DEFAULT_FIDD_SIGNATURE_EXT = ".sign";
    String ENCRYPTED_EXT = ".crypt";
    String ENCRYPTED_KEY_FILE_EXT = "." + KEY_FILE_NAME + ENCRYPTED_EXT;

    String FIDD_FILE_NAME_PREFIX = MESSAGE_FILE_NAME + ".";
    String FIDD_KEY_FILE_NAME_PREFIX = KEY_FILE_NAME + ".";
}
