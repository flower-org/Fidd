package com.fidd.cryptor.forms;

import com.flower.crypt.HexTool;
import com.flower.crypt.keys.Aes256KeyContext;
import com.flower.crypt.keys.KeyContext;
import com.flower.crypt.keys.RsaKeyContext;
import com.flower.crypt.keys.forms.AesRawKeyProvider;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.fidd.cryptor.transform.AesTransformerProvider;
import com.fidd.cryptor.transform.CertificateVerifier;
import com.fidd.cryptor.transform.CsrSigner;
import com.fidd.cryptor.transform.FileSignatureChecker;
import com.fidd.cryptor.transform.FileToByteTransformer;
import com.fidd.cryptor.transform.FileTransformer;
import com.fidd.cryptor.transform.RsaTransformerProvider;
import com.fidd.cryptor.transform.SignatureChecker;
import com.fidd.cryptor.transform.Transformer;
import com.fidd.cryptor.transform.TransformerProvider;
import com.flower.crypt.Cryptor;
import com.flower.fxutils.JavaFxUtils;
import com.flower.crypt.PkiUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static com.fidd.cryptor.transform.RsaTransformerProvider.MAX_RSA_2048_PLAINTEXT_SIZE;
import static com.fidd.cryptor.transform.RsaTransformerProvider.MAX_RSA_2048_CIPHERTEXT_SIZE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.fidd.cryptor.transform.RsaTransformerProvider.Mode;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    final static String KEY_NOT_SUPPORTED = "Selected key(s) not supported";

    @Nullable Stage mainStage;

    @FXML @Nullable AnchorPane topPane;

    @FXML @Nullable TextArea plaintextTextArea;
    @FXML @Nullable TextArea ciphertextTextArea;

    @FXML @Nullable TextArea signPlaintextTextArea;
    @FXML @Nullable TextArea signSignatureTextArea;

    @FXML @Nullable Tab generatorTab;

    @FXML @Nullable TextField generatedAes256KeyTextField;
    @FXML @Nullable TextField generatedAes256IvTextField;
    @FXML @Nullable TextArea generatedCertificateTextArea;
    @FXML @Nullable TextArea generatedPrivateKeyTextArea;

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

    @FXML @Nullable TextArea rsaSignHashFileTextArea;

    @Nullable TabKeyProvider keyProvider;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Cryptor v 0.0.11", ButtonType.OK);
        alert.showAndWait();
    }

    public void init(Stage mainStage) {
        this.mainStage = mainStage;

        keyProvider = buildMainKeyProvider(mainStage);
        AnchorPane keyProviderForm = keyProvider.tabContent();
        checkNotNull(topPane).getChildren().add(keyProviderForm);
        AnchorPane.setTopAnchor(keyProviderForm, 0.0);
        AnchorPane.setBottomAnchor(keyProviderForm, 0.0);
        AnchorPane.setLeftAnchor(keyProviderForm, 0.0);
        AnchorPane.setRightAnchor(keyProviderForm, 0.0);

        keyProvider.initPreferences();

        initBase64CheckBoxes();
    }

    private static TabKeyProvider buildMainKeyProvider(Stage mainStage) {
        RsaPkcs11KeyProvider rsaPkcs11KeyProvider = new RsaPkcs11KeyProvider(mainStage);
        RsaFileKeyProvider rsaFileKeyProvider = new RsaFileKeyProvider(mainStage);
        RsaRawKeyProvider rsaRawKeyProvider = new RsaRawKeyProvider();
        MultiKeyProvider rsaTabPane = new MultiKeyProvider(mainStage, "RSA-2048",
                List.of(rsaPkcs11KeyProvider, rsaFileKeyProvider, rsaRawKeyProvider));

        AesRawKeyProvider aesRawKeyProvider = new AesRawKeyProvider();
        return new MultiKeyProvider(mainStage, "",
                List.of(rsaTabPane, aesRawKeyProvider));
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

    protected TransformerProvider getCurrentTransformerProvider() {
        KeyContext keyContext = checkNotNull(keyProvider).getKeyContext();
        return getCurrentTransformerProvider(keyContext);
    }

    protected TransformerProvider getCurrentTransformerProvider(KeyContext keyContext) {
        if (keyContext instanceof RsaKeyContext) {
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
            X509Certificate certificate = ((RsaKeyContext) keyContext).certificate();
            PrivateKey key = ((RsaKeyContext) keyContext).privateKey();

            Mode mode = encryptWithPublicKey ? Mode.PUBLIC_KEY_ENCRYPT : Mode.PRIVATE_KEY_ENCRYPT;
            RsaTransformerProvider.EncryptionFormat encryptionFormat = useRsaAesHybrid
                    ? RsaTransformerProvider.EncryptionFormat.RSA_AES_256_HYBRID : RsaTransformerProvider.EncryptionFormat.RSA;
            return new RsaTransformerProvider(certificate.getPublicKey(), key, certificate, mode, encryptionFormat);
        } else if (keyContext instanceof Aes256KeyContext) {
            byte[] aes256Key = ((Aes256KeyContext) keyContext).aes256Key();
            byte[] aes256Iv = ((Aes256KeyContext) keyContext).aes256Iv();
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
            String hexEncodedKey = HexTool.bytesToHex(key);
            checkNotNull(generatedAes256KeyTextField).textProperty().set(hexEncodedKey);
        } catch (Exception e) {
            LOGGER.error("AES-256 key generation error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void generateAes256Iv() {
        try {
            byte[] iv = Cryptor.generateAESIV();
            String hexEncodedIv = HexTool.bytesToHex(iv);
            checkNotNull(generatedAes256IvTextField).textProperty().set(hexEncodedIv);
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
                if (isRsaPureFileCrypt() && plainFile.length() > MAX_RSA_2048_PLAINTEXT_SIZE) {
                    if (JavaFxUtils.YesNo.NO == JavaFxUtils.showYesNoDialog(
                            "Pure RSA-2048 plaintext size can't exceed " + MAX_RSA_2048_PLAINTEXT_SIZE +
                                    "b; got " + plainFile.length() + "b. Proceed anyway?")) {
                        return;
                    }
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

    protected boolean isRsaPureFileCrypt() {
        return !checkNotNull(rsaFileUseRsaAesHybridCheckBox).selectedProperty().get();
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
                if (isRsaPureFileCrypt() && cipherFile.length() > MAX_RSA_2048_CIPHERTEXT_SIZE) {
                    if (JavaFxUtils.YesNo.NO == JavaFxUtils.showYesNoDialog(
                            "Pure RSA-2048 ciphertext size can't exceed " + MAX_RSA_2048_CIPHERTEXT_SIZE +
                                    "b; got " + cipherFile.length() + "b. Proceed anyway?")) {
                        return;
                    }
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
            if (!cipherFile.exists() || cipherFile.getPath().equals(checkNotNull(rsaCryptPlainFileTextField).textProperty().get())) {
                if (!cipherFile.getName().endsWith(".crp")) {
                    cipherFile = new File(cipherFile.getPath()  + ".crp");
                }
            }

            checkNotNull(rsaCryptCipherFileTextField).textProperty().set(cipherFile.getPath());
        } catch (Exception e) {
            LOGGER.error("Cipher file open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    // ------------------------------------------------------

    public void signFile() {
        try {
            FileToByteTransformer signFileTransformer = getCurrentTransformerProvider().getSignFileTransformer();
            if (signFileTransformer != null) {
                String plainFileName = checkNotNull(rsaSignPlainFileTextField).textProperty().get();
                if (StringUtils.isBlank(plainFileName)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File plainFile = new File(plainFileName);
                if (!plainFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file doesn't exist", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String signFileName = checkNotNull(rsaSignHashFileTextField).textProperty().get();
                File signFile = null;
                if (!StringUtils.isBlank(signFileName)) {
                    signFile = new File(signFileName);
                }

                if (signFile != null) {
                    JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Sign will overwrite Sign/Hash file. Continue?");
                    if (result == JavaFxUtils.YesNo.NO) { return; }
                }

                byte[] signature = signFileTransformer.transform(plainFile);
                if (signFile != null) {
                    Files.write(signFile.toPath(), signature);
                }
                String signature64 = Base64.getEncoder().encodeToString(signature);
                checkNotNull(rsaSignHashFileTextArea).textProperty().set(signature64);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("File signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void checkFileSignature() {
        try {
            FileSignatureChecker fileSignatureChecker = getCurrentTransformerProvider().getFileSignatureChecker();

            if (fileSignatureChecker != null) {
                String plainFileName = checkNotNull(rsaSignPlainFileTextField).textProperty().get();
                if (StringUtils.isBlank(plainFileName)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file not selected", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                File plainFile = new File(plainFileName);
                if (!plainFile.exists()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file doesn't exist", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }

                String signFileName = checkNotNull(rsaSignHashFileTextField).textProperty().get();
                File signFile = new File(signFileName);
                if (signFile.exists()) {
                    if (fileSignatureChecker.checkSignature(plainFile, signFile)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Signature File OK", ButtonType.OK);
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Signature File INVALID", ButtonType.OK);
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Signature File not found", ButtonType.OK);
                    alert.showAndWait();
                }

                String signature = checkNotNull(rsaSignHashFileTextArea).textProperty().get();
                if (!StringUtils.isBlank(signature)) {
                    byte[] signatureBytes = Base64.getDecoder().decode(signature);
                    if (fileSignatureChecker.checkSignature(plainFile, signatureBytes)) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Text Signature OK", ButtonType.OK);
                        alert.showAndWait();
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Text Signature INVALID", ButtonType.OK);
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Text Signature empty", ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, KEY_NOT_SUPPORTED, ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Signature check error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void sha256File() {
        try {
            String plainFileName = checkNotNull(rsaSignPlainFileTextField).textProperty().get();
            if (StringUtils.isBlank(plainFileName)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file not selected", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            File plainFile = new File(plainFileName);
            if (!plainFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plain file doesn't exist", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            String sha256FileName = checkNotNull(rsaSignHashFileTextField).textProperty().get();
            File sha256File = null;
            if (!StringUtils.isBlank(sha256FileName)) {
                sha256File = new File(sha256FileName);
            }

            if (sha256File != null) {
                JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("SHA-256 will overwrite Sign/Hash file. Continue?");
                if (result == JavaFxUtils.YesNo.NO) { return; }
            }

            byte[] hash = PkiUtil.getSha256(plainFile);
            if (sha256File != null) {
                Files.write(sha256File.toPath(), hash);
            }
            String hexHash = HexTool.bytesToHex(hash);
            checkNotNull(rsaSignHashFileTextArea).textProperty().set(hexHash);
        } catch (Exception e) {
            LOGGER.error("File signage error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
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
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Detached Signature (*.sign, *.sig, *.bin)", "*.sign", "*.sig", "*.bin"));
            fileChooser.setTitle("Save signature to file");

            File signatureFile = fileChooser.showSaveDialog(mainStage);
            if (signatureFile == null) { return; }

            if (!signatureFile.getName().endsWith(".bin") && !signatureFile.getName().endsWith(".sig") && !signatureFile.getName().endsWith(".sign")) {
                signatureFile = new File(signatureFile.getPath()  + ".sign");
            }

            checkNotNull(rsaSignHashFileTextField).textProperty().set(signatureFile.getPath());

            if (signatureFile.exists()) {
                byte[] signature = Files.readAllBytes(signatureFile.toPath());
                String signature64 = Base64.getEncoder().encodeToString(signature);
                checkNotNull(rsaSignHashFileTextArea).textProperty().set(signature64);
            }
        } catch (Exception e) {
            LOGGER.error("Signature file open error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}
