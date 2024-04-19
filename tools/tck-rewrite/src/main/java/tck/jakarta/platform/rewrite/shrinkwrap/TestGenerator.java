package tck.jakarta.platform.rewrite.shrinkwrap;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import jakartatck.jar2shrinkwrap.EarFileProcessor;
import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarFileProcessor;
import jakartatck.jar2shrinkwrap.JarProcessor;
import jakartatck.jar2shrinkwrap.WarFileProcessor;

/**
 * TestGenerator
 *
 * @author Scott Marlow
 */
public class TestGenerator {

    public static String saveOutput(JarProcessor jarProcessor) {
        if (jarProcessor instanceof EarFileProcessor earFileProcessor) {
            return saveOutput(earFileProcessor);
        } else if (jarProcessor instanceof WarFileProcessor) {
            WarFileProcessor warFileProcessor = (WarFileProcessor)jarProcessor;
            return saveOutput(warFileProcessor);
        } else {
            JarFileProcessor jarFileProcessor = (JarFileProcessor)jarProcessor;
            return saveOutput(jarFileProcessor);
        }
    }

    public static String saveOutput(JarFileProcessor jarProcessor) {
        StringWriter writer = new StringWriter();
        boolean includeImports = false;
        final String indent = " ";

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
            /* The library jars
            // Class thisClass = MethodHandles.lookup().lookupClass();
            printWriter.println(indent.repeat(2)+"List<JavaArchive> warJars = LibraryUtil.getJars(#{});\n");

            // Start war creation
            printWriter.print(indent.repeat(2)+"return ShrinkWrap.create(WebArchive.class, ");
            printWriter.println("\"" + testclient + ".war\")");

            printWriter.println(indent.repeat(3)+".addAsLibraries(warJars)");
            */

            for (String name : jarProcessor.getClasses()) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addClass(");
                    printWriter.print(name);
                    printWriter.println(")");
                }
            }
            for (String name : jarProcessor.getWebinf()) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addAsWebInfResource(\"");
                    printWriter.print(name);
                    printWriter.println("\")");
                }
            }
            printWriter.println("}");
            return writer.toString();
        }
    }

    public static void saveOutputWar(WarFileProcessor warProcessor, PrintWriter printWriter ) {
        String archiveName =  warProcessor.getArchivePath().toFile().getName();
        final String newLine = "\n";
        final String indent = " ";
            // WebArchive war = ShrinkWrap.create(WebArchive.class, name)
            printWriter.println(newLine + indent + "WebArchive %s = ShrinkWrap.create(WebArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
            for (String webinfFile : warProcessor.getWebinf()) {
                if (!ignoreFile(webinfFile)) {
                    printWriter.println(indent.repeat(3) + "%s.addAsWebInfResource(\"%s\");".formatted(archiveName(archiveName), webinfFile));
                }
            }
            for (String otherFile : warProcessor.getOtherFiles()) {
                if (!ignoreFile(otherFile)) {
                    printWriter.println(indent.repeat(3) + "%s.addAsWebResource(\"%s\");".formatted(archiveName(archiveName), otherFile));
                }
            }

            for (String warlibrary : warProcessor.getLibraries()) {
                JarProcessor warLibraryProcessor = warProcessor.getLibrary(warlibrary);
                printWriter.println(newLine + indent + "{");  // we can add multiple variations of the same archive so enclose it in a code block
                printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(warlibrary), warlibrary));
                for (String className: warLibraryProcessor.getClasses()) {
                    if (!ignoreFile(className)) {
                        printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(warlibrary), className));
                    }
                }
                for (String otherFile: warLibraryProcessor.getOtherFiles()) {
                    if (!ignoreFile(otherFile)) {
                        printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), otherFile));
                    }
                }
                for (String metainf : warLibraryProcessor.getMetainf()) {
                    if (!ignoreFile(metainf)) {
                        printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), metainf));
                    }
                }
                printWriter.println(indent.repeat(1)+"%s.addAsLibrary(%s);".formatted(archiveName(archiveName),archiveName(warlibrary)));
                printWriter.println(newLine + indent + "}");  // we can add multiple variations of the same archive so enclose it in a code block
            }
            // add classes
            for (String className: warProcessor.getClasses()) {
                if (!ignoreFile(className)) {
                    printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(archiveName), className));
                }
            }
    }

    public static String saveOutput(WarFileProcessor warProcessor) {
        boolean includeImports = false;
        StringWriter writer = new StringWriter();
        final String indent = " ";
        final String newLine = "\n";

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
            saveOutputWar(warProcessor, printWriter);
            printWriter.println("}");
            return writer.toString();
        }
    }

    public static String saveOutput(EarFileProcessor earProcessor) {
        StringWriter writer = new StringWriter();
        boolean includeImports = false;
        final String indent = " ";
        final String newLine = "\n";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if (includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.Archive;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;");
            }

            printWriter.println("@Deployment(testable = false)");
            printWriter.println("public static Archive<?> deployment() {");
            printWriter.println(newLine + indent.repeat(1) + "final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, \"%s\");".formatted(earProcessor.getArchivePath().toFile().getName()));
            // The EAR library jars
            if (earProcessor.getLibraries().size() > 0) {
            /* The #{} here is a parameter substitution indicator for the test class being processed
            https://docs.openrewrite.org/concepts-explanations/javatemplate#untyped-substitution-indicators
             */
                printWriter.println(indent.repeat(1) + "// TODO: filter/eliminate the library jar classes that shouldn't be included");
                printWriter.println(indent.repeat(1) + "// Add ear/lib jars");
                // Write out the classes seen in the EE10 jars in a comment as a hint
                List<File> libraryFiles = new ArrayList<>();
                for (String archiveName : earProcessor.getLibraries()) {
                    JarProcessor jarProcessor = earProcessor.getLibrary(archiveName);
                    printWriter.println(newLine + indent + "{");  // we can add multiple variations of the same archive so enclose it in a code block
                    printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
                    for (String className : jarProcessor.getClasses()) {
                        if (!ignoreFile(className)) {
                            printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(archiveName), className));
                        }
                    }
                    printWriter.println(indent.repeat(1) + "ear.addAsLibrary(%s);".formatted(archiveName(archiveName)));
                    printWriter.println(newLine + indent + "}");  // we can add multiple variations of the same archive so enclose it in a code block
                }

            }
            // TODO: these need to be built up the same as library jars
            if (earProcessor.getSubModules().size() > 0) {
                printWriter.println(indent.repeat(1) + "// Add ear submodules");
                for (String archiveName : earProcessor.getSubModules()) {
                    JarProcessor jarProcessor = earProcessor.getSubmodule(archiveName);
                    printWriter.println(newLine + indent + "{");  // we can add multiple variations of the same archive so enclose it in a code block
                    if (jarProcessor instanceof WarFileProcessor) {
                        saveOutputWar((WarFileProcessor)jarProcessor, printWriter);
                     } else {
                        // JavaArchive jar  = ShrinkWrap.create(JavaArchive.class);
                        printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
                        for (String className : jarProcessor.getClasses()) {
                            if (!ignoreFile(className)) {
                                printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(archiveName), className));
                            }
                        }
                    }
                    // add war/jar to ear
                    printWriter.println(indent + "ear.addAsModule(%s);".formatted(archiveName(archiveName)));
                    printWriter.println(newLine + indent + "}");  // we can add multiple variations of the same archive so enclose it in a code block
                }
            }

            printWriter.println(indent.repeat(1) + "return ear;");
            printWriter.println("}");
        }
        return writer.toString();
    }

    protected static String archiveName(String archiveName) {
        return archiveName.replace(".", "_");
    }

    protected static boolean ignoreFile(String filename) {
        return filename.isEmpty() || filename.endsWith(".java") || filename.contains("com.sun.ts.lib.");
    }

}
