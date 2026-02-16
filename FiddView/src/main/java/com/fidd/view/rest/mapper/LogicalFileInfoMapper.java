package com.fidd.view.rest.mapper;

import com.fidd.view.rest.model.FileRegion;
import com.fidd.view.rest.model.LogicalFileInfo;
import com.fidd.view.rest.model.LogicalFileMetadata;
import com.fidd.view.rest.model.FiddKeySection;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

public class LogicalFileInfoMapper {
    private static final ModelMapper MODEL_MAPPER = new ModelMapper();

    static {
        MODEL_MAPPER.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        // Register TypeMaps and attach FluentConverter as post-converter to preserve default mapping
        MODEL_MAPPER.createTypeMap(com.fidd.service.LogicalFileInfo.class, LogicalFileInfo.class);

        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.LogicalFileMetadata.class, LogicalFileMetadata.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.ImmutableLogicalFileMetadata.class, LogicalFileMetadata.class);

        MODEL_MAPPER.createTypeMap(com.fidd.core.fiddkey.FiddKey.Section.class, FiddKeySection.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.fiddkey.ImmutableSection.class, FiddKeySection.class);

        // Nested logical metadata lists
        MODEL_MAPPER.createTypeMap(com.fidd.core.common.FiddSignature.class, com.fidd.view.rest.model.FiddSignature.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.common.ImmutableFiddSignature.class, com.fidd.view.rest.model.FiddSignature.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.common.ProgressiveCrc.class, com.fidd.view.rest.model.ProgressiveCrc.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.common.ImmutableProgressiveCrc.class, com.fidd.view.rest.model.ProgressiveCrc.class);

        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.class, com.fidd.view.rest.model.ExternalResource.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.ImmutableExternalResource.class, com.fidd.view.rest.model.ExternalResource.class);

        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.LogicalFileMetadata.ExternalResource.FileRegion.class, com.fidd.view.rest.model.FileRegion.class);
        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.ImmutableFileRegion.class, com.fidd.view.rest.model.FileRegion.class);

        // No idea why it wouldn't map automatically
        MODEL_MAPPER.createTypeMap(com.fidd.core.logicalfile.ResourceDescriptorType.class, com.fidd.view.rest.model.FileRegion.ResourceDescriptorTypeEnum.class)
                .setPostConverter(ctx ->
                ctx.getSource() != null ? FileRegion.ResourceDescriptorTypeEnum.valueOf(ctx.getSource().name()) : null);

        // No idea why it wouldn't map automatically
        MODEL_MAPPER.createTypeMap(com.fidd.service.ImmutableLogicalFileInfo.class, LogicalFileInfo.class)
                .setPostConverter(ctx -> {
                            if (ctx.getSource() == null) { return null; }
                            com.fidd.service.LogicalFileInfo src = ctx.getSource();
                            LogicalFileInfo dst = new LogicalFileInfo();
                            dst.setFileOffset(src.fileOffset());
                            dst.setMetadata(MODEL_MAPPER.map(src.metadata(), LogicalFileMetadata.class));
                            dst.setSection(MODEL_MAPPER.map(src.section(), FiddKeySection.class));
                            return dst;
                        }
                );
    }

    public static LogicalFileInfo toDto(com.fidd.service.LogicalFileInfo logicalFileInfo) {
        return MODEL_MAPPER.map(logicalFileInfo, LogicalFileInfo.class);
    }

    public static List<LogicalFileInfo> toDtoList(List<com.fidd.service.LogicalFileInfo> logicalFileInfo) {
        return logicalFileInfo.stream().map(LogicalFileInfoMapper::toDto).collect(Collectors.toList());
    }
}
