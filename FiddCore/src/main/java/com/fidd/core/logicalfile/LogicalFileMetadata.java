package com.fidd.core.logicalfile;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableLogicalFileMetadata.class)
@JsonDeserialize(as = ImmutableLogicalFileMetadata.class)
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

    // We can't replace this with just 1 timestamp, because squash OVERRIDE
    // needs to show both the creation time and last update time.
    @Nullable Long createdAt();
    @Nullable Long updatedAt();

    @Nullable String authorsFileSignatureFormat();
    @Nullable byte[] authorsFileSignature();
}
