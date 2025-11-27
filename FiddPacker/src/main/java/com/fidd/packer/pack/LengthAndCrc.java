package com.fidd.packer.pack;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableLengthAndCrc.class)
@JsonDeserialize(as = ImmutableLengthAndCrc.class)
public interface LengthAndCrc {
    long length();
    @Nullable byte[] crc();

    static LengthAndCrc of(long length, @Nullable byte[] crc) {
        ImmutableLengthAndCrc.Builder lengthAndCrcBuilder = ImmutableLengthAndCrc.builder()
                .length(length);
        if (crc != null) { lengthAndCrcBuilder.crc(crc); }
        return lengthAndCrcBuilder.build();
    }
}
