/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.SignatureRecord;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PackageUtil}.
 */
class PackageUtilTest {

    // @TempDir(cleanup = CleanupMode.NEVER)
    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // parsePackages
    // -------------------------------------------------------------------------

    @Test
    void parsePackages_emptyStringReturnsEmptySet() {
        assertTrue(PackageUtil.parsePackages("").isEmpty());
        assertTrue(PackageUtil.parsePackages("   ").isEmpty());
        assertTrue(PackageUtil.parsePackages(null).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "jakarta.servlet,              jakarta.servlet",
        "jakarta.servlet.*,            jakarta.servlet",
        "jakarta.servlet.**,           jakarta.servlet",
        "jakarta.servlet.http.*,       jakarta.servlet.http",
        "jakarta.servlet.http.**,      jakarta.servlet.http",
    })
    void parsePackages_stripsWildcardSuffixes(String input, String expected) {
        Set<String> result = PackageUtil.parsePackages(input.trim());
        assertEquals(Set.of(expected.trim()), result);
    }

    @Test
    void parsePackages_supportsMultipleDelimiters() {
        Set<String> result = PackageUtil.parsePackages("jakarta.servlet,jakarta.ws.rs;jakarta.json:jakarta.persistence");
        assertEquals(Set.of("jakarta.servlet", "jakarta.ws.rs", "jakarta.json", "jakarta.persistence"), result);
    }

    @Test
    void parsePackages_deduplicatesEntries() {
        Set<String> result = PackageUtil.parsePackages("jakarta.servlet,jakarta.servlet,jakarta.servlet.*");
        assertEquals(1, result.size());
        assertTrue(result.contains("jakarta.servlet"));
    }

    // -------------------------------------------------------------------------
    // discoverPackages
    // -------------------------------------------------------------------------

    @Test
    void discoverPackages_findsPackagesInJar() throws Exception {
        File jar = buildJar(
                "jakarta/servlet/Servlet.class",
                "jakarta/servlet/http/HttpServlet.class",
                "jakarta/servlet/http/HttpServletRequest.class"
        );

        Set<String> packages = PackageUtil.discoverPackages(jar);

        assertTrue(packages.contains("jakarta.servlet"), "Expected jakarta.servlet");
        assertTrue(packages.contains("jakarta.servlet.http"), "Expected jakarta.servlet.http");
    }

    @Test
    void discoverPackages_ignoresModuleInfo() throws Exception {
        File jar = buildJar("module-info.class", "jakarta/servlet/Servlet.class");

        Set<String> packages = PackageUtil.discoverPackages(jar);

        assertFalse(packages.stream().anyMatch(p -> p.contains("module")),
                "module-info.class should not produce a package entry");
        assertTrue(packages.contains("jakarta.servlet"));
    }

    @Test
    void discoverPackages_ignoresPackageInfo() throws Exception {
        File jar = buildJar(
                "jakarta/servlet/package-info.class",
                "jakarta/servlet/Servlet.class"
        );

        Set<String> packages = PackageUtil.discoverPackages(jar);
        // package-info entries should not create duplicate / extra entries
        assertEquals(1, packages.size());
        assertTrue(packages.contains("jakarta.servlet"));
    }

    @Test
    void discoverPackages_defaultPackageClassIgnored() throws Exception {
        // A class in the default (unnamed) package has no slash in its name.
        File jar = buildJar("DefaultClass.class");

        Set<String> packages = PackageUtil.discoverPackages(jar);
        assertTrue(packages.isEmpty(), "Default-package class should not produce a package entry");
    }

    @Test
    void discoverPackages_throwsOnMissingJar() {
        File missing = tempDir.resolve("nonexistent.jar").toFile();
        assertThrows(Exception.class, () -> PackageUtil.discoverPackages(missing));
    }

    // -------------------------------------------------------------------------
    // discoverPackagesFromDirectory
    // -------------------------------------------------------------------------

    @Test
    void discoverPackagesFromDirectory_findsPackagesInTree() throws Exception {
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir.resolve("com/example/api"));
        Files.createDirectories(classesDir.resolve("com/example/internal"));
        Files.writeString(classesDir.resolve("com/example/api/Service.class"), "stub");
        Files.writeString(classesDir.resolve("com/example/internal/Helper.class"), "stub");

        Set<String> packages = PackageUtil.discoverPackagesFromDirectory(classesDir.toFile());

