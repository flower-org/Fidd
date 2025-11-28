package com.fidd.core.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.common.FiddSignature;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadataContainer.class)
@JsonDeserialize(as = ImmutableMetadataContainer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface MetadataContainer {
    String metadataFormat();
    byte[] metadata();

    @Nullable List<FiddSignature> signatures();
}
