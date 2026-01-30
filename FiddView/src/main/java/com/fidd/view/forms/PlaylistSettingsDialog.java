package com.fidd.view.forms;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

public class PlaylistSettingsDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PlaylistSettingsDialog.class);

    @FXML @Nullable TextField filterInTextField;
    @FXML @Nullable TextField filterOutTextField;
    @FXML @Nullable ComboBox<String> sortingComboBox;
    @FXML @Nullable CheckBox filterInCheckBox;
    @FXML @Nullable CheckBox filterOutCheckBox;
    @FXML @Nullable CheckBox sortingCheckBox;
    @FXML @Nullable CheckBox includeSubfoldersCheckBox;
    @FXML @Nullable Button addButton;

    public PlaylistSettingsDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PlaylistSettingsDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // TODO:
    }
}
