package com.fidd.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableProgressiveCrc.class)
@JsonDeserialize(as = ImmutableProgressiveCrc.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface ProgressiveCrc extends FiddSignature {
    long progressiveCrcChunkSize();

    static ProgressiveCrc of(String format, byte[] bytes, long progressiveCrcChunkSize) {
        return ImmutableProgressiveCrc.builder().format(format).bytes(bytes).progressiveCrcChunkSize(progressiveCrcChunkSize).build();
    }
}
