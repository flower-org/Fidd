package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class RsaPkcs11KeyProvider extends AnchorPane implements TabKeyProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(RsaPkcs11KeyProvider.class);

    @FXML @Nullable TextField pkcs11LibTextField;
    @FXML @Nullable PasswordField pkcs11TokenPinTextField;
    @FXML @Nullable ComboBox<String> certificatesComboBox;
    @FXML @Nullable ComboBox<String> privateKeysComboBox;

    public RsaPkcs11KeyProvider() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RsaPkcs11KeyProvider.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void testPkcs11Keys() {}
    public void loadPkcs11() {}

    @Override
    public String tabName() {
        return "PKCS#11";
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