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

    public ArrayList<String> getLibraries();

    public ArrayList<String> getMetainf();

    public ArrayList<String> getWebinf();

    public ArrayList<String> getClasses();

    public ArrayList<String> getOtherFiles();

}
