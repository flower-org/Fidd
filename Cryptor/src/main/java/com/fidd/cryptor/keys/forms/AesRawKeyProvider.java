package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class AesRawKeyProvider extends AnchorPane implements TabKeyProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(AesRawKeyProvider.class);

    @FXML @Nullable TextField aes256KeyTextField;
    @FXML @Nullable CheckBox aes256IvCheckBox;
    @FXML @Nullable TextField aes256IvTextField;

    public AesRawKeyProvider() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AesRawKeyProvider.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String tabName() {
        return "AES-256";
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
