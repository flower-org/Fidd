package com.fidd.view.forms;

import static com.fidd.connectors.folder.FolderFiddConstants.FOLDER_FIDD_CONNECTOR_FACTORY_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnectorFactory;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.view.blog.FiddBlog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FiddBlogAddDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddBlogAddDialog.class);
    final static Repository<FiddConnectorFactory> FIDD_CONNECTOR_FACTORY_REPO =
            new DefaultBaseRepositories().fiddConnectorFactoryRepo();
    final static Repository<PublicKeySerializer> PUBLIC_KEY_FORMAT_REPO =
            new DefaultBaseRepositories().publicKeyFormatRepo();

    @FXML @Nullable ComboBox<String> connectorTypeComboBox;
    @FXML @Nullable TextField blogNameTextField;
    @FXML @Nullable TextField urlTextField;
    @FXML @Nullable Button addButton;
    @FXML @Nullable Button selectFolderButon;

    @FXML @Nullable ComboBox<String> publisherCertTypeComboBox;
    @FXML @Nullable TextArea publisherCertTextArea;
    @FXML @Nullable TextField publisherCertFileTextField;

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

        checkNotNull(publisherCertTypeComboBox).itemsProperty().get().addAll(PUBLIC_KEY_FORMAT_REPO.listEntryNames());
        checkNotNull(publisherCertTypeComboBox).getSelectionModel().select(PUBLIC_KEY_FORMAT_REPO.defaultKey());

        if (fiddBlogToEdit != null) {
            checkNotNull(addButton).textProperty().set("Edit");
            checkNotNull(connectorTypeComboBox).getSelectionModel().select(fiddBlogToEdit.connectorType());
            checkNotNull(blogNameTextField).textProperty().set(fiddBlogToEdit.blogName());
            checkNotNull(urlTextField).textProperty().set(fiddBlogToEdit.blogUrl().toString());

            if (fiddBlogToEdit.publicKeyBytes() != null) {
                checkNotNull(publisherCertTypeComboBox).selectionModelProperty().get().select(checkNotNull(fiddBlogToEdit.publicKeyFormat()));
                checkNotNull(publisherCertTextArea).textProperty().set(new String(fiddBlogToEdit.publicKeyBytes()));
            }
        }

        checkNotNull(connectorTypeComboBox).setOnAction(event -> {
            updateSelectFolderButon();
        });
        updateSelectFolderButon();

        checkNotNull(publisherCertFileTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                try {
                    File certificatFile = new File(newValue);
                    if (certificatFile.exists()) {
                        byte[] certBytes = Files.readAllBytes(certificatFile.toPath());
                        String certContent = new String(certBytes);
                        checkNotNull(publisherCertTextArea).setText(certContent);
                    } else {
                        throw new FileNotFoundException(certificatFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error opening file: " + newValue, e);
                    checkNotNull(publisherCertTextArea).setText("");
                }
            }
        });
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
                // Form URL
                URL blogUrl = new URL(urlStr);

                byte[] certBytes = null;
                String certType = null;
                String certStr = checkNotNull(publisherCertTextArea).textProperty().get();
                if (!StringUtils.isBlank(certStr)) {
                    certType = checkNotNull(publisherCertTypeComboBox).valueProperty().get();
                    PublicKeySerializer publicKeySerializer = checkNotNull(PUBLIC_KEY_FORMAT_REPO.get(certType));

                    certBytes = certStr.getBytes(StandardCharsets.UTF_8);
                    // Validating that cert is deserializable and not some random bytes
                    publicKeySerializer.deserialize(certBytes);
                }

                returnFiddBlog = FiddBlog.of(connectorType, blogName, blogUrl, certType, certBytes);
                checkNotNull(stage).close();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "WorkspaceDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("WorkspaceDialog close Error:", e);
            alert.showAndWait();
        }
    }

    public @Nullable FiddBlog getFiddBlog() {
        return returnFiddBlog;
    }

    public void selectPublisherCertFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Certificate (*.crt)", "*.crt"));
        fileChooser.setTitle("Load Certificate");
        File certificatFile = fileChooser.showOpenDialog(checkNotNull(stage));
        if (certificatFile != null) {
            checkNotNull(publisherCertFileTextField).textProperty().set(certificatFile.getPath());
        }
    }
}
