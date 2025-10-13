package com.fidd.core.fiddfile;

import javax.annotation.Nullable;

public interface FiddFileMetadata {
    // TODO: Can't read this from the file itself
    // String fiddFileMetadataFormatVersion();
    String logicalFileMetadataFormatVersion();

    Long messageNumber();
    String postId();
    Integer versionNumber();

    boolean isNewOrSquash();
    boolean isDelete();
    @Nullable Long previousMessageNumber();
}
