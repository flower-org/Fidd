package com.fidd.core.fiddkey;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddKey.class)
@JsonDeserialize(as = ImmutableFiddKey.class)
public interface FiddKey {
    @Value.Immutable
    @JsonSerialize(as = ImmutableSection.class)
    @JsonDeserialize(as = ImmutableSection.class)
    interface Section {
        long sectionOffset();
        long sectionLength();

        @Nullable String encryptionAlgorithm();
        /** Since for different algorithms this could be a key of varied length, or key + IV, it's algo-specific */
        @Nullable byte[] encryptionKeyData();

        @Nullable String crcAlgorithm();
        @Nullable byte[] crc();
    }

    Section fiddFileMetadata();
    List<Section> logicalFilesMetadata();
    List<Section> logicalFiles();
}
