package com.fidd.core.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddSignature.class)
@JsonDeserialize(as = ImmutableFiddSignature.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddSignature {
    String format();
    byte[] bytes();

    static FiddSignature of(String format, byte[] bytes) {
        return ImmutableFiddSignature.builder().format(format).bytes(bytes).build();
    }
}
