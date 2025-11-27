package com.fidd.core.fiddfile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddFileMetadata.class)
@JsonDeserialize(as = ImmutableFiddFileMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddFileMetadata {
    String logicalFileMetadataFormatVersion();

    Long messageNumber();
    /** Post originally published as msg number */
    Long originalMessageNumber(); //
    @Nullable Long previousMessageNumber();
    String postId();
    Integer versionNumber();

    boolean isNewOrSquash();
    boolean isDelete();

    @Nullable Long messageCreationTime();

    @Nullable String authorsPublicKeyFormat();
    @Nullable byte[] authorsPublicKey();

    @Nullable String authorsFiddFileSignatureFormat();
    @Nullable String authorsFiddKeyFileSignatureFormat();
}
