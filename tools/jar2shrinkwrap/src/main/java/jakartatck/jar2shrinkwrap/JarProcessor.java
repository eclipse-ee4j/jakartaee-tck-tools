package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JarProcessor
 *
 * @author Scott Marlow
 */
public interface JarProcessor {

    void process(final ZipInputStream zipInputStream, final ZipEntry entry);

    /**
     * Write the Arquillian @Deployment method java code into the given StringWriter.
     * @param writer - writer to use for the deployment method java source text
     * @param includeImports - flag to indicate whether the arquillian imports should be added to the
     *                       source output
     */
    void saveOutput(Writer writer, boolean includeImports);

    void saveOutput(final File FileInputArchive);

    ArrayList<String> getLibraries();
    File getLibDir();

    ArrayList<String> getMetainf();

    ArrayList<String> getWebinf();

    ArrayList<String> getClasses();

    ArrayList<String> getOtherFiles();

}
