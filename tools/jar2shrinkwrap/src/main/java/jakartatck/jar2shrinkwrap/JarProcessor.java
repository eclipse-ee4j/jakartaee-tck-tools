package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

/**
 * JarProcessor
 *
 * @author Scott Marlow
 */
public interface JarProcessor {

    void process(final ZipEntry entry);

    void saveOutput(final File FileInputArchive);

    ArrayList<String> getLibraries();

    ArrayList<String> getMetainf();

    ArrayList<String> getWebinf();

    ArrayList<String> getClasses();

    ArrayList<String> getOtherFiles();

}
