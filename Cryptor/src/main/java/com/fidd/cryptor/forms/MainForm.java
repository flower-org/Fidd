package com.fidd.cryptor.forms;

import com.fidd.cryptor.transform.AesTransformerProvider;
import com.fidd.cryptor.transform.CertificateVerifier;
import com.fidd.cryptor.transform.CsrSigner;
import com.fidd.cryptor.transform.FileTransformer;
import com.fidd.cryptor.transform.RsaTransformerProvider;
import com.fidd.cryptor.transform.SignatureChecker;
import com.fidd.cryptor.transform.Transformer;
import com.fidd.cryptor.transform.TransformerProvider;
import com.fidd.cryptor.utils.CertificateChooserUserPreferencesManager;
import com.fidd.cryptor.utils.Cryptor;
import com.fidd.cryptor.utils.JavaFxUtils;
import com.fidd.cryptor.utils.PkiUtil;
import javafx.beans.value.ChangeListener;
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
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    @FXML @Nullable CheckBox base64CryptPlaintextCheckBox;
    @FXML @Nullable CheckBox base64SignPlaintextCheckBox;

    @FXML @Nullable TextArea csrSignPlaintextTextArea;
    @FXML @Nullable TextArea signedCertificateTextArea;

    @FXML @Nullable TextField rsaCryptPlainFileTextField;
    @FXML @Nullable TextField rsaCryptCipherFileTextField;
    @FXML @Nullable CheckBox rsaFileEncryptWithPublicKeyCheckBox;
    @FXML @Nullable CheckBox rsaFileUseRsaAesHybridCheckBox;

    @FXML @Nullable TextField rsaSignHashFileTextField;
    @FXML @Nullable TextField rsaSignPlainFileTextField;

    @FXML @Nullable TabPane functionTabPane;
    @FXML @Nullable Tab cryptTextFunctionTab;
    @FXML @Nullable Tab cryptFileFunctionTab;

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
        initBase64CheckBoxes();
    }

    public void initBase64CheckBoxes() {
        checkNotNull(base64CryptPlaintextCheckBox).selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean _old, Boolean _new) {
                try {
                    boolean selected = checkNotNull(base64CryptPlaintextCheckBox).selectedProperty().get();
                    if (selected) {
                        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Base64 Encode plaintext?");
                        if (result == JavaFxUtils.YesNo.YES) {
                            String text = checkNotNull(plaintextTextArea).textProperty().get();
                            String text64 = Base64.getEncoder().encodeToString(text.getBytes());
                            checkNotNull(plaintextTextArea).textProperty().set(text64);
                        }
                    } else {
                        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Base64 Decode plaintext?");
                        if (result == JavaFxUtils.YesNo.YES) {
                            String text64 = checkNotNull(plaintextTextArea).textProperty().get();
                            String text = new String(Base64.getDecoder().decode(text64));
                            checkNotNull(plaintextTextArea).textProperty().set(text);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Base64 operation error", e);
                    Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });

        checkNotNull(base64SignPlaintextCheckBox).selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean _old, Boolean _new) {
                try {
                    boolean selected = checkNotNull(base64SignPlaintextCheckBox).selectedProperty().get();
                    if (selected) {
                        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Base64 Encode plaintext?");
                        if (result == JavaFxUtils.YesNo.YES) {
                            String text = checkNotNull(signPlaintextTextArea).textProperty().get();
                            String text64 = Base64.getEncoder().encodeToString(text.getBytes());
                            checkNotNull(signPlaintextTextArea).textProperty().set(text64);
                        }
                    } else {
                        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Base64 Decode plaintext?");
                        if (result == JavaFxUtils.YesNo.YES) {
                            String text64 = checkNotNull(signPlaintextTextArea).textProperty().get();
                            String text = new String(Base64.getDecoder().decode(text64));
                            checkNotNull(signPlaintextTextArea).textProperty().set(text);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Base64 operation error", e);
                    Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });
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
            Tab selectedFunctionTab = checkNotNull(functionTabPane).getSelectionModel().getSelectedItem();

            boolean encryptWithPublicKey = true;
            boolean useRsaAesHybrid = true;
            if (selectedFunctionTab == cryptTextFunctionTab) {
                encryptWithPublicKey = checkNotNull(rsaEncryptWithPublicKeyCheckBox).isSelected();
                useRsaAesHybrid = checkNotNull(rsaUseRsaAesHybridCheckBox).isSelected();
            } else if (selectedFunctionTab == cryptFileFunctionTab) {
                encryptWithPublicKey = checkNotNull(rsaFileEncryptWithPublicKeyCheckBox).isSelected();
                useRsaAesHybrid = checkNotNull(rsaFileUseRsaAesHybridCheckBox).isSelected();
            }

            Tab selectedRsaTab = checkNotNull(rsaTabPane).getSelectionModel().getSelectedItem();
            if (selectedRsaTab == pkcs11Tab) {
                String certAlias = checkNotNull(certificatesComboBox).getSelectionModel().getSelectedItem();
                String keyAlias = checkNotNull(privateKeysComboBox).getSelectionModel().getSelectedItem();

                if (pkcs11KeyStore == null) {
                    throw new RuntimeException("PKCS#11 store not loaded");
                }
                Certificate certificate = PkiUtil.getCertificateFromKeyStore(checkNotNull(pkcs11KeyStore), certAlias);
                PrivateKey key = (PrivateKey)PkiUtil.getKeyFromKeyStore(pkcs11KeyStore, keyAlias);
                Mode mode = encryptWithPublicKey
                        ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                RsaTransformerProvider.EncryptionFormat encryptionFormat = useRsaAesHybrid
                        ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                return new RsaTransformerProvider(certificate.getPublicKey(), key, (X509Certificate)certificate, mode, encryptionFormat);
            } else if (selectedRsaTab == filesTab) {
                try {
                    if (fileCertificate == null) {
                        throw new RuntimeException("Certificate not loaded");
                    }
                    if (fileKey == null) {
                        throw new RuntimeException("Key not loaded");
                    }
                    Mode mode = encryptWithPublicKey
                            ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                    RsaTransformerProvider.EncryptionFormat encryptionFormat = useRsaAesHybrid
                            ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                    return new RsaTransformerProvider(fileCertificate.getPublicKey(), fileKey, (X509Certificate)fileCertificate, mode, encryptionFormat);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else if (selectedRsaTab == rawTab) {
                try {
                    String certificateStr = checkNotNull(rawCertificateTextArea).textProperty().get();
                    String keyStr = checkNotNull(rawPrivateKeyTextArea).textProperty().get();
                    X509Certificate certificate = PkiUtil.getCertificateFromString(certificateStr);
                    PrivateKey key = PkiUtil.getPrivateKeyFromString(keyStr);

                    Mode mode = encryptWithPublicKey
                            ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
                    RsaTransformerProvider.EncryptionFormat encryptionFormat = useRsaAesHybrid
                            ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
                    return new RsaTransformerProvider(certificate.getPublicKey(), key, certificate, mode, encryptionFormat);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Unknown RSA mode
                return TransformerProvider.of(null, null, null, null,
                        null, null, null, null,
                        null, null);
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
            return TransformerProvider.of(null, null, null, null,
                    null, null, null, null,
                    null, null);
        }
    }

    public void encryptText() {
        try {
            Transformer encryptTransformer = getCurrentTransformerProvider().getEncryptTransformer();

            if (encryptTransformer != null) {
                byte[] plaintextBytes = getCryptPlaintextBytes();
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
                if (checkNotNull(base64CryptPlaintextCheckBox).selectedProperty().get()) {
                    plaintext = Base64.getEncoder().encodeToString(plaintext.getBytes());
                }

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

    protected byte[] getCryptPlaintextBytes() {
        String plaintext = checkNotNull(plaintextTextArea).textProperty().get();
        byte[] plaintextBytes;
        if (checkNotNull(base64CryptPlaintextCheckBox).selectedProperty().get()) {
            plaintextBytes = Base64.getDecoder().decode(plaintext);
        } else {
            plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        }
        return plaintextBytes;
    }

    protected byte[] getSignPlaintextBytes() {
        String plaintext = checkNotNull(signPlaintextTextArea).textProperty().get();
        byte[] plaintextBytes;
        if (checkNotNull(base64SignPlaintextCheckBox).selectedProperty().get()) {
            plaintextBytes = Base64.getDecoder().decode(plaintext);
        } else {
            plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        }
        return plaintextBytes;
    }

    public void sha256Text() {
        try {
            byte[] plaintextBytes = getSignPlaintextBytes();
            String sha256Hex = PkiUtil.getSha256Hex(plaintextBytes);
            checkNotNull(signSignatureTextArea).textProperty().set(sha256Hex);
        } catch (Exception e) {
            LOGGER.error("Signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void signText() {
        try {
            Transformer signTransformer = getCurrentTransformerProvider().getSignTransformer();

            if (signTransformer != null) {
                byte[] plaintextBytes = getSignPlaintextBytes();
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
                byte[] plaintextBytes = getSignPlaintextBytes();
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

    public void saveCryptPlaintext() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
            fileChooser.setTitle("Save Crypt Plaintext");
            File saveFile = fileChooser.showSaveDialog(checkNotNull(mainStage));
            if (saveFile == null) { return; }

            if (!saveFile.getName().endsWith(".txt")) {
                saveFile = new File(saveFile.getPath()  + ".txt");
            }

            byte[] plaintextBytes = getCryptPlaintextBytes();
            saveByteArrayToFile(plaintextBytes, saveFile);
        } catch (Exception e) {
            LOGGER.error("Encryption error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    protected void saveByteArrayToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    public void saveSignPlaintext() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
            fileChooser.setTitle("Save Sign Plaintext");
            File saveFile = fileChooser.showSaveDialog(checkNotNull(mainStage));
            if (saveFile == null) { return; }

            if (!saveFile.getName().endsWith(".txt")) {
                saveFile = new File(saveFile.getPath()  + ".txt");
            }

            byte[] plaintextBytes = getSignPlaintextBytes();
            saveByteArrayToFile(plaintextBytes, saveFile);
        } catch (Exception e) {
            LOGGER.error("Save sign plaintext error", e);
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
            if (certificateFile == null) { return; }

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
            if (keyFile == null) { return; }

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

    public void loadCsr() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("CSR request (*.req)", "*.req"));
            fileChooser.setTitle("Load CSR request");
            File csrFile = fileChooser.showOpenDialog(mainStage);
            if (csrFile == null) { return; }

            String csr = Files.readString(csrFile.toPath());
            PKCS10CertificationRequest pkcs10 = PkiUtil.loadCsr(csr);

            checkNotNull(csrSignPlaintextTextArea).textProperty().set(csr);
        } catch (Exception e) {
            LOGGER.error("CSR load error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void loadSignedCertificate() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Certificate (*.crt)", "*.crt"));
            fileChooser.setTitle("Load certificate");
            File csrFile = fileChooser.showOpenDialog(mainStage);
            if (csrFile == null) { return; }

            PkiUtil.getCertificateFromStream(new FileInputStream(csrFile));

            String csr = Files.readString(csrFile.toPath());
            checkNotNull(signedCertificateTextArea).textProperty().set(csr);
        } catch (Exception e) {
            LOGGER.error("Certificate load error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void signCsr() {
        try {
            CsrSigner csrSigner = getCurrentTransformerProvider().getCsrSigner();
            if (csrSigner != null) {
                String csrString = checkNotNull(csrSignPlaintextTextArea).textProperty().get();
                if (StringUtils.isBlank(csrString)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "CSR is empty", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                PKCS10CertificationRequest csr = PkiUtil.loadCsr(csrString);
                X509Certificate certificate = csrSigner.signCsr(csr);

                String certificateStr = PkiUtil.getCertificateAsPem(certificate);
                checkNotNull(signedCertificateTextArea).textProperty().set(certificateStr);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("CSR signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void checkSignedCertificate() {
        try {
            CertificateVerifier certificateVerifier = getCurrentTransformerProvider().getCertificateVerifier();
            if (certificateVerifier != null) {
                String signedCertificateString = checkNotNull(signedCertificateTextArea).textProperty().get();
                if (!StringUtils.isBlank(signedCertificateString)) {
                    X509Certificate certificate = PkiUtil.getCertificateFromString(signedCertificateString);
                    if (certificateVerifier.verifyCertificateSignature(certificate)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Certificate valid", ButtonType.OK);
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Certificate validation failed", ButtonType.OK);
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Certificate is empty", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("CSR check error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void saveSignedCertificate() {
        try {
            String signedCertificateString = checkNotNull(signedCertificateTextArea).textProperty().get();
            if (!StringUtils.isBlank(signedCertificateString)) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Certificate files (*.crt)", "*.crt"));
                fileChooser.setTitle("Save signed certificate");
                File saveFile = fileChooser.showSaveDialog(checkNotNull(mainStage));
                if (saveFile == null) { return; }

                if (!saveFile.getName().endsWith(".crt")) {
                    saveFile = new File(saveFile.getPath()  + ".crt");
                }

                byte[] certBytes = signedCertificateString.getBytes();
                saveByteArrayToFile(certBytes, saveFile);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Certificate is empty", ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("CSR save error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    // ------------------------------------------------------

    public void encryptFile() {
        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Encrypt will overwrite Cipher file. Continue?");
        if (result == JavaFxUtils.YesNo.NO) { return; }

        try {
            FileTransformer encryptTransformer = getCurrentTransformerProvider().getEncryptFileTransformer();
            if (encryptTransformer != null) {
                String plainFileStr = checkNotNull(rsaCryptPlainFileTextField).textProperty().get();
                if (StringUtils.isBlank(plainFileStr)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File plainFile = new File(plainFileStr);
                if (!plainFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file doesn't exist", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String cipherFileStr = checkNotNull(rsaCryptCipherFileTextField).textProperty().get();
                if (StringUtils.isBlank(cipherFileStr)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cipher file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File cipherFile = new File(cipherFileStr);
                encryptTransformer.transform(plainFile, cipherFile);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File encryption success", ButtonType.OK);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("File encrypt error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void decryptFile() {
        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Decrypt will overwrite Plain file. Continue?");
        if (result == JavaFxUtils.YesNo.NO) { return; }

        try {
            FileTransformer decryptTransformer = getCurrentTransformerProvider().getDecryptFileTransformer();
            if (decryptTransformer != null) {
                String cipherFileStr = checkNotNull(rsaCryptCipherFileTextField).textProperty().get();
                if (StringUtils.isBlank(cipherFileStr)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cipher file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File cipherFile = new File(cipherFileStr);
                if (!cipherFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cipher file doesn't exist", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String plainFileStr = checkNotNull(rsaCryptPlainFileTextField).textProperty().get();
                if (StringUtils.isBlank(plainFileStr)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File plainFile = new File(plainFileStr);

                decryptTransformer.transform(cipherFile, plainFile);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "File decryption success", ButtonType.OK);
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("File decrypt error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void openCryptPlainFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Any file (*.*)", "*.*"));
            fileChooser.setTitle("Choose plain file");

            File plainFile = fileChooser.showSaveDialog(mainStage);
            if (plainFile == null) { return; }

            checkNotNull(rsaCryptPlainFileTextField).textProperty().set(plainFile.getPath());
        } catch (Exception e) {
            LOGGER.error("Plain file open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void openCryptCipherFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Any file (*.*)", "*.*"));
            fileChooser.setTitle("Choose cipher file");

            File cipherFile = fileChooser.showSaveDialog(mainStage);
            if (cipherFile == null) { return; }

            checkNotNull(rsaCryptCipherFileTextField).textProperty().set(cipherFile.getPath());
        } catch (Exception e) {
            LOGGER.error("Cipher file open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    // ------------------------------------------------------

    public void signFile() {
        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Sign will overwrite Sign/Hash file. Continue?");
        if (result == JavaFxUtils.YesNo.NO) { return; }
    }

    public void checkFileSignature() {
        //
    }

    public void sha256File() {
        JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("SHA-256 will overwrite Sign/Hash file. Continue?");
        if (result == JavaFxUtils.YesNo.NO) { return; }
    }

    public void openSignPlainFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Any file (*.*)", "*.*"));
            fileChooser.setTitle("Open file to sign");

            File fileToSign = fileChooser.showOpenDialog(mainStage);
            if (fileToSign == null) { return; }

            checkNotNull(rsaSignPlainFileTextField).textProperty().set(fileToSign.getPath());
        } catch (Exception e) {
            LOGGER.error("File to sign open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void openSignHashFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Detached Signature (*.bin, *.sig)", "*.bin", "*.sig"));
            fileChooser.setTitle("Save signature to file");

            File signatureFile = fileChooser.showSaveDialog(mainStage);
            if (signatureFile == null) { return; }

            if (!signatureFile.getName().endsWith(".bin") && !signatureFile.getName().endsWith(".sig")) {
                signatureFile = new File(signatureFile.getPath()  + ".bin");
            }

            checkNotNull(rsaSignHashFileTextField).textProperty().set(signatureFile.getPath());
        } catch (Exception e) {
            LOGGER.error("Signature file open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}
