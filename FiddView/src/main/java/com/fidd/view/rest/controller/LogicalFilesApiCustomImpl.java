package com.fidd.view.rest.controller;

import com.fidd.view.rest.invoker.ApiResponse;
import com.fidd.view.rest.model.LogicalFileInfo;
import io.vertx.core.Future;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.handler.HttpException;

import java.util.Collections;
import java.util.List;

public class LogicalFilesApiCustomImpl implements LogicalFilesApi {
    @Override
    public Future<ApiResponse<List<LogicalFileInfo>>> getLogicalFileInfos(String fiddId, Long messageNumber) {
        // TODO: Replace with real lookup logic.
        return Future.succeededFuture(new ApiResponse<>(Collections.emptyList()));
    }

    @Override
    public Future<ApiResponse<FileUpload>> readLogicalFile(String fiddId, Long messageNumber, String logicalFilePath, String range, String _list, List<String> filterIn, List<String> filterOut, String sort, Boolean includeSubfolders) {
        // TODO: Implement actual file streaming behavior.
        return Future.failedFuture(new HttpException(501));
    }
}

