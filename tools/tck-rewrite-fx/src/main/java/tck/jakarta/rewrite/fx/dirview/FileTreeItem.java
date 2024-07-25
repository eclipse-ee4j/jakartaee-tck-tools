package tck.jakarta.rewrite.fx.dirview;

import io.quarkus.logging.Log;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;


/**
 * A wrapper around a File object that allows it to be used in a TreeView and
 * that allows one to search for a child node by relative path.
 */
public class FileTreeItem extends TreeItem<FileItem> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;
    private int pathIndex;

    /**
     *
     * @param f File from which a tree should be built
     * @param pathIndex index of the path to use for the node name
     */
    public FileTreeItem(FileItem f, int pathIndex) {
        super(f);
        this.pathIndex = pathIndex;
    }
    public FileTreeItem(File f) {
        this(new FileItem(f), 0);
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

    // Path interface
    public Path getFileName() {
        return getValue().getFile().toPath().getName(pathIndex);
    }
    public int getNameCount() {
        return pathIndex+1;
    }
    Path getPath() {
        return getValue().getFile().toPath();
    }

    public FileTreeItem resolve(@NotNull Path other) {
        if(other.isAbsolute()) {
            throw new IllegalArgumentException("other is not a relative path");
        }
        String fileName = other.getName(0).toString();
        if(fileName.isEmpty() || fileName.equals(".")) {
            return this;
        }
        // Look for a child with the same name
        //Log.info("Looking for child with name: " + fileName);
        for(TreeItem<FileItem> child : getChildren()) {
            FileTreeItem childItem = (FileTreeItem) child;
            //Log.infof("childItem: %s", childItem.getFileName());
            if(childItem.getFileName().toString().equals(fileName)) {
                if(other.getNameCount() == 1) {
                    return childItem;
                }
                return childItem.resolve(other.subpath(1, other.getNameCount()));
            }
        }

        return null;
    }


    //

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
            Arrays.sort(files);
            ObservableList<TreeItem<FileItem>> children = FXCollections
                    .observableArrayList();

            // Exclude build.xml files
            for (File childFile : files) {
                if(childFile.getName().equals("build.xml")) {
                    continue;
                }
                FileItem treeItem = new FileItem(childFile);
                children.add(new FileTreeItem(treeItem, pathIndex+1));
            }

            return children;
        }

        return FXCollections.emptyObservableList();
    }

}