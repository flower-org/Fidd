package com.fidd.view.rest.invoker;

import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.controller.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.handler.StaticHandler;

public class FiddHttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(FiddHttpServerVerticle.class);
    private final String specFile;
    private static final int NETTY_FILE_SERVER_PORT = 4198;

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
        HttpClient httpClient = vertx.createHttpClient();

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
                        httpClient.request(req.method(), NETTY_FILE_SERVER_PORT, "localhost", req.uri())
                                .compose(clientReq -> {
                                    req.headers().forEach(e -> clientReq.putHeader(e.getKey(), e.getValue()));                                    if (req.method() == HttpMethod.GET || req.method() == HttpMethod.HEAD) {
                                        return clientReq.send();
                                    }
                                    return clientReq.send(req);
                                })
                                .onSuccess(clientResp -> {
                                    HttpServerResponse resp = ctx.response();
                                    resp.setStatusCode(clientResp.statusCode());
                                    clientResp.headers().forEach(e -> resp.putHeader(e.getKey(), e.getValue()));
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
}
