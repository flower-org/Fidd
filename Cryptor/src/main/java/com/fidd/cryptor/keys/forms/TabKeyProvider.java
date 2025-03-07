package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyProvider;
import javafx.scene.layout.AnchorPane;

public interface TabKeyProvider extends KeyProvider {
    String tabName();
    // Do we need this?
    AnchorPane tabContent();
}
