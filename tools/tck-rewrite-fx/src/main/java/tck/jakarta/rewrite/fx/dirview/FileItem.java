package tck.jakarta.rewrite.fx.dirview;

import java.io.File;

public class FileItem {
    private File file;
    public FileItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
    public String toString() {
        return file.getName();
    }

    public boolean isFile() {
        return file.isFile();
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }
    public File[] listFiles() {
        return file.listFiles();
    }
}
