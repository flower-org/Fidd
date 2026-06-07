package com.fidd.core.info;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMetadataSectionInfo.class)
@JsonDeserialize(as = ImmutableMetadataSectionInfo.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public interface MetadataSectionInfo {
    long offset();
    int length();
}
