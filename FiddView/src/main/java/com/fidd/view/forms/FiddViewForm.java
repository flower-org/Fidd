package com.fidd.view.forms;

import com.fidd.connectors.folder.FolderFiddConnector;
import com.fidd.core.connection.FiddConnection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddViewForm extends AnchorPane  {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddViewForm.class);

    // https://iconscout.com/free-icon-pack/free-free-user-interface-icons-set-icon-pack_37661 - source
    // https://iconscout.com/icon-pack/arrow-and-navigation-icon-pack_66887 - nice folder icon, but costs money
    static final Image FOLDER_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/doc.png")));
    static final Image DOWN_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/down.png")));
    static final Image UP_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/up-arrow.png")));

    @Nullable Stage stage;
    @Nullable @FXML TreeView<FiddTreeNode> fiddStructureTreeView;

    protected final FiddConnection fiddConnection;
    protected final FolderFiddConnector folderFiddConnector;
    protected final TreeItem<FiddTreeNode> rootItem;
    protected final AtomicBoolean loading = new AtomicBoolean(false);

    public interface FiddTreeNode {
        @Nullable ImageView getImage();
        default void action() {}
    }

    public static class FiddNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(FOLDER_ICON);
        protected final String fiddName;
        public FiddNode(String fiddName) { this.fiddName = fiddName; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return fiddName; }
    }

    public static class FiddMessageNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(FOLDER_ICON);
        protected final String messageName;
        public FiddMessageNode(String messageName) { this.messageName = messageName; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return messageName; }
    }

    public class FiddExpandNode implements FiddTreeNode {
        public enum ExpandDirection { PREVIOUS, NEXT }
        protected final ImageView imageView;
        protected final ExpandDirection direction;
        public FiddExpandNode(ExpandDirection direction) { this.direction = direction;
            imageView = new ImageView( direction == ExpandDirection.PREVIOUS ? DOWN_ICON : UP_ICON); }
        public ExpandDirection direction() { return direction; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return "Load more (Double-click)..."; }
        public void action() {
            // TODO: dedup
            // TODO: load up
            Integer myPosition = null;
            for (int i = 0; i < rootItem.getChildren().size(); i++) {
                TreeItem<FiddTreeNode> child = rootItem.getChildren().get(i);
                if (this == child.valueProperty().get()) {
                    myPosition = i;
                    break;
                }
            }
            int index = checkNotNull(myPosition);
            rootItem.getChildren().remove(index);
            loadNextBatch(index);
        }
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
        FiddTreeNode rootNode = new FiddNode(fiddConnection.getName());
        rootItem = new TreeItem<>(rootNode, rootNode.getImage());
        checkNotNull(fiddStructureTreeView).rootProperty().set(rootItem);

        // Double-Click
        fiddStructureTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
                if (treeItem != null) {
                    FiddTreeNode node = treeItem.getValue();
                    node.action();
                }
            }
        });
        // DOWN/ENTER/PAGE_DOWN
        fiddStructureTreeView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN:
                case ENTER:
                case PAGE_DOWN:
                    TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
                    if (treeItem != null) {
                        FiddTreeNode node = treeItem.getValue();
                        node.action();
                    }
                    break;
            }
        });

        this.folderFiddConnector = new FolderFiddConnector(fiddConnection.url());
        loadNextBatch(0);
    }

    protected void loadNextBatch(int position) {
        if (loading.compareAndSet(false, true)) {
            try {
                Task<List<FiddTreeNode>> task = new Task<>() {
                    @Override
                    protected List<FiddTreeNode> call() {
                        return fetchNextNodesFromServerOrDB();
                    }
                };

                task.setOnSucceeded(e -> Platform.runLater(
                    () -> {
                        int pos = position;
                        for (FiddTreeNode node : task.getValue()) {
                            addFiddMessageToTree(node, pos++);
                        }
                    }));

                new Thread(task).start();
            } finally {
                loading.compareAndSet(true, false);
            }
        }
    }

    protected void addFiddMessageToTree(FiddTreeNode node, int pos) {
        TreeItem<FiddTreeNode> treeItem = new TreeItem<>(node, node.getImage());
        rootItem.getChildren().add(pos, treeItem);
    }

    protected List<FiddTreeNode> fetchNextNodesFromServerOrDB() {
        List<FiddTreeNode> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new FiddMessageNode(UUID.randomUUID().toString()));
        }
        list.add(new FiddExpandNode(FiddExpandNode.ExpandDirection.PREVIOUS));
        return list;
    }
}