        assertTrue(packages.contains("com.example.api"), "Expected com.example.api");
        assertTrue(packages.contains("com.example.internal"), "Expected com.example.internal");
    }

    @Test
    void discoverPackagesFromDirectory_emptyDirectoryReturnsEmptySet() throws Exception {
        Path classesDir = tempDir.resolve("empty-classes");
        Files.createDirectories(classesDir);

        Set<String> packages = PackageUtil.discoverPackagesFromDirectory(classesDir.toFile());

        assertTrue(packages.isEmpty(), "Empty directory should produce no packages");
    }

    @Test
    void discoverPackagesFromDirectory_defaultPackageClassIgnored() throws Exception {
        Path classesDir = tempDir.resolve("default-pkg");
        Files.createDirectories(classesDir);
        Files.writeString(classesDir.resolve("RootClass.class"), "stub");

        Set<String> packages = PackageUtil.discoverPackagesFromDirectory(classesDir.toFile());

        assertTrue(packages.isEmpty(), "Class at root (default package) should not produce an entry");
    }

    // -------------------------------------------------------------------------
    // <classes> directory integration — scan own output directory
    // -------------------------------------------------------------------------

    @Test
    void generate_scansClassesDirectory() throws Exception {
        // Compile a real class into a temp directory, then use the generator
        // directly (bypassing the Mojo wiring) to verify the directory path works.
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(classesDir);
        compileInto(classesDir,
                "package com.example;\n"
                + "public class MyService {\n"
                + "    public void process() {}\n"
                + "}\n",
                "MyService.java");

        Set<String> packages = PackageUtil.discoverPackagesFromDirectory(classesDir.toFile());
        assertFalse(packages.isEmpty(), "Should discover at least one package");
        assertTrue(packages.contains("com.example"));

        SignatureGenerator gen = new SignatureGenerator(
                List.of(classesDir.toFile()), packages, Set.of());
        SignatureRecord record = gen.generate("1.0");

        Optional<ClassRecord> cls = record.classes.stream()
                .filter(c -> c.name.equals("com.example.MyService"))
                .findFirst();
        assertTrue(cls.isPresent(), "com.example.MyService should be in the signature record");
        assertNotNull(cls.get().methods, "Methods should be recorded");
        assertTrue(cls.get().methods.stream().anyMatch(m -> m.name.equals("process")),
                "process() method should be present");
    }

    @Test
    void generate_classesDirectoryWithExplicitPackageFilter() throws Exception {
        // Two packages in the directory; only one is requested via the package filter.
        Path classesDir = tempDir.resolve("classes-filter");
        Files.createDirectories(classesDir);
        compileInto(classesDir,
                "package com.example.api;\n"
                + "public class PublicService {}\n",
                "PublicService.java");
        compileInto(classesDir,
                "package com.example.internal;\n"
                + "public class InternalHelper {}\n",
                "InternalHelper.java");

        Set<String> explicitPackages = Set.of("com.example.api");
        SignatureGenerator gen = new SignatureGenerator(
                List.of(classesDir.toFile()), explicitPackages, Set.of());
        SignatureRecord record = gen.generate("1.0");

        assertTrue(record.classes.stream().anyMatch(c -> c.name.equals("com.example.api.PublicService")),
                "Public API class should be present");
        assertFalse(record.classes.stream().anyMatch(c -> c.name.equals("com.example.internal.InternalHelper")),
                "Internal class should be excluded by the package filter");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a JAR in {@link #tempDir} containing stub entries for each
     * provided path. The entries are empty (zero bytes) — the tests only care
     * about package discovery, not class content.
     */
    private File buildJar(String... entryNames) throws Exception {
        File jar = tempDir.resolve("test.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            for (String name : entryNames) {
                jos.putNextEntry(new JarEntry(name));
                jos.closeEntry();
            }
        }
        return jar;
    }

    /** Compiles a single Java source file into the given output directory. */
    private void compileInto(Path outDir, String source, String fileName) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available — run tests with a JDK");
        String pkg = extractPackageName(source);
        Path srcDir = tempDir.resolve("src");
        if (!pkg.isEmpty()) {
            srcDir = srcDir.resolve(pkg.replace('.', File.separatorChar));
        }
        Files.createDirectories(srcDir);
        Path srcFile = srcDir.resolve(fileName);
        Files.writeString(srcFile, source);
        int rc = compiler.run(null, null, null,
                "-d", outDir.toString(),
                "-cp", outDir.toString(),
                "--release", "11",
                srcFile.toString());
        assertTrue(rc == 0, "Compilation failed for: " + srcFile);
    }

    private static String extractPackageName(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring("package ".length()).replace(";", "").trim();
            }
        }
        return "";
    }
}
