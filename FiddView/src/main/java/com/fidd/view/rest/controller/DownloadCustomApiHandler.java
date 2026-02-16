package com.fidd.view.rest.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.HttpHeaders.ACCEPT_RANGES;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_RANGE;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

public class DownloadCustomApiHandler extends DownloadApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(DownloadCustomApiHandler.class);

    protected static void addHeader(MultiMap headers, String header, @Nullable String value) {
        if (value == null) { return; }
        headers.add(header, value);
    }

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

        api.readLogicalFile(fiddId, messageNumber, logicalFilePath, range, _list, filterIn, filterOut, sort, includeSubfolders)
                .onSuccess(apiResponse -> {
                    routingContext.response().setStatusCode(apiResponse.getStatusCode());
                    if (apiResponse.hasData()) {
                        FileInfo fileInfo = apiResponse.getData();
                        addHeader(routingContext.response().headers(), ACCEPT_RANGES, fileInfo.getAcceptRanges());
                        addHeader(routingContext.response().headers(), CONTENT_LENGTH, fileInfo.getContentLength());
                        addHeader(routingContext.response().headers(), CONTENT_DISPOSITION, fileInfo.getContentDisposition());
                        addHeader(routingContext.response().headers(), CONTENT_RANGE, fileInfo.getContentRange());
                        addHeader(routingContext.response().headers(), CONTENT_TYPE, fileInfo.getContentType());

                        routingContext.vertx().executeBlocking(
                            promise -> {
                                routingContext.response().setChunked(true);

                                InputStream in = fileInfo.getInputStream();
                                if (in != null) {
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
                                } else {
                                    promise.complete();
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
                .onFailure(exc -> routingContext.fail(exc));
    }
}
