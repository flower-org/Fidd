package com.fidd.view.forms;

import com.fidd.core.connection.FiddConnection;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.ModalWindow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    @Nullable Stage mainStage;

    @FXML @Nullable AnchorPane topPane;
    @FXML @Nullable TableView<FiddConnection> fiddBlogTableView;

    @Nullable TabKeyProvider keyProvider;
    @Nullable ObservableList<FiddConnection> fiddConnections;


    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "FiddView v 0.1.0", ButtonType.OK);
        alert.showAndWait();
    }

    public void init(Stage mainStage) {
        this.mainStage = mainStage;

        keyProvider = buildMainKeyProvider(mainStage);
        AnchorPane keyProviderForm = keyProvider.tabContent();
        checkNotNull(topPane).getChildren().add(keyProviderForm);
        AnchorPane.setTopAnchor(keyProviderForm, 0.0);
        AnchorPane.setBottomAnchor(keyProviderForm, 0.0);
        AnchorPane.setLeftAnchor(keyProviderForm, 0.0);
        AnchorPane.setRightAnchor(keyProviderForm, 0.0);

        keyProvider.initPreferences();

        fiddConnections = FXCollections.observableArrayList();
        checkNotNull(fiddBlogTableView).itemsProperty().set(fiddConnections);
    }

    private static TabKeyProvider buildMainKeyProvider(Stage mainStage) {
        RsaPkcs11KeyProvider rsaPkcs11KeyProvider = new RsaPkcs11KeyProvider(mainStage);
        RsaFileKeyProvider rsaFileKeyProvider = new RsaFileKeyProvider(mainStage);
        RsaRawKeyProvider rsaRawKeyProvider = new RsaRawKeyProvider();
        MultiKeyProvider rsaTabPane = new MultiKeyProvider(mainStage, "RSA-2048",
                List.of(rsaPkcs11KeyProvider, rsaFileKeyProvider, rsaRawKeyProvider));

        return new MultiKeyProvider(mainStage, "", List.of(rsaTabPane));
    }

    public void quit() { checkNotNull(mainStage).close(); }

    protected void saveByteArrayToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    // ---------- Blog operations ----------

    public void openBlog() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddBlogTableView).getSelectionModel().getSelectedItem();
            if (selectedFiddConnection != null) {
                checkNotNull(fiddConnections).remove(selectedFiddConnection);
                checkNotNull(fiddBlogTableView).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Opening Blog connection: " + e, ButtonType.OK);
            LOGGER.error("Error Opening Blog connection: ", e);
            alert.showAndWait();
        }
    }

    protected void addFiddBlog(FiddConnection fiddConnection) {
        Platform.runLater(() -> {
            checkNotNull(fiddConnections).add(fiddConnection);
            checkNotNull(fiddBlogTableView).refresh();
        });
    }

    public void addBlog() {
        try {
            FiddBlogAddDialog socksNodeAddDialog = new FiddBlogAddDialog(null);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> { socksNodeAddDialog.setStage(stage); return socksNodeAddDialog; },
                    "Add new Blog Connection");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            FiddConnection fiddConnection = socksNodeAddDialog.getFiddBlog();
                            if (fiddConnection != null) {
                                addFiddBlog(fiddConnection);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Blog Connection: " + e, ButtonType.OK);
                            LOGGER.error("Error adding Blog Connection: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Blog Connection: " + e, ButtonType.OK);
            LOGGER.error("Error adding Blog Connection: ", e);
            alert.showAndWait();
        }
    }

    public void removeBlog() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddBlogTableView).getSelectionModel().getSelectedItem();
            if (selectedFiddConnection != null) {
                checkNotNull(fiddConnections).remove(selectedFiddConnection);
                checkNotNull(fiddBlogTableView).refresh();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Removing Blog connection: " + e, ButtonType.OK);
            LOGGER.error("Error Removing Blog connection: ", e);
            alert.showAndWait();
        }
    }

    public void editBlog() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddBlogTableView).getSelectionModel().getSelectedItem();
            int selectedIndex = checkNotNull(fiddBlogTableView).getSelectionModel().getSelectedIndex();
            if (selectedFiddConnection != null) {
                FiddBlogAddDialog fiddBlogAddDialog = new FiddBlogAddDialog(selectedFiddConnection);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                        stage -> {
                            fiddBlogAddDialog.setStage(stage);
                            return fiddBlogAddDialog;
                        },
                        "Edit Blog Connection");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                FiddConnection fiddConnection = fiddBlogAddDialog.getFiddBlog();
                                if (fiddConnection != null) {
                                    checkNotNull(fiddConnections).remove(selectedIndex);
                                    checkNotNull(fiddConnections).add(selectedIndex, fiddConnection);
                                    checkNotNull(fiddBlogTableView).refresh();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Error editing Blog Connection: " + e, ButtonType.OK);
                                LOGGER.error("Error editing Blog Connection: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error editing Blog Connection: " + e, ButtonType.OK);
            LOGGER.error("Error editing Blog Connection: ", e);
            alert.showAndWait();
        }
    }

    // ---------- Blog List operations ----------

    public void saveBlogs() {
        try {
            // TODO: Open save file dialog
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving Blog Connections: " + e, ButtonType.OK);
            LOGGER.error("Error saving Blog Connection: ", e);
            alert.showAndWait();
        }
    }

    public void loadBlogs() {
        try {
            // TODO: Open load file dialog
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading Blog Connections: " + e, ButtonType.OK);
            LOGGER.error("Error loading Blog Connections: ", e);
            alert.showAndWait();
        }
    }
}
