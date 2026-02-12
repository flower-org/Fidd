package com.fidd.view.rest.controller;

import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class DownloadCustomApiHandler extends DownloadApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCustomApiHandler.class);

    private final DownloadApi api;

    public DownloadCustomApiHandler(DownloadApi api) {
        super(api);
        this.api = api;
    }

    @Deprecated
    public DownloadCustomApiHandler() {
        this(new DownloadApiImpl());
    }

    public void mount(RouterBuilder builder) {
        builder.operation("readLogicalFile").handler(this::readLogicalFile);
    }

    public void readLogicalFile(RoutingContext routingContext) {
        logger.info("readLogicalFile()");

        // Param extraction
        RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

        String fiddId = checkNotNull(requestParameters.pathParameter("fiddId").getString());
        Long messageNumber = checkNotNull(requestParameters.pathParameter("messageNumber").getLong());
        String logicalFilePath = checkNotNull(requestParameters.pathParameter("logicalFilePath").getString());
        String range = requestParameters.headerParameter("Range") != null ? requestParameters.headerParameter("Range").getString() : null;
        String _list = requestParameters.queryParameter("list") != null ? requestParameters.queryParameter("list").getString() : null;
        List<String> filterIn = requestParameters.queryParameter("filterIn") != null ? DatabindCodec.mapper().convertValue(requestParameters.queryParameter("filterIn").get(), new TypeReference<List<String>>(){}) : null;
        List<String> filterOut = requestParameters.queryParameter("filterOut") != null ? DatabindCodec.mapper().convertValue(requestParameters.queryParameter("filterOut").get(), new TypeReference<List<String>>(){}) : null;
        String sort = requestParameters.queryParameter("sort") != null ? requestParameters.queryParameter("sort").getString() : "NUMERICAL_ASC";
        Boolean includeSubfolders = requestParameters.queryParameter("includeSubfolders") != null ? requestParameters.queryParameter("includeSubfolders").getBoolean() : false;

        logger.debug("Parameter fiddId is {}", fiddId);
        logger.debug("Parameter messageNumber is {}", messageNumber);
        logger.debug("Parameter logicalFilePath is {}", logicalFilePath);
        logger.debug("Parameter range is {}", range);
        logger.debug("Parameter _list is {}", _list);
        logger.debug("Parameter filterIn is {}", filterIn);
        logger.debug("Parameter filterOut is {}", filterOut);
        logger.debug("Parameter sort is {}", sort);
        logger.debug("Parameter includeSubfolders is {}", includeSubfolders);

        api.readLogicalFile(fiddId, messageNumber, logicalFilePath, range, _list, filterIn, filterOut, sort, includeSubfolders)
                .onSuccess(apiResponse -> {
                    routingContext.response().setStatusCode(apiResponse.getStatusCode());
                    if (apiResponse.hasData()) {
                        InputStream in = apiResponse.getData();
                        routingContext.response() .putHeader("Content-Type", "application/octet-stream") .setChunked(true);

                        routingContext.vertx().executeBlocking(
                            promise -> {
                                try (in) {
                                    byte[] buffer = new byte[8192];
                                    int read;
                                    while ((read = in.read(buffer)) != -1) {
                                        routingContext.response().write(Buffer.buffer(Arrays.copyOf(buffer, read)));
                                    }
                                    promise.complete();
                                } catch (Exception e) {
                                    promise.fail(e);
                                }
                            }, ar -> {
                                if (ar.succeeded()) {
                                    routingContext.response().end();
                                } else {
                                    routingContext.fail(ar.cause());
                                }
                            }
                        );
                    } else {
                        routingContext.response().end();
                    }
                })
                .onFailure(routingContext::fail);
    }
}
