package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * JarFileProcessor
 *
 * @author Scott Marlow
 */
public class JarFileProcessor extends AbstractFileProcessor {

    public JarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
    }

    @Override
    public void saveOutput(final File fileInputArchive) {
        String testclient = "Client";
        File output = new File(fileInputArchive.getParentFile(), testclient + ".java");
        System.out.println("generating " + output.getName() + " for input file " + fileInputArchive.getName());
        output.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(output)) {
            saveOutput(fileWriter, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutput(Writer writer, boolean includeImports) {
        String testclient = "Client";
        String indent = "\t";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if(includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");

            }

            printWriter.println(indent+"@Deployment(testable = false)");
            printWriter.println(indent+"public static WebArchive getTestArchive() throws Exception {");
            // The libary jars
            // Class thisClass = MethodHandles.lookup().lookupClass();
            printWriter.println(indent.repeat(2)+"List<JavaArchive> warJars = LibraryUtil.getJars(#{});\n");

            // Start war creation
            printWriter.print(indent.repeat(2)+"return ShrinkWrap.create(WebArchive.class, ");
            printWriter.println("\"" + testclient + ".war\")");

            printWriter.println(indent.repeat(3)+".addAsLibraries(warJars)");

            for (String name : classes) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addClass(");
                    printWriter.print(name);
                    printWriter.println(".class)");
                }
            }
            for (String name : webinf) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addAsWebInfResource(\"");
                    printWriter.print(name);
                    printWriter.println("\")");
                }
            }
            printWriter.println("}");
        }
    }

}
