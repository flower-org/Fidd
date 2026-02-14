package com.fidd.view.forms;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.FiddConnectorFactory;
import com.fidd.core.connection.FiddConnection;
import com.fidd.core.connection.FiddConnectionList;
import com.fidd.core.connection.yaml.YamlFiddConnectionListSerializer;
import com.fidd.service.FiddContentService;
import com.fidd.service.wrapper.WrapperFiddContentService;
import com.fidd.view.serviceCache.FiddContentServiceCache;
import com.flower.crypt.HybridAesEncryptor;
import com.flower.crypt.keys.KeyContext;
import com.flower.crypt.keys.RsaKeyContext;
import com.flower.crypt.keys.UserPreferencesManager;
import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.JavaFxUtils;
import com.flower.fxutils.ModalWindow;
import com.google.common.base.Supplier;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.prefs.Preferences;

import static com.fidd.connectors.folder.FolderFiddConstants.ENCRYPTED_EXT;
import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    final static Logger LOGGER = LoggerFactory.getLogger(MainForm.class);

    // TODO: auto-load connection file on startup?
    // TODO: or at least fill in the filename from last session
    // TODO: then check if changed and ask if you want to save on close
    final static String FIDD_CONNECTION_LIST_EXT = ".conn";
    final static String ENCRYPTED_FIDD_CONNECTION_LIST_EXT = ".conn.crypt";
    final static FileChooser.ExtensionFilter FIDD_CONNECTION_LIST_EXTENSION_FILTER =
            new FileChooser.ExtensionFilter("Fidd Connection List (*" + FIDD_CONNECTION_LIST_EXT + ")",
                    "*" + FIDD_CONNECTION_LIST_EXT);
    final static FileChooser.ExtensionFilter ENCRYPTED_FIDD_CONNECTION_LIST_EXTENSION_FILTER =
            new FileChooser.ExtensionFilter("Fidd Connection List (*" + ENCRYPTED_FIDD_CONNECTION_LIST_EXT + ")",
                    "*" + ENCRYPTED_FIDD_CONNECTION_LIST_EXT);
    final static BaseRepositories BASE_REPOSITORIES = new DefaultBaseRepositories();
    final static int DEFAULT_HTTP_PORT = 80;

    static final String FIDD_VIEW_FIDD_CONNECTIONS_FILE = "FIDD_VIEW_FIDD_CONNECTIONS_FILE";
    static final String FIDD_VIEW_ENCRYPT_DECRYPT = "FIDD_VIEW_ENCRYPT_DECRYPT";

    public static class KeySupplier implements Supplier<Pair<X509Certificate, PrivateKey>> {
        public final TabKeyProvider keyProvider;
        public KeySupplier(TabKeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        @Override
        public @Nullable Pair<X509Certificate, PrivateKey> get() {
            KeyContext keyContext = null;
            try {
                keyContext = keyProvider.getKeyContext();
            } catch (Exception e) {
                return null;
            }
            if (keyContext == null) {
                // No cert - will only show messages with unencrypted keys
                return null;
            } else {
                if (keyContext instanceof RsaKeyContext) {
                    X509Certificate certificate = ((RsaKeyContext) keyContext).certificate();
                    PrivateKey key = ((RsaKeyContext) keyContext).privateKey();
                    return Pair.of(certificate, key);
                } else {
                    throw new RuntimeException("Unsupported Key Context: " + keyContext.getClass());
                }
            }
        }
    }

    @Nullable Stage mainStage;
    @Nullable BaseRepositories repositories;
    @Nullable FiddContentServiceCache fiddContentServiceCache;
    @Nullable String fiddApiHost;
    @Nullable Integer fiddApiPort;

    @FXML @Nullable TabPane mainTabPane;
    @FXML @Nullable Tab fiddConnectionsTab;
    @FXML @Nullable AnchorPane topPane;
    @FXML @Nullable TableView<FiddConnection> fiddConnectionTableView;
    @FXML @Nullable CheckBox encryptDecryptFiddConnectionsCheckBox;
    @FXML @Nullable TextField fiddConnectionsFileTextField;

    @Nullable TabKeyProvider keyProvider;
    @Nullable ObservableList<FiddConnection> fiddConnections;
    @Nullable public KeySupplier keySupplier;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "FiddView v 0.2.4", ButtonType.OK);
        alert.showAndWait();
    }

    public void init(Stage mainStage, BaseRepositories repositories, FiddContentServiceCache fiddContentServiceCache,
                     String fiddApiHost, @Nullable Integer fiddApiPort) {
        this.mainStage = mainStage;
        this.repositories = repositories;
        this.fiddContentServiceCache = fiddContentServiceCache;
        this.fiddApiHost = fiddApiHost;
        this.fiddApiPort = fiddApiPort == null || fiddApiPort == DEFAULT_HTTP_PORT ? null : fiddApiPort;

        keyProvider = buildMainKeyProvider(mainStage);
        AnchorPane keyProviderForm = keyProvider.tabContent();
        checkNotNull(topPane).getChildren().add(keyProviderForm);
        AnchorPane.setTopAnchor(keyProviderForm, 0.0);
        AnchorPane.setBottomAnchor(keyProviderForm, 0.0);
        AnchorPane.setLeftAnchor(keyProviderForm, 0.0);
        AnchorPane.setRightAnchor(keyProviderForm, 0.0);

        keyProvider.initPreferences();

        keySupplier = new KeySupplier(keyProvider);

        fiddConnections = FXCollections.observableArrayList();
        checkNotNull(fiddConnectionTableView).itemsProperty().set(fiddConnections);

        String fiddConnectionsFileStr = UserPreferencesManager.getUserPreference(FIDD_VIEW_FIDD_CONNECTIONS_FILE);
        String encryptDecryptFileStr = UserPreferencesManager.getUserPreference(FIDD_VIEW_ENCRYPT_DECRYPT);
        try {
            if (!StringUtils.isBlank(encryptDecryptFileStr)) {
                boolean encryptDecryptFile = Boolean.parseBoolean(encryptDecryptFileStr);
                checkNotNull(encryptDecryptFiddConnectionsCheckBox).selectedProperty().set(encryptDecryptFile);
            }
        } catch (Exception e) { }

        if (!StringUtils.isBlank(fiddConnectionsFileStr)) {
            checkNotNull(fiddConnectionsFileTextField).textProperty().set(fiddConnectionsFileStr);
        }
        checkNotNull(fiddConnectionsFileTextField).textProperty().addListener(
                (observableValue, s, t1) -> fiddConnectionsFileChanged());

        checkNotNull(mainTabPane).getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == fiddConnectionsTab && checkNotNull(fiddConnections).isEmpty()) {
                loadFiddConnections(false);
            }
        });
    }

    private static TabKeyProvider buildMainKeyProvider(Stage mainStage) {
        RsaPkcs11KeyProvider rsaPkcs11KeyProvider = new RsaPkcs11KeyProvider(mainStage);
        RsaFileKeyProvider rsaFileKeyProvider = new RsaFileKeyProvider(mainStage);
        RsaRawKeyProvider rsaRawKeyProvider = new RsaRawKeyProvider();
        MultiKeyProvider rsaTabPane = new MultiKeyProvider(mainStage, "RSA-2048",
                List.of(rsaPkcs11KeyProvider, rsaFileKeyProvider, rsaRawKeyProvider));

        return new MultiKeyProvider(mainStage, "", List.of(rsaTabPane));
    }

    public void quit() { checkNotNull(mainStage).close(); }

    protected void saveByteArrayToFile(byte[] data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    // ---------- Fidd Connection operations ----------

    public void fiddConnectionDoubleClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            openFiddConnection();
        }
    }

    public void openFiddConnection() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddConnectionTableView).getSelectionModel().getSelectedItem();
            if (selectedFiddConnection != null) {
                openFiddTab(selectedFiddConnection);
            } else {
                JavaFxUtils.showMessage("Fidd Connection not selected.");
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Opening Fidd connection: " + e, ButtonType.OK);
            LOGGER.error("Error Opening Fidd connection: ", e);
            alert.showAndWait();
        }
    }

    public void openFiddTab(FiddConnection fiddConnection) {
        if (keyProvider == null) {
            throw new RuntimeException("Key Provider not found.");
        }

        FiddContentService fiddContentService = checkNotNull(fiddContentServiceCache).getService(fiddConnection.name());
        FiddViewForm fiddViewForm = new FiddViewForm(fiddConnection.name(), checkNotNull(fiddContentService), checkNotNull(fiddApiHost), fiddApiPort);
        fiddViewForm.setStage(checkNotNull(mainStage));
        final Tab tab = new Tab(fiddConnection.name(), fiddViewForm);
        tab.setClosable(true);

        addTab(tab);
    }

    void addTab(Tab tab) {
        checkNotNull(mainTabPane).getTabs().add(tab);
        mainTabPane.getSelectionModel().select(tab);
    }

    FiddContentService getFiddContentServiceForConnection(FiddConnection fiddConnection) {
        FiddConnectorFactory fiddConnectorFactory = BASE_REPOSITORIES.fiddConnectorFactoryRepo().get(fiddConnection.connectorType());
        FiddConnector fiddConnector = checkNotNull(fiddConnectorFactory).createConnector(fiddConnection.url());
        return new WrapperFiddContentService(BASE_REPOSITORIES, fiddConnector, keySupplier);
    }

    protected void addFiddConnection(FiddConnection fiddConnection) {
        Platform.runLater(() -> {
            checkNotNull(fiddConnections).add(fiddConnection);
            checkNotNull(fiddContentServiceCache).addServiceIfAbsent(fiddConnection.name(), getFiddContentServiceForConnection(fiddConnection));
            checkNotNull(fiddConnectionTableView).refresh();
        });
    }

    public void addFiddConnection() {
        try {
            FiddConnectionAddDialog fiddConnectionAddDialog = new FiddConnectionAddDialog(null);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> { fiddConnectionAddDialog.setStage(stage); return fiddConnectionAddDialog; },
                    "Add new Fidd Connection");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            FiddConnection fiddConnection = fiddConnectionAddDialog.getFiddConnection();
                            if (fiddConnection != null) {
                                addFiddConnection(fiddConnection);
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Fidd Connection: " + e, ButtonType.OK);
                            LOGGER.error("Error adding Fidd Connection: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error adding Fidd Connection: " + e, ButtonType.OK);
            LOGGER.error("Error adding Fidd Connection: ", e);
            alert.showAndWait();
        }
    }

    public void removeFiddConnection() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddConnectionTableView).getSelectionModel().getSelectedItem();
            if (selectedFiddConnection != null) {
                checkNotNull(fiddConnections).remove(selectedFiddConnection);
                checkNotNull(fiddConnectionTableView).refresh();
                checkNotNull(fiddContentServiceCache).removeService(selectedFiddConnection.name());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error Removing Fidd connection: " + e, ButtonType.OK);
            LOGGER.error("Error Removing Fidd connection: ", e);
            alert.showAndWait();
        }
    }

    public void editFiddConnection() {
        try {
            FiddConnection selectedFiddConnection = checkNotNull(fiddConnectionTableView).getSelectionModel().getSelectedItem();
            int selectedIndex = checkNotNull(fiddConnectionTableView).getSelectionModel().getSelectedIndex();
            if (selectedFiddConnection != null) {
                FiddConnectionAddDialog fiddConnectionAddDialog = new FiddConnectionAddDialog(selectedFiddConnection);
                Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainStage),
                        stage -> {
                            fiddConnectionAddDialog.setStage(stage);
                            return fiddConnectionAddDialog;
                        },
                        "Edit Fidd Connection");

                workspaceStage.setOnHidden(
                        ev -> {
                            try {
                                FiddConnection fiddConnection = fiddConnectionAddDialog.getFiddConnection();
                                if (fiddConnection != null) {
                                    FiddConnection oldFiddConnection = checkNotNull(fiddConnections).remove(selectedIndex);
                                    checkNotNull(fiddConnections).add(selectedIndex, fiddConnection);

                                    checkNotNull(fiddContentServiceCache).removeService(oldFiddConnection.name());
                                    checkNotNull(fiddContentServiceCache).addServiceIfAbsent(fiddConnection.name(), getFiddContentServiceForConnection(fiddConnection));

                                    checkNotNull(fiddConnectionTableView).refresh();
                                }
                            } catch (Exception e) {
                                Alert alert = new Alert(Alert.AlertType.ERROR, "Error editing Fidd Connection: " + e, ButtonType.OK);
                                LOGGER.error("Error editing Fidd Connection: ", e);
                                alert.showAndWait();
                            }
                        }
                );
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error editing Fidd Connection: " + e, ButtonType.OK);
            LOGGER.error("Error editing Fidd Connection: ", e);
            alert.showAndWait();
        }
    }

    // ---------- Fidd Connection List operations ----------

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

    protected void writeBytesToFile(byte[] data, File file) throws IOException {
        // Ensure parent directory exists
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs(); // create directories if needed
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    public void saveFiddConnections() {
        try {
            boolean encrypt = checkNotNull(encryptDecryptFiddConnectionsCheckBox).isSelected();
            Pair<X509Certificate, PrivateKey> pair = null;
            if (encrypt) {
                pair = getCurrentCertificate();
                if (pair == null) {
                    JavaFxUtils.showMessage("Certificate load error",
                            "Certificate load error. If you do not plan to use a certificate, uncheck \"Decrypt / Encrypt\" checkbox.");
                    return;
                }
            }

            String oldPath = checkNotNull(fiddConnectionsFileTextField).textProperty().get();
            FileChooser fileChooser = new FileChooser();
            if (!StringUtils.isBlank(oldPath)) {
                try {
                    File oldFile = new File(oldPath);
                    fileChooser.setInitialFileName(oldFile.getName());
                    fileChooser.setInitialDirectory(oldFile.getParentFile());
                } catch (Exception e) {}
            }
            fileChooser.getExtensionFilters().addAll(
                    encrypt ? ENCRYPTED_FIDD_CONNECTION_LIST_EXTENSION_FILTER : FIDD_CONNECTION_LIST_EXTENSION_FILTER);
            fileChooser.setTitle("Save Fidd Connections File");

            File fiddConnectionListFile = fileChooser.showSaveDialog(checkNotNull(mainStage));
            if (fiddConnectionListFile != null) {
                if (!fiddConnectionListFile.exists()) {
                    String path = fiddConnectionListFile.getPath();
                    String extension = encrypt ? ENCRYPTED_FIDD_CONNECTION_LIST_EXT : FIDD_CONNECTION_LIST_EXT;
                    if (!path.endsWith(extension)) {
                        path += extension;
                        fiddConnectionListFile = new File(path);
                    }
                }
                checkNotNull(fiddConnectionsFileTextField).textProperty().set(fiddConnectionListFile.getAbsolutePath());

                if (fiddConnectionListFile.exists()) {
                    if (JavaFxUtils.YesNo.NO == JavaFxUtils.showYesNoDialog("Save Fidd Connections File",
                            "Save will overwrite Fidd Connections file. " + fiddConnectionListFile.getAbsolutePath() + " Continue?")) {
                        return;
                    }
                    fiddConnectionListFile.delete();
                }

                // serialize fidd connections
                FiddConnectionList fiddConnectionList = FiddConnectionList.of(checkNotNull(fiddConnections).toArray(new FiddConnection[0]));
                byte[] fiddConnectionListBytes = new YamlFiddConnectionListSerializer().serialize(fiddConnectionList);

                // encrypt if needed
                if (encrypt) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(fiddConnectionListBytes);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    HybridAesEncryptor.encrypt(bis, bos, HybridAesEncryptor.Mode.PUBLIC_KEY_ENCRYPT,
                            null, checkNotNull(pair).getLeft().getPublicKey(), null);
                    fiddConnectionListBytes = bos.toByteArray();
                }

                // save bytes to File
                writeBytesToFile(fiddConnectionListBytes, fiddConnectionListFile);

                Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Fidd Connections file saved successfully " + fiddConnectionListFile.getAbsolutePath(),
                        ButtonType.OK);
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving Fidd Connections: " + e, ButtonType.OK);
            LOGGER.error("Error saving Fidd Connection: ", e);
            alert.showAndWait();
        }
    }

    public void openFiddConnections() {
        File fiddConnectionListFile = null;
        try {
            boolean decrypt = checkNotNull(encryptDecryptFiddConnectionsCheckBox).isSelected();
            if (decrypt) {
                Pair<X509Certificate, PrivateKey> pair = getCurrentCertificate();
                if (pair == null) {
                    JavaFxUtils.showMessage("Certificate load error",
                            "Certificate load error. If you do not plan to use a certificate, uncheck \"Decrypt / Encrypt\" checkbox.");
                    return;
                }
            }

            String oldPath = checkNotNull(fiddConnectionsFileTextField).textProperty().get();
            FileChooser fileChooser = new FileChooser();
            if (!StringUtils.isBlank(oldPath)) {
                try {
                    File oldFile = new File(oldPath);
                    fileChooser.setInitialFileName(oldFile.getName());
                    fileChooser.setInitialDirectory(oldFile.getParentFile());
                } catch (Exception e) {}
            }
            fileChooser.getExtensionFilters().addAll(
                    decrypt ? ENCRYPTED_FIDD_CONNECTION_LIST_EXTENSION_FILTER : FIDD_CONNECTION_LIST_EXTENSION_FILTER);
            fileChooser.setTitle("Open Fidd Connections File");

            fiddConnectionListFile = fileChooser.showOpenDialog(checkNotNull(mainStage));
            if (fiddConnectionListFile != null) {
                checkNotNull(fiddConnectionsFileTextField).textProperty().set(fiddConnectionListFile.getPath());
            } else {
                return;
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading Fidd Connections: " + e, ButtonType.OK);
            LOGGER.error("Error loading Fidd Connections: ", e);
            alert.showAndWait();
            return;
        }

        loadFiddConnections(true);
    }

    public void loadFiddConnections(boolean showAlerts) {
        if (showAlerts && !checkNotNull(fiddConnections).isEmpty()) {
            if (JavaFxUtils.YesNo.NO ==
                    JavaFxUtils.showYesNoDialog("Load Fidd Connections File",
                            "Current Fidd Connections will be lost. Continue?")) {
                return;
            }
        }

        try {
            String fiddConnectionListFilePath = checkNotNull(fiddConnectionsFileTextField).textProperty().get();
            File fiddConnectionListFile = new File(fiddConnectionListFilePath);
            if (!fiddConnectionListFile.exists()) {
                LOGGER.warn("Error loading fidd Connections file - file doesn't exist: {}", fiddConnectionListFilePath);
                if (showAlerts) {
                    Alert alert = new Alert(Alert.AlertType.ERROR,
                            "Error loading Fidd Connections file - file doesn't exist: " + fiddConnectionListFilePath, ButtonType.OK);
                    alert.showAndWait();
                }
            } else {
                boolean decrypt = checkNotNull(encryptDecryptFiddConnectionsCheckBox).isSelected();
                byte[] fiddConnectionListBytes;
                if (decrypt) {
                    Pair<X509Certificate, PrivateKey> pair = getCurrentCertificate();
                    if (pair == null) {
                        if (showAlerts) {
                            JavaFxUtils.showMessage("Certificate load error",
                                    "Certificate load error. If you do not plan to use a certificate, uncheck \"Decrypt / Encrypt\" checkbox.");
                        }
                        return;
                    }

                    // Decrypt file
                    ByteArrayOutputStream fiddConnectionListOutputStream = new ByteArrayOutputStream();
                    try (FileInputStream fis = new FileInputStream(fiddConnectionListFile);
                         DataInputStream dis = new DataInputStream(fis)) {
                        PrivateKey privateKey = checkNotNull(pair).getRight();
                        HybridAesEncryptor.decrypt(fis, fiddConnectionListOutputStream, HybridAesEncryptor.Mode.PUBLIC_KEY_ENCRYPT,
                                privateKey, null, null);
                    }

                    fiddConnectionListBytes = fiddConnectionListOutputStream.toByteArray();
                } else {
                    fiddConnectionListBytes = Files.readAllBytes(fiddConnectionListFile.toPath());
                }

                // Deserialize fiddConnectionList
                FiddConnectionList fiddConnectionList = new YamlFiddConnectionListSerializer().deserialize(fiddConnectionListBytes);

                // Apply loaded fiddConnectionList to fiddConnections table
                checkNotNull(fiddConnections).clear();
                checkNotNull(fiddConnections).addAll(fiddConnectionList.fiddConnectionList());
                checkNotNull(fiddConnectionTableView).refresh();

                checkNotNull(fiddContentServiceCache).clear();
                for (FiddConnection fiddConnection : fiddConnectionList.fiddConnectionList()) {
                    checkNotNull(fiddContentServiceCache).addServiceIfAbsent(fiddConnection.name(), getFiddContentServiceForConnection(fiddConnection));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading Fidd Connections file: ", e);
            if (showAlerts) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading Fidd Connections file: " + e, ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    // a.k.a. checkBox updated
    public void updateFiddConnectionsFileExtension() {
        boolean selected = checkNotNull(encryptDecryptFiddConnectionsCheckBox).selectedProperty().get();
        UserPreferencesManager.updateUserPreference(Preferences.userRoot(), FIDD_VIEW_ENCRYPT_DECRYPT, Boolean.toString(selected));

        String fiddConnectionsFilePath = checkNotNull(fiddConnectionsFileTextField).textProperty().get();
        if (!StringUtils.isBlank(fiddConnectionsFilePath)) {
            if (selected) {
                if (!fiddConnectionsFilePath.endsWith(ENCRYPTED_EXT)) {
                    fiddConnectionsFilePath += ENCRYPTED_EXT;
                    checkNotNull(fiddConnectionsFileTextField).textProperty().set(fiddConnectionsFilePath);
                }
            } else {
                if (fiddConnectionsFilePath.endsWith(ENCRYPTED_EXT)) {
                    fiddConnectionsFilePath = fiddConnectionsFilePath.substring(0, fiddConnectionsFilePath.length() - ENCRYPTED_EXT.length());
                    checkNotNull(fiddConnectionsFileTextField).textProperty().set(fiddConnectionsFilePath);
                }
            }
        }
    }

    public void fiddConnectionsFileChanged() {
        String fiddConnectionsFilePath = checkNotNull(fiddConnectionsFileTextField).textProperty().get();
        fiddConnectionsFilePath = StringUtils.defaultIfBlank(fiddConnectionsFilePath, "");
        UserPreferencesManager.updateUserPreference(Preferences.userRoot(), FIDD_VIEW_FIDD_CONNECTIONS_FILE, fiddConnectionsFilePath);
    }
}
