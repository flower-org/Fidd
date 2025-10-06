package com.fidd.core.fiddfile;

import javax.annotation.Nullable;

public interface FiddFileMetadata {
    String fiddFileMetadataFormatVersion();
    String logicalFileMetadataFormatVersion();

    Long messageNumber();
    String postId();
    Integer versionNumber();

    boolean isNewOrSquash();
    boolean isDelete();
    @Nullable Long previousMessageNumber();
}
