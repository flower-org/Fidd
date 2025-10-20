package com.fidd.core.fiddfile;

import javax.annotation.Nullable;

public interface FiddFileMetadata {
    String logicalFileMetadataFormatVersion();

    Long messageNumber();
    String postId();
    Integer versionNumber();

    boolean isNewOrSquash();
    boolean isDelete();
    @Nullable Long previousMessageNumber();

    @Nullable String authorsPublicKeyFormat();
    @Nullable byte[] authorsPublicKey();

    // Format of FiddFileMetadata Signature section
    @Nullable String authorsFiddFileMetadataSignatureFormat();
}
