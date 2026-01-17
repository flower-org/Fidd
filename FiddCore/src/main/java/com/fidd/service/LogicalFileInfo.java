package com.fidd.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableLogicalFileInfo.class)
@JsonDeserialize(as = ImmutableLogicalFileInfo.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface LogicalFileInfo {
    LogicalFileMetadata metadata();
    FiddKey.Section section();
    long fileOffset();

    static LogicalFileInfo of(LogicalFileMetadata metadata, FiddKey.Section section, long fileOffset) {
        return ImmutableLogicalFileInfo.builder()
                .metadata(metadata)
                .section(section)
                .fileOffset(fileOffset)
                .build();
    }
}
