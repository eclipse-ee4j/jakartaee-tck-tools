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

    private final File archiveFile;


    public JarVisit(File target) {
        this.archiveFile = target;
    }

    public JarProcessor execute() {

        if (archiveFile.isDirectory()) {
            throw new RuntimeException("Specify an archive file name instead of a folder name.");
        }

        JarProcessor jarProcessor;
        if (archiveFile.getName().endsWith(".war"))
            jarProcessor = new WarFileProcessor(archiveFile);
        else if (archiveFile.getName().endsWith(".jar"))
            jarProcessor = new JarFileProcessor(archiveFile);
        else if (archiveFile.getName().endsWith(".ear"))
            jarProcessor = new EarFileProcessor(archiveFile);
        else
            throw new IllegalStateException("unsupported file type extension: " + archiveFile);
        final byte[] buffer = new byte[100 * 1024];
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archiveFile));
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                jarProcessor.process(zipInputStream, entry);
                entry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
            return jarProcessor;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
