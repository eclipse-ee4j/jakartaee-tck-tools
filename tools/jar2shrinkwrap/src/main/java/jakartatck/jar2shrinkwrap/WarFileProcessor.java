package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * WarFileProcessor
 *
 * @author Scott Marlow
 */
public class WarFileProcessor extends AbstractFileProcessor {


    public WarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
        baseDir = new File(archiveFile.getAbsolutePath()+".lib");
        if(!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void process(ZipInputStream zipInputStream, ZipEntry entry, ClassNameRemapping classNameRemapping) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith("WEB-INF/classes/")) {
            addClass(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/lib/")) {
            String jarName = entry.getName().substring("WEB-INF/lib/".length());
            File libFile = new File(baseDir, jarName);
            processLibrary(jarName, libFile, zipInputStream, classNameRemapping);
        } else if (entry.toString().startsWith("WEB-INF/")) {
            addWebinf(entry.getName().substring("WEB-INF/".length()));
        } else {
            super.process(zipInputStream, entry, classNameRemapping);
        }
    }

    @Override
    public void saveOutput(Writer writer, boolean includeImports) {
        String testclient = "Client";
        String indent = " ";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if(includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");

            }

            printWriter.println(indent+"@Deployment(testable = false)");
            printWriter.println(indent+"public static WebArchive getWarTestArchive() throws Exception {");
            saveOutputWar(printWriter,includeImports, archiveFile.getName());
            printWriter.println("}");
        }
    }

 }
