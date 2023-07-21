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

/**
 * EarFileProcessor
 *
 * @author Scott Marlow
 */
public class EarFileProcessor extends AbstractFileProcessor {
    private Map<String, JarProcessor> subModuleContent = new HashMap<>();

    public EarFileProcessor(File archiveFile) {
        this.archiveFile = archiveFile;
        baseDir = new File(archiveFile.getParentFile().getAbsolutePath());
        if(!baseDir.exists()) {
            baseDir.mkdirs();
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
            File libFile = new File(baseDir, jarName);
            processLibrary(jarName, libFile, zipInputStream);
        } else if (entry.getName().endsWith(".jar") || entry.getName().endsWith(".war") ) {
            String jarName = entry.getName();
            File libFile = new File(baseDir, jarName);
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
        final String indent = "\t";
        final String newLine = "\n";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if(includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;" + newLine);

            }

            printWriter.println("@Deployment(testable = false)");
            printWriter.println("public static Archive<?> deployment() {");
            printWriter.println(newLine + indent.repeat(1) +"final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, \"%s\");".formatted(archiveFile.getName()));
            // The EAR library jars
            if(getLibraries().size() > 0) {
            /* The #{} here is a parameter substitution indicator for the test class being processed
            https://docs.openrewrite.org/concepts-explanations/javatemplate#untyped-substitution-indicators
             */
                printWriter.println(indent.repeat(1) + "// TODO: filter/eliminate the library jar classes that shouldn't be included");
                printWriter.println(indent.repeat(1) + "// Add ear/lib jars");
                // Write out the classes seen in the EE10 jars in a comment as a hint
                List<File> libraryFiles = new ArrayList<>();
                for (String archiveName : getLibraries()) {
                    JarProcessor jarProcessor = getLibrary(archiveName);
                    printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
                    for (String className: jarProcessor.getClasses()) {
                        if (!ignoreFile(className)) {
                            printWriter.println(indent + "%s.addClass(\"%s\");".formatted(archiveName(archiveName), className));
                        }
                    }
                    printWriter.println(indent.repeat(1)+"ear.addAsLibrary(%s);".formatted(archiveName(archiveName)));
                }

            }
            if (getSubModules().size() > 0) {
                printWriter.println(indent.repeat(1) + "// Add ear submodules");
                for (String archiveName : getSubModules()) {
                    JarProcessor jarProcessor = getSubmodule(archiveName);
                    if ( jarProcessor instanceof WarFileProcessor) {
                        jarProcessor.saveOutputWar(printWriter,includeImports, archiveName);
                    } else {
                        // JavaArchive jar  = ShrinkWrap.create(JavaArchive.class);
                        printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
                    }
                    // add war/jar to ear
                    printWriter.println(indent+"ear.addModule(\"%s\");".formatted(archiveName(archiveName)));
                    // add classes
                    for (String className: jarProcessor.getClasses()) {
                        if (!ignoreFile(className)) {
                            printWriter.println(indent + "%s.addClass(\"%s\");".formatted(archiveName(archiveName), className));
                        }
                    }

                }

            }

            printWriter.println(indent.repeat(1)+"return ear;");
            printWriter.println("}");
        }
    }

}
