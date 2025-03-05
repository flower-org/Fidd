package com.fidd.cryptor.forms;

import com.fidd.cryptor.transform.AesTransformerProvider;
import com.fidd.cryptor.transform.RsaTransformerProvider;
import com.fidd.cryptor.transform.SignatureChecker;
import com.fidd.cryptor.transform.Transformer;
import com.fidd.cryptor.transform.TransformerProvider;
import com.fidd.cryptor.utils.Cryptor;
import com.fidd.cryptor.utils.PkiUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.fidd.cryptor.transform.RsaTransformerProvider.Mode;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    @Nullable Stage mainStage;
    @FXML @Nullable TextField pkcs11LibTextField;
    @FXML @Nullable TextField pkcs11TokenPinTextField;
    @FXML @Nullable ComboBox<String> certificatesComboBox;
    @FXML @Nullable ComboBox<String> privateKeysComboBox;

    @FXML @Nullable TextArea plaintextTextArea;
    @FXML @Nullable TextArea ciphertextTextArea;

    @FXML @Nullable TextArea signPlaintextTextArea;
    @FXML @Nullable TextArea signSignatureTextArea;

    @FXML @Nullable TabPane mainGroupTabPane;
    @FXML @Nullable Tab rsaTab;
    @FXML @Nullable Tab aes256Tab;
    @FXML @Nullable Tab generatorTab;
    @FXML @Nullable TabPane rsaTabPane;
    @FXML @Nullable Tab pkcs11Tab;
    @FXML @Nullable Tab filesTab;
    @FXML @Nullable Tab rawTab;

    @FXML @Nullable TextField aes256KeyTextField;

    @FXML @Nullable TextField generatedAes256KeyTextField;
    @FXML @Nullable TextField generatedAes256IvTextField;
    @FXML @Nullable TextArea generatedCertificateTextArea;
    @FXML @Nullable TextArea generatedPrivateKeyTextArea;

    @FXML @Nullable CheckBox aes256IvCheckBox;
    @FXML @Nullable TextField aes256IvTextField;

    @FXML @Nullable CheckBox rsaPkcsEncryptWithPublicKeyCheckBox;
    @FXML @Nullable CheckBox rsaPkcsUseRsaAesHybridCheckBox;

    @FXML @Nullable CheckBox rsaRawEncryptWithPublicKeyCheckBox;
    @FXML @Nullable CheckBox rsaRawUseRsaAesHybridCheckBox;

    @FXML @Nullable TextArea rawCertificateTextArea;
    @FXML @Nullable TextArea rawPrivateKeyTextArea;

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

    protected TransformerProvider getCurrentTransformerProvider() {
        Tab selectedMainTab = checkNotNull(mainGroupTabPane).getSelectionModel().getSelectedItem();
        if (selectedMainTab == rsaTab) {
            Tab selectedRsaTab = checkNotNull(rsaTabPane).getSelectionModel().getSelectedItem();
            if (selectedRsaTab == pkcs11Tab) {
                String certAlias = checkNotNull(certificatesComboBox).getSelectionModel().getSelectedItem();
                String keyAlias = checkNotNull(privateKeysComboBox).getSelectionModel().getSelectedItem();

                if (pkcs11KeyStore == null) {
                    throw new RuntimeException("PKCS#11 store not loaded");
                }
                Certificate certificate = PkiUtil.getCertificateFromKeyStore(checkNotNull(pkcs11KeyStore), certAlias);
                PrivateKey key = (PrivateKey)PkiUtil.getKeyFromKeyStore(pkcs11KeyStore, keyAlias);
                Mode mode = checkNotNull(rsaPkcsEncryptWithPublicKeyCheckBox).isSelected()
                        ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                RsaTransformerProvider.EncryptionFormat encryptionFormat = checkNotNull(rsaPkcsUseRsaAesHybridCheckBox).isSelected()
                        ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                return new RsaTransformerProvider(certificate.getPublicKey(), key, mode, encryptionFormat);
            } else if (selectedRsaTab == filesTab) {
                return TransformerProvider.of(null, null, null, null);
            } else if (selectedRsaTab == rawTab) {
                try {
                    String certificateStr = checkNotNull(rawCertificateTextArea).textProperty().get();
                    String keyStr = checkNotNull(rawPrivateKeyTextArea).textProperty().get();
                    Certificate certificate = PkiUtil.getCertificateFromString(certificateStr);
                    PrivateKey key = PkiUtil.getPrivateKeyFromString(keyStr);

                    Mode mode = checkNotNull(rsaRawEncryptWithPublicKeyCheckBox).isSelected()
                            ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                    RsaTransformerProvider.EncryptionFormat encryptionFormat = checkNotNull(rsaRawUseRsaAesHybridCheckBox).isSelected()
                            ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                    return new RsaTransformerProvider(certificate.getPublicKey(), key, mode, encryptionFormat);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Unknown RSA mode
                return TransformerProvider.of(null, null, null, null);
            }
        } else if (selectedMainTab == aes256Tab) {
            String aes256Base64Key = checkNotNull(aes256KeyTextField).textProperty().get();
            byte[] aes256Key = Base64.getDecoder().decode(aes256Base64Key);

            byte[] aes256Iv;
            if (checkNotNull(aes256IvCheckBox).isSelected()) {
                String aes256Base64Iv = checkNotNull(aes256IvTextField).textProperty().get();
                aes256Iv = Base64.getDecoder().decode(aes256Base64Iv);
            } else {
                aes256Iv = new byte[16];
            }
            return new AesTransformerProvider(aes256Key, aes256Iv);
        } else {
            // Unknown scheme
            return TransformerProvider.of(null, null, null, null);
        }
    }

    public void encryptText() {
        try {
            Transformer encryptTransformer = getCurrentTransformerProvider().getEncryptTransformer();

            if (encryptTransformer != null) {
                String plaintext = checkNotNull(plaintextTextArea).textProperty().get();
                byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
                byte[] ciphertextBytes = encryptTransformer.transform(plaintextBytes);
                String ciphertext64 = Base64.getEncoder().encodeToString(ciphertextBytes);

                checkNotNull(ciphertextTextArea).textProperty().set(ciphertext64);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Selection not supported", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Encryption error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void decryptText() {
        try {
            Transformer decryptTransformer = getCurrentTransformerProvider().getDecryptTransformer();

            if (decryptTransformer != null) {
                String ciphertext64 = checkNotNull(ciphertextTextArea).textProperty().get();
                byte[] ciphertextBytes = Base64.getDecoder().decode(ciphertext64);
                byte[] plaintextBytes = decryptTransformer.transform(ciphertextBytes);
                String plaintext = new String(plaintextBytes);

                checkNotNull(plaintextTextArea).textProperty().set(plaintext);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Selection not supported", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Decryption error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void signText() {
        try {
            Transformer signTransformer = getCurrentTransformerProvider().getSignTransformer();

            if (signTransformer != null) {
                String plaintext = checkNotNull(signPlaintextTextArea).textProperty().get();
                byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);

                byte[] signatureBytes = signTransformer.transform(plaintextBytes);
                String signature64 = Base64.getEncoder().encodeToString(signatureBytes);

                checkNotNull(signSignatureTextArea).textProperty().set(signature64);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Selection not supported", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void checkSignature() {
        try {
            SignatureChecker signTransformer = getCurrentTransformerProvider().getSignatureChecker();

            if (signTransformer != null) {
                String plaintext = checkNotNull(signPlaintextTextArea).textProperty().get();
                byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
                String signature = checkNotNull(signSignatureTextArea).textProperty().get();
                byte[] signatureBytes = Base64.getDecoder().decode(signature);

                if (signTransformer.checkSignature(plaintextBytes, signatureBytes)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Signature OK", ButtonType.OK);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Signature INVALID", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Selection not supported", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void generateAes256Key() {
        try {
            byte[] key = Cryptor.generateAESKeyRaw();
            String base64EncodedKey = Base64.getEncoder().encodeToString(key);
            checkNotNull(generatedAes256KeyTextField).textProperty().set(base64EncodedKey);
        } catch (Exception e) {
            LOGGER.error("AES-256 key generation error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void generateAes256Iv() {
        try {
            byte[] iv = Cryptor.generateAESIV();
            String base64EncodedIv = Base64.getEncoder().encodeToString(iv);
            checkNotNull(generatedAes256IvTextField).textProperty().set(base64EncodedIv);
        } catch (Exception e) {
            LOGGER.error("AES-256 IV generation error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void generateSelfSignedCertificate() {
        try {
            KeyPair keyPair = PkiUtil.generateRsa2048KeyPair();
            X500Principal subject = new X500Principal("CN=Self-Generated Certificate");
            X509Certificate certificate = PkiUtil.generateSelfSignedCertificate(keyPair, subject);

            String certificateStr = PkiUtil.getCertificateAsPem(certificate);
            String keyStr = PkiUtil.getKeyAsPem(keyPair.getPrivate());

            checkNotNull(generatedCertificateTextArea).textProperty().set(certificateStr);
            checkNotNull(generatedPrivateKeyTextArea).textProperty().set(keyStr);
        } catch (Exception e) {
            LOGGER.error("Self-Signed certificate generation error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void testPkcs11Keys() {
        try {
            String certAlias = checkNotNull(certificatesComboBox).getSelectionModel().getSelectedItem();
            String keyAlias = checkNotNull(privateKeysComboBox).getSelectionModel().getSelectedItem();

            if (pkcs11KeyStore == null) {
                throw new RuntimeException("PKCS#11 store not loaded");
            }
            Certificate certificate = PkiUtil.getCertificateFromKeyStore(checkNotNull(pkcs11KeyStore), certAlias);
            PrivateKey key = (PrivateKey)PkiUtil.getKeyFromKeyStore(pkcs11KeyStore, keyAlias);

            testKeys(certificate, key);
        } catch (Exception e) {
            LOGGER.error("PKCS11 keys test error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
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

    protected void testKeys(Certificate certificate, PrivateKey key) throws NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, SignatureException {
        String encryptTestResult = PkiUtil.testKeyPairMatchByEncrypting(certificate.getPublicKey(), key) ? "SUCCESS" : "FAIL";
        String signTestResult = PkiUtil.testKeyPairMatchBySigning(certificate.getPublicKey(), key) ? "SUCCESS" : "FAIL";

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                String.format("Encryption test: %s.\nSignature test: %s", encryptTestResult, signTestResult),
                ButtonType.OK);
        alert.showAndWait();
    }
}
