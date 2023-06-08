package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * EarFileProcessor
 *
 * @author Scott Marlow
 */
public class EarFileProcessor extends AbstractFileProcessor {
    public EarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
        libDir = new File(archiveFile.getAbsolutePath()+".lib");
        if(!libDir.exists()) {
            libDir.mkdirs();
        }

    }

    @Override
    public void process(ZipInputStream zipInputStream, ZipEntry entry) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.getName().startsWith("lib/")) {
            String jarName = entry.getName().substring("lib/".length());
            File libFile = new File(libDir, jarName);
            if (!libFile.exists()) { // Typical usage for EAR is that module archives will already exist but if not, create them)
                try (FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                    byte[] libContent = zipInputStream.readAllBytes();
                    libFileOS.write(libContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            addLibrary(entry.getName());
        } else if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war") ) {
            String jarName = entry.getName().substring("lib/".length());
            File libFile = new File(libDir, jarName);
            if (!libFile.exists()) { // Typical usage for EAR is that module archives will already exist but if not, create them)
                try (FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                    byte[] libContent = zipInputStream.readAllBytes();
                    libFileOS.write(libContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            JarVisit subModuleVisitor = new JarVisit(libFile);
            JarProcessor subModuleProcessor = subModuleVisitor.execute();
            addModule(subModuleProcessor);
        } else {
            super.process(zipInputStream, entry);
        }
    }

    @Override
    public void saveOutput(Writer writer, boolean includeImports) {

    }

    @Override
    public void saveOutput(File FileInputArchive) {

    }
}
