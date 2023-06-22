package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * EarFileProcessor
 *
 * @author Scott Marlow
 */
public class EarFileProcessor extends AbstractFileProcessor {
    private HashMap<String, JarProcessor> subModuleContent = new HashMap<>();

    public EarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
        libDir = new File(archiveFile.getParentFile().getAbsolutePath());
        if(!libDir.exists()) {
            libDir.mkdirs();
        }

    }

    public JarProcessor getSubmodule(String name) {
        return subModuleContent.get(name);
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
            addLibrary(libFile.getName());
        } else if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war") ) {
            String jarName = entry.getName();
            File libFile = new File(libDir, jarName);
            if (!libFile.exists()) { // Typical usage for EAR is that module archives will already exist but if not, create them)
                try (FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                    byte[] libContent = zipInputStream.readAllBytes();
                    libFileOS.write(libContent);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // Load the submodule content
            JarVisit visit = new JarVisit(libFile);
            JarProcessor jar = visit.execute();
            subModuleContent.put(jarName, jar);
            addModule(libFile.getName());
        } else {
            super.process(zipInputStream, entry);
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
                printWriter.println("import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;\n");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");

            }

            printWriter.println(indent+"@Deployment(testable = false)");
            printWriter.println(indent+"public static Archive<?> deployment() {");
            printWriter.print(indent+"final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, \"%s\"\n);".formatted(archiveFile.getName()));
            // The libary jars
            if(getLibraries().size() > 0) {
            /* The #{} here is a parameter substitution indicator for the test class being processed
            https://docs.openrewrite.org/concepts-explanations/javatemplate#untyped-substitution-indicators
             */
                printWriter.println(indent.repeat(2) + "// TODO, check the library jar classes\n");
                // Write out the classes seen in the EE10 jars in a comment as a hint
                List<File> libraryFiles = new ArrayList<>();
                for (String jarName : getLibraries()) {
                    File jarFile = new File(getLibDir(), jarName);
                    libraryFiles.add(jarFile);
                }
                List<JavaArchive> EarLibJars = libraryFiles.stream()
                        .map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))
                        .toList();
                printWriter.println("/* Add each jar via ear.addAsLibrary() \n");
                for (JavaArchive jar : EarLibJars) {
                    printWriter.print("%s/lib/%s\n".formatted(indent.repeat(2), jar.getName()));
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
                printWriter.print(indent.repeat(3)+".addClass(");
                printWriter.print(name);
                printWriter.println(".class)");
            }
            for (String name : webinf) {
                printWriter.print(indent.repeat(3)+".addAsWebInfResource(\"");
                printWriter.print(name);
                printWriter.println("\");");
            }
            printWriter.println("}");
        }
    }


}
