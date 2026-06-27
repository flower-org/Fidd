package com.fidd.core.info;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableFiddMetadataInfo.class)
@JsonDeserialize(as = ImmutableFiddMetadataInfo.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public interface FiddMetadataInfo {
    @Value.Immutable
    @JsonSerialize(as = ImmutableBlogPostInfo.class)
    @JsonDeserialize(as = ImmutableBlogPostInfo.class)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    interface BlogPostInfo {
        long configOffset();
        int configLength();

        long previewOffset();
        int previewLength();

        long postOffset();
        int postLength();

        long coverOffset();
        int coverLength();
    }

    long offset();
    int length();

    @Nullable BlogPostInfo blogPostInfo();
}
