package com.fidd.view.rest.controller;

import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.invoker.ApiResponse;
import io.vertx.core.Future;
import io.vertx.ext.web.handler.HttpException;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class DownloadCustomApi implements DownloadApi {
    protected final @Nullable FiddContentServiceManager fiddContentServiceManager;

    public DownloadCustomApi(@Nullable FiddContentServiceManager fiddContentServiceManager) {
        this.fiddContentServiceManager = fiddContentServiceManager;
    }

    @Override
    public Future<ApiResponse<InputStream>> readLogicalFile(String fiddId, Long messageNumber, String logicalFilePath, String range,
                                                            String _list, List<String> filterIn, List<String> filterOut,
                                                            String sort, Boolean includeSubfolders) {
        //FiddContentService service = fiddContentServiceManager.getContentService(fiddId);

        // TODO: Implement actual file streaming behavior.
        try {
            return Future.succeededFuture(new ApiResponse<>(200,
                    new FileInputStream("/home/qpvd1i9k4d/Pictures/Screenshots/Models promise.png")));
        } catch (FileNotFoundException e) {
            return Future.failedFuture(new HttpException(501));
        }

    }
}
