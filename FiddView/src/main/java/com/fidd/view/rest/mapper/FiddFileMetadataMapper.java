package com.fidd.view.rest.mapper;

import com.fidd.view.rest.model.FiddFileMetadata;
import org.modelmapper.ModelMapper;

import java.util.*;
import java.util.stream.Collectors;

public final class FiddFileMetadataMapper {
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    static {
        MODEL_MAPPER.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        MODEL_MAPPER.createTypeMap(com.fidd.core.fiddfile.FiddFileMetadata.class, FiddFileMetadata.class);
    }

    public static FiddFileMetadata toDto(com.fidd.core.fiddfile.FiddFileMetadata metadata) {
        return MODEL_MAPPER.map(metadata, FiddFileMetadata.class);
    }

    public static List<FiddFileMetadata> toDtoList(List<com.fidd.core.fiddfile.FiddFileMetadata> metadata) {
        return metadata.stream().map(FiddFileMetadataMapper::toDto).collect(Collectors.toList());
    }
}
