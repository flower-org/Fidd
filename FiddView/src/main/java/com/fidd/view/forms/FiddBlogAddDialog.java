package com.fidd.view.forms;

import static com.fidd.connectors.folder.FolderFiddConstants.FOLDER_FIDD_CONNECTOR_FACTORY_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnectorFactory;
import com.fidd.view.blog.FiddBlog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FiddBlogAddDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddBlogAddDialog.class);
    final static Repository<FiddConnectorFactory> FIDD_CONNECTOR_FACTORY_REPO =
            new DefaultBaseRepositories().fiddConnectorFactoryRepo();

    @FXML @Nullable ComboBox<String> connectorTypeComboBox;
    @FXML @Nullable TextField blogNameTextField;
    @FXML @Nullable TextField urlTextField;
    @FXML @Nullable Button addButton;
    @FXML @Nullable Button selectFolderButon;

    @FXML @Nullable Label certLabel;
    @FXML @Nullable GridPane certChoiceGridPane;

    @Nullable Stage stage;
    @Nullable volatile FiddBlog returnFiddBlog = null;

    public FiddBlogAddDialog(@Nullable FiddBlog fiddBlogToEdit) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FiddBlogAddDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(connectorTypeComboBox).itemsProperty().get().addAll(FIDD_CONNECTOR_FACTORY_REPO.listEntryNames());
        checkNotNull(connectorTypeComboBox).getSelectionModel().select(FIDD_CONNECTOR_FACTORY_REPO.defaultKey());

        if (fiddBlogToEdit != null) {
            checkNotNull(addButton).textProperty().set("Edit");
            checkNotNull(connectorTypeComboBox).getSelectionModel().select(fiddBlogToEdit.connectorType());
            checkNotNull(blogNameTextField).textProperty().set(fiddBlogToEdit.blogName());
            checkNotNull(urlTextField).textProperty().set(fiddBlogToEdit.blogUrl().toString());
        }

        checkNotNull(connectorTypeComboBox).setOnAction(event -> {
            updateSelectFolderButon();
        });
        updateSelectFolderButon();
    }

    protected void updateSelectFolderButon() {
        String selectedItem = checkNotNull(connectorTypeComboBox).getSelectionModel().getSelectedItem();
        checkNotNull(selectFolderButon).visibleProperty().set(selectedItem.equals(FOLDER_FIDD_CONNECTOR_FACTORY_NAME));
    }

    public void selectFolder() throws MalformedURLException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Load Fidd Folder");
        File fiddFolder = directoryChooser.showDialog(checkNotNull(stage));
        if (fiddFolder != null) {
            URL url = fiddFolder.toURI().toURL();
            checkNotNull(urlTextField).textProperty().set(url.toString());
        }
    }

    public void selectConnectorType(String connectorType) {
        checkNotNull(connectorTypeComboBox).getSelectionModel().select(connectorType);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void okClose() {
        try {
            String connectorType = checkNotNull(connectorTypeComboBox).valueProperty().get();
            String blogName = checkNotNull(blogNameTextField).textProperty().get();
            String urlStr = checkNotNull(urlTextField).textProperty().get();

            if (StringUtils.isBlank(connectorType)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify Connector Type", ButtonType.OK);
                alert.showAndWait();
            } else if (StringUtils.isBlank(blogName)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify Blog Name", ButtonType.OK);
                alert.showAndWait();
            } else if (StringUtils.isBlank(urlStr)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify URL", ButtonType.OK);
                alert.showAndWait();
            } else {
                URL blogUrl = new URL(urlStr);
                returnFiddBlog = FiddBlog.of(connectorType, blogName, blogUrl);
                checkNotNull(stage).close();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "WorkspaceDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("WorkspaceDialog close Error:", e);
            alert.showAndWait();
        }
    }

    @Nullable public FiddBlog getFiddBlog() {
        return returnFiddBlog;
    }
}
