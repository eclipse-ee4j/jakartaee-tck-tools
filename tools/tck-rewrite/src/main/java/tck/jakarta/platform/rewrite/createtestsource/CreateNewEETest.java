package tck.jakarta.platform.rewrite.createtestsource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
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
public class CreateNewEETest {

    public static File generateJavaSourceFileContent(JarProcessor jarProcessor, Set<String> testMethodNameSet, String testPackageName, String testClientClassName) throws IOException {
        String generateEETestClassName;
        if (testClientClassName.equals("EEClient")) {
            // write over current file
            generateEETestClassName = testClientClassName;
        } else {
            generateEETestClassName = "EE" + testClientClassName;
        }
        File outputFile = new File(generateEETestClassName + ".java");
        if (outputFile.exists()) {
            if (!outputFile.delete()) { // delete the current file contents
                throw new IOException("could not delete " + outputFile.getName());
            }
        }
        Writer writer = new FileWriter(outputFile);
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            CreateTestSourceFile testSourceFile = new CreateTestSourceFile(printWriter);

            testSourceFile.addPackage(testPackageName).
                    addImport("org.jboss.arquillian.config.descriptor.api.DefaultProtocolDef").
                    addImport("org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar").
                    addImport("org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor").
                    addImport("org.jboss.arquillian.container.test.impl.MapObject").
                    addImport("org.jboss.arquillian.container.test.api.Deployment").
                    addImport("org.jboss.shrinkwrap.api.Archive").
                    addImport("org.jboss.shrinkwrap.api.ShrinkWrap").
                    addImport("org.jboss.shrinkwrap.api.spec.JavaArchive").
                    addImport("org.jboss.shrinkwrap.api.spec.WebArchive").
                    addImport("org.jboss.shrinkwrap.api.spec.EnterpriseArchive").
                    // addImport("org.jboss.arquillian.protocol.appclient.AppClientProtocolConfiguration").
                    // addImport("org.junit.Assert").
                    addImport("org.junit.jupiter.api.Assertions").
                    addImport("org.junit.jupiter.api.Test").
                    // addImport("org.junit.Test").
                    emptyLine().
                    addImport("java.io.File").
                    addImport("java.io.IOException").
                    addImport("java.util.Map").
                    emptyLine().
                    publicClass(generateEETestClassName, testClientClassName).
                    emptyLine();

            if (jarProcessor instanceof EarFileProcessor earFileProcessor) {
                saveOutput(earFileProcessor, testSourceFile);
            } else if (jarProcessor instanceof WarFileProcessor warFileProcessor) {
                saveOutput(warFileProcessor, testSourceFile);
            } else {
                JarFileProcessor jarFileProcessor = (JarFileProcessor) jarProcessor;
                saveOutput(jarFileProcessor, testSourceFile);
            }
            for (String methodName : testMethodNameSet) {
                testSourceFile.methodAnnotation("@Test").
                        addTestMethod(methodName).
                        startBlock().
                        endBlock();
            }
            testSourceFile.endBlock();
        }
        writer.close();
        return outputFile;
    }

    public static void saveOutput(EarFileProcessor earProcessor, CreateTestSourceFile createTestSourceFile) {

        createTestSourceFile.addMethod("@Deployment(testable = false)",
                "public static Archive<?> getEarTestArchive()",
                "final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, \"" + earProcessor.getArchivePath().toFile().getName() + "\");\n");

        // The EAR library jars
        if (earProcessor.getLibraries().size() > 0) {
            // Write out the classes seen in the EE10 jars in a comment as a hint
            for (String archiveName : earProcessor.getLibraries()) {
                JarProcessor jarProcessor = earProcessor.getLibrary(archiveName);
                // we can add multiple variations of the same archive so enclose it in a code block
                createTestSourceFile.startBlock().
                        emptyLine().
                        addStatement(String.format("JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");\n".formatted(archiveName(archiveName), archiveName(archiveName))));
                for (String className : jarProcessor.getClasses()) {
                    if (!ignoreFile(className)) {
                        createTestSourceFile.addStatement(String.format("%s.addClass(%s);\n".formatted(archiveName(archiveName), className)));
                    }
                }
                createTestSourceFile.addStatement(String.format("ear.addAsLibrary(%s);\n".formatted(archiveName(archiveName)))).
                        emptyLine().
                        endBlock(); // we can add multiple variations of the same archive so mark end of code block
            }
        }
        if (earProcessor.getSubModules().size() > 0) {
            for (String archiveName : earProcessor.getSubModules()) {
                JarProcessor jarProcessor = earProcessor.getSubmodule(archiveName);
                createTestSourceFile.startBlock();  // we can add multiple variations of the same archive so enclose it in a code block
                if (jarProcessor instanceof WarFileProcessor) {
                    saveOutputWar((WarFileProcessor) jarProcessor, createTestSourceFile, false);
                } else {
                    // JavaArchive jar  = ShrinkWrap.create(JavaArchive.class);
                    createTestSourceFile.addStatement(String.format("JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");\n".formatted(archiveName(archiveName), archiveName(archiveName))));
                    for (String className : jarProcessor.getClasses()) {
                        if (!ignoreFile(className)) {
                            createTestSourceFile.addStatement(String.format("%s.addClass(%s);\n".formatted(archiveName(archiveName), className)));
                        }
                    }
                }
                // add war/jar to ear
                createTestSourceFile.addStatement(String.format("ear.addAsModule(%s);\n".formatted(archiveName(archiveName))));
                createTestSourceFile.emptyLine().
                        endBlock();  // we can add multiple variations of the same archive so mark end of code block
            }
        }

        createTestSourceFile.addStatement("return ear;\n");
        createTestSourceFile.endBlock();
    }

    public static void saveOutput(WarFileProcessor warProcessor, CreateTestSourceFile createTestSourceFile) {

        createTestSourceFile.methodAnnotation("@Deployment(testable = false)").
                addMethod("public static WebArchive getWarTestArchive   () throws Exception ");
        saveOutputWar(warProcessor, createTestSourceFile, true);
        createTestSourceFile.endBlock();
    }

    public static void saveOutputWar(WarFileProcessor warProcessor, CreateTestSourceFile createTestSourceFile, boolean retWarArchive) {
        String archiveName = warProcessor.getArchivePath().toFile().getName();
        // WebArchive war = ShrinkWrap.create(WebArchive.class, name)
        createTestSourceFile.addStatement(String.format("WebArchive %s = ShrinkWrap.create(WebArchive.class, \"%s\");\n".formatted(archiveName(archiveName), archiveName(archiveName))));
        for (String webinfFile : warProcessor.getWebinf()) {
            if (!ignoreFile(webinfFile)) {
                createTestSourceFile.addStatement(String.format("%s.addAsWebInfResource(\"%s\");\n".formatted(archiveName(archiveName), webinfFile)));
            }
        }
        for (String otherFile : warProcessor.getOtherFiles()) {
            if (!ignoreFile(otherFile)) {
                createTestSourceFile.addStatement(String.format("%s.addAsWebResource(\"%s\");\n".formatted(archiveName(archiveName), otherFile)));
            }
        }

        for (String warlibrary : warProcessor.getLibraries()) {
            JarProcessor warLibraryProcessor = warProcessor.getLibrary(warlibrary);
            createTestSourceFile.startBlock();  // we can add multiple variations of the same archive so enclose it in a code block
            createTestSourceFile.addStatement(String.format("JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");\n".formatted(archiveName(warlibrary), warlibrary)));
            for (String className : warLibraryProcessor.getClasses()) {
                if (!ignoreFile(className)) {
                    createTestSourceFile.addStatement(String.format("%s.addClass(%s);\n".formatted(archiveName(warlibrary), className)));
                }
            }
            for (String otherFile : warLibraryProcessor.getOtherFiles()) {
                if (!ignoreFile(otherFile)) {
                    createTestSourceFile.addStatement(String.format("%s.addAsManifestResource(\"%s\");\n".formatted(archiveName(warlibrary), otherFile)));
                }
            }
            for (String metainf : warLibraryProcessor.getMetainf()) {
                if (!ignoreFile(metainf)) {
                    createTestSourceFile.addStatement(String.format("%s.addAsManifestResource(\"%s\");\n".formatted(archiveName(warlibrary), metainf)));
                }
            }
            createTestSourceFile.addStatement(String.format("%s.addAsLibrary(%s);\n".formatted(archiveName(archiveName), archiveName(warlibrary))));
            createTestSourceFile.endBlock();  // we can add multiple variations of the same archive so enclose it in a code block
        }
        // add classes
        for (String className : warProcessor.getClasses()) {
            if (!ignoreFile(className)) {
                createTestSourceFile.addStatement("%s.addClass(%s);\n".formatted(archiveName(archiveName), className));
            }
        }
        if (retWarArchive) {
            createTestSourceFile.addStatement("return %s;\n".formatted(archiveName(archiveName)));
        }
    }

    public static void saveOutput(JarFileProcessor jarProcessor, CreateTestSourceFile createTestSourceFile) {
        throw new RuntimeException("not implemented yet"); // add handling code if this is called in the future
    }

    protected static String archiveName(String archiveName) {
        return archiveName.replace(".", "_");
    }

    protected static boolean ignoreFile(String filename) {
        return filename.isEmpty() || filename.endsWith(".java") || filename.contains("com.sun.ts.lib.harness");
    }

}
