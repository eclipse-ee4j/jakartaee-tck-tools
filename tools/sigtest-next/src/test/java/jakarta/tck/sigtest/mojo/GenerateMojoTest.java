/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.mojo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the static helper methods on {@link GenerateMojo}.
 */
class GenerateMojoTest {

    @TempDir(cleanup = CleanupMode.NEVER)
    // @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // parsePackages
    // -------------------------------------------------------------------------

    @Test
    void parsePackages_emptyStringReturnsEmptySet() {
        assertTrue(GenerateMojo.parsePackages("").isEmpty());
        assertTrue(GenerateMojo.parsePackages("   ").isEmpty());
        assertTrue(GenerateMojo.parsePackages(null).isEmpty());
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
        Set<String> result = GenerateMojo.parsePackages(input.trim());
        assertEquals(Set.of(expected.trim()), result);
    }

    @Test
    void parsePackages_supportsMultipleDelimiters() {
        Set<String> result = GenerateMojo.parsePackages("jakarta.servlet,jakarta.ws.rs;jakarta.json:jakarta.persistence");
        assertEquals(Set.of("jakarta.servlet", "jakarta.ws.rs", "jakarta.json", "jakarta.persistence"), result);
    }

    @Test
    void parsePackages_deduplicatesEntries() {
        Set<String> result = GenerateMojo.parsePackages("jakarta.servlet,jakarta.servlet,jakarta.servlet.*");
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

        Set<String> packages = GenerateMojo.discoverPackages(jar);

        assertTrue(packages.contains("jakarta.servlet"), "Expected jakarta.servlet");
        assertTrue(packages.contains("jakarta.servlet.http"), "Expected jakarta.servlet.http");
    }

    @Test
    void discoverPackages_ignoresModuleInfo() throws Exception {
        File jar = buildJar("module-info.class", "jakarta/servlet/Servlet.class");

        Set<String> packages = GenerateMojo.discoverPackages(jar);

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

        Set<String> packages = GenerateMojo.discoverPackages(jar);
        // package-info entries should not create duplicate / extra entries
        assertEquals(1, packages.size());
        assertTrue(packages.contains("jakarta.servlet"));
    }

    @Test
    void discoverPackages_defaultPackageClassIgnored() throws Exception {
        // A class in the default (unnamed) package has no slash in its name.
        File jar = buildJar("DefaultClass.class");

        Set<String> packages = GenerateMojo.discoverPackages(jar);
        assertTrue(packages.isEmpty(), "Default-package class should not produce a package entry");
    }

    @Test
    void discoverPackages_throwsOnMissingJar() {
        File missing = tempDir.resolve("nonexistent.jar").toFile();
        assertThrows(Exception.class, () -> GenerateMojo.discoverPackages(missing));
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
}
