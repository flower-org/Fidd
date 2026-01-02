package com.fidd.packer.forms;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.subscription.SubscriberList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SubscriberAddDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(SubscriberAddDialog.class);
    final static Repository<PublicKeySerializer> PUBLIC_KEY_FORMAT_REPO = new DefaultBaseRepositories().publicKeyFormatRepo();

    @FXML @Nullable TextField subscriberIdTextField;
    @FXML @Nullable ComboBox<String> certTypeComboBox;
    @FXML @Nullable TextField certTextField;
    @FXML @Nullable TextArea certTextArea;
    @FXML @Nullable Button addButton;

    @Nullable Stage stage;

    @Nullable volatile SubscriberList.Subscriber returnSubscriber = null;

    public SubscriberAddDialog(@Nullable SubscriberList.Subscriber subscriberToEdit) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SubscriberAddDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(certTypeComboBox).itemsProperty().get().clear();
        PUBLIC_KEY_FORMAT_REPO.listEntryNames().forEach(
                type -> checkNotNull(certTypeComboBox).itemsProperty().get().add(type));
        checkNotNull(certTypeComboBox).selectionModelProperty().get()
                .select(PUBLIC_KEY_FORMAT_REPO.defaultKey());

        checkNotNull(certTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.equals(oldValue)) {
                try {
                    File certificatFile = new File(newValue);
                    if (certificatFile.exists()) {
                        byte[] certBytes = Files.readAllBytes(certificatFile.toPath());
                        String certContent = new String(certBytes);
                        checkNotNull(certTextArea).setText(certContent);
                    } else {
                        throw new FileNotFoundException(certificatFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error opening file: " + newValue, e);
                    checkNotNull(certTextArea).setText("");
                }
            }
        });

        if (subscriberToEdit != null) {
            checkNotNull(addButton).textProperty().set("Edit");

            checkNotNull(subscriberIdTextField).textProperty().set(subscriberToEdit.subscriberId());
            checkNotNull(certTypeComboBox).getSelectionModel().select(subscriberToEdit.publicKeyFormat());
            checkNotNull(certTextArea).textProperty().set(new String(subscriberToEdit.publicKeyBytes()));
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void selectCert() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Certificate (*.crt)", "*.crt"));
        fileChooser.setTitle("Load Certificate");
        File certificatFile = fileChooser.showOpenDialog(checkNotNull(stage));
        if (certificatFile != null) {
            checkNotNull(certTextField).textProperty().set(certificatFile.getPath());
        }
    }

    public void okClose() {
        try {
            String subscriberId = checkNotNull(subscriberIdTextField).getText();
            String certContent = checkNotNull(certTextArea).getText();
            String certType = checkNotNull(certTypeComboBox).getSelectionModel().getSelectedItem();
            if (StringUtils.isBlank(certType)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please specify Certificate Type", ButtonType.OK);
                alert.showAndWait();
            } else if (StringUtils.isBlank(certContent)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please enter Certificate Content", ButtonType.OK);
                alert.showAndWait();
            } else {
                PublicKeySerializer publicKeySerializer = PUBLIC_KEY_FORMAT_REPO.get(certType);
                if (publicKeySerializer != null) {
                    try {
                        byte[] certBytes = certContent.getBytes(StandardCharsets.UTF_8);
                        // Validating that cert is deserializable and not some random bytes
                        publicKeySerializer.deserialize(certBytes);

                        returnSubscriber = SubscriberList.Subscriber.of(subscriberId, certType, certBytes);
                        checkNotNull(stage).close();
                    } catch (Exception e) {
                        String errorStr = String.format("Certificate can't be deserialized as %s", certType);
                        Alert alert = new Alert(Alert.AlertType.ERROR, errorStr + ": " + e, ButtonType.OK);
                        LOGGER.error(errorStr, e);
                        alert.showAndWait();
                    }
                } else {
                    throw new IllegalArgumentException("PublicKeySerializer not found: " + certType);
                }
            }
       } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "SubscriberAddDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("SubscriberAddDialog close Error:", e);
            alert.showAndWait();
        }
    }

    @Nullable SubscriberList.Subscriber getReturnSubscriber() {
        return returnSubscriber;
    }
}
