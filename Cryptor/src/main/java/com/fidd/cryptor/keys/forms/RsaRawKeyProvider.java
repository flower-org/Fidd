package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class RsaRawKeyProvider extends AnchorPane implements TabKeyProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(RsaRawKeyProvider.class);

    @FXML @Nullable TextArea rawCertificateTextArea;
    @FXML @Nullable TextArea rawPrivateKeyTextArea;

    public RsaRawKeyProvider() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("RsaRawKeyProvider.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void testRawKeys() {}

    @Override
    public String tabName() {
        return "Raw";
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