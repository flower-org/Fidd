package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyProvider;
import javafx.scene.layout.AnchorPane;

public interface TabKeyProvider extends KeyProvider {
    String tabName();
    AnchorPane tabContent();
    void initPreferences();
}
