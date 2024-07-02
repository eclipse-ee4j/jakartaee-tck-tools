package tck.jakarta.platform.rewrite.shrinkwrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakartatck.jar2shrinkwrap.EarFileProcessor;
import jakartatck.jar2shrinkwrap.JarFileProcessor;
import jakartatck.jar2shrinkwrap.JarProcessor;
import jakartatck.jar2shrinkwrap.WarFileProcessor;

/**
 * CreateNewEETest
 *
 * @author Scott Marlow
 */
public class TestGenerator {

    public static String saveOutput(JarProcessor jarProcessor) {
        if (jarProcessor instanceof EarFileProcessor earFileProcessor) {
            return saveOutput(earFileProcessor);
        } else if (jarProcessor instanceof WarFileProcessor) {
            WarFileProcessor warFileProcessor = (WarFileProcessor) jarProcessor;
            return saveOutput(warFileProcessor);
        } else {
            JarFileProcessor jarFileProcessor = (JarFileProcessor) jarProcessor;
            return saveOutput(jarFileProcessor);
        }
    }

    public static File generateJavaSourceFileContent(JarProcessor jarProcessor, Set<String> testMethodNameSet, String testPackageName, String testClientClassName) throws IOException {
        String deploymentMethod;
        String generateEETestClassName = "EE" + testClientClassName;
        File outputFile = new File(generateEETestClassName + ".java");
        Writer writer = new FileWriter(outputFile);
        if (jarProcessor instanceof EarFileProcessor earFileProcessor) {
            deploymentMethod = saveOutput(earFileProcessor);
        } else if (jarProcessor instanceof WarFileProcessor) {
            WarFileProcessor warFileProcessor = (WarFileProcessor) jarProcessor;
            deploymentMethod = saveOutput(warFileProcessor);
        } else {
            JarFileProcessor jarFileProcessor = (JarFileProcessor) jarProcessor;
            deploymentMethod = saveOutput(jarFileProcessor);
        }
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            printWriter.printf("package %s;\n", testPackageName);
            printWriter.println("import org.jboss.arquillian.config.descriptor.api.DefaultProtocolDef;");
            printWriter.println("import org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar;");
            printWriter.println("import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;");
            printWriter.println("import org.jboss.arquillian.container.test.impl.MapObject;");
            // printWriter.println("import org.jboss.arquillian.protocol.appclient.AppClientProtocolConfiguration;");
            printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
            printWriter.println("import org.jboss.shrinkwrap.api.Archive;");
            printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
            printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
            printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
            printWriter.println("import org.junit.Assert;");
            printWriter.println("import org.junit.Test;");
            printWriter.println("");
            printWriter.println("import java.io.File;");
            printWriter.println("import java.io.IOException;");
            printWriter.println("import java.util.Map;");
            printWriter.println("");

            printWriter.printf("public class %s extends %s {", generateEETestClassName, testClientClassName);

            printWriter.println("" );
            printWriter.printf(deploymentMethod );

            for (String methodName: testMethodNameSet) {
                printWriter.println("\n@Test" );
                printWriter.printf("public void %s() throws Exception { ", methodName );

                printWriter.println("}");
            }
            printWriter.println("}");


        }

        writer.close();
        return outputFile;
    }

    public static String saveOutput(JarFileProcessor jarProcessor) {
        StringWriter writer = new StringWriter();
        boolean includeImports = false;
        final String indent = " ";

        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if (includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");

            }

            printWriter.println(indent + "@Deployment(testable = false)");
            printWriter.println(indent + "public static JarArchive getJarTestArchive() throws Exception {");
            for (String name : jarProcessor.getClasses()) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addClass(");
                    printWriter.print(name);
                    printWriter.println(")");
                }
            }
            for (String name : jarProcessor.getMetainf()) {
                if (!ignoreFile(name)) {
                    printWriter.print(indent.repeat(3) + ".addAsManifestResource(\"");
                    printWriter.print(name);
                    printWriter.println("\")");
                }
            }
            printWriter.println("}");
            return writer.toString();
        }
    }

    public static void saveOutputWar(WarFileProcessor warProcessor, PrintWriter printWriter, boolean retWarArchive) {
        String archiveName = warProcessor.getArchivePath().toFile().getName();
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
            for (String className : warLibraryProcessor.getClasses()) {
                if (!ignoreFile(className)) {
                    printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(warlibrary), className));
                }
            }
            for (String otherFile : warLibraryProcessor.getOtherFiles()) {
                if (!ignoreFile(otherFile)) {
                    printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), otherFile));
                }
            }
            for (String metainf : warLibraryProcessor.getMetainf()) {
                if (!ignoreFile(metainf)) {
                    printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), metainf));
                }
            }
            printWriter.println(indent.repeat(1) + "%s.addAsLibrary(%s);".formatted(archiveName(archiveName), archiveName(warlibrary)));
            printWriter.println(newLine + indent + "}");  // we can add multiple variations of the same archive so enclose it in a code block
        }
        // add classes
        for (String className : warProcessor.getClasses()) {
            if (!ignoreFile(className)) {
                printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(archiveName), className));
            }
        }
        if (retWarArchive) {
            printWriter.println(indent + "return %s;".formatted(archiveName(archiveName)));
        }
    }

    public static String saveOutput(WarFileProcessor warProcessor) {
        boolean includeImports = false;
        StringWriter writer = new StringWriter();
        final String indent = " ";
        final String newLine = "\n";

        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if (includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.JavaArchive;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
                printWriter.println("import jakartatck.jar2shrinkwrap.LibraryUtil;\n");
            }

            printWriter.println(indent + "@Deployment(testable = false)");
            printWriter.println(indent + "public static WebArchive getWarTestArchive() throws Exception {");
            saveOutputWar(warProcessor, printWriter, true);
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
            printWriter.println("public static Archive<?> getEarTestArchive() {");
            printWriter.println(newLine + indent.repeat(1) + "final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, \"%s\");".formatted(earProcessor.getArchivePath().toFile().getName()));
            // The EAR library jars
            if (earProcessor.getLibraries().size() > 0) {
            /* The #{} here is a parameter substitution indicator for the test class being processed
            https://docs.openrewrite.org/concepts-explanations/javatemplate#untyped-substitution-indicators
             */
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
            if (earProcessor.getSubModules().size() > 0) {
                printWriter.println(indent.repeat(1) + "// Add ear submodules");
                for (String archiveName : earProcessor.getSubModules()) {
                    JarProcessor jarProcessor = earProcessor.getSubmodule(archiveName);
                    printWriter.println(newLine + indent + "{");  // we can add multiple variations of the same archive so enclose it in a code block
                    if (jarProcessor instanceof WarFileProcessor) {
                        saveOutputWar((WarFileProcessor) jarProcessor, printWriter, false);
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
        return filename.isEmpty() || filename.endsWith(".java") || filename.contains("com.sun.ts.lib.harness");
    }

}
