package com.fidd.packer.forms;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.flower.crypt.Cryptor;
import com.flower.crypt.HexTool;
import com.flower.crypt.PkiUtil;
import com.flower.crypt.keys.KeyContext;
import com.flower.crypt.keys.RsaKeyContext;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.prefs.Preferences;

import static com.flower.crypt.keys.UserPreferencesManager.getUserPreference;
import static com.flower.crypt.keys.UserPreferencesManager.updateUserPreference;
import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    final static String ENCRYPTION_ALGORITHM = "ENCRYPTION_ALGORITHM";
    final static String FIDD_KEY = "FIDD_KEY";
    final static String FIDD_FILE_METADATA = "FIDD_FILE_METADATA";
    final static String LOGICAL_FILE_METADATA = "LOGICAL_FILE_METADATA";
    final static String PUBLIC_KEY_FORMAT = "PUBLIC_KEY_FORMAT";
    final static String SIGNATURE_FORMAT = "SIGNATURE_FORMAT";
    final static String RANDOM_GENERATOR = "RANDOM_GENERATOR";
    final static String ADD_AUTHOR_SIGNATURES = "ADD_AUTHOR_SIGNATURES";
    final static String MIN_GAP_SIZE_TEXT_FIELD = "MIN_GAP_SIZE_TEXT_FIELD";
    final static String MAX_GAP_SIZE_TEXT_FIELD = "MAX_GAP_SIZE_TEXT_FIELD";

    @Nullable Stage mainStage;

    @FXML @Nullable AnchorPane topPane;

    @Nullable TabKeyProvider keyProvider;

    @FXML @Nullable TextField generatedAes256KeyTextField;
    @FXML @Nullable TextField generatedAes256IvTextField;
    @FXML @Nullable TextArea generatedCertificateTextArea;
    @FXML @Nullable TextArea generatedPrivateKeyTextArea;

    @FXML @Nullable TextField originalFolderTextField;
    @FXML @Nullable TextField packedContentFolderTextField;

    @FXML @Nullable ComboBox<String> fiddKeyComboBox;
    @FXML @Nullable ComboBox<String> fiddFileMetadataComboBox;
    @FXML @Nullable ComboBox<String> logicalFileMetadataComboBox;
    @FXML @Nullable ComboBox<String> publicKeyFormatComboBox;
    @FXML @Nullable ComboBox<String> signatureFormatComboBox;
    @FXML @Nullable ComboBox<String> encryptionAlgorithmComboBox;
    @FXML @Nullable ComboBox<String> randomGeneratorComboBox;
    @FXML @Nullable CheckBox addAuthorSignaturesCheckBox;
    @FXML @Nullable TextField minGapSizeTextField;
    @FXML @Nullable TextField maxGapSizeTextField;

    BaseRepositories baseRepositories;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly

        baseRepositories = new DefaultBaseRepositories();
    }

    protected X509Certificate getCurrentCertificate() {
        KeyContext keyContext = checkNotNull(keyProvider).getKeyContext();

        if (keyContext instanceof RsaKeyContext) {
            X509Certificate certificate = ((RsaKeyContext) keyContext).certificate();
            //PrivateKey key = ((RsaKeyContext) keyContext).privateKey();
            return certificate;
        } else {
            throw new RuntimeException("Unsupported Key Context: " + keyContext.getClass().toString());
        }
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

        loadFiddPackerChooserPreferences();
        setFiddPackerPreferencesHandlers();
    }

    public void loadFiddPackerChooserPreferences() {
        initRepositoryComboBox(baseRepositories.encryptionAlgorithmRepo(), checkNotNull(encryptionAlgorithmComboBox), getUserPreference(ENCRYPTION_ALGORITHM));
        initRepositoryComboBox(baseRepositories.fiddKeyFormatRepo(), checkNotNull(fiddKeyComboBox), getUserPreference(FIDD_KEY));
        initRepositoryComboBox(baseRepositories.fiddFileMetadataFormatRepo(), checkNotNull(fiddFileMetadataComboBox), getUserPreference(FIDD_FILE_METADATA));
        initRepositoryComboBox(baseRepositories.logicalFileMetadataFormatRepo(), checkNotNull(logicalFileMetadataComboBox), getUserPreference(LOGICAL_FILE_METADATA));
        initRepositoryComboBox(baseRepositories.publicKeyFormatRepo(), checkNotNull(publicKeyFormatComboBox), getUserPreference(PUBLIC_KEY_FORMAT));
        initRepositoryComboBox(baseRepositories.signatureFormatRepo(), checkNotNull(signatureFormatComboBox), getUserPreference(SIGNATURE_FORMAT));
        initRepositoryComboBox(baseRepositories.randomGeneratorsRepo(), checkNotNull(randomGeneratorComboBox), getUserPreference(RANDOM_GENERATOR));

        String addAuthorSignaturesCheckedStr = getUserPreference(ADD_AUTHOR_SIGNATURES);
        if (!StringUtils.isBlank(addAuthorSignaturesCheckedStr)) {
            Boolean addAuthorSignaturesChecked = null;
            try {
                addAuthorSignaturesChecked = Boolean.parseBoolean(addAuthorSignaturesCheckedStr);
            } catch (Exception ignored) {}

            if (addAuthorSignaturesChecked != null) {
                checkNotNull(addAuthorSignaturesCheckBox).selectedProperty().set(addAuthorSignaturesChecked);
            }
        }

        String minGapSizeStr = getUserPreference(MIN_GAP_SIZE_TEXT_FIELD);
        if (StringUtils.isBlank(minGapSizeStr)) { minGapSizeStr = "0"; }
        checkNotNull(minGapSizeTextField).textProperty().set(minGapSizeStr);

        String maxGapSizeStr = getUserPreference(MAX_GAP_SIZE_TEXT_FIELD);
        if (StringUtils.isBlank(maxGapSizeStr)) { maxGapSizeStr = "0"; }
        checkNotNull(maxGapSizeTextField).textProperty().set(maxGapSizeStr);
    }

    public void setFiddPackerPreferencesHandlers() {
        checkNotNull(encryptionAlgorithmComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(fiddKeyComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(fiddFileMetadataComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(logicalFileMetadataComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(publicKeyFormatComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(signatureFormatComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(randomGeneratorComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(addAuthorSignaturesCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(minGapSizeTextField).textProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(maxGapSizeTextField).textProperty().addListener(this::fiddPackerTextChanged);
    }

    public void fiddPackerTextChanged(ObservableValue<? extends String> observable, String _old, String _new) {
        updateFiddPackerPreferences();
    }

    public void fiddPackerBoolChanged(ObservableValue<? extends Boolean> observable, Boolean _old, Boolean _new) {
        updateFiddPackerPreferences();
    }

    public void updateFiddPackerPreferences() {
        String encryptionAlgorithm = checkNotNull(encryptionAlgorithmComboBox).valueProperty().get();
        String fiddKey = checkNotNull(fiddKeyComboBox).valueProperty().get();
        String fiddFileMetadata = checkNotNull(fiddFileMetadataComboBox).valueProperty().get();
        String logicalFileMetadata = checkNotNull(logicalFileMetadataComboBox).valueProperty().get();
        String publicKeyFormat = checkNotNull(publicKeyFormatComboBox).valueProperty().get();
        String signatureFormat = checkNotNull(signatureFormatComboBox).valueProperty().get();
        String randomGenerator = checkNotNull(randomGeneratorComboBox).valueProperty().get();
        boolean addAuthorSignatures = checkNotNull(addAuthorSignaturesCheckBox).selectedProperty().get();
        String minGapSize = checkNotNull(minGapSizeTextField).textProperty().get();
        String maxGapSize = checkNotNull(maxGapSizeTextField).textProperty().get();

        updateFiddPackerPreferences(encryptionAlgorithm, fiddKey, fiddFileMetadata, logicalFileMetadata, publicKeyFormat,
                signatureFormat, randomGenerator, Boolean.toString(addAuthorSignatures), minGapSize, maxGapSize);

    }

    public static void updateFiddPackerPreferences(String encryptionAlgorithm, String fiddKey, String fiddFileMetadata,
                                                String logicalFileMetadata, String publicKeyFormat, String signatureFormat,
                                                String randomGenerator, String addAuthorSignatures, String minGapSize,
                                                String maxGapSize) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, ENCRYPTION_ALGORITHM, StringUtils.defaultIfBlank(encryptionAlgorithm, ""));
        updateUserPreference(userPreferences, FIDD_KEY, StringUtils.defaultIfBlank(fiddKey, ""));
        updateUserPreference(userPreferences, FIDD_FILE_METADATA, StringUtils.defaultIfBlank(fiddFileMetadata, ""));
        updateUserPreference(userPreferences, LOGICAL_FILE_METADATA, StringUtils.defaultIfBlank(logicalFileMetadata, ""));
        updateUserPreference(userPreferences, PUBLIC_KEY_FORMAT, StringUtils.defaultIfBlank(publicKeyFormat, ""));
        updateUserPreference(userPreferences, SIGNATURE_FORMAT, StringUtils.defaultIfBlank(signatureFormat, ""));
        updateUserPreference(userPreferences, RANDOM_GENERATOR, StringUtils.defaultIfBlank(randomGenerator, ""));
        updateUserPreference(userPreferences, ADD_AUTHOR_SIGNATURES, StringUtils.defaultIfBlank(addAuthorSignatures, ""));
        updateUserPreference(userPreferences, MIN_GAP_SIZE_TEXT_FIELD, StringUtils.defaultIfBlank(minGapSize, ""));
        updateUserPreference(userPreferences, MAX_GAP_SIZE_TEXT_FIELD, StringUtils.defaultIfBlank(maxGapSize, ""));
    }

    static <J> void initRepositoryComboBox(Repository<J> repo, ComboBox<String> combo, @Nullable String selected) {
        ObservableList<String> list = FXCollections.observableArrayList(repo.listEntryNames());
        combo.setItems(list);

        if (StringUtils.isBlank(selected)) {
            selected = repo.defaultKey();
        }
        if (!StringUtils.isBlank(selected)) {
            combo.getSelectionModel().select(selected);
        }
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
        File initialDirectory;
        initialDirectory = new File(checkNotNull(originalFolderTextField).textProperty().get());
        File directory = chooseDirectory(initialDirectory);
        if (directory != null) {
            checkNotNull(originalFolderTextField).textProperty().set(directory.getPath());
        }
    }

    public void openPackedContentFolder() {
        File initialDirectory;
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
