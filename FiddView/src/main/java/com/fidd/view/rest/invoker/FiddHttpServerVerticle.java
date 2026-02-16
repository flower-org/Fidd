package com.fidd.view.rest.invoker;

import com.fidd.base.BaseRepositories;
import com.fidd.service.FiddContentServiceManager;
import com.fidd.view.rest.controller.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import io.vertx.ext.web.validation.RequestParameter;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.impl.RequestParametersImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FiddHttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(FiddHttpServerVerticle.class);
    private final String specFile;

    private final DownloadCustomApiHandler downloadApiHandler;
    private final MessagesApiHandler messagesHandler;

    protected final FiddContentServiceManager fiddContentServiceManager;

    public FiddHttpServerVerticle(String specFile, FiddContentServiceManager fiddContentServiceManager, BaseRepositories repositories) {
        this.specFile = specFile;
        this.fiddContentServiceManager = fiddContentServiceManager;

        downloadApiHandler = new DownloadCustomApiHandler(new DownloadCustomApi(fiddContentServiceManager, repositories));
        messagesHandler = new MessagesApiHandler(new MessagesApiCustomImpl(fiddContentServiceManager));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        RouterBuilder.create(vertx, specFile)
            .map(builder -> {
              builder.setOptions(new RouterBuilderOptions()
                  // For production use case, you need to enable this flag and provide the proper security handler
                  .setRequireSecurityHandlers(false)
              );

              downloadApiHandler.mount(builder);
              messagesHandler.mount(builder);

              Router router = builder.createRouter();
              // Custom router for file download
              router.routeWithRegex("/([^/]+)/([^/]+)/(.+)")
                  .handler(rc -> {
                      String fullPath = rc.request().path().substring(1); // remove leading slash
                      String[] parts = fullPath.split("/", 3);

                      String fiddId = parts[0];
                      Long messageNumber = Long.valueOf(parts[1]);
                      String logicalFilePath = parts[2]; // includes slashes

                      // Build RequestParameters so your handler receives them normally
                      RequestParametersImpl params = new RequestParametersImpl();
                      params.setPathParameters(Map.of("fiddId", RequestParameter.create(fiddId),
                              "messageNumber", RequestParameter.create(messageNumber),
                              "logicalFilePath", RequestParameter.create(logicalFilePath)));

                      // TODO: also add query parameters for playlists

                      // Inject into routing context so your handler sees it
                      rc.put(ValidationHandler.REQUEST_CONTEXT_KEY, params);

                      downloadApiHandler.readLogicalFile(rc);
                  });
              router.errorHandler(400, this::validationFailureHandler);

              return router;
            })
            .compose(router ->
                vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8080)
            )
            .onSuccess(server -> logger.info("Http verticle deploy successful"))
            .onFailure(t -> logger.error("Http verticle failed to deploy", t))
            // Complete the start promise
            .<Void>mapEmpty().onComplete(startPromise);
    }

    private void validationFailureHandler(RoutingContext rc) {
         rc.response().setStatusCode(400)
                 .end("Bad Request : " + rc.failure().getMessage());
    }
}
