package com.fidd.core.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadataContainer.class)
@JsonDeserialize(as = ImmutableMetadataContainer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface MetadataContainer {
    String metadataFormat();
    byte[] metadata();

    @Nullable String signatureFormat();
    @Nullable byte[] signature();
}
