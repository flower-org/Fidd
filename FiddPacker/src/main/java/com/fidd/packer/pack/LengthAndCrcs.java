package com.fidd.packer.pack;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableLengthAndCrcs.class)
@JsonDeserialize(as = ImmutableLengthAndCrcs.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface LengthAndCrcs {
    long length();
    @Nullable List<byte[]> crcs();
    @Nullable Integer headerLength();

    static LengthAndCrcs of(long length, @Nullable List<byte[]> crcs) {
        ImmutableLengthAndCrcs.Builder lengthAndCrcBuilder = ImmutableLengthAndCrcs.builder()
                .length(length);
        if (crcs != null) { lengthAndCrcBuilder.crcs(crcs); }
        return lengthAndCrcBuilder.build();
    }

    static LengthAndCrcs of(long length, @Nullable List<byte[]> crcs, @Nullable Integer headerLength) {
        ImmutableLengthAndCrcs.Builder lengthAndCrcBuilder = ImmutableLengthAndCrcs.builder()
                .length(length)
                .headerLength(headerLength);
        if (crcs != null) { lengthAndCrcBuilder.crcs(crcs); }
        return lengthAndCrcBuilder.build();
    }
}
