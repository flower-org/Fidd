package com.fidd.view.rest.controller;

import com.fidd.service.FiddContentService;
import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.invoker.ApiResponse;
import com.fidd.view.rest.mapper.LogicalFileInfoMapper;
import com.fidd.view.rest.model.FiddFileMetadata;

import java.util.Collections;
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
        // TODO: Replace with real lookup logic.
        return Future.succeededFuture(new ApiResponse<>(204));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersBefore(String fiddId, Long messageNumber, Integer count, Boolean inclusive) {
        return Future.succeededFuture(new ApiResponse<>(Collections.emptyList()));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersBetween(String fiddId, Long latestMessage, Boolean inclusiveLatest, Long earliestMessage, Boolean inclusiveEarliest, Integer count, Boolean getLatest) {
        return Future.succeededFuture(new ApiResponse<>(Collections.emptyList()));
    }

    @Override
    public Future<ApiResponse<List<Long>>> getMessageNumbersTail(String fiddId, Integer count) {
        return Future.succeededFuture(new ApiResponse<>(Collections.emptyList()));
    }

    @Override
    public Future<ApiResponse<List<LogicalFileInfo>>> getLogicalFileInfos(String fiddId, Long messageNumber) {
        FiddContentService service = checkNotNull(fiddContentServiceManager).getContentService(fiddId);
        List<com.fidd.service.LogicalFileInfo> logicalFileInfos = service.getLogicalFileInfos(messageNumber);

        List<com.fidd.view.rest.model.LogicalFileInfo> dtoLogicalFileInfos = LogicalFileInfoMapper.INSTANCE.toDtoList(logicalFileInfos);
        return Future.succeededFuture(new ApiResponse<>(dtoLogicalFileInfos));
    }
}

