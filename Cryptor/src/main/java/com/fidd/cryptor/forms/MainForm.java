package com.fidd.cryptor.forms;

import com.fidd.cryptor.transform.AesTransformerProvider;
import com.fidd.cryptor.transform.RsaTransformerProvider;
import com.fidd.cryptor.transform.SignatureChecker;
import com.fidd.cryptor.transform.Transformer;
import com.fidd.cryptor.transform.TransformerProvider;
import com.fidd.cryptor.utils.CertificateChooserUserPreferencesManager;
import com.fidd.cryptor.utils.Cryptor;
import com.fidd.cryptor.utils.PkiUtil;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.fidd.cryptor.transform.RsaTransformerProvider.Mode;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    final static String KEY_NOT_SUPPORTED = "Selected key(s) not supported";

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

    @FXML @Nullable TextArea rawCertificateTextArea;
    @FXML @Nullable TextArea rawPrivateKeyTextArea;

    @FXML @Nullable TextField fileCertificateTextField;
    @FXML @Nullable TextField filePrivateKeyTextField;

    @FXML @Nullable CheckBox rsaEncryptWithPublicKeyCheckBox;
    @FXML @Nullable CheckBox rsaUseRsaAesHybridCheckBox;

    @Nullable KeyStore pkcs11KeyStore;
    @Nullable Certificate fileCertificate;
    @Nullable PrivateKey fileKey;

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

    public void init() {
        loadCertificateChooserPreferences();
        setCertificateChooserPreferencesHandlers();
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void loadPkcs11() {
        try {
            String pkcs11Lib = checkNotNull(pkcs11LibTextField).textProperty().get();
            String pkcs11TokenPin = checkNotNull(pkcs11TokenPinTextField).textProperty().get();

            if (StringUtils.isBlank(pkcs11Lib)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "PKCS#11 Library Path is empty", ButtonType.OK);
                alert.showAndWait();
                return;
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

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "PKCS#11 successfully loaded", ButtonType.OK);
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.error("Error loading PKCS#11", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void setCertificateChooserPreferencesHandlers() {
        checkNotNull(pkcs11LibTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(aes256KeyTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(aes256IvTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(fileCertificateTextField).textProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(filePrivateKeyTextField).textProperty().addListener(this::certificateChooserTextChanged);

        checkNotNull(rawCertificateTextArea).textProperty().addListener(this::certificateChooserTextChanged);;
        checkNotNull(rawPrivateKeyTextArea).textProperty().addListener(this::certificateChooserTextChanged);

        checkNotNull(certificatesComboBox).valueProperty().addListener(this::certificateChooserTextChanged);
        checkNotNull(privateKeysComboBox).valueProperty().addListener(this::certificateChooserTextChanged);

        checkNotNull(aes256IvCheckBox).selectedProperty().addListener(this::certificateChooserBoolChanged);
    }

    public void certificateChooserTextChanged(ObservableValue<? extends String> observable, String _old, String _new) {
        updateCertificateChooserPreferences();
    }

    public void certificateChooserBoolChanged(ObservableValue<? extends Boolean> observable, Boolean _old, Boolean _new) {
        updateCertificateChooserPreferences();
    }

    public void updateCertificateChooserPreferences() {
        String pkcs11LibraryPath = checkNotNull(pkcs11LibTextField).textProperty().get();
        String pkcs11CertificateAlias = checkNotNull(certificatesComboBox).valueProperty().get();
        String pkcs11PrivateKeyAlias = checkNotNull(privateKeysComboBox).valueProperty().get();

        String fileCertificate = checkNotNull(fileCertificateTextField).textProperty().get();
        String filePrivateKey = checkNotNull(filePrivateKeyTextField).textProperty().get();

        String rawCertificate = checkNotNull(rawCertificateTextArea).textProperty().get();
        String rawPrivateKey = checkNotNull(rawPrivateKeyTextArea).textProperty().get();

        String aesKey = checkNotNull(aes256KeyTextField).textProperty().get();
        String aesIvCheckbox = checkNotNull(aes256IvTextField).textProperty().get();
        boolean aesIv = checkNotNull(aes256IvCheckBox).selectedProperty().get();

        CertificateChooserUserPreferencesManager.updateUserPreferences(pkcs11LibraryPath, pkcs11CertificateAlias,
                    pkcs11PrivateKeyAlias,
                    fileCertificate, filePrivateKey,
                    rawCertificate, rawPrivateKey,
                    aesKey, aesIvCheckbox, Boolean.toString(aesIv));
    }

    public void loadCertificateChooserPreferences() {
        checkNotNull(pkcs11LibTextField).textProperty().set(CertificateChooserUserPreferencesManager.pkcs11LibraryPath());
        checkNotNull(certificatesComboBox).getSelectionModel().select(CertificateChooserUserPreferencesManager.pkcs11CertificateAlias());
        checkNotNull(privateKeysComboBox).getSelectionModel().select(CertificateChooserUserPreferencesManager.pkcs11PrivateKeyAlias());

        checkNotNull(fileCertificateTextField).textProperty().set(CertificateChooserUserPreferencesManager.fileCertificate());
        checkNotNull(filePrivateKeyTextField).textProperty().set(CertificateChooserUserPreferencesManager.filePrivateKey());

        checkNotNull(rawCertificateTextArea).textProperty().set(CertificateChooserUserPreferencesManager.rawCertificate());
        checkNotNull(rawPrivateKeyTextArea).textProperty().set(CertificateChooserUserPreferencesManager.rawPrivateKey());

        checkNotNull(aes256KeyTextField).textProperty().set(CertificateChooserUserPreferencesManager.aesKey());
        String ivCheckboxStr = CertificateChooserUserPreferencesManager.aesIvCheckbox();
        boolean ivCheckbox = StringUtils.isBlank(ivCheckboxStr) ? false : Boolean.parseBoolean(ivCheckboxStr);
        checkNotNull(aes256IvCheckBox).selectedProperty().set(ivCheckbox);
        checkNotNull(aes256IvTextField).textProperty().set(CertificateChooserUserPreferencesManager.aesIv());
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
                Mode mode = checkNotNull(rsaEncryptWithPublicKeyCheckBox).isSelected()
                        ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                RsaTransformerProvider.EncryptionFormat encryptionFormat = checkNotNull(rsaUseRsaAesHybridCheckBox).isSelected()
                        ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                return new RsaTransformerProvider(certificate.getPublicKey(), key, mode, encryptionFormat);
            } else if (selectedRsaTab == filesTab) {
                try {
                    if (fileCertificate == null) {
                        throw new RuntimeException("Certificate not loaded");
                    }
                    if (fileKey == null) {
                        throw new RuntimeException("Key not loaded");
                    }
                    Mode mode = checkNotNull(rsaEncryptWithPublicKeyCheckBox).isSelected()
                            ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                    RsaTransformerProvider.EncryptionFormat encryptionFormat = checkNotNull(rsaUseRsaAesHybridCheckBox).isSelected()
                            ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                    return new RsaTransformerProvider(fileCertificate.getPublicKey(), fileKey, mode, encryptionFormat);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (selectedRsaTab == rawTab) {
                try {
                    String certificateStr = checkNotNull(rawCertificateTextArea).textProperty().get();
                    String keyStr = checkNotNull(rawPrivateKeyTextArea).textProperty().get();
                    Certificate certificate = PkiUtil.getCertificateFromString(certificateStr);
                    PrivateKey key = PkiUtil.getPrivateKeyFromString(keyStr);

                    Mode mode = checkNotNull(rsaEncryptWithPublicKeyCheckBox).isSelected()
                            ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                    RsaTransformerProvider.EncryptionFormat encryptionFormat = checkNotNull(rsaUseRsaAesHybridCheckBox).isSelected()
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
            if (checkNotNull(aes256IvCheckBox).selectedProperty().get()) {
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
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
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
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
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
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
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
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
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

    public void openCertificateFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Certificate (*.crt)", "*.crt"));
            fileChooser.setTitle("Load Certificate");
            File certificateFile = fileChooser.showOpenDialog(checkNotNull(mainStage));
            checkNotNull(fileCertificateTextField).textProperty().set(certificateFile.getPath());

            loadCertificateFromFile(certificateFile);
        } catch (Exception e) {
            LOGGER.error("Error opening certificate file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void loadCertificateFileKey(KeyEvent event) {
        try {
            if (event.getCode() == KeyCode.ENTER) {
                loadCertificateFile();
            }
        } catch (Exception e) {
            LOGGER.error("Error loading certificate from file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void loadCertificateFile() {
        try {
            File certificateFile = new File(checkNotNull(fileCertificateTextField).textProperty().get());
            loadCertificateFromFile(certificateFile);
        } catch (Exception e) {
            LOGGER.error("Error loading certificate from file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    protected void loadCertificateFromFile(File certificateFile) throws IOException {
        fileCertificate = PkiUtil.getCertificateFromStream(new FileInputStream(certificateFile));
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Certificate successfully loaded: " + certificateFile.getPath(), ButtonType.OK);
        alert.showAndWait();
    }

    public void openPrivateKeyFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Key (*.key)", "*.key"));
            fileChooser.setTitle("Load Key");
            File keyFile = fileChooser.showOpenDialog(checkNotNull(mainStage));
            checkNotNull(filePrivateKeyTextField).textProperty().set(keyFile.getPath());

            loadPrivateKeyFromFile(keyFile);
        } catch (Exception e) {
            LOGGER.error("Error opening private key file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void loadPrivateKeyFileKey(KeyEvent event) {
        try {
            if (event.getCode() == KeyCode.ENTER) {
                loadPrivateKeyFile();
            }
        } catch (Exception e) {
            LOGGER.error("Error loading private key from file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void loadPrivateKeyFile() {
        try {
            File keyFile = new File(checkNotNull(filePrivateKeyTextField).textProperty().get());
            loadPrivateKeyFromFile(keyFile);
        } catch (Exception e) {
            LOGGER.error("Error loading private key from file", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    protected void loadPrivateKeyFromFile(File keyFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        fileKey = PkiUtil.getPrivateKeyFromStream(new FileInputStream(keyFile));
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Key successfully loaded: " + keyFile.getPath(), ButtonType.OK);
        alert.showAndWait();
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

    public void testFileKeys() {
        try {
            if (fileCertificate == null) {
                throw new RuntimeException("Certificate not loaded");
            }
            if (fileKey == null) {
                throw new RuntimeException("Key not loaded");
            }
            testKeys(fileCertificate, fileKey);
        } catch (Exception e) {
            LOGGER.error("File keys test error", e);
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
