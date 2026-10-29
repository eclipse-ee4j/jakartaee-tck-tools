/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.check;

import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.PackageRecord;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignatureCheckerTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // extractPackages tests
    // -------------------------------------------------------------------------

    @Test
    void extractPackages_nullOrEmpty() {
        assertTrue(SignatureChecker.extractPackages(null).isEmpty());

        SignatureRecord empty = new SignatureRecord();
        assertTrue(SignatureChecker.extractPackages(empty).isEmpty());
    }

    @Test
    void extractPackages_fromClassesAndPackages() {
        SignatureRecord record = new SignatureRecord();

        PackageRecord pkg1 = new PackageRecord();
        pkg1.name = "com.example.pkg1";
        record.packages = List.of(pkg1);

        ClassRecord cls1 = new ClassRecord();
        cls1.name = "com.example.pkg2.Foo";
        ClassRecord cls2 = new ClassRecord();
        cls2.name = "com.example.pkg2.Foo$Inner";
        ClassRecord clsDefault = new ClassRecord();
        clsDefault.name = "DefaultClass";

        record.classes = List.of(cls1, cls2, clsDefault);

        Set<String> pkgs = SignatureChecker.extractPackages(record);
        assertEquals(Set.of("com.example.pkg1", "com.example.pkg2"), pkgs);
    }

    // -------------------------------------------------------------------------
    // ClassLoader-based checking tests
    // -------------------------------------------------------------------------

    @Test
    void check_viaClassLoader_matchingBaseline_noViolations() throws Exception {
        Path classesDir = tempDir.resolve("classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "}\n");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);
        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));

        List<Violation> violations = SignatureChecker.check(baseline, loader);
        assertTrue(violations.isEmpty(), "Expected no violations for matching signature: " + violations);
    }

    @Test
    void check_viaClassLoader_withMismatch_returnsViolations() throws Exception {
        Path baselineDir = tempDir.resolve("classes_base");
        compile(baselineDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute();\n" +
                "}\n");
        SignatureRecord baseline = generateBaseline(baselineDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live");
        compile(liveDir,
                "package com.example.api;\n" +
                "public interface Service {\n" +
                "    void execute(int mode);\n" +
                "}\n");

        URLClassLoader loader = new URLClassLoader(new URL[]{liveDir.toUri().toURL()}, null);

        List<Violation> violations = SignatureChecker.check(baseline, loader);
        assertFalse(violations.isEmpty(), "Expected violations for mismatched signature");
        assertTrue(violations.stream().anyMatch(v -> v.kind == Violation.Kind.MISSING_METHOD));
        assertTrue(violations.stream().anyMatch(v -> v.kind == Violation.Kind.ADDED_ABSTRACT_METHOD));
    }

    @Test
    void check_viaClassLoader_addedClassInSameRoot_detected() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_added");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_added");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n",
                "package com.example.api.extra;\n" +
                "public class Service2 {}\n");

        URLClassLoader loader = new URLClassLoader(new URL[]{liveDir.toUri().toURL()}, null);

        // Service2 is in com.example.api.extra, which is discovered in the same classpath root
        List<Violation> violations = SignatureChecker.check(baseline, loader);
        assertTrue(violations.stream().anyMatch(v -> v.kind == Violation.Kind.ADDED_CLASS && v.className.equals("com.example.api.extra.Service2")));
    }

    @Test
    void check_viaClassLoader_withExcludes_filtersOutAddedClass() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_exc");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));

        Path liveDir = tempDir.resolve("classes_live_exc");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n",
                "package com.example.api.internal;\n" +
                "public class Service2 {}\n");

        URLClassLoader loader = new URLClassLoader(new URL[]{liveDir.toUri().toURL()}, null);

        // Without excludes: ADDED_CLASS
        List<Violation> withAdditions = SignatureChecker.check(baseline, loader);
        assertTrue(withAdditions.stream().anyMatch(v -> v.kind == Violation.Kind.ADDED_CLASS));

        // With explicit excludes: no violations
        List<Violation> filtered = SignatureChecker.check(baseline, loader, Set.of("com.example.api.internal"));
        assertTrue(filtered.isEmpty(), "Expected excluded package not to be reported as addition: " + filtered);
    }

    @Test
    void check_viaClassLoader_usesBaselineExcludesByDefault() throws Exception {
        Path baseDir = tempDir.resolve("classes_base_bexc");
        compile(baseDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");
        SignatureRecord baseline = generateBaseline(baseDir, Set.of("com.example.api"));
        baseline.excludes = List.of("com.example.api.internal");

        Path liveDir = tempDir.resolve("classes_live_bexc");
        compile(liveDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n",
                "package com.example.api.internal;\n" +
                "public class Service2 {}\n");

        URLClassLoader loader = new URLClassLoader(new URL[]{liveDir.toUri().toURL()}, null);

        // By default, baseline.excludes is used
        List<Violation> violations = SignatureChecker.check(baseline, loader);
        assertTrue(violations.isEmpty(), "Expected baseline.excludes to be applied by default: " + violations);
    }

    // -------------------------------------------------------------------------
    // Target JAR / Directory checking tests
    // -------------------------------------------------------------------------

    @Test
    void check_viaTargetJar_and_Classpath() throws Exception {
        Path classesDir = tempDir.resolve("jar_classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");

        Path jarPath = tempDir.resolve("service.jar");
        createJarFromDirectory(classesDir, jarPath);

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));

        List<Violation> violations = SignatureChecker.check(baseline, jarPath.toFile(), List.of(jarPath.toFile()), null);
        assertTrue(violations.isEmpty(), "Expected no violations: " + violations);

        // Path overload
        List<Violation> pathViolations = SignatureChecker.check(baseline, jarPath);
        assertTrue(pathViolations.isEmpty(), "Expected no violations for path overload: " + pathViolations);
    }

    @Test
    void check_viaTargetDirectory() throws Exception {
        Path classesDir = tempDir.resolve("dir_classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));

        List<Violation> violations = SignatureChecker.check(baseline, classesDir.toFile());
        assertTrue(violations.isEmpty(), "Expected no violations: " + violations);
    }

    @Test
    void check_viaClasspathList() throws Exception {
        Path classesDir = tempDir.resolve("cp_classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));

        List<Violation> violations = SignatureChecker.check(baseline, List.of(classesDir.toFile()));
        assertTrue(violations.isEmpty(), "Expected no violations: " + violations);
    }

    // -------------------------------------------------------------------------
    // Baseline file / path / resource overloads
    // -------------------------------------------------------------------------

    @Test
    void check_viaBaselineFileAndResource() throws Exception {
        Path classesDir = tempDir.resolve("resource_classes");
        compile(classesDir,
                "package com.example.api;\n" +
                "public class Service1 {}\n");

        SignatureRecord baseline = generateBaseline(classesDir, Set.of("com.example.api"));

        Path sigFile = classesDir.resolve("baseline.sig.json");
        SignatureIO.save(baseline, sigFile);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()}, null);

        // File baseline overload
        List<Violation> vFile = SignatureChecker.check(sigFile.toFile(), loader);
        assertTrue(vFile.isEmpty(), "Expected empty violations for file baseline: " + vFile);

        // Path baseline overload
        List<Violation> vPath = SignatureChecker.check(sigFile, loader);
        assertTrue(vPath.isEmpty(), "Expected empty violations for path baseline: " + vPath);

        // Resource baseline overload
        List<Violation> vRes = SignatureChecker.check("baseline.sig.json", loader);
        assertTrue(vRes.isEmpty(), "Expected empty violations for resource baseline: " + vRes);

        // Baseline file with target file
        List<Violation> vFileTarget = SignatureChecker.check(sigFile.toFile(), classesDir.toFile(), null, null);
        assertTrue(vFileTarget.isEmpty(), "Expected empty violations: " + vFileTarget);

        // Baseline path with target file
        List<Violation> vPathTarget = SignatureChecker.check(sigFile, classesDir.toFile(), null, null);
        assertTrue(vPathTarget.isEmpty(), "Expected empty violations: " + vPathTarget);
    }

    // -------------------------------------------------------------------------
    // Error handling & null argument validation
    // -------------------------------------------------------------------------

    @Test
    void check_nullArgumentsThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> SignatureChecker.check((SignatureRecord) null, getClass().getClassLoader()));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check(new SignatureRecord(), (ClassLoader) null));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check((SignatureRecord) null, new File(".")));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check(new SignatureRecord(), (File) null));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check((File) null, getClass().getClassLoader()));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check((Path) null, getClass().getClassLoader()));
        assertThrows(NullPointerException.class, () -> SignatureChecker.check((String) null, getClass().getClassLoader()));
    }

    @Test
    void check_emptyBaseline_returnsEmptyViolations() throws Exception {
        SignatureRecord empty = new SignatureRecord();
        empty.apiVersion = "1.0.0";
        List<Violation> violations = SignatureChecker.check(empty, getClass().getClassLoader());
        assertTrue(violations.isEmpty());
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
        throw new IllegalArgumentException("No public class found");
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
