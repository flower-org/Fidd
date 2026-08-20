package com.fidd.view.rest.invoker;

import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.controller.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FiddHttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(FiddHttpServerVerticle.class);
    private final String specFile;
    private static final int NETTY_FILE_SERVER_PORT = 4198;
    private static final boolean NETTY_FILE_SERVER_SSL = System.getProperty("ssl") != null;
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final MessagesApiHandler messagesHandler;
    private final int fiddApiServerPort;

    protected final FiddContentServiceManager fiddContentServiceManager;

    public FiddHttpServerVerticle(String specFile, FiddContentServiceManager fiddContentServiceManager, int fiddApiServerPort) {
        this.specFile = specFile;
        this.fiddContentServiceManager = fiddContentServiceManager;
        this.fiddApiServerPort = fiddApiServerPort;

        messagesHandler = new MessagesApiHandler(new MessagesApiCustomImpl(fiddContentServiceManager));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        HttpClientOptions fileServerClientOptions = new HttpClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(NETTY_FILE_SERVER_PORT)
                .setSsl(NETTY_FILE_SERVER_SSL);
        if (NETTY_FILE_SERVER_SSL) {
            fileServerClientOptions.setTrustAll(true).setVerifyHost(false);
        }
        HttpClient httpClient = vertx.createHttpClient(fileServerClientOptions);

        RouterBuilder.create(vertx, specFile)
                .map(builder -> {
                    builder.setOptions(new RouterBuilderOptions()
                            .setRequireSecurityHandlers(false));

                    messagesHandler.mount(builder);

                    Router router = builder.createRouter();
                    router.errorHandler(400, this::validationFailureHandler);

                    StaticHandler spaRootHandler = StaticHandler.create("spa")
                            .setIndexPage("index.html");

                    router.route("/fidds/*").handler(ctx -> {
                        HttpServerRequest req = ctx.request();
                        httpClient.request(req.method(), req.uri())
                                .compose(clientReq -> {
                                    MultiMap forwardedHeaders = filterEndToEndHeaders(req.headers());
                                    forwardedHeaders.forEach(e -> clientReq.headers().add(e.getKey(), e.getValue()));

                                    if (HttpMethod.GET.equals(req.method()) || HttpMethod.HEAD.equals(req.method())) {
                                        return clientReq.send();
                                    }
                                    return clientReq.send(req);
                                })
                                .onSuccess(clientResp -> {
                                    HttpServerResponse resp = ctx.response();
                                    resp.setStatusCode(clientResp.statusCode());
                                    MultiMap proxiedHeaders = filterEndToEndHeaders(clientResp.headers());
                                    proxiedHeaders.forEach(e -> resp.headers().add(e.getKey(), e.getValue()));
                                    clientResp.pipeTo(resp);
                                })
                                .onFailure(ctx::fail);
                    });

            //Статика spa
            router.route("/assets/*").handler(StaticHandler.create("spa/assets"));
            router.route("/vite.svg").handler(StaticHandler.create("spa"));
            router.route("/favicon.ico").handler(StaticHandler.create("spa"));

            //нормализация index пути и корень
            router.get("/index.html/").handler(ctx ->
                ctx.response()
                    .setStatusCode(301)
                    .putHeader("Location", "/")
                    .end()
            );
            router.get("/index.html").handler(ctx -> ctx.reroute("/"));
            router.get("/").handler(spaRootHandler);

                    //fallback для react router (только для клиентских путей)
                    router.get("/*").handler(ctx -> {
                        String path = ctx.normalizedPath();

                        if (path.startsWith("/fidds/")
                                || path.startsWith("/api/")
                                || path.startsWith("/instances")
                                || path.contains(".")) {
                            ctx.next();
                            return;
                        }

                        ctx.reroute("/");
                    });

                    return router;
                })
                .compose(router ->
                        vertx.createHttpServer()
                                .requestHandler(router)
                                .listen(fiddApiServerPort)
                )
                .onSuccess(server -> logger.info("Http verticle deploy successful"))
                .onFailure(t -> logger.error("Http verticle failed to deploy", t))
                .<Void>mapEmpty()
                .onComplete(startPromise);
    }


    private void validationFailureHandler(RoutingContext rc) {
         rc.response().setStatusCode(400)
                 .end("Bad Request : " + rc.failure().getMessage());
    }

    private static MultiMap filterEndToEndHeaders(MultiMap headers) {
        MultiMap filteredHeaders = MultiMap.caseInsensitiveMultiMap();
        Set<String> connectionScopedHeaders = extractConnectionScopedHeaders(headers);

        headers.forEach(entry -> {
            String headerName = entry.getKey();
            String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(normalizedHeaderName)
                    || connectionScopedHeaders.contains(normalizedHeaderName)) {
                return;
            }
            filteredHeaders.add(headerName, entry.getValue());
        });

        return filteredHeaders;
    }

    private static Set<String> extractConnectionScopedHeaders(MultiMap headers) {
        Set<String> connectionScopedHeaders = new HashSet<>();
        for (String connectionHeaderValue : headers.getAll("Connection")) {
            Arrays.stream(connectionHeaderValue.split(","))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(token -> token.toLowerCase(Locale.ROOT))
                    .forEach(connectionScopedHeaders::add);
        }
        return connectionScopedHeaders;
    }
}
