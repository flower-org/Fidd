package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.Aes256KeyContext;
import com.fidd.cryptor.keys.KeyContext;
import com.fidd.cryptor.utils.CertificateChooserUserPreferencesManager;
import javafx.beans.value.ObservableValue;
import org.apache.commons.lang3.StringUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Base64;

import static com.google.common.base.Preconditions.checkNotNull;

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
    public KeyContext geKeyContext() {
        String aes256Base64Key = checkNotNull(aes256KeyTextField).textProperty().get();
        byte[] aes256Key = Base64.getDecoder().decode(aes256Base64Key);

        byte[] aes256Iv = null;
        if (checkNotNull(aes256IvCheckBox).selectedProperty().get()) {
            String aes256Base64Iv = checkNotNull(aes256IvTextField).textProperty().get();
            aes256Iv = Base64.getDecoder().decode(aes256Base64Iv);
        }
        return Aes256KeyContext.of(aes256Key, aes256Iv);
    }

    @Override
    public void initPreferences() {
        loadCertificateChooserPreferences();
        setCertificateChooserPreferencesHandlers();
    }

    public void loadCertificateChooserPreferences() {
        checkNotNull(aes256KeyTextField).textProperty().set(CertificateChooserUserPreferencesManager.aesKey());
        String ivCheckboxStr = CertificateChooserUserPreferencesManager.aesIvCheckbox();
        boolean ivCheckbox = StringUtils.isBlank(ivCheckboxStr) ? false : Boolean.parseBoolean(ivCheckboxStr);
        checkNotNull(aes256IvCheckBox).selectedProperty().set(ivCheckbox);
        checkNotNull(aes256IvTextField).textProperty().set(CertificateChooserUserPreferencesManager.aesIv());
    }

    public void setCertificateChooserPreferencesHandlers() {
        checkNotNull(aes256KeyTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(aes256IvTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(aes256IvCheckBox).selectedProperty().addListener(this::certificateChooserBoolChanged);
    }

    public void certificateChooserTextChanged(ObservableValue<? extends String> observable, String _old, String _new) {
        updateCertificateChooserPreferences();
    }

    public void certificateChooserBoolChanged(ObservableValue<? extends Boolean> observable, Boolean _old, Boolean _new) {
        updateCertificateChooserPreferences();
    }

    public void updateCertificateChooserPreferences() {
        String aesKey = checkNotNull(aes256KeyTextField).textProperty().get();
        String aesIvCheckbox = checkNotNull(aes256IvTextField).textProperty().get();
        boolean aesIv = checkNotNull(aes256IvCheckBox).selectedProperty().get();

        CertificateChooserUserPreferencesManager.updateAesUserPreferences(
                    aesKey, aesIvCheckbox, Boolean.toString(aesIv));
    }
}
