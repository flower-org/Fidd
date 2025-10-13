package com.fidd.core.logicalfile;

import javax.annotation.Nullable;

public interface LogicalFileMetadata {
    enum FiddUpdateType {
        CREATE_OVERRIDE,
        DELETE
        //TOO: add diffs
    }

    FiddUpdateType updateType();

    /** e.g. "text/css" */
    @Nullable String mimeType();
    /** e.g. "zip" */
    @Nullable String encodingType();

    @Nullable Long createdAt();
    @Nullable Long updatedAt();

    @Nullable String authorsFileSignatureFormat();
    @Nullable byte[] authorsFileSignature();
}
