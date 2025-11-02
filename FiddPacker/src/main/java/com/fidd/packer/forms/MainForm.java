package com.fidd.packer.forms;

import com.flower.crypt.Cryptor;
import com.flower.crypt.HexTool;
import com.flower.crypt.PkiUtil;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    @Nullable Stage mainStage;

    @FXML @Nullable AnchorPane topPane;

    @Nullable TabKeyProvider keyProvider;

    @FXML @Nullable TextField generatedAes256KeyTextField;
    @FXML @Nullable TextField generatedAes256IvTextField;
    @FXML @Nullable TextArea generatedCertificateTextArea;
    @FXML @Nullable TextArea generatedPrivateKeyTextArea;

    @FXML @Nullable TextField originalFolderTextField;
    @FXML @Nullable TextField packedContentFolderTextField;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "FiddPacker v 0.0.1", ButtonType.OK);
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
    }

    private static TabKeyProvider buildMainKeyProvider(Stage mainStage) {
        RsaPkcs11KeyProvider rsaPkcs11KeyProvider = new RsaPkcs11KeyProvider(mainStage);
        RsaFileKeyProvider rsaFileKeyProvider = new RsaFileKeyProvider(mainStage);
        RsaRawKeyProvider rsaRawKeyProvider = new RsaRawKeyProvider();
        MultiKeyProvider rsaTabPane = new MultiKeyProvider(mainStage, "RSA-2048",
                List.of(rsaPkcs11KeyProvider, rsaFileKeyProvider, rsaRawKeyProvider));

        //AesRawKeyProvider aesRawKeyProvider = new AesRawKeyProvider();
        return new MultiKeyProvider(mainStage, "",
                List.of(rsaTabPane/*, aesRawKeyProvider*/));
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void packFolder() {}

    public void unpackFolder() {}

    private @Nullable File chooseDirectory(@Nullable File initialDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (initialDirectory != null && initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }
        directoryChooser.setTitle("Select a Directory");

        // Open the directory chooser dialog
        return directoryChooser.showDialog(mainStage);
    }

    public void openOriginalFolder() {
        File initialDirectory = null;
        initialDirectory = new File(checkNotNull(originalFolderTextField).textProperty().get());
        File directory = chooseDirectory(initialDirectory);
        if (directory != null) {
            checkNotNull(originalFolderTextField).textProperty().set(directory.getPath());
        }
    }

    public void openPackedContentFolder() {
        File initialDirectory = null;
        initialDirectory = new File(checkNotNull(packedContentFolderTextField).textProperty().get());
        File directory = chooseDirectory(initialDirectory);
        if (directory != null) {
            checkNotNull(packedContentFolderTextField).textProperty().set(directory.getPath());
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
}
