package com.fidd.core.fiddkey;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddKeyV2.class)
@JsonDeserialize(as = ImmutableFiddKeyV2.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddKeyV2 {
    Section fiddFileMetadata();
    List<Section> logicalFileMetadatas();
    List<Section> logicalFiles();
}
