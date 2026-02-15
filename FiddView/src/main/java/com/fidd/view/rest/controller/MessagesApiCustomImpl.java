package com.fidd.view.rest.controller;

import com.fidd.service.FiddContentService;
import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.invoker.ApiResponse;
import com.fidd.view.rest.mapper.FiddFileMetadataMapper;
import com.fidd.view.rest.mapper.LogicalFileInfoMapper;
import com.fidd.view.rest.model.FiddFileMetadata;

import java.util.List;

import com.fidd.view.rest.model.LogicalFileInfo;
import io.vertx.core.Future;

import static com.google.common.base.Preconditions.checkNotNull;

public class MessagesApiCustomImpl implements MessagesApi {
    protected final FiddContentServiceManager fiddContentServiceManager;

    public MessagesApiCustomImpl(FiddContentServiceManager fiddContentServiceManager) {
        this.fiddContentServiceManager = fiddContentServiceManager;
    }

    @Override
    public Future<ApiResponse<FiddFileMetadata>> getFiddFileMetadata(String fiddId, Long messageNumber) {
        FiddContentService service = fiddContentServiceManager.getService(fiddId);
        com.fidd.core.fiddfile.FiddFileMetadata metadata = checkNotNull(service).getFiddFileMetadata(messageNumber);

        com.fidd.view.rest.model.FiddFileMetadata dtoMetadata = FiddFileMetadataMapper.toDto(metadata);
        return Future.succeededFuture(new ApiResponse<>(dtoMetadata));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersBefore(String fiddId, Long messageNumber, Integer count,
                                                                   Boolean inclusive) {
        FiddContentService service = fiddContentServiceManager.getService(fiddId);
        List<Long> list = checkNotNull(service).getMessageNumbersBefore(messageNumber, count, inclusive);
        return Future.succeededFuture(new ApiResponse<>(list));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersBetween(String fiddId, Long latestMessage,
                                                                    Boolean inclusiveLatest, Long earliestMessage,
                                                                    Boolean inclusiveEarliest, Integer count, Boolean getLatest) {
        FiddContentService service = fiddContentServiceManager.getService(fiddId);
        List<Long> list = checkNotNull(service).getMessageNumbersBetween(latestMessage, inclusiveLatest, earliestMessage, inclusiveEarliest, count, getLatest);
        return Future.succeededFuture(new ApiResponse<>(list));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersTail(String fiddId, Integer count) {
        FiddContentService service = fiddContentServiceManager.getService(fiddId);
        List<Long> list = checkNotNull(service).getMessageNumbersTail(count);
        return Future.succeededFuture(new ApiResponse<>(list));
    }

    @Override
    public Future<ApiResponse<List<LogicalFileInfo>>> getLogicalFileInfos(String fiddId, Long messageNumber) {
        FiddContentService service = fiddContentServiceManager.getService(fiddId);
        List<com.fidd.service.LogicalFileInfo> logicalFileInfos = checkNotNull(service).getLogicalFileInfos(messageNumber);

        List<com.fidd.view.rest.model.LogicalFileInfo> dtoLogicalFileInfos =
                LogicalFileInfoMapper.toDtoList(logicalFileInfos == null ? List.of() : logicalFileInfos);
        return Future.succeededFuture(new ApiResponse<>(dtoLogicalFileInfos));
    }
}

