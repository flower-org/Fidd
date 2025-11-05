package com.fidd.core.fiddkey;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        @JsonProperty
        long sectionOffset();
        @JsonProperty
        long sectionLength();

        @JsonProperty
        @Nullable String encryptionAlgorithm();
        /** Since for different algorithms this could be a key of varied length, or key + IV, it's algo-specific */
        @JsonProperty
        @Nullable byte[] encryptionKeyData();

        @JsonProperty
        @Nullable String crcAlgorithm();
        @JsonProperty
        @Nullable byte[] crc();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableLogicalFileSection.class)
    @JsonDeserialize(as = ImmutableLogicalFileSection.class)
    interface LogicalFileSection extends Section {
        @JsonProperty
        String filePath();
    }

    @JsonProperty
    Section fiddFileMetadata();
    @JsonProperty
    List<LogicalFileSection> logicalFiles();

    // Author's signature of the whole Fidd File (package integrity)
    @JsonProperty
    @Nullable String authorsFiddFileSignatureFormat();
    @JsonProperty
    @Nullable byte[] authorsFiddFileSignature();
}
