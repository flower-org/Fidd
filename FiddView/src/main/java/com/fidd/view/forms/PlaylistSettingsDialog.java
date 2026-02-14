package com.fidd.view.forms;

import com.fidd.view.common.PlaylistSettings;
import com.fidd.view.common.PlaylistSort;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class PlaylistSettingsDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PlaylistSettingsDialog.class);

    @FXML @Nullable CheckBox filterInCheckBox;
    @FXML @Nullable TextField filterInTextField;

    @FXML @Nullable CheckBox filterOutCheckBox;
    @FXML @Nullable TextField filterOutTextField;

    @FXML @Nullable CheckBox sortingCheckBox;
    @FXML @Nullable ComboBox<String> sortingComboBox;

    @FXML @Nullable CheckBox includeSubfoldersCheckBox;

    @FXML @Nullable Button addButton;

    @Nullable Stage stage;

    public PlaylistSettingsDialog(PlaylistSettings playlistSettings) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PlaylistSettingsDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // Filter In
        if (playlistSettings.filterIn().isEmpty()) {
            checkNotNull(filterInCheckBox).selectedProperty().set(false);
            checkNotNull(filterInTextField).textProperty().set("");
            checkNotNull(filterInTextField).setDisable(true);
        } else {
            checkNotNull(filterInCheckBox).selectedProperty().set(true);
            checkNotNull(filterInTextField).textProperty().set(String.join(";", playlistSettings.filterIn()));
            checkNotNull(filterInTextField).setDisable(false);
        }

        // Filter Out
        if (playlistSettings.filterOut().isEmpty()) {
            checkNotNull(filterOutCheckBox).selectedProperty().set(false);
            checkNotNull(filterOutTextField).textProperty().set("");
            checkNotNull(filterOutTextField).setDisable(true);
        } else {
            checkNotNull(filterOutCheckBox).selectedProperty().set(true);
            checkNotNull(filterOutTextField).textProperty().set(String.join(";", playlistSettings.filterOut()));
            checkNotNull(filterOutTextField).setDisable(false);
        }

        // Sorting
        checkNotNull(sortingComboBox).itemsProperty().get().addAll(
            PlaylistSort.NUMERICAL_ASC.name(),
            PlaylistSort.NUMERICAL_DESC.name(),
            PlaylistSort.ALPHABETICAL_ASC.name(),
            PlaylistSort.ALPHABETICAL_ASC.name()
        );

        if (PlaylistSort.NONE.equals(playlistSettings.sort())) {
            checkNotNull(sortingCheckBox).selectedProperty().set(false);
            checkNotNull(sortingComboBox).setDisable(true);
        } else {
            checkNotNull(sortingCheckBox).selectedProperty().set(true);
            checkNotNull(sortingComboBox).setDisable(false);
            checkNotNull(sortingComboBox).getSelectionModel().select(playlistSettings.sort().name());
        }

        // Include subfolders
        checkNotNull(includeSubfoldersCheckBox).selectedProperty().set(playlistSettings.includeSubfolders());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public PlaylistSettings getPlaylistSettings() {
        List<String> filterIn = List.of();
        if (checkNotNull(filterInCheckBox).selectedProperty().get()
                && !StringUtils.isBlank(checkNotNull(filterInTextField).textProperty().get())) {
            String[] parts = checkNotNull(filterInTextField).textProperty().get().split(";");
            filterIn = Arrays.asList(parts);
        }

        List<String> filterOut = List.of();
        if (checkNotNull(filterOutCheckBox).selectedProperty().get()
                && !StringUtils.isBlank(checkNotNull(filterOutTextField).textProperty().get())) {
            String[] parts = checkNotNull(filterOutTextField).textProperty().get().split(";");
            filterOut = Arrays.asList(parts);
        }

        PlaylistSort sort;
        if (!checkNotNull(sortingCheckBox).selectedProperty().get()
                || StringUtils.isBlank(checkNotNull(sortingComboBox).getSelectionModel().getSelectedItem())) {
            sort = PlaylistSort.NONE;
        } else {
            sort = PlaylistSort.valueOf(checkNotNull(sortingComboBox).getSelectionModel().getSelectedItem());
        }

        boolean includeSubfolders = checkNotNull(includeSubfoldersCheckBox).selectedProperty().get();

        return new PlaylistSettings(filterIn, filterOut, sort, includeSubfolders);
    }

    public void filterInChecked() {
        checkNotNull(filterInTextField).setDisable(
                !checkNotNull(filterInCheckBox).selectedProperty().get());
    }

    public void filterOutChecked() {
        checkNotNull(filterOutTextField).setDisable(
                !checkNotNull(filterOutCheckBox).selectedProperty().get());
    }

    public void sortingChecked() {
        checkNotNull(sortingComboBox).setDisable(
                !checkNotNull(sortingCheckBox).selectedProperty().get());
    }

    public void okClose() {
        try {
            checkNotNull(stage).close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "WorkspaceDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("WorkspaceDialog close Error:", e);
            alert.showAndWait();
        }
    }
}
