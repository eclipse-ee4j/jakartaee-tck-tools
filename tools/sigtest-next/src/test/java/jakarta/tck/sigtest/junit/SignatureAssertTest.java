/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.junit;

import jakarta.tck.sigtest.check.Violation;
import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.SignatureRecord;
import jakarta.tck.sigtest.util.SignatureIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureAssertTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // assertCompatible / forResource happy path
    // -------------------------------------------------------------------------

    @Test
    void assertCompatible_matchingBaseline_passes() throws Exception {
        Path classesDir = tempDir.resolve("classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "}\n");

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));
        Path sigFile = classesDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertDoesNotThrow(() -> SignatureAssert.assertCompatible("test.sig.json"),
                    "Expected no AssertionError for matching baseline");
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    // -------------------------------------------------------------------------
    // assertCompatible failure scenarios
    // -------------------------------------------------------------------------

    @Test
    void assertCompatible_missingMethod_throwsAssertionError() throws Exception {
        Path baseDir = tempDir.resolve("classes_base");
        compile(baseDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "    void extra();\n" +
                "}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live");
        compile(liveDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            AssertionError error = assertThrows(AssertionError.class,
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)
                            .check());
            String msg = error.getMessage();
            assertNotNull(msg);
            assertTrue(msg.contains("violation"), "Message should mention violation count: " + msg);
            assertTrue(msg.contains("com.example.api.Service"), "Message should name the affected class: " + msg);
            assertTrue(msg.contains("MISSING_METHOD"), "Message should contain violation kind: " + msg);
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void assertCompatible_addedClass_throwsAssertionError() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_add");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_add");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service {}\n",
                "package com.example.api;\n" +
                "public class ServiceExtra {}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            AssertionError error = assertThrows(AssertionError.class,
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)
                            .check());
            assertTrue(error.getMessage().contains("ADDED_CLASS"), "Expected ADDED_CLASS violation: " + error.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void assertCompatible_changedMethod_throwsAssertionError() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_chg");
        compile(baseDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_chg");
        compile(liveDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute(int mode);\n" +
                "}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            AssertionError error = assertThrows(AssertionError.class,
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)
                            .check());
            String msg = error.getMessage();
            assertTrue(msg.contains("MISSING_METHOD") || msg.contains("ADDED_ABSTRACT_METHOD"),
                    "Expected method-level violation: " + msg);
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    // -------------------------------------------------------------------------
    // excludes option
    // -------------------------------------------------------------------------

    @Test
    void excludes_suppressesAddedPackageViolation() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_excl");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_excl");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service {}\n",
                "package com.example.api.internal;\n" +
                "public class Helper {}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)
                            .excludes("com.example.api.internal")
                            .check(),
                    "Expected excluded addition not to trigger failure");
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void excludes_arrayOverload_suppressesViolation() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_excla");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_excla");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service {}\n",
                "package com.example.api.internal;\n" +
                "public class Helper {}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)
                            .excludes("com.example.api.internal")
                            .check());
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    // -------------------------------------------------------------------------
    // liveApi(Path) option
    // -------------------------------------------------------------------------

    @Test
    void liveApi_fromJar_passesForMatchingBaseline() throws Exception {
        Path classesDir = tempDir.resolve("classes_jar");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service {}\n");

        Path jarPath = tempDir.resolve("service.jar");
        createJarFromDirectory(classesDir, jarPath);

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));
        Path sigFile = classesDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(jarPath)
                            .check(),
                    "Expected no failure for matching jar");
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void liveApi_takesHighestPriority_overSystemProperty() throws Exception {
        Path baseDir = tempDir.resolve("classes_prio_base");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        // liveDir matches baseline → should pass
        Path liveDir = tempDir.resolve("classes_prio_live");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service {}\n");

        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        // Set system property to a non-existent path — but liveApi() override must win
        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        String previousValue = System.getProperty(SignatureAssert.RESOLVE_PROPERTY);
        System.setProperty(SignatureAssert.RESOLVE_PROPERTY, "/nonexistent/path/api.jar");
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json")
                            .liveApi(liveDir)   // explicit wins — system prop not reached
                            .check());
        } finally {
            if (previousValue == null) {
                System.clearProperty(SignatureAssert.RESOLVE_PROPERTY);
            } else {
                System.setProperty(SignatureAssert.RESOLVE_PROPERTY, previousValue);
            }
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    // -------------------------------------------------------------------------
    // System property sigtest-next.resolve.api
    // -------------------------------------------------------------------------

    @Test
    void resolveProperty_validJar_usedForCheck() throws Exception {
        Path classesDir = tempDir.resolve("classes_prop");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service {}\n");

        Path jarPath = tempDir.resolve("service_prop.jar");
        createJarFromDirectory(classesDir, jarPath);

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));
        Path sigFile = classesDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        String previousValue = System.getProperty(SignatureAssert.RESOLVE_PROPERTY);
        System.setProperty(SignatureAssert.RESOLVE_PROPERTY, jarPath.toAbsolutePath().toString());
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json").check(),
                    "Expected no failure when resolve property points to matching jar");
        } finally {
            if (previousValue == null) {
                System.clearProperty(SignatureAssert.RESOLVE_PROPERTY);
            } else {
                System.setProperty(SignatureAssert.RESOLVE_PROPERTY, previousValue);
            }
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void resolveProperty_missingFile_throwsIllegalState() throws Exception {
        Path baseDir = tempDir.resolve("classes_prop_miss");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));
        Path sigFile = baseDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{baseDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        String previousValue = System.getProperty(SignatureAssert.RESOLVE_PROPERTY);
        System.setProperty(SignatureAssert.RESOLVE_PROPERTY, "/no/such/file.jar");
        try {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> SignatureAssert.forResource("test.sig.json").check());
            assertTrue(ex.getMessage().contains(SignatureAssert.RESOLVE_PROPERTY),
                    "Exception message should reference the system property: " + ex.getMessage());
        } finally {
            if (previousValue == null) {
                System.clearProperty(SignatureAssert.RESOLVE_PROPERTY);
            } else {
                System.setProperty(SignatureAssert.RESOLVE_PROPERTY, previousValue);
            }
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    void resolveProperty_unresolvedPlaceholder_fallsBackToClasspath() throws Exception {
        Path classesDir = tempDir.resolve("classes_prop_ph");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service {}\n");

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));
        Path sigFile = classesDir.resolve("test.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);
        Thread.currentThread().setContextClassLoader(loader);
        String previousValue = System.getProperty(SignatureAssert.RESOLVE_PROPERTY);
        // Surefire may forward an unresolved Maven property as a literal "${...}" string
        System.setProperty(SignatureAssert.RESOLVE_PROPERTY, "${sigtest-next.resolve.api}");
        try {
            assertDoesNotThrow(
                    () -> SignatureAssert.forResource("test.sig.json").check(),
                    "Unresolved placeholder should fall back to classpath scanning");
        } finally {
            if (previousValue == null) {
                System.clearProperty(SignatureAssert.RESOLVE_PROPERTY);
            } else {
                System.setProperty(SignatureAssert.RESOLVE_PROPERTY, previousValue);
            }
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    // -------------------------------------------------------------------------
    // formatViolations message structure
    // -------------------------------------------------------------------------

    @Test
    void formatViolations_singleViolation_mentionsSingular() {
        Violation v = Violation.of(Violation.Kind.MISSING_METHOD, "com.example.Foo",
                "method void doSomething() missing");
        String msg = SignatureAssert.formatViolations(List.of(v));
        assertTrue(msg.contains("1 violation "), "Singular expected: " + msg);
        assertTrue(msg.contains("com.example.Foo"), msg);
        assertTrue(msg.contains("MISSING_METHOD"), msg);
        assertTrue(msg.contains("method void doSomething() missing"), msg);
    }

    @Test
    void formatViolations_multipleViolations_mentionsPlural() {
        Violation v1 = Violation.of(Violation.Kind.MISSING_METHOD, "com.example.Foo", "method A missing");
        Violation v2 = Violation.of(Violation.Kind.ADDED_CLASS, "com.example.Bar", "Bar was added");
        String msg = SignatureAssert.formatViolations(List.of(v1, v2));
        assertTrue(msg.contains("2 violations"), "Plural expected: " + msg);
        assertTrue(msg.contains("com.example.Bar"), msg);
        assertTrue(msg.contains("com.example.Foo"), msg);
    }

    @Test
    void formatViolations_groupedByClass_sortedAlphabetically() {
        Violation v1 = Violation.of(Violation.Kind.MISSING_METHOD, "com.example.Zoo", "method Z");
        Violation v2 = Violation.of(Violation.Kind.MISSING_FIELD, "com.example.Zoo", "field Z");
        Violation v3 = Violation.of(Violation.Kind.ADDED_CLASS, "com.example.Alpha", "added");
        String msg = SignatureAssert.formatViolations(List.of(v1, v2, v3));
        // Alpha should appear before Zoo
        int alphaPos = msg.indexOf("com.example.Alpha");
        int zooPos = msg.indexOf("com.example.Zoo");
        assertTrue(alphaPos < zooPos, "Alpha should sort before Zoo: " + msg);
        // Both violations of Zoo appear in the same block
        int secondZooOccurrence = msg.indexOf("com.example.Zoo", zooPos + 1);
        assertEquals(-1, secondZooOccurrence, "Zoo class name should appear only once as a header: " + msg);
    }

    // -------------------------------------------------------------------------
    // Error-handling: missing / null resource
    // -------------------------------------------------------------------------

    @Test
    void forResource_nullResourceName_throwsNPE() {
        assertThrows(NullPointerException.class, () -> SignatureAssert.forResource(null));
    }

    @Test
    void assertCompatible_nullResourceName_throwsNPE() {
        assertThrows(NullPointerException.class, () -> SignatureAssert.assertCompatible(null));
    }

    @Test
    void check_missingResource_throwsIllegalState() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> SignatureAssert.forResource("/nonexistent/resource.sig.json").check());
        assertTrue(ex.getMessage().contains("/nonexistent/resource.sig.json"),
                "Exception message should name the missing resource: " + ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SignatureRecord generateBaseline(Path classesDir, Set<String> packages) throws Exception {
        SignatureGenerator gen = new SignatureGenerator(List.of(classesDir.toFile()), packages, Collections.emptySet());
        return gen.generate("1.0.0");
    }

    private void compile(Path targetDir, String... sources) throws Exception {
        Files.createDirectories(targetDir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler required");

        Path srcRoot = tempDir.resolve("src_" + System.nanoTime());
        Files.createDirectories(srcRoot);

        String[] srcPaths = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            String src = sources[i];
            String pkg = extractPackage(src);
            String cls = extractClass(src);
            Path pkgDir = srcRoot.resolve(pkg.replace('.', File.separatorChar));
            Files.createDirectories(pkgDir);
            Path srcFile = pkgDir.resolve(cls + ".java");
            Files.writeString(srcFile, src);
            srcPaths[i] = srcFile.toString();
        }

        String[] args = new String[6 + srcPaths.length];
        args[0] = "-d";
        args[1] = targetDir.toString();
        args[2] = "-cp";
        args[3] = targetDir.toString();
        args[4] = "--release";
        args[5] = "11";
        System.arraycopy(srcPaths, 0, args, 6, srcPaths.length);

        int rc = compiler.run(null, null, null, args);
        assertEquals(0, rc, "Compilation failed");
    }

    private static String extractPackage(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring("package ".length()).replace(";", "").trim();
            }
        }
        return "";
    }

    private static String extractClass(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("public ")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    String kw = parts[i];
                    if (kw.equals("class") || kw.equals("interface") || kw.equals("enum") || kw.equals("record")) {
                        return parts[i + 1].replaceAll("[{(<].*", "").trim();
                    }
                }
            }
        }
        throw new IllegalArgumentException("No public class found in source");
    }

    private static void createJarFromDirectory(Path dir, Path jarPath) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            Files.walk(dir).filter(Files::isRegularFile).forEach(p -> {
                String entryName = dir.relativize(p).toString().replace(File.separatorChar, '/');
                try {
                    jos.putNextEntry(new JarEntry(entryName));
                    Files.copy(p, jos);
                    jos.closeEntry();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
