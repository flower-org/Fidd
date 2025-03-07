package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class RsaFileKeyProvider extends AnchorPane implements TabKeyProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(RsaFileKeyProvider.class);

    @FXML @Nullable TextField fileCertificateTextField;
    @FXML @Nullable TextField filePrivateKeyTextField;

    public RsaFileKeyProvider() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RsaFileKeyProvider.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void openCertificateFile() {}
    public void openPrivateKeyFile() {}
    public void loadCertificateFileKey() {}
    public void loadPrivateKeyFileKey() {}
    public void testFileKeys() {}
    public void loadCertificateFile() {}
    public void loadPrivateKeyFile() {}

    @Override
    public String tabName() {
        return "Files";
    }

    @Override
    public AnchorPane tabContent() {
        return this;
    }

    @Override
    @Nullable public KeyContext geKeyContext() {
        return null;
    }
}