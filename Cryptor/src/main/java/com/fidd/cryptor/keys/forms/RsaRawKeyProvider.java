package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import com.fidd.cryptor.utils.CertificateChooserUserPreferencesManager;
import javafx.beans.value.ObservableValue;
import com.fidd.cryptor.keys.RsaKeyContext;
import com.fidd.cryptor.utils.PkiUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import static com.google.common.base.Preconditions.checkNotNull;

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

    public void testRawKeys() {
        try {
            String certificateStr = checkNotNull(rawCertificateTextArea).textProperty().get();
            String keyStr = checkNotNull(rawPrivateKeyTextArea).textProperty().get();
            Certificate certificate = PkiUtil.getCertificateFromString(certificateStr);
            PrivateKey key = PkiUtil.getPrivateKeyFromString(keyStr);

            testKeys(certificate, key);
        } catch (Exception e) {
            LOGGER.error("Raw keys test error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public static void testKeys(Certificate certificate, PrivateKey key) throws NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SignatureException {
        String encryptTestResult = PkiUtil.testKeyPairMatchByEncrypting(certificate.getPublicKey(), key) ? "SUCCESS" : "FAIL";
        String signTestResult = PkiUtil.testKeyPairMatchBySigning(certificate.getPublicKey(), key) ? "SUCCESS" : "FAIL";

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                String.format("Encryption test: %s.\nSignature test: %s", encryptTestResult, signTestResult),
                ButtonType.OK);
        alert.showAndWait();
    }

    @Override
    public String tabName() {
        return "Raw";
    }

    @Override
    public AnchorPane tabContent() {
        return this;
    }

    @Override
    public KeyContext geKeyContext() {
        try {
            String certificateStr = checkNotNull(rawCertificateTextArea).textProperty().get();
            String keyStr = checkNotNull(rawPrivateKeyTextArea).textProperty().get();
            X509Certificate certificate = PkiUtil.getCertificateFromString(certificateStr);
            PrivateKey key = PkiUtil.getPrivateKeyFromString(keyStr);

            return RsaKeyContext.of(certificate.getPublicKey(), key, certificate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initPreferences() {
        loadCertificateChooserPreferences();
        setCertificateChooserPreferencesHandlers();
    }

    public void loadCertificateChooserPreferences() {
        checkNotNull(rawCertificateTextArea).textProperty().set(CertificateChooserUserPreferencesManager.rawCertificate());
        checkNotNull(rawPrivateKeyTextArea).textProperty().set(CertificateChooserUserPreferencesManager.rawPrivateKey());
    }
    public void setCertificateChooserPreferencesHandlers() {
        checkNotNull(rawCertificateTextArea).textProperty().addListener(this::certificateChooserTextChanged);;
        checkNotNull(rawPrivateKeyTextArea).textProperty().addListener(this::certificateChooserTextChanged);
    }

    public void certificateChooserTextChanged(ObservableValue<? extends String> observable, String _old, String _new) {
        updateCertificateChooserPreferences();
    }

    public void updateCertificateChooserPreferences() {
        String rawCertificate = checkNotNull(rawCertificateTextArea).textProperty().get();
        String rawPrivateKey = checkNotNull(rawPrivateKeyTextArea).textProperty().get();

        CertificateChooserUserPreferencesManager.updateUserPreferences(rawCertificate, rawPrivateKey);
    }
}