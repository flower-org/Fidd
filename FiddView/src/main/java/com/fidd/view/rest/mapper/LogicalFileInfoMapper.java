package com.fidd.view.rest.mapper;

import com.fidd.view.rest.model.LogicalFileInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface LogicalFileInfoMapper {
    LogicalFileInfoMapper INSTANCE = Mappers.getMapper(LogicalFileInfoMapper.class);

    LogicalFileInfo toDto(com.fidd.service.LogicalFileInfo logicalFileInfo);
    List<LogicalFileInfo> toDtoList(List<com.fidd.service.LogicalFileInfo> logicalFileInfo);
}
