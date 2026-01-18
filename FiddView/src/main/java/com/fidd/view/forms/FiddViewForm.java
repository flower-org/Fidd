package com.fidd.view.forms;

import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.service.FiddContentService;
import com.fidd.service.LogicalFileInfo;
import com.flower.fxutils.JavaFxUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddViewForm extends AnchorPane  {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddViewForm.class);

    static final int MESSAGE_LOAD_BATCH_SIZE = 5;
    static final int FILE_SAVE_BUFFER_SIZE = 8192;

    // https://iconscout.com/free-icon-pack/free-free-user-interface-icons-set-icon-pack_37661 - source
    // https://iconscout.com/icon-pack/arrow-and-navigation-icon-pack_66887 - nice folder icon, but costs money
    static final Image MESSAGE_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/doc.png")));
    static final Image FOLDER_ICON_ = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/folder.png")));
    static final Image FILE_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/file.png")));
    static final Image DOWN_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/down.png")));
    static final Image UP_ICON = new Image(checkNotNull(FiddViewForm.class.getResourceAsStream("/icons/up-arrow.png")));

    public interface FiddTreeNode {
        @Nullable ImageView getImage();
    }

    public static class FiddRootNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(MESSAGE_ICON);
        protected final String fiddName;
        public FiddRootNode(String fiddName) { this.fiddName = fiddName; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return fiddName; }
    }

    public static class FiddMessageNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(MESSAGE_ICON);
        protected final String messageName;
        protected final long messageNumber;
        public FiddMessageNode(String messageName, long messageNumber) { this.messageName = messageName; this.messageNumber = messageNumber; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return messageName; }
    }

    public static class FiddFolderNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(FOLDER_ICON_);
        protected final String folderName;
        public FiddFolderNode(String folderName) { this.folderName = folderName; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return folderName; }
    }

    public static class FiddFileNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(FILE_ICON);
        protected final String fileName;
        protected final LogicalFileInfo logicalFileInfo;
        public FiddFileNode(String fileName, LogicalFileInfo logicalFileInfo) { this.fileName = fileName; this.logicalFileInfo = logicalFileInfo;}
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return fileName; }
    }

    public static class FiddTextNode implements FiddTreeNode {
        protected final ImageView imageView = new ImageView(MESSAGE_ICON);
        protected final String fiddName;
        public FiddTextNode(String fiddName) { this.fiddName = fiddName; }
        @Override public @Nullable ImageView getImage() { return imageView; }
        @Override public String toString() { return fiddName; }
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
        public void expand() {
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

    @Nullable Stage stage;
    @Nullable @FXML TreeView<FiddTreeNode> fiddStructureTreeView;
    @Nullable @FXML Button saveFileButton;
    @Nullable @FXML Button getFileUrlButton;

    protected final String blogName;
    protected final FiddContentService fiddContentService;
    protected final TreeItem<FiddTreeNode> rootItem;
    protected final AtomicBoolean loading = new AtomicBoolean(false);
    protected @Nullable Long lastContiguouslyLoadedMessageNumber = null;

    public FiddViewForm(String fiddName, FiddContentService fiddContentService) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FiddViewForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.blogName = fiddName;
        this.fiddContentService = fiddContentService;

        FiddTreeNode rootNode = new FiddRootNode(fiddName);
        rootItem = new TreeItem<>(rootNode, rootNode.getImage());
        checkNotNull(fiddStructureTreeView).rootProperty().set(rootItem);

        // Double-Click - TODO: move to FXML
        fiddStructureTreeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
                if (treeItem != null) {
                    FiddTreeNode node = treeItem.getValue();
                    if (node instanceof FiddExpandNode expandNode) {
                        expandNode.expand();
                    }
                }
            }
        });
        // DOWN/ENTER/PAGE_DOWN: TODO: move to FXML
        fiddStructureTreeView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case DOWN:
                case ENTER:
                case PAGE_DOWN:
                    TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
                    if (treeItem != null) {
                        FiddTreeNode node = treeItem.getValue();
                        if (node instanceof FiddExpandNode expandNode) {
                            expandNode.expand();
                        }
                    }
                    break;
            }
        });
        fiddStructureTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                FiddTreeNode value = newSel.getValue();
                if (value instanceof FiddFileNode) {
                    checkNotNull(saveFileButton).setDisable(false);
                    checkNotNull(getFileUrlButton).setDisable(false);
                    return;
                }
            }

            checkNotNull(saveFileButton).setDisable(true);
            checkNotNull(getFileUrlButton).setDisable(true);
        });

        loadNextBatch(0);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
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

    public class FiddMessageTreeItem extends TreeItem<FiddTreeNode> {
        private boolean childrenLoaded = false;
        private final long messageNumber;

        public FiddMessageTreeItem(FiddMessageNode fiddMessageNode) {
            super(fiddMessageNode, fiddMessageNode.getImage());
            messageNumber = fiddMessageNode.messageNumber;

            // Add dummy child so the expand arrow appears
            this.getChildren().add(new TreeItem<>(new FiddTextNode("Loading...")));

            // Load children when expanded
            this.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && !childrenLoaded) {
                    childrenLoaded = true;
                    loadChildren();
                }
            });
        }

        public static void addLogicalFile(TreeItem<FiddTreeNode> root, LogicalFileInfo logicalFileInfo) {
            String relativePath = logicalFileInfo.metadata().filePath();
            String[] parts = relativePath.split("/");

            TreeItem<FiddTreeNode> current = root;

            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (!StringUtils.isBlank(part)) {
                    // Look for an existing child with this name
                    TreeItem<FiddTreeNode> child = findChild(current, part);
                    if (child == null) {
                        if (i == parts.length - 1) {
                            // Add file
                            FiddFileNode fileNode = new FiddFileNode(part, logicalFileInfo);
                            child = new TreeItem<>(fileNode, fileNode.getImage());
                        } else {
                            // Add folder
                            FiddFolderNode folderNode = new FiddFolderNode(part);
                            child = new TreeItem<>(folderNode, folderNode.getImage());
                        }
                        current.getChildren().add(child);
                    }

                    current = child;
                }
            }
        }

        private static @Nullable TreeItem<FiddTreeNode> findChild(TreeItem<FiddTreeNode> parent, String value) {
            for (TreeItem<FiddTreeNode> child : parent.getChildren()) {
                if (child.getValue().toString().equals(value)) {
                    return child;
                }
            }
            return null;
        }

        private void loadChildren() {
            Task<List<LogicalFileInfo>> task = new Task<>() {
                @Override
                protected List<LogicalFileInfo> call() {
                    return fiddContentService.getLogicalFileInfos(messageNumber);
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(
                () -> {
                    List<LogicalFileInfo> logicalFileInfos = task.getValue();

                    // Remove dummy
                    this.getChildren().clear();

                    for (LogicalFileInfo node : logicalFileInfos) {
                        String relativePath = node.metadata().filePath();
                        if (!StringUtils.isBlank(relativePath)) {
                            addLogicalFile(this, node);
                        }
                    }
                }));

            new Thread(task).start();
        }
    }

    protected void addFiddMessageToTree(FiddTreeNode node, int pos) {
        TreeItem<FiddTreeNode> treeItem;
        if (node instanceof FiddMessageNode) {
            treeItem = new FiddMessageTreeItem((FiddMessageNode) node);
        } else {
            treeItem = new TreeItem<>(node, node.getImage());
        }
        rootItem.getChildren().add(pos, treeItem);
    }

    protected List<FiddTreeNode> fetchNextNodesFromServerOrDB() {
        List<Long> messages;
        if (lastContiguouslyLoadedMessageNumber == null) {
            messages = fiddContentService.getMessageNumbersTail(MESSAGE_LOAD_BATCH_SIZE);
        } else {
            messages = fiddContentService.getMessageNumbersBefore(lastContiguouslyLoadedMessageNumber, MESSAGE_LOAD_BATCH_SIZE, false);
        }

        List<FiddTreeNode> list = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            long messageNumber = messages.get(i);
            FiddFileMetadata fiddFileMetadata = null;
            try {
                fiddFileMetadata = fiddContentService.getFiddFileMetadata(messageNumber);
                lastContiguouslyLoadedMessageNumber = messageNumber;
            } catch (Exception e) {
                LOGGER.debug("Message failed to load: blog " + blogName + " message " + messageNumber, e);
            }

            String treeNodeText = "#" + messageNumber + " " +
                    (fiddFileMetadata != null
                            ? fiddFileMetadata.postId() + " [v" + fiddFileMetadata.versionNumber() + "]"
                            : "(Can't load message)");
            list.add(new FiddMessageNode(treeNodeText, messageNumber));
        }
        if (messages.size() >= MESSAGE_LOAD_BATCH_SIZE) {
            list.add(new FiddExpandNode(FiddExpandNode.ExpandDirection.PREVIOUS));
        }
        return list;
    }

    long findMessageNumber(TreeItem<FiddTreeNode> treeItem) {
        while (treeItem.getParent() != null) {
            treeItem = treeItem.getParent();

            FiddTreeNode node = treeItem.getValue();
            if (node instanceof FiddMessageNode messageNode) {
                return messageNode.messageNumber;
            }
        }
        throw new RuntimeException("FiddMessage ancestor not found for FileNode");
    }

    public void saveFile() {
        TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
        if (treeItem != null) {
            FiddTreeNode node = treeItem.getValue();
            if (node instanceof FiddFileNode fileNode) {
                long messageNumber = findMessageNumber(treeItem);
                LogicalFileInfo logicalFileInfo = fileNode.logicalFileInfo;

                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialFileName(fileNode.fileName);
                File outputFileFile = fileChooser.showSaveDialog(checkNotNull(stage));

                try (InputStream in = fiddContentService.readLogicalFile(messageNumber, logicalFileInfo);
                    OutputStream out = new FileOutputStream(outputFileFile)) {

                    byte[] buffer = new byte[FILE_SAVE_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                JavaFxUtils.showMessage("File Saved", "File " + fileNode.fileName +
                        " saved to: " + outputFileFile.getAbsolutePath());
            }
        }
    }

    public void getFileUrl() {
        TreeItem<FiddTreeNode> treeItem = checkNotNull(fiddStructureTreeView).getSelectionModel().getSelectedItem();
        if (treeItem != null) {
            FiddTreeNode node = treeItem.getValue();
            if (node instanceof FiddFileNode fileNode) {
                // TODO: implement
            }
        }
    }
}
