package com.fidd.view.rest.controller;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public static @Nullable List<String> getRequestParametersList(RequestParameters requestParameters, String name) {
        RequestParameter prm = requestParameters.queryParameter(name);
        if (prm == null || prm.isEmpty() || prm.isNull()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        if (prm.isBoolean()) {
            list.add(prm.getBoolean().toString());
        } else if (prm.isNumber()) {
            list.add(prm.getDouble().toString());
        } else if (prm.isString()) {
            list.add(prm.getString());
        } else if (prm.isJsonArray()) {
            JsonArray arr = prm.getJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                list.add(arr.getString(i));
            }
        } else if (prm.get() instanceof List<?>) {
            ((List)prm.get()).forEach(elm -> list.add(elm.toString()));
        } else {
            throw new IllegalArgumentException("Unsupported parameter type");
        }

        List<String> decodedList = new ArrayList<>();
        list.forEach(elm -> decodedList.add(URLDecoder.decode(elm, StandardCharsets.UTF_8)));
        return decodedList;
    }

    public static @Nullable String getFirstRequestParameter(RequestParameters requestParameters, String name, @Nullable String defaultValue) {
        List<String> list = getRequestParametersList(requestParameters, name);
        if (list == null || list.isEmpty()) {
            return defaultValue;
        }
        return list.get(0);
    }

    public void readLogicalFile(RoutingContext routingContext) {
        // Param extraction
        RequestParameters requestParameters = routingContext.get(ValidationHandler.REQUEST_CONTEXT_KEY);

        String fiddId = URLDecoder.decode(checkNotNull(requestParameters.pathParameter("fiddId").getString()), StandardCharsets.UTF_8);
        Long messageNumber = checkNotNull(requestParameters.pathParameter("messageNumber").getLong());
        String logicalFilePath = URLDecoder.decode(checkNotNull(requestParameters.pathParameter("logicalFilePath").getString()), StandardCharsets.UTF_8);

        String range = requestParameters.headerParameter("Range") != null
                ? URLDecoder.decode(requestParameters.headerParameter("Range").getString(), StandardCharsets.UTF_8)
                : null;

        String _list = getFirstRequestParameter(requestParameters, "list", null);
        List<String> filterIn = getRequestParametersList(requestParameters, "filterIn");
        List<String> filterOut = getRequestParametersList(requestParameters, "filterOut");
        String sort = getFirstRequestParameter(requestParameters, "sort", "NUMERICAL_ASC");
        Boolean includeSubfolders = Boolean.parseBoolean(getFirstRequestParameter(requestParameters, "includeSubfolders", "false"));

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
