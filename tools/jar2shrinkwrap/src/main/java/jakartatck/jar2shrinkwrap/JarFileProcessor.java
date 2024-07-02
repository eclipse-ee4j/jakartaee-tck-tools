package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * JarFileProcessor
 *
 * @author Scott Marlow
 */
public class JarFileProcessor extends AbstractFileProcessor {
    private static final Logger log = Logger.getLogger(Jar2ShrinkWrap.class.getName());
    public JarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
    }

    @Override
    public void saveOutput(final File fileInputArchive) {
        String testclient = "Client";
        File output = new File(fileInputArchive.getParentFile(), testclient + ".java");
        log.fine("generating " + output.getName() + " for input file " + fileInputArchive.getName());
        output.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(output)) {
            saveOutput(fileWriter, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutput(Writer writer, boolean includeImports) {
        String indent = " ";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if(includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");

            }

            printWriter.println(indent+"@Deployment(testable = false)");
            printWriter.println(indent+"public static JavaArchive getJarTestArchive() throws Exception {");

            for (String name : classes) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addClass(");
                    printWriter.print(name);
                    printWriter.println(")");
                }
            }
            for (String name : metainf) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addAsManifestResource(\"");
                    printWriter.print(name);
                    printWriter.println("\")");
                }
            }
            printWriter.println("}");
        }
    }

}
