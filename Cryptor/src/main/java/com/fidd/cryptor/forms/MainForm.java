package com.fidd.cryptor.forms;

import com.google.common.base.Preconditions;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import javax.annotation.Nullable;

public class MainForm {
    @Nullable Stage mainStage;

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

    public void quit() { Preconditions.checkNotNull(mainStage).close(); }
}
