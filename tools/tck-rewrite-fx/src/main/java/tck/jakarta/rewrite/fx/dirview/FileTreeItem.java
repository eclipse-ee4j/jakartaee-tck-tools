package tck.jakarta.rewrite.fx.dirview;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.File;

public class FileTreeItem extends TreeItem<FileItem> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;

    /**
     *
     * @param f File from which a tree should be built
     */
    public FileTreeItem(FileItem f) {
        super(f);
    }
    public FileTreeItem(File f) {
        this(new FileItem(f));
    }

    /*

     */
    @Override
    public ObservableList<TreeItem<FileItem>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;

            /*
             * First getChildren() call, so we actually go off and determine the
             * children of the File contained in this TreeItem.
             */
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    /*
     * Is this a file (leaf) or a directory
     */
    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            FileItem f = getValue();
            isLeaf = f.isFile();
        }

        return isLeaf;
    }

    /**
     * Returning a collection of type ObservableList containing TreeItems, which
     * represent all children available in handed TreeItem.
     *
     * @param TreeItem
     *            the root node from which children a collection of TreeItem
     *            should be created.
     * @return an ObservableList<TreeItem<File>> containing TreeItems, which
     *         represent all children available in handed TreeItem. If the
     *         handed TreeItem is a leaf, an empty list is returned.
     */
    private ObservableList<TreeItem<FileItem>> buildChildren(TreeItem<FileItem> TreeItem) {
        FileItem f = TreeItem.getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<FileItem>> children = FXCollections
                        .observableArrayList();

                for (File childFile : files) {
                    FileItem treeItem = new FileItem(childFile);
                    children.add(new FileTreeItem(treeItem));
                }

                return children;
            }
        }

        return FXCollections.emptyObservableList();
    }

}