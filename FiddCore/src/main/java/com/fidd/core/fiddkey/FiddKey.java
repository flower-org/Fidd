package com.fidd.core.fiddkey;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.common.FiddSignature;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddKey.class)
@JsonDeserialize(as = ImmutableFiddKey.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface FiddKey {
    @Value.Immutable
    @JsonSerialize(as = ImmutableSection.class)
    @JsonDeserialize(as = ImmutableSection.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Section {
        long sectionOffset();
        long sectionLength();

        @Nullable String encryptionAlgorithm();
        /** Since for different algorithms this could be a key of varied length, or key + IV, it's algo-specific */
        @Nullable byte[] encryptionKeyData();

        @Nullable List<FiddSignature> crcs();

        /** If there's a header in section, its size may be specified (v1.1) */
        @Nullable Integer headerLength();
    }

    Section fiddFileMetadata();
    List<Section> logicalFiles();
}
