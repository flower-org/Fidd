package com.fidd.packer.forms;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.random.RandomGeneratorType;
import com.fidd.packer.pack.FiddPackManager;
import com.flower.crypt.PkiUtil;
import com.flower.crypt.keys.KeyContext;
import com.flower.crypt.keys.RsaKeyContext;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.JavaFxUtils;
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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
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
    final static String METADATA_CONTAINER = "METADATA_CONTAINER";
    final static String CRC_CALCULATOR = "CRC_CALCULATOR";
    final static String SIGN_FIDD_FILE_AND_FIDD_KEY = "SIGN_FIDD_FILE_AND_FIDD_KEY";
    final static String SIGN_LOGICAL_FILES = "SIGN_LOGICAL_FILES";
    final static String SIGN_LOGICAL_FILE_METADATAS = "SIGN_LOGICAL_FILE_METADATAS";
    final static String SIGN_FIDD_FILE_METADATA = "SIGN_FIDD_FILE_METADATA";
    final static String ADD_CRCS_TO_FIDD_KEY = "ADD_CRCS_TO_FIDD_KEY";
    final static String INCLUDE_PUBLIC_KEY = "INCLUDE_PUBLIC_KEY";

    final static String MIN_GAP_SIZE_TEXT_FIELD = "MIN_GAP_SIZE_TEXT_FIELD";
    final static String MAX_GAP_SIZE_TEXT_FIELD = "MAX_GAP_SIZE_TEXT_FIELD";

    @Nullable Stage mainStage;

    @FXML @Nullable AnchorPane topPane;

    @Nullable TabKeyProvider keyProvider;

    @FXML @Nullable TextArea generatedCertificateTextArea;
    @FXML @Nullable TextArea generatedPrivateKeyTextArea;

    @FXML @Nullable TextField originalFolderTextField;
    @FXML @Nullable TextField packedContentFolderTextField;

    @FXML @Nullable TextField messageNumberTextField;
    @FXML @Nullable TextField postIdTextField;

    @FXML @Nullable ComboBox<String> fiddKeyComboBox;
    @FXML @Nullable ComboBox<String> metadataContainerComboBox;
    @FXML @Nullable ComboBox<String> fiddFileMetadataComboBox;
    @FXML @Nullable ComboBox<String> logicalFileMetadataComboBox;
    @FXML @Nullable ComboBox<String> publicKeyFormatComboBox;
    @FXML @Nullable ComboBox<String> signatureFormatComboBox;
    @FXML @Nullable ComboBox<String> encryptionAlgorithmComboBox;
    @FXML @Nullable ComboBox<String> randomGeneratorComboBox;
    @FXML @Nullable ComboBox<String> crcCalculatorComboBox;

    @FXML @Nullable CheckBox signFiddFileAndFiddKeyCheckBox;
    @FXML @Nullable CheckBox signLogicalFilesCheckBox;
    @FXML @Nullable CheckBox signLogicalFileMetadatasCheckBox;
    @FXML @Nullable CheckBox signFiddFileMetadataCheckBox;
    @FXML @Nullable CheckBox addCrcsToFiddKeyCheckBox;
    @FXML @Nullable CheckBox includePublicKeyCheckBox;

    @FXML @Nullable TextField minGapSizeTextField;
    @FXML @Nullable TextField maxGapSizeTextField;

    BaseRepositories baseRepositories;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly

        baseRepositories = new DefaultBaseRepositories();
    }

    protected @Nullable Pair<X509Certificate, PrivateKey> getCurrentCertificate() {
        if (keyProvider == null) { return null; }
        KeyContext keyContext = keyProvider.getKeyContext();

        if (keyContext instanceof RsaKeyContext) {
            X509Certificate certificate = ((RsaKeyContext) keyContext).certificate();
            PrivateKey key = ((RsaKeyContext) keyContext).privateKey();
            return Pair.of(certificate, key);
        } else {
            throw new RuntimeException("Unsupported Key Context: " + keyContext.getClass());
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

    public void checkUncheckAllSignatures() {
        boolean toggle = false;

        if (!checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get() ||
                !checkNotNull(signLogicalFilesCheckBox).selectedProperty().get() ||
                !checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get() ||
                !checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get()) {
            toggle = true;
        }

        checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().set(toggle);
        checkNotNull(signLogicalFilesCheckBox).selectedProperty().set(toggle);
        checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().set(toggle);
        checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().set(toggle);
    }

    public void checkUncheckAllSignaturesAndCrc() {
        boolean toggle = false;

        if (!checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get() ||
            !checkNotNull(signLogicalFilesCheckBox).selectedProperty().get() ||
            !checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get() ||
            !checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get() ||
            !checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get()) {
            toggle = true;
        }

        checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().set(toggle);
        checkNotNull(signLogicalFilesCheckBox).selectedProperty().set(toggle);
        checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().set(toggle);
        checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().set(toggle);
        checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().set(toggle);
    }

    static void initCheckBox(CheckBox checkBox, @Nullable String checkedStr) {
        if (!StringUtils.isBlank(checkedStr)) {
            Boolean checked = null;
            try {
                checked = Boolean.parseBoolean(checkedStr);
            } catch (Exception ignored) {}

            if (checked != null) { checkNotNull(checkBox).selectedProperty().set(checked); }
        }
    }

    public void loadFiddPackerChooserPreferences() {
        initRepositoryComboBox(baseRepositories.encryptionAlgorithmRepo(), checkNotNull(encryptionAlgorithmComboBox), getUserPreference(ENCRYPTION_ALGORITHM));
        initRepositoryComboBox(baseRepositories.fiddKeyFormatRepo(), checkNotNull(fiddKeyComboBox), getUserPreference(FIDD_KEY));
        initRepositoryComboBox(baseRepositories.metadataContainerFormatRepo(), checkNotNull(metadataContainerComboBox), getUserPreference(METADATA_CONTAINER));
        initRepositoryComboBox(baseRepositories.fiddFileMetadataFormatRepo(), checkNotNull(fiddFileMetadataComboBox), getUserPreference(FIDD_FILE_METADATA));
        initRepositoryComboBox(baseRepositories.logicalFileMetadataFormatRepo(), checkNotNull(logicalFileMetadataComboBox), getUserPreference(LOGICAL_FILE_METADATA));
        initRepositoryComboBox(baseRepositories.publicKeyFormatRepo(), checkNotNull(publicKeyFormatComboBox), getUserPreference(PUBLIC_KEY_FORMAT));
        initRepositoryComboBox(baseRepositories.signatureFormatRepo(), checkNotNull(signatureFormatComboBox), getUserPreference(SIGNATURE_FORMAT));
        initRepositoryComboBox(baseRepositories.randomGeneratorsRepo(), checkNotNull(randomGeneratorComboBox), getUserPreference(RANDOM_GENERATOR));
        initRepositoryComboBox(baseRepositories.metadataContainerFormatRepo(), checkNotNull(metadataContainerComboBox), getUserPreference(METADATA_CONTAINER));
        initRepositoryComboBox(baseRepositories.crcCalculatorsRepo(), checkNotNull(crcCalculatorComboBox), getUserPreference(CRC_CALCULATOR));

        initCheckBox(checkNotNull(signFiddFileAndFiddKeyCheckBox), getUserPreference(SIGN_FIDD_FILE_AND_FIDD_KEY));
        initCheckBox(checkNotNull(signLogicalFilesCheckBox), getUserPreference(SIGN_LOGICAL_FILES));
        initCheckBox(checkNotNull(signLogicalFileMetadatasCheckBox), getUserPreference(SIGN_LOGICAL_FILE_METADATAS));
        initCheckBox(checkNotNull(signFiddFileMetadataCheckBox), getUserPreference(SIGN_FIDD_FILE_METADATA));
        initCheckBox(checkNotNull(addCrcsToFiddKeyCheckBox), getUserPreference(ADD_CRCS_TO_FIDD_KEY));
        initCheckBox(checkNotNull(includePublicKeyCheckBox), StringUtils.defaultIfBlank(getUserPreference(INCLUDE_PUBLIC_KEY), "true"));

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
        checkNotNull(metadataContainerComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(crcCalculatorComboBox).valueProperty().addListener(this::fiddPackerTextChanged);

        checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signLogicalFilesCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(includePublicKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);

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
        String crcCalculator = checkNotNull(crcCalculatorComboBox).valueProperty().get();
        String metadataContainer = checkNotNull(metadataContainerComboBox).valueProperty().get();
        boolean signFiddFileAndFiddKey = checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get();
        boolean signLogicalFiles = checkNotNull(signLogicalFilesCheckBox).selectedProperty().get();
        boolean signLogicalFileMetadatas = checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get();
        boolean signFiddFileMetadata = checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get();
        boolean addCrcsToFiddKey = checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get();
        boolean includePublicKey = checkNotNull(includePublicKeyCheckBox).selectedProperty().get();

        String minGapSize = checkNotNull(minGapSizeTextField).textProperty().get();
        String maxGapSize = checkNotNull(maxGapSizeTextField).textProperty().get();

        updateFiddPackerPreferences(encryptionAlgorithm, fiddKey, fiddFileMetadata, logicalFileMetadata, publicKeyFormat,
                signatureFormat, randomGenerator, metadataContainer, crcCalculator, Boolean.toString(signFiddFileAndFiddKey),
                Boolean.toString(signLogicalFiles), Boolean.toString(signLogicalFileMetadatas),
                Boolean.toString(signFiddFileMetadata), Boolean.toString(addCrcsToFiddKey), Boolean.toString(includePublicKey),
                minGapSize, maxGapSize);
    }

    public static void updateFiddPackerPreferences(String encryptionAlgorithm, String fiddKey, String fiddFileMetadata,
                                                String logicalFileMetadata, String publicKeyFormat, String signatureFormat,
                                                String randomGenerator, String metadataContainer, String crcCalculator,
                                                String signFiddFileAndFiddKey,
                                                String signLogicalFiles, String signLogicalFileMetadatas,
                                                String signFiddFileMetadata, String addCrcsToFiddKey, String includePublicKey,
                                                String minGapSize,
                                                String maxGapSize) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, ENCRYPTION_ALGORITHM, StringUtils.defaultIfBlank(encryptionAlgorithm, ""));
        updateUserPreference(userPreferences, FIDD_KEY, StringUtils.defaultIfBlank(fiddKey, ""));
        updateUserPreference(userPreferences, FIDD_FILE_METADATA, StringUtils.defaultIfBlank(fiddFileMetadata, ""));
        updateUserPreference(userPreferences, LOGICAL_FILE_METADATA, StringUtils.defaultIfBlank(logicalFileMetadata, ""));
        updateUserPreference(userPreferences, PUBLIC_KEY_FORMAT, StringUtils.defaultIfBlank(publicKeyFormat, ""));
        updateUserPreference(userPreferences, SIGNATURE_FORMAT, StringUtils.defaultIfBlank(signatureFormat, ""));
        updateUserPreference(userPreferences, RANDOM_GENERATOR, StringUtils.defaultIfBlank(randomGenerator, ""));
        updateUserPreference(userPreferences, METADATA_CONTAINER, StringUtils.defaultIfBlank(metadataContainer, ""));
        updateUserPreference(userPreferences, CRC_CALCULATOR, StringUtils.defaultIfBlank(crcCalculator, ""));
        updateUserPreference(userPreferences, SIGN_FIDD_FILE_AND_FIDD_KEY, StringUtils.defaultIfBlank(signFiddFileAndFiddKey, ""));
        updateUserPreference(userPreferences, SIGN_LOGICAL_FILES, StringUtils.defaultIfBlank(signLogicalFiles, ""));
        updateUserPreference(userPreferences, SIGN_LOGICAL_FILE_METADATAS, StringUtils.defaultIfBlank(signLogicalFileMetadatas, ""));
        updateUserPreference(userPreferences, SIGN_FIDD_FILE_METADATA, StringUtils.defaultIfBlank(signFiddFileMetadata, ""));
        updateUserPreference(userPreferences, ADD_CRCS_TO_FIDD_KEY, StringUtils.defaultIfBlank(addCrcsToFiddKey, ""));
        updateUserPreference(userPreferences, INCLUDE_PUBLIC_KEY, StringUtils.defaultIfBlank(includePublicKey, ""));

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

    static <J> J getComboBoxSelectionFromRepo(Repository<J> repo, @Nullable ComboBox<String> comboBox) {
        return repo.get(checkNotNull(comboBox).valueProperty().get());
    }

    public void packFolder() {
        try {
            File originalDirectory = new File(checkNotNull(originalFolderTextField).textProperty().get());
            if (!originalDirectory.exists()) {
                JavaFxUtils.showMessage("Original Folder doesn't exist");
                return;
            }
            File packedContentDirectory = new File(checkNotNull(packedContentFolderTextField).textProperty().get());
            if (!packedContentDirectory.exists()) {
                JavaFxUtils.showMessage("Packed Content Folder doesn't exist");
                return;
            }

            FiddKeySerializer fiddKeySerializer = getComboBoxSelectionFromRepo(baseRepositories.fiddKeyFormatRepo(), fiddKeyComboBox);
            MetadataContainerSerializer metadataContainerSerializer = getComboBoxSelectionFromRepo(baseRepositories.metadataContainerFormatRepo(), metadataContainerComboBox);
            FiddFileMetadataSerializer fiddFileMetadataSerializer = getComboBoxSelectionFromRepo(baseRepositories.fiddFileMetadataFormatRepo(), fiddFileMetadataComboBox);
            LogicalFileMetadataSerializer logicalFileMetadataSerializer = getComboBoxSelectionFromRepo(baseRepositories.logicalFileMetadataFormatRepo(), logicalFileMetadataComboBox);
            EncryptionAlgorithm encryptionAlgorithm = getComboBoxSelectionFromRepo(baseRepositories.encryptionAlgorithmRepo(), encryptionAlgorithmComboBox);
            RandomGeneratorType randomGenerator = getComboBoxSelectionFromRepo(baseRepositories.randomGeneratorsRepo(), randomGeneratorComboBox);
            long minGapSize;
            String minGapSizeStr = checkNotNull(minGapSizeTextField).textProperty().get();
            try { minGapSize = Long.parseLong(minGapSizeStr);
            } catch (NumberFormatException e) {
                JavaFxUtils.showMessage("Value Mismatch", "Cant parse Min Gap Size as an integer number: " + minGapSizeStr);
                return;
            }
            long maxGapSize;
            String maxGapSizeStr = checkNotNull(maxGapSizeTextField).textProperty().get();
            try { maxGapSize = Long.parseLong(maxGapSizeStr);
            } catch (NumberFormatException e) {
                JavaFxUtils.showMessage("Value Mismatch", "Cant parse Max Gap Size as an integer number: " + maxGapSizeStr);
                return;
            }

            boolean createFileAndKeySignatures = checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get();
            boolean addFiddFileMetadataSignature = checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get();
            boolean addLogicalFileSignatures = checkNotNull(signLogicalFilesCheckBox).selectedProperty().get();
            boolean addLogicalFileMetadataSignatures = checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get();
            boolean includePublicKey = checkNotNull(includePublicKeyCheckBox).selectedProperty().get();

            PublicKeySerializer publicKeySerializer = getComboBoxSelectionFromRepo(baseRepositories.publicKeyFormatRepo(), publicKeyFormatComboBox);
            SignerChecker signerChecker = getComboBoxSelectionFromRepo(baseRepositories.signatureFormatRepo(), signatureFormatComboBox);

            boolean addCrcsToFiddKey = checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get();
            CrcCalculator crcCalculator = getComboBoxSelectionFromRepo(baseRepositories.crcCalculatorsRepo(), crcCalculatorComboBox);

            long messageNumber;
            String messageNumberStr = checkNotNull(messageNumberTextField).textProperty().get();
            try { messageNumber = Long.parseLong(messageNumberStr);
            } catch (NumberFormatException e) {
                JavaFxUtils.showMessage("Value Mismatch", "Cant parse Message Number as an integer number: " + messageNumberStr);
                return;
            }
            String postId = checkNotNull(postIdTextField).textProperty().get();
            if (StringUtils.isBlank(postId)) {
                JavaFxUtils.showMessage("Value Mismatch", "PostId can't be blank");
                return;
            }
            X509Certificate currentCert = null;
            PrivateKey privateKey = null;
            if (createFileAndKeySignatures || addFiddFileMetadataSignature || addLogicalFileSignatures || addLogicalFileMetadataSignatures) {
                try {
                    Pair<X509Certificate, PrivateKey> pair = getCurrentCertificate();
                    if (pair == null) {
                        JavaFxUtils.showMessage("Certificate load error", "Certificate load error. If you do not plan to use a certificate, click \"Uncheck All Signatures\".");
                        return;
                    }
                    currentCert = pair.getLeft();
                    privateKey = pair.getRight();
                } catch (Exception e) {
                    JavaFxUtils.showMessage("Certificate load error", "Certificate load error. If you do not plan to use a certificate, click \"Uncheck All Signatures\".\n" + e.getMessage());
                    LOGGER.error("Certificate was not loaded", e);
                    return;
                }
            }

            // TODO: Progress Bar modal window

            FiddPackManager.fiddPackNewPost(
                    originalDirectory,
                    packedContentDirectory,
                    currentCert,
                    privateKey,
                    messageNumber,
                    postId,

                    fiddKeySerializer,
                    metadataContainerSerializer,
                    fiddFileMetadataSerializer,
                    logicalFileMetadataSerializer,
                    encryptionAlgorithm,
                    randomGenerator,
                    minGapSize,
                    maxGapSize,

                    createFileAndKeySignatures,
                    addFiddFileMetadataSignature,
                    addLogicalFileSignatures,
                    addLogicalFileMetadataSignatures,

                    includePublicKey,
                    publicKeySerializer,
                    signerChecker,

                    addCrcsToFiddKey,
                    crcCalculator
            );

            JavaFxUtils.showMessage("Fidd Pack Complete!");
        } catch (Exception e) {
            LOGGER.error("Packing error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

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

    public void generateSelfSignedCertificateRsa2048() {
        // TODO: Progress Bar modal window

        generateSelfSignedCertificateRsa(2048);
    }

    public void generateSelfSignedCertificateRsa4096() {
        // TODO: Progress Bar modal window

        generateSelfSignedCertificateRsa(4096);
    }

    public void generateSelfSignedCertificateRsa8192() {
        // TODO: Progress Bar modal window

        generateSelfSignedCertificateRsa(8192);
    }

    public void generateSelfSignedCertificateRsa(int keysize) {
        try {
            KeyPair keyPair = PkiUtil.generateRsaKeyPair(keysize);
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

    public void openPackedContentFolderForUnpack() {
        // TODO: implement
    }

    public void openContentFolderForUnpack() {
        // TODO: implement
    }

    public void checkUncheckAllValidations() {
        // TODO: implement
    }
}
