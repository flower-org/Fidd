package com.fidd.view;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.view.forms.MainForm;
import com.fidd.view.http.HttpFiddApiServer;
import com.fidd.view.serviceCache.FiddContentServiceCache;
import com.fidd.view.serviceCache.concurrent.ConcurrentFiddContentServiceCache;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Don't run this class directly, use `AppLauncher`.
 * For whatever reason, running this directly will fail with an error.
 */
public class App extends Application {
    final static Logger LOGGER = LoggerFactory.getLogger(App.class);

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

            int fiddApiServerPort = 8080;
            HttpFiddApiServer server = HttpFiddApiServer.runServer(fiddContentServiceCache, repositories, fiddApiServerPort);
            LOGGER.info("Started HTTP API server on port {}", fiddApiServerPort);

            MainForm mainForm = fxmlLoader.getController();
            mainForm.init(mainStage, repositories, fiddContentServiceCache);

            Scene mainScene = new Scene(rootNode, 1024, 768);

            //Close all threads when we close JavaFX windows.
            mainStage.setOnHidden(event -> {
                try {
                    server.stopServer();
                } catch (Exception e) {
                    LOGGER.error("Error stopping HTTP server", e);
                } finally {
                    Platform.exit();
                }
            });

            mainStage.setTitle("FiddView");
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
