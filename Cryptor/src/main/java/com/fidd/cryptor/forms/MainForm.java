package com.fidd.cryptor.forms;

import com.fidd.cryptor.utils.PkiUtil;
import com.google.common.base.Preconditions;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import java.security.KeyStore;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    @Nullable Stage mainStage;
    @FXML @Nullable TextField pkcs11LibTextField;
    @FXML @Nullable TextField pkcs11TokenPinTextField;
    @FXML @Nullable ComboBox<String> certificatesComboBox;
    @FXML @Nullable ComboBox<String> privateKeysComboBox;

    @Nullable KeyStore pkcs11KeyStore;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void setMainStage(@Nullable Stage mainStage) {
        this.mainStage = mainStage;
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Cryptor v 0.0.1", ButtonType.OK);
        alert.showAndWait();
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void loadPkcs11() {
        String pkcs11Lib = checkNotNull(pkcs11LibTextField).textProperty().get();
        String pkcs11TokenPin = checkNotNull(pkcs11TokenPinTextField).textProperty().get();

        if (StringUtils.isBlank(pkcs11Lib)) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "PKCS#11 Library Path is empty", ButtonType.OK);
            alert.showAndWait();
        }

        pkcs11KeyStore = PkiUtil.loadPKCS11KeyStore(pkcs11Lib, pkcs11TokenPin);

        List<String> keyAliases = PkiUtil.getKeyAliasesFromKeyStore(pkcs11KeyStore);
        List<String> certificateAliases = PkiUtil.getCertificateAliasesFromKeyStore(pkcs11KeyStore);

        String certificateBefore = checkNotNull(certificatesComboBox).valueProperty().get();
        String keyBefore = checkNotNull(privateKeysComboBox).valueProperty().get();

        checkNotNull(certificatesComboBox).getItems().clear();
        checkNotNull(certificatesComboBox).getItems().addAll(certificateAliases);
        if (!certificateAliases.isEmpty()) {
            if (!certificateAliases.contains(certificateBefore)) {
                certificateBefore = certificateAliases.get(0);
            }
            checkNotNull(certificatesComboBox).valueProperty().set(certificateBefore);
        }

        checkNotNull(privateKeysComboBox).getItems().clear();
        checkNotNull(privateKeysComboBox).getItems().addAll(keyAliases);
        if (!keyAliases.isEmpty()) {
            if (!keyAliases.contains(keyBefore)) {
                keyBefore = keyAliases.get(0);
            }
            checkNotNull(privateKeysComboBox).valueProperty().set(keyBefore);
        }
    }
}
