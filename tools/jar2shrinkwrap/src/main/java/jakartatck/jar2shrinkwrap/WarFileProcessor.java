package jakartatck.jar2shrinkwrap;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public void process(ZipInputStream zipInputStream, ZipEntry entry) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith("WEB-INF/classes/")) {
            addClass(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/lib/")) {
            String jarName = entry.getName().substring("WEB-INF/lib/".length());
            File libFile = new File(baseDir, jarName);
            processLibrary(jarName, libFile, zipInputStream);
        } else if (entry.toString().startsWith("WEB-INF/")) {
            addWebinf(entry.getName().substring("WEB-INF/".length()));
        } else {
            super.process(zipInputStream, entry);
        }
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
            // The library jars
            if(getLibraries().size() > 0) {
            /* The #{} here is a parameter substitution indicator for the test class being processed
            https://docs.openrewrite.org/concepts-explanations/javatemplate#untyped-substitution-indicators
             */
                printWriter.println(indent.repeat(2) + "// TODO, check the library jar classes\n");
                // Write out the classes seen in the EE10 jars in a comment as a hint
                List<File> libraryFiles = new ArrayList<>();
                for (String jarName : getLibraries()) {
                    File jarFile = new File(getBaseDir(), jarName);
                    libraryFiles.add(jarFile);
                }
                List<JavaArchive> warJars = libraryFiles.stream()
                        .map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))
                        .toList();
                printWriter.println("/*");
                for (JavaArchive jar : warJars) {
                    printWriter.print("%sWEB-INF/lib/%s\n".formatted(indent.repeat(2), jar.getName()));
                    Map<ArchivePath, Node> content = jar.getContent();
                    for (ArchivePath path : content.keySet()) {
                        Asset asset = content.get(path).getAsset();
                        if (asset != null) {
                            printWriter.print("%s%s\n".formatted(indent.repeat(3), path.get()));
                        }
                    }
                }
                printWriter.println("*/");
            }
            // The code only contains a stub class to the LibraryUtil.getJars() method
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
                    printWriter.println("\");");
                }
            }
            // I don't think this is valid in general as a custom manifest would not be added to a deployment
            /*
            for (String name : metainf) {
                printWriter.print(".addAsManifestResource(");
                printWriter.print(name);
                printWriter.println(")");
            }
            */
            printWriter.println("}");
        }
    }

 }
