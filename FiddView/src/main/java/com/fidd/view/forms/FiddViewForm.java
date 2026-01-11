package com.fidd.view.forms;

import com.fidd.core.connection.FiddConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddViewForm extends AnchorPane  {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddViewForm.class);

    @Nullable Stage stage;
    @Nullable @FXML
    TreeView<FiddTreeNode> fiddStructureTreeView;

    final FiddConnection fiddConnection;
    final TreeItem<FiddTreeNode> rootItem;

    interface FiddTreeNode {
    }

    interface FiddNode extends FiddTreeNode {
    }

    interface FiddMessageNode extends FiddTreeNode {
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public FiddViewForm(FiddConnection fiddConnection) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FiddViewForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.fiddConnection = fiddConnection;
        FiddTreeNode rootNode = new FiddNode() {
            @Override
            public String toString() {
                return fiddConnection.getName();
            }
        };
        rootItem = new TreeItem<>(rootNode);
        checkNotNull(fiddStructureTreeView).rootProperty().set(rootItem);
    }
}
