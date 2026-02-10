package com.fidd.view.rest.controller;

import com.fidd.view.rest.invoker.ApiResponse;
import com.fidd.view.rest.model.FiddFileMetadata;

import java.util.Collections;
import java.util.List;
import io.vertx.core.Future;

public class MessagesApiCustomImpl implements MessagesApi {
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
}

