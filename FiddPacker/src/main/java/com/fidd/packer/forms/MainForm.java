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
import com.fidd.packer.pack.FiddUnpackManager;
import com.flower.crypt.PkiUtil;
import com.flower.crypt.keys.KeyContext;
import com.flower.crypt.keys.RsaKeyContext;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.JavaFxUtils;
import javafx.beans.value.ChangeListener;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import static com.fidd.packer.pack.FiddPackManager.DEFAULT_FIDD_FILE_NAME;
import static com.fidd.packer.pack.FiddPackManager.DEFAULT_FIDD_FILE_NAME_PREF;
import static com.fidd.packer.pack.FiddPackManager.DEFAULT_FIDD_KEY_FILE_NAME;
import static com.fidd.packer.pack.FiddPackManager.DEFAULT_FIDD_KEY_FILE_NAME_PREF;
import static com.fidd.packer.pack.FiddPackManager.DEFAULT_FIDD_SIGNATURE_EXT;
import static com.fidd.packer.pack.FiddUnpackManager.PublicKeySource.MESSAGE_EMBEDDED;
import static com.fidd.packer.pack.FiddUnpackManager.PublicKeySource.MESSAGE_FALL_BACK_TO_PARAMETER;
import static com.fidd.packer.pack.FiddUnpackManager.PublicKeySource.SUPPLIED_PARAMETER;
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
    final static String INCLUDE_MESSAGE_CREATION_TIME = "INCLUDE_MESSAGE_CREATION_TIME";

    final static String MIN_GAP_SIZE_TEXT_FIELD = "MIN_GAP_SIZE_TEXT_FIELD";
    final static String MAX_GAP_SIZE_TEXT_FIELD = "MAX_GAP_SIZE_TEXT_FIELD";

    final static String IGNORE_VALIDATION_FAILURES = "IGNORE_VALIDATION_FAILURES";
    final static String VALIDATE_FIDD_FILE_AND_FIDD_KEY = "VALIDATE_FIDD_FILE_AND_FIDD_KEY";
    final static String VALIDATE_CRCS_IN_FIDD_KEY = "VALIDATE_CRCS_IN_FIDD_KEY";
    final static String VALIDATE_FIDD_FILE_METADATA = "VALIDATE_FIDD_FILE_METADATA";
    final static String VALIDATE_LOGICAL_FILE_METADATAS = "VALIDATE_LOGICAL_FILE_METADATAS";
    final static String VALIDATE_LOGICAL_FILES = "VALIDATE_LOGICAL_FILES";

    final static String ADD_PROGRESSIVE_CRC = "ADD_PROGRESSIVE_CRC";
    final static String PROGRESSIVE_CRC_MIN_FILE_SIZE = "PROGRESSIVE_CRC_MIN_FILE_SIZE";
    final static String PROGRESSIVE_CRC_CALCULATOR = "PROGRESSIVE_CRC_CALCULATOR";

    final static String SIGNATURE_FILES_DELIMITER = ";";

    final static String PUBLIC_KEY_SOURCE = "PUBLIC_KEY_SOURCE";

    final static String PUBLIC_KEY_OPTION_MESSAGE = "Use Public Key from Message";
    final static String PUBLIC_KEY_OPTION_UI = "Use Public Key from UI";
    final static String PUBLIC_KEY_OPTION_MESSAGE_FALL_BACK_TO_UI = "Use from Message, fall back to UI";

    final static long ONE_MEBIBYTE = 1024L*1024L;

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
    @FXML @Nullable ComboBox<String> progressiveCrcCalculatorComboBox;

    @FXML @Nullable CheckBox signFiddFileAndFiddKeyCheckBox;
    @FXML @Nullable CheckBox signLogicalFilesCheckBox;
    @FXML @Nullable CheckBox signLogicalFileMetadatasCheckBox;
    @FXML @Nullable CheckBox signFiddFileMetadataCheckBox;
    @FXML @Nullable CheckBox addCrcsToFiddKeyCheckBox;
    @FXML @Nullable CheckBox includePublicKeyCheckBox;
    @FXML @Nullable CheckBox includeMessageCreationTimeCheckBox;

    @FXML @Nullable TextField minGapSizeTextField;
    @FXML @Nullable TextField maxGapSizeTextField;

    @FXML @Nullable CheckBox ignoreValidationFailuresCheckBox;
    @FXML @Nullable CheckBox validateFiddFileAndFiddKeyCheckBox;
    @FXML @Nullable CheckBox validateCrcsInFiddKeyCheckBox;
    @FXML @Nullable CheckBox validateFiddFileMetadataCheckBox;
    @FXML @Nullable CheckBox validateLogicalFileMetadatasCheckBox;
    @FXML @Nullable CheckBox validateLogicalFilesCheckBox;

    @FXML @Nullable TextField packedContentFolderForUnpackTextField;
    @FXML @Nullable TextField contentFolderForUnpackTextField;
    @FXML @Nullable TextField fiddFileTextField;
    @FXML @Nullable TextField fiddFileSignaturesTextField;
    @FXML @Nullable TextField fiddKeyFileTextField;
    @FXML @Nullable TextField fiddKeyFileSignaturesTextField;

    @FXML @Nullable ComboBox<String> publicKeySourceComboBox;

    @FXML @Nullable TextArea unpackLogTextArea;

    @FXML @Nullable CheckBox addProgressiveCrcCheckBox;
    @FXML @Nullable TextField progressiveCrcMinFileSizeTextField;

    BaseRepositories baseRepositories;

    final FiddUnpackManager.ProgressCallback mainFormProgressCallback = new FiddUnpackManager.ProgressCallback() {
        private static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        public static String timestamp() {
            return LocalDateTime.now().format(FORMATTER);
        }

        @Override
        public void log(String log) {
            LOGGER.info(log);
            checkNotNull(unpackLogTextArea).appendText(timestamp() + " " + log + '\n');
        }

        @Override
        public void warn(String log) {
            LOGGER.warn(log);
            checkNotNull(unpackLogTextArea).appendText(timestamp() + " " + "WARNING! " + log + '\n');
        }
    };

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

        checkNotNull(packedContentFolderForUnpackTextField).textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String _old, String _new) {
                findFiddFiles(_new);
            }
        });

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

    static void initTextField(TextField textField, @Nullable String text) {
        if (StringUtils.isBlank(text)) { text = ""; }
        checkNotNull(textField).textProperty().set(text);
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
        initRepositoryComboBox(baseRepositories.crcCalculatorsRepo(), checkNotNull(progressiveCrcCalculatorComboBox), getUserPreference(PROGRESSIVE_CRC_CALCULATOR));

        initCheckBox(checkNotNull(signFiddFileAndFiddKeyCheckBox), getUserPreference(SIGN_FIDD_FILE_AND_FIDD_KEY));
        initCheckBox(checkNotNull(signLogicalFilesCheckBox), getUserPreference(SIGN_LOGICAL_FILES));
        initCheckBox(checkNotNull(signLogicalFileMetadatasCheckBox), getUserPreference(SIGN_LOGICAL_FILE_METADATAS));
        initCheckBox(checkNotNull(signFiddFileMetadataCheckBox), getUserPreference(SIGN_FIDD_FILE_METADATA));
        initCheckBox(checkNotNull(addCrcsToFiddKeyCheckBox), getUserPreference(ADD_CRCS_TO_FIDD_KEY));
        initCheckBox(checkNotNull(includePublicKeyCheckBox), StringUtils.defaultIfBlank(getUserPreference(INCLUDE_PUBLIC_KEY), "true"));
        initCheckBox(checkNotNull(includeMessageCreationTimeCheckBox), getUserPreference(INCLUDE_MESSAGE_CREATION_TIME));
        initCheckBox(checkNotNull(addProgressiveCrcCheckBox), getUserPreference(ADD_PROGRESSIVE_CRC));

        initCheckBox(checkNotNull(ignoreValidationFailuresCheckBox), StringUtils.defaultIfBlank(getUserPreference(IGNORE_VALIDATION_FAILURES), "true"));
        initCheckBox(checkNotNull(validateFiddFileAndFiddKeyCheckBox), StringUtils.defaultIfBlank(getUserPreference(VALIDATE_FIDD_FILE_AND_FIDD_KEY), "true"));
        initCheckBox(checkNotNull(validateCrcsInFiddKeyCheckBox), StringUtils.defaultIfBlank(getUserPreference(VALIDATE_CRCS_IN_FIDD_KEY), "true"));
        initCheckBox(checkNotNull(validateFiddFileMetadataCheckBox), StringUtils.defaultIfBlank(getUserPreference(VALIDATE_FIDD_FILE_METADATA), "true"));
        initCheckBox(checkNotNull(validateLogicalFileMetadatasCheckBox), StringUtils.defaultIfBlank(getUserPreference(VALIDATE_LOGICAL_FILE_METADATAS), "true"));
        initCheckBox(checkNotNull(validateLogicalFilesCheckBox), StringUtils.defaultIfBlank(getUserPreference(VALIDATE_LOGICAL_FILES), "true"));

        initComboBox(List.of(PUBLIC_KEY_OPTION_MESSAGE, PUBLIC_KEY_OPTION_UI, PUBLIC_KEY_OPTION_MESSAGE_FALL_BACK_TO_UI),
                checkNotNull(publicKeySourceComboBox),
                StringUtils.defaultIfBlank(getUserPreference(PUBLIC_KEY_SOURCE), PUBLIC_KEY_OPTION_MESSAGE));

        initTextField(checkNotNull(minGapSizeTextField), StringUtils.defaultIfBlank(getUserPreference(MIN_GAP_SIZE_TEXT_FIELD), "0"));
        initTextField(checkNotNull(maxGapSizeTextField), StringUtils.defaultIfBlank(getUserPreference(MAX_GAP_SIZE_TEXT_FIELD), "0"));
        initTextField(checkNotNull(progressiveCrcMinFileSizeTextField), StringUtils.defaultIfBlank(getUserPreference(PROGRESSIVE_CRC_MIN_FILE_SIZE), "1"));
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
        checkNotNull(progressiveCrcCalculatorComboBox).valueProperty().addListener(this::fiddPackerTextChanged);

        checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signLogicalFilesCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(includePublicKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(includeMessageCreationTimeCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(addProgressiveCrcCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);

        checkNotNull(minGapSizeTextField).textProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(maxGapSizeTextField).textProperty().addListener(this::fiddPackerTextChanged);
        checkNotNull(progressiveCrcMinFileSizeTextField).textProperty().addListener(this::fiddPackerTextChanged);

        checkNotNull(ignoreValidationFailuresCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(validateFiddFileAndFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(validateCrcsInFiddKeyCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(validateFiddFileMetadataCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(validateLogicalFileMetadatasCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);
        checkNotNull(validateLogicalFilesCheckBox).selectedProperty().addListener(this::fiddPackerBoolChanged);

        checkNotNull(publicKeySourceComboBox).valueProperty().addListener(this::fiddPackerTextChanged);
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
        String progressiveCrcCalculator = checkNotNull(progressiveCrcCalculatorComboBox).valueProperty().get();
        String metadataContainer = checkNotNull(metadataContainerComboBox).valueProperty().get();
        boolean signFiddFileAndFiddKey = checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get();
        boolean signLogicalFiles = checkNotNull(signLogicalFilesCheckBox).selectedProperty().get();
        boolean signLogicalFileMetadatas = checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get();
        boolean signFiddFileMetadata = checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get();
        boolean addCrcsToFiddKey = checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get();
        boolean includePublicKey = checkNotNull(includePublicKeyCheckBox).selectedProperty().get();
        boolean includeMessageCreationTime = checkNotNull(includeMessageCreationTimeCheckBox).selectedProperty().get();

        String minGapSize = checkNotNull(minGapSizeTextField).textProperty().get();
        String maxGapSize = checkNotNull(maxGapSizeTextField).textProperty().get();

        boolean ignoreValidationFailures = checkNotNull(ignoreValidationFailuresCheckBox).selectedProperty().get();
        boolean validateFiddFileAndFiddKey = checkNotNull(validateFiddFileAndFiddKeyCheckBox).selectedProperty().get();
        boolean validateCrcsInFiddKey = checkNotNull(validateCrcsInFiddKeyCheckBox).selectedProperty().get();
        boolean validateFiddFileMetadata = checkNotNull(validateFiddFileMetadataCheckBox).selectedProperty().get();
        boolean validateLogicalFileMetadatas = checkNotNull(validateLogicalFileMetadatasCheckBox).selectedProperty().get();
        boolean validateLogicalFiles = checkNotNull(validateLogicalFilesCheckBox).selectedProperty().get();

        String publicKeySource = checkNotNull(publicKeySourceComboBox).valueProperty().get();

        boolean addProgressiveCrc = checkNotNull(addProgressiveCrcCheckBox).selectedProperty().get();
        String progressiveCrcMinFileSize = checkNotNull(progressiveCrcMinFileSizeTextField).textProperty().get();

        updateFiddPackerPreferences(encryptionAlgorithm, fiddKey, fiddFileMetadata, logicalFileMetadata, publicKeyFormat,
                signatureFormat, randomGenerator, metadataContainer, crcCalculator, progressiveCrcCalculator, Boolean.toString(signFiddFileAndFiddKey),
                Boolean.toString(signLogicalFiles), Boolean.toString(signLogicalFileMetadatas),
                Boolean.toString(signFiddFileMetadata), Boolean.toString(addCrcsToFiddKey), Boolean.toString(includePublicKey), Boolean.toString(includeMessageCreationTime),
                minGapSize, maxGapSize,

                Boolean.toString(ignoreValidationFailures), Boolean.toString(validateFiddFileAndFiddKey),
                Boolean.toString(validateCrcsInFiddKey), Boolean.toString(validateFiddFileMetadata),
                Boolean.toString(validateLogicalFileMetadatas), Boolean.toString(validateLogicalFiles),

                publicKeySource,

                Boolean.toString(addProgressiveCrc), progressiveCrcMinFileSize
        );
    }

    public static void updateFiddPackerPreferences(String encryptionAlgorithm, String fiddKey, String fiddFileMetadata,
                                                String logicalFileMetadata, String publicKeyFormat, String signatureFormat,
                                                String randomGenerator, String metadataContainer, String crcCalculator, String progressiveCrcCalculator,
                                                String signFiddFileAndFiddKey,
                                                String signLogicalFiles, String signLogicalFileMetadatas,
                                                String signFiddFileMetadata, String addCrcsToFiddKey, String includePublicKey, String includeMessageCreationTime,
                                                String minGapSize,
                                                String maxGapSize,

                                                String ignoreValidationFailures,
                                                String validateFiddFileAndFiddKey,
                                                String validateCrcsInFiddKey,
                                                String validateFiddFileMetadata,
                                                String validateLogicalFileMetadatas,
                                                String validateLogicalFiles,

                                                String publicKeySource,

                                                String addProgressiveCrc,
                                                String progressiveCrcMinFileSize
                                               ) {
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
        updateUserPreference(userPreferences, PROGRESSIVE_CRC_CALCULATOR, StringUtils.defaultIfBlank(progressiveCrcCalculator, ""));
        updateUserPreference(userPreferences, SIGN_FIDD_FILE_AND_FIDD_KEY, StringUtils.defaultIfBlank(signFiddFileAndFiddKey, ""));
        updateUserPreference(userPreferences, SIGN_LOGICAL_FILES, StringUtils.defaultIfBlank(signLogicalFiles, ""));
        updateUserPreference(userPreferences, SIGN_LOGICAL_FILE_METADATAS, StringUtils.defaultIfBlank(signLogicalFileMetadatas, ""));
        updateUserPreference(userPreferences, SIGN_FIDD_FILE_METADATA, StringUtils.defaultIfBlank(signFiddFileMetadata, ""));
        updateUserPreference(userPreferences, ADD_CRCS_TO_FIDD_KEY, StringUtils.defaultIfBlank(addCrcsToFiddKey, ""));
        updateUserPreference(userPreferences, INCLUDE_PUBLIC_KEY, StringUtils.defaultIfBlank(includePublicKey, ""));
        updateUserPreference(userPreferences, INCLUDE_MESSAGE_CREATION_TIME, StringUtils.defaultIfBlank(includeMessageCreationTime, ""));

        updateUserPreference(userPreferences, MIN_GAP_SIZE_TEXT_FIELD, StringUtils.defaultIfBlank(minGapSize, ""));
        updateUserPreference(userPreferences, MAX_GAP_SIZE_TEXT_FIELD, StringUtils.defaultIfBlank(maxGapSize, ""));

        updateUserPreference(userPreferences, IGNORE_VALIDATION_FAILURES, StringUtils.defaultIfBlank(ignoreValidationFailures, ""));
        updateUserPreference(userPreferences, VALIDATE_FIDD_FILE_AND_FIDD_KEY, StringUtils.defaultIfBlank(validateFiddFileAndFiddKey, ""));
        updateUserPreference(userPreferences, VALIDATE_CRCS_IN_FIDD_KEY, StringUtils.defaultIfBlank(validateCrcsInFiddKey, ""));
        updateUserPreference(userPreferences, VALIDATE_FIDD_FILE_METADATA, StringUtils.defaultIfBlank(validateFiddFileMetadata, ""));
        updateUserPreference(userPreferences, VALIDATE_LOGICAL_FILE_METADATAS, StringUtils.defaultIfBlank(validateLogicalFileMetadatas, ""));
        updateUserPreference(userPreferences, VALIDATE_LOGICAL_FILES, StringUtils.defaultIfBlank(validateLogicalFiles, ""));

        updateUserPreference(userPreferences, PUBLIC_KEY_SOURCE, StringUtils.defaultIfBlank(publicKeySource, ""));

        updateUserPreference(userPreferences, ADD_PROGRESSIVE_CRC, StringUtils.defaultIfBlank(addProgressiveCrc, ""));
        updateUserPreference(userPreferences, PROGRESSIVE_CRC_MIN_FILE_SIZE, StringUtils.defaultIfBlank(progressiveCrcMinFileSize, ""));
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

    static void initComboBox(List<String> options, ComboBox<String> combo, @Nullable String selected) {
        ObservableList<String> list = FXCollections.observableArrayList(options);
        combo.setItems(list);

        if (StringUtils.isBlank(selected)) {
            selected = list.get(0);
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
                JavaFxUtils.showMessage("Original Folder doesn't exist", originalDirectory.getAbsolutePath());
                return;
            }
            File packedContentDirectoryRoot = new File(checkNotNull(packedContentFolderTextField).textProperty().get());
            if (!packedContentDirectoryRoot.exists()) {
                JavaFxUtils.showMessage("Packed Content Root Folder doesn't exist", packedContentDirectoryRoot.getAbsolutePath());
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

            boolean includeMessageCreationTime = checkNotNull(includeMessageCreationTimeCheckBox).selectedProperty().get();
            boolean createFileAndKeySignatures = checkNotNull(signFiddFileAndFiddKeyCheckBox).selectedProperty().get();
            boolean addFiddFileMetadataSignature = checkNotNull(signFiddFileMetadataCheckBox).selectedProperty().get();
            boolean addLogicalFileSignatures = checkNotNull(signLogicalFilesCheckBox).selectedProperty().get();
            boolean addLogicalFileMetadataSignatures = checkNotNull(signLogicalFileMetadatasCheckBox).selectedProperty().get();
            boolean includePublicKey = checkNotNull(includePublicKeyCheckBox).selectedProperty().get();

            PublicKeySerializer publicKeySerializer = getComboBoxSelectionFromRepo(baseRepositories.publicKeyFormatRepo(), publicKeyFormatComboBox);
            SignerChecker signerChecker = getComboBoxSelectionFromRepo(baseRepositories.signatureFormatRepo(), signatureFormatComboBox);

            boolean addCrcsToFiddKey = checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get();
            CrcCalculator crcCalculator = getComboBoxSelectionFromRepo(baseRepositories.crcCalculatorsRepo(), crcCalculatorComboBox);

            boolean addProgressiveCrcs = checkNotNull(addCrcsToFiddKeyCheckBox).selectedProperty().get();
            long minProgressiveCrcFileSize = 0L;
            CrcCalculator progressiveCrcCalculator = null;
            if (addProgressiveCrcs) {
                minProgressiveCrcFileSize = Long.parseLong(checkNotNull(progressiveCrcMinFileSizeTextField).textProperty().get());
                minProgressiveCrcFileSize *= ONE_MEBIBYTE;
                progressiveCrcCalculator = getComboBoxSelectionFromRepo(baseRepositories.crcCalculatorsRepo(), progressiveCrcCalculatorComboBox);
            }

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

            File packedContentDirectory = new File(packedContentDirectoryRoot, Long.toString(messageNumber));
            if (packedContentDirectory.exists()) {
                JavaFxUtils.YesNo result = JavaFxUtils.showYesNoDialog("Packed Content Folder already exists. Try to delete?", packedContentDirectory.getAbsolutePath());
                if (result == JavaFxUtils.YesNo.YES) {
                    if (!packedContentDirectory.delete()) {
                        JavaFxUtils.showMessage("Deletion failed: The Packed Content Folder may contain files or subfolders.", packedContentDirectory.getAbsolutePath());
                        return;
                    }
                } else {
                    return;
                }
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

            if (!packedContentDirectory.mkdir()) {
                JavaFxUtils.showMessage("Failed to create Packed Content Folder: ", packedContentDirectory.getAbsolutePath());
                return;
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

                    includeMessageCreationTime,

                    createFileAndKeySignatures,
                    addFiddFileMetadataSignature,
                    addLogicalFileSignatures,
                    addLogicalFileMetadataSignatures,

                    includePublicKey,
                    publicKeySerializer,
                    List.of(signerChecker),

                    addCrcsToFiddKey,
                    List.of(crcCalculator),

                    addProgressiveCrcs,
                    minProgressiveCrcFileSize,
                    ONE_MEBIBYTE,
                    progressiveCrcCalculator == null ? null : List.of(progressiveCrcCalculator)
            );

            JavaFxUtils.showMessage("Fidd Pack Complete!");

            formMessageNumber(packedContentDirectoryRoot);
        } catch (Exception e) {
            LOGGER.error("Packing error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }

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
            checkNotNull(postIdTextField).textProperty().set(directory.getName());
        }
    }

    public void openPackedContentFolder() {
        File initialDirectory;
        initialDirectory = new File(checkNotNull(packedContentFolderTextField).textProperty().get());
        File directory = chooseDirectory(initialDirectory);
        if (directory != null) {
            checkNotNull(packedContentFolderTextField).textProperty().set(directory.getPath());
            formMessageNumber(directory);
        }
    }

    protected void formMessageNumber(File directory) {
        File[] subfolders = directory.listFiles(File::isDirectory);
        if (subfolders != null) {
            long maxMessageNumber = 0L;
            for (File subfolder : subfolders) {
                try {
                    long number = Long.parseLong(subfolder.getName());
                    if (number > maxMessageNumber) {
                        maxMessageNumber = number;
                    }
                } catch (Exception e) {
                    // Ignored
                }
            }

            maxMessageNumber += 1L;

            checkNotNull(messageNumberTextField).textProperty().set(Long.toString(maxMessageNumber));
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
        File packedContentFolder;
        packedContentFolder = new File(checkNotNull(packedContentFolderForUnpackTextField).textProperty().get());
        File directory = chooseDirectory(packedContentFolder);
        if (directory != null) {
            checkNotNull(packedContentFolderForUnpackTextField).textProperty().set(directory.getPath());
            // find other files - logic is "on change" event for packedContentFolderForUnpackTextField
        }
    }

    protected void findFiddFiles(String directoryStr) {
        checkNotNull(fiddFileTextField).textProperty().set("");
        checkNotNull(fiddFileSignaturesTextField).textProperty().set("");
        checkNotNull(fiddKeyFileTextField).textProperty().set("");
        checkNotNull(fiddKeyFileSignaturesTextField).textProperty().set("");

        try {
            File directory = new File(directoryStr);
            File[] subFiles = directory.listFiles(File::isFile);
            if (subFiles != null) {
                List<Long> fiddFileSignatures = new ArrayList<>();
                List<Long> fiddKeyFileSignatures = new ArrayList<>();
                for (File subFile : subFiles) {
                    String filename = subFile.getName();
                    if (filename.equals(DEFAULT_FIDD_FILE_NAME)) {
                        checkNotNull(fiddFileTextField).textProperty().set(filename);
                    } else if (filename.equals(DEFAULT_FIDD_KEY_FILE_NAME)) {
                        checkNotNull(fiddKeyFileTextField).textProperty().set(filename);
                    } else if (filename.startsWith(DEFAULT_FIDD_FILE_NAME_PREF) && filename.endsWith(DEFAULT_FIDD_SIGNATURE_EXT)) {
                        Long signatureNum = getSignatureNum(filename, DEFAULT_FIDD_FILE_NAME_PREF, DEFAULT_FIDD_SIGNATURE_EXT);
                        if (signatureNum != null) {
                            fiddFileSignatures.add(signatureNum);
                        }
                    } else if (filename.startsWith(DEFAULT_FIDD_KEY_FILE_NAME_PREF) && filename.endsWith(DEFAULT_FIDD_SIGNATURE_EXT)) {
                        Long keySignatureNum = getSignatureNum(filename, DEFAULT_FIDD_KEY_FILE_NAME_PREF, DEFAULT_FIDD_SIGNATURE_EXT);
                        if (keySignatureNum != null) {
                            fiddKeyFileSignatures.add(keySignatureNum);
                        }
                    }
                }

                Collections.sort(fiddFileSignatures);
                Collections.sort(fiddKeyFileSignatures);

                checkNotNull(fiddFileSignaturesTextField).textProperty().set(String.join(SIGNATURE_FILES_DELIMITER,
                        fiddFileSignatures.stream().map(n -> DEFAULT_FIDD_FILE_NAME_PREF + n + DEFAULT_FIDD_SIGNATURE_EXT).toList()));
                checkNotNull(fiddKeyFileSignaturesTextField).textProperty().set(String.join(SIGNATURE_FILES_DELIMITER,
                        fiddKeyFileSignatures.stream().map(n -> DEFAULT_FIDD_KEY_FILE_NAME_PREF + n + DEFAULT_FIDD_SIGNATURE_EXT).toList()));
            }
        } catch (Exception ignored) {}
    }

    @Nullable public static Long getSignatureNum(String filename, String prefix, String postfix) {
        String num = stripPrefixAndPostfix(filename, prefix, postfix);
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException ne) {
            return null;
        }
    }

    public static String stripPrefixAndPostfix(String str, @Nullable String prefix, @Nullable String postfix) {
        // Remove prefix if present
        if (prefix != null && str.startsWith(prefix)) {
            str = str.substring(prefix.length());
        }

        // Remove postfix if present
        if (postfix != null && str.endsWith(postfix)) {
            str = str.substring(0, str.length() - postfix.length());
        }

        return str;
    }

    public void openContentFolderForUnpack() {
        File contentFolder = new File(checkNotNull(contentFolderForUnpackTextField).textProperty().get());
        File directory = chooseDirectory(contentFolder);
        if (directory != null) {
            checkNotNull(contentFolderForUnpackTextField).textProperty().set(directory.getPath());
        }
    }

    public void checkUncheckAllValidations() {
        boolean toggle = false;

        if (!checkNotNull(validateFiddFileAndFiddKeyCheckBox).selectedProperty().get() ||
                !checkNotNull(validateCrcsInFiddKeyCheckBox).selectedProperty().get() ||
                !checkNotNull(validateFiddFileMetadataCheckBox).selectedProperty().get() ||
                !checkNotNull(validateLogicalFileMetadatasCheckBox).selectedProperty().get() ||
                !checkNotNull(validateLogicalFilesCheckBox).selectedProperty().get()) {
            toggle = true;
        }

        checkNotNull(validateFiddFileAndFiddKeyCheckBox).selectedProperty().set(toggle);
        checkNotNull(validateCrcsInFiddKeyCheckBox).selectedProperty().set(toggle);
        checkNotNull(validateFiddFileMetadataCheckBox).selectedProperty().set(toggle);
        checkNotNull(validateLogicalFileMetadatasCheckBox).selectedProperty().set(toggle);
        checkNotNull(validateLogicalFilesCheckBox).selectedProperty().set(toggle);
    }

    public void unpackFolder() {
        try {
            File packedContentFolder = new File(checkNotNull(packedContentFolderForUnpackTextField).textProperty().get());
            if (!packedContentFolder.exists()) {
                JavaFxUtils.showMessage("Packed Content Folder doesn't exist", packedContentFolder.getAbsolutePath());
                return;
            }
            File contentFolder = new File(checkNotNull(contentFolderForUnpackTextField).textProperty().get());
            if (!contentFolder.exists()) {
                JavaFxUtils.showMessage("Content Root Folder doesn't exist", contentFolder.getAbsolutePath());
                return;
            }

            String fiddFileName = checkNotNull(fiddFileTextField).textProperty().get();
            if (StringUtils.isBlank(fiddFileName)) {
                JavaFxUtils.showMessage("Fidd File not found");
                return;
            }
            File fiddFile = new File(packedContentFolder, fiddFileName);
            if (!fiddFile.exists()) {
                JavaFxUtils.showMessage("Fidd File doesn't exist", fiddFile.getAbsolutePath());
                return;
            }

            String fiddKeyFileName = checkNotNull(fiddKeyFileTextField).textProperty().get();
            if (StringUtils.isBlank(fiddKeyFileName)) {
                JavaFxUtils.showMessage("Fidd Key File not found");
                return;
            }
            File fiddKeyFile = new File(packedContentFolder, fiddKeyFileName);
            if (!fiddKeyFile.exists()) {
                JavaFxUtils.showMessage("Fidd Key File doesn't exist", fiddKeyFile.getAbsolutePath());
                return;
            }

            List<File> fiddFileSignatures = new ArrayList<>();
            String fiddFileSignatureFileNames = checkNotNull(fiddFileSignaturesTextField).textProperty().get();
            if (!StringUtils.isBlank(fiddFileSignatureFileNames)) {
                String[] signatureFileNames = fiddFileSignatureFileNames.split(SIGNATURE_FILES_DELIMITER);
                for (String signatureFileName : signatureFileNames) {
                    File signatureFile = new File(packedContentFolder, signatureFileName);
                    if (!signatureFile.exists()) {
                        JavaFxUtils.showMessage("Fidd Signature File doesn't exist", signatureFile.getAbsolutePath());
                        return;
                    }
                    fiddFileSignatures.add(signatureFile);
                }
            }

            List<File> fiddKeyFileSignatures = new ArrayList<>();
            String fiddKeyFileSignatureFileNames = checkNotNull(fiddKeyFileSignaturesTextField).textProperty().get();
            if (!StringUtils.isBlank(fiddKeyFileSignatureFileNames)) {
                String[] signatureFileNames = fiddKeyFileSignatureFileNames.split(SIGNATURE_FILES_DELIMITER);
                for (String signatureFileName : signatureFileNames) {
                    File signatureFile = new File(packedContentFolder, signatureFileName);
                    if (!signatureFile.exists()) {
                        JavaFxUtils.showMessage("Fidd Key Signature File doesn't exist", signatureFile.getAbsolutePath());
                        return;
                    }
                    fiddKeyFileSignatures.add(signatureFile);
                }
            }

            boolean ignoreValidationFailures = checkNotNull(ignoreValidationFailuresCheckBox).selectedProperty().get();
            boolean validateFiddFileAndFiddKey = checkNotNull(validateFiddFileAndFiddKeyCheckBox).selectedProperty().get();
            boolean validateCrcsInFiddKey = checkNotNull(validateCrcsInFiddKeyCheckBox).selectedProperty().get();
            boolean validateFiddFileMetadata = checkNotNull(validateFiddFileMetadataCheckBox).selectedProperty().get();
            boolean validateLogicalFileMetadatas = checkNotNull(validateLogicalFileMetadatasCheckBox).selectedProperty().get();
            boolean validateLogicalFiles = checkNotNull(validateLogicalFilesCheckBox).selectedProperty().get();

            FiddUnpackManager.PublicKeySource publicKeySource;
            String publicKeySourceStr = checkNotNull(publicKeySourceComboBox).valueProperty().get();
            switch (publicKeySourceStr) {
                case PUBLIC_KEY_OPTION_MESSAGE:
                    publicKeySource = MESSAGE_EMBEDDED;
                    break;
                case PUBLIC_KEY_OPTION_UI:
                    publicKeySource = SUPPLIED_PARAMETER;
                    break;
                case PUBLIC_KEY_OPTION_MESSAGE_FALL_BACK_TO_UI:
                    publicKeySource = MESSAGE_FALL_BACK_TO_PARAMETER;
                    break;
                default:
                    throw new RuntimeException("Unknown PublicKeySource: " + publicKeySourceStr);
            }

            X509Certificate currentCert = null;
            try {
                Pair<X509Certificate, PrivateKey> pair = getCurrentCertificate();
                if (pair == null) {
                    JavaFxUtils.showMessage("Certificate load error",
                            "Certificate load error. If you do not plan to use a certificate, click \"Uncheck All Signatures\".");
                    return;
                }
                currentCert = pair.getLeft();
            } catch (Exception e) {
                // TODO: log textarea output?
                LOGGER.error("Certificate was not loaded", e);
                if (publicKeySource == SUPPLIED_PARAMETER || publicKeySource == MESSAGE_FALL_BACK_TO_PARAMETER) {
                    JavaFxUtils.showMessage("Certificate load error",
                            "Certificate load error. If you do not plan to specify a certificate, choose \"" +
                                    PUBLIC_KEY_OPTION_MESSAGE + "\".\n" + e.getMessage());
                    return;
                }
            }

            // TODO: Progress Bar modal window

            boolean throwOnValidationFailures = !ignoreValidationFailures;
            FiddUnpackManager.fiddUnpackPost(baseRepositories,
                    fiddFile,
                    fiddKeyFile,
                    fiddFileSignatures,
                    fiddKeyFileSignatures,
                    contentFolder,

                    throwOnValidationFailures,
                    validateFiddFileAndFiddKey,
                    validateCrcsInFiddKey,
                    validateFiddFileMetadata,
                    validateLogicalFileMetadatas,
                    validateLogicalFiles,

                    publicKeySource,
                    currentCert,
                    mainFormProgressCallback);

            JavaFxUtils.showMessage("Fidd Unpack Complete!");
        } catch (Exception e) {
            LOGGER.error("Unpacking error", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.OK);
            alert.showAndWait();
        }
    }
}
