package com.fidd.view.forms;

import com.fidd.view.blog.FiddBlog;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.ModalWindow;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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

    @Nullable TabKeyProvider keyProvider;

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
        // TODO: open selected blog
    }

    public void addBlog() {
        // TODO: open dialog to add blog
        try {
            FiddBlogAddDialog socksNodeAddDialog = new FiddBlogAddDialog(null);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> { socksNodeAddDialog.setStage(stage); return socksNodeAddDialog; },
                    "Add new server");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            FiddBlog fiddBlog = socksNodeAddDialog.getFiddBlog();
                            if (fiddBlog != null) {
                                // TODO: implement
                                //addFiddBlog(fiddBlog);

                                // TODO: implement
                                //refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
                            LOGGER.error("Error adding known server: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding known server: " + e, ButtonType.OK);
            LOGGER.error("Error adding known server: ", e);
            alert.showAndWait();
        }
    }

    public void removeBlog() {
        // TODO: remove selected blog
    }

    public void editBlog() {
        // TODO: open dialog to edit blog
    }

    // ---------- Blog List operations ----------

    public void saveBlogs() {
        // TODO: Open save file dialog
    }

    public void loadBlogs() {
        // TODO: Open load file dialog
    }
}
