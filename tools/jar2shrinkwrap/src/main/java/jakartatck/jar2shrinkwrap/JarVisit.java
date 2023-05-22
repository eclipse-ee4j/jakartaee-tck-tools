package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JarVisit
 *
 * @author Scott Marlow
 */
public class JarVisit {

    private final String targetFolder;
    private final String archiveFile;

    public JarVisit(String archiveFile, String targetFolder) {
        this.archiveFile = archiveFile;
        this.targetFolder = targetFolder;
    }

    public void execute() {
        File fileTargetFolder = new File(targetFolder);
        if (!fileTargetFolder.exists()) {
            fileTargetFolder.mkdirs();
        }
        JarProcessor jarProcessor;
        if (archiveFile.endsWith(".war"))
            jarProcessor = new WarFileProcessor(archiveFile, fileTargetFolder);
        else
            throw new IllegalStateException("unsupported file type extension: " + archiveFile);

        System.out.println("output will be in " + fileTargetFolder.getName());
        final byte[] buffer = new byte[100 * 1024];
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archiveFile));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                jarProcessor.process(entry);
                entry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
            jarProcessor.saveOutput();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
