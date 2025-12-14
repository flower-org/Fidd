package com.fidd.core.logicalfile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fidd.core.common.FiddSignature;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableLogicalFileMetadata.class)
@JsonDeserialize(as = ImmutableLogicalFileMetadata.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface LogicalFileMetadata {
    enum FiddUpdateType {
        CREATE_OVERRIDE,
        DELETE
        //TODO: add diffs
    }

    FiddUpdateType updateType();
    String filePath();

    // We can't replace this with just 1 timestamp, because squash OVERRIDE
    // needs to show both the creation time and last update time.
    @Nullable Long createdAt();
    @Nullable Long updatedAt();

    @Nullable List<FiddSignature> authorsFileSignatures();
    @Nullable List<FiddSignature> progressiveCrcs();

    // Optional: Alternative locations to download file from
    @Value.Immutable
    @JsonSerialize(as = ImmutableExternalResource.class)
    @JsonDeserialize(as = ImmutableExternalResource.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface ExternalResource {
        @Value.Immutable
        @JsonSerialize(as = ImmutableFileRegion.class)
        @JsonDeserialize(as = ImmutableFileRegion.class)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        interface FileRegion {
            long offset();
            long length();
            String regionFileName();

            ResourceDescriptorType resourceDescriptorType();
            byte[] resourceDescriptor();
        }

        List<FileRegion> fileRegions();
    }

    @Nullable List<ExternalResource> externalLinks();
}
