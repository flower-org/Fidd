package com.fidd.view;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.connectors.cache.ram.RamCache;
import com.fidd.connectors.ydisk.YandexDiskConnectorFactory;
import com.fidd.view.forms.MainForm;
import com.fidd.view.http.HttpFiddApiServer;
import com.fidd.view.rest.invoker.FiddHttpServerVerticle;
import com.fidd.view.serviceCache.FiddContentServiceCache;
import com.fidd.view.serviceCache.concurrent.ConcurrentFiddContentServiceCache;
import io.vertx.core.Vertx;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static com.fidd.core.common.Format.FMT;

/**
 * Don't run this class directly, use `AppLauncher`.
 * For whatever reason, running this directly will fail with an error.
 */
public class App extends Application {
    final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    final static long FIDD_KEY_CANDIDATES_CACHE_CAPACITY = 1024;
    final static long FIDD_KEY_CACHE_CAPACITY = 1024;
    final static long UNENCRYPTED_FIDD_KEY_CACHE_CAPACITY = 1024;
    final static long FIDD_MESSAGE_CHUNK_CACHE_CAPACITY = 1024;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage mainStage) throws Exception {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("forms/MainForm.fxml"));
            Parent rootNode = fxmlLoader.load();

            BaseRepositories repositories = new DefaultBaseRepositories();
            FiddContentServiceCache fiddContentServiceCache = new ConcurrentFiddContentServiceCache();

            int nettyFileDownloadServerPort = 4198;
            HttpFiddApiServer server = HttpFiddApiServer.runServer(fiddContentServiceCache, repositories, nettyFileDownloadServerPort);
            LOGGER.info(FMT.format(Instant.now()) + " Started HTTP API server on port {}", nettyFileDownloadServerPort);

            int fiddApiServerPort = 4199;

            Vertx vertx = Vertx.vertx();

            // Deploy the generated Vert.x/Netty HTTP server verticle
            vertx.deployVerticle(new FiddHttpServerVerticle("openapi/openapi.yaml", fiddContentServiceCache, fiddApiServerPort))
                    .onSuccess(id -> LOGGER.info("Vert.x/Netty server started successfully. Deployment ID: " + id))
                    .onFailure(err -> {
                        LOGGER.error("Failed to start HTTP server", err);
                        err.printStackTrace();
                        vertx.close();
                    });

            LOGGER.info("Started FiddContentService HTTP/REST API server on port {}", fiddApiServerPort);

            RamCache ramCache = new RamCache(FIDD_KEY_CANDIDATES_CACHE_CAPACITY, FIDD_KEY_CACHE_CAPACITY,
                    UNENCRYPTED_FIDD_KEY_CACHE_CAPACITY, FIDD_MESSAGE_CHUNK_CACHE_CAPACITY);

            MainForm mainForm = fxmlLoader.getController();
            mainForm.init(mainStage, repositories, fiddContentServiceCache, "localhost",
                    nettyFileDownloadServerPort, ramCache);

            Scene mainScene = new Scene(rootNode, 1024, 768);

            //Close all threads when we close JavaFX windows.
            mainStage.setOnHidden(event -> {
                try {
                    server.stopServer();
                    vertx.close().result();
                    // TODO: improve this?
                    YandexDiskConnectorFactory.shutdownOkHttpClient();
                    // TODO: problem with clean shutdown when something is streaming, using nuclear option for now
                    System.exit(0);
                    LOGGER.info("Vert.x/Netty server stopped.");
                } catch (Exception e) {
                    LOGGER.error("Error stopping HTTP server", e);
                } finally {
                    Platform.exit();
                }
            });

            mainStage.setTitle("FiddView 1.4.5");
            mainStage.setScene(mainScene);
            mainStage.setResizable(true);
            mainStage.show();
            LOGGER.info("FiddView App started successfully");
        } catch (Exception e) {
            LOGGER.error("Error starting FiddView", e);
            e.printStackTrace();
            throw e;
        }
    }
}
