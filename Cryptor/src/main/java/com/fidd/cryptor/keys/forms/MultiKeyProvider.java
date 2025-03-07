package com.fidd.cryptor.keys.forms;

import com.fidd.cryptor.keys.KeyContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MultiKeyProvider extends AnchorPane implements TabKeyProvider {
    final static Logger LOGGER = LoggerFactory.getLogger(MultiKeyProvider.class);

    @Nullable @FXML TabPane childProvidersTabPane;

    protected final String tabName;
    protected final Map<Tab, TabKeyProvider> providerMap;

    public MultiKeyProvider(String tabName, Collection<TabKeyProvider> childKeyProviders) {
        providerMap = new HashMap<>();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MultiKeyProvider.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.tabName = tabName;

        childKeyProviders.forEach(prov -> providerMap.put(addTab(prov), prov));
    }

    public Tab addTab(TabKeyProvider tabKeyProvider) {
        String tabName = tabKeyProvider.tabName();
        AnchorPane tabContent = tabKeyProvider.tabContent();

        //TODO: is this required?
        //tabContent.setStage(checkNotNull(mainStage));

        final Tab tab = new Tab(tabName, tabContent);
        //tab.setClosable(true);

        checkNotNull(childProvidersTabPane).getTabs().add(tab);
//        childProvidersTabPane.getSelectionModel().select(tab);
        return tab;
    }

    @Override
    @Nullable public KeyContext geKeyContext() {
        Tab selectedTab = checkNotNull(childProvidersTabPane).getSelectionModel().getSelectedItem();
        TabKeyProvider provider = checkNotNull(providerMap.get(selectedTab));
        return provider.geKeyContext();
    }

    @Override
    public String tabName() {
        return tabName;
    }

    @Override
    public AnchorPane tabContent() {
        return this;
    }
}
