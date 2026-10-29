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
import org.junit.jupiter.api.io.TempDir;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathUtilTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // urlToClasspathEntry
    // -------------------------------------------------------------------------

    @Test
    void urlToClasspathEntry_resolvesDirectoryFromPackageDepth() throws Exception {
        Path rootDir = Files.createDirectories(tempDir.resolve("classes"));
        Path pkgDir = Files.createDirectories(rootDir.resolve("com/example/service"));
        URL url = pkgDir.toUri().toURL();

        File resolved = ClasspathUtil.urlToClasspathEntry(url, "com.example.service");

        assertNotNull(resolved);
        assertEquals(rootDir.toFile().getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void urlToClasspathEntry_resolvesDirectoryWithSpacesInPath() throws Exception {
        Path rootDir = Files.createDirectories(tempDir.resolve("classes with spaces"));
        Path pkgDir = Files.createDirectories(rootDir.resolve("org/demo/api"));
        URL url = pkgDir.toUri().toURL();

        File resolved = ClasspathUtil.urlToClasspathEntry(url, "org.demo.api");

        assertNotNull(resolved);
        assertEquals(rootDir.toFile().getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void urlToClasspathEntry_resolvesJarFromJarUrl() throws Exception {
        File jarFile = createJar(tempDir.resolve("sample-api.jar"), "jakarta/servlet/Servlet.class");
        URL jarUrl = new URL("jar:" + jarFile.toURI().toURL() + "!/jakarta/servlet");

        File resolved = ClasspathUtil.urlToClasspathEntry(jarUrl, "jakarta.servlet");

        assertNotNull(resolved);
        assertEquals(jarFile.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void urlToClasspathEntry_resolvesJarWithSpacesInPath() throws Exception {
        Path spaceDir = Files.createDirectories(tempDir.resolve("dir with spaces"));
        File jarFile = createJar(spaceDir.resolve("sample api.jar"), "jakarta/json/Json.class");
        URL jarUrl = new URL("jar:" + jarFile.toURI().toURL() + "!/jakarta/json");

        File resolved = ClasspathUtil.urlToClasspathEntry(jarUrl, "jakarta.json");

        assertNotNull(resolved);
        assertEquals(jarFile.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void urlToClasspathEntry_resolvesDirectJarFileUrl() throws Exception {
        File jarFile = createJar(tempDir.resolve("direct.jar"), "com/example/Direct.class");
        URL url = jarFile.toURI().toURL();

        File resolved = ClasspathUtil.urlToClasspathEntry(url, "com.example");

        assertNotNull(resolved);
        assertEquals(jarFile.getCanonicalPath(), resolved.getCanonicalPath());
    }

    @Test
    void urlToClasspathEntry_returnsNullForNullOrNonFileUrl() throws Exception {
        assertNull(ClasspathUtil.urlToClasspathEntry(null, "com.example"));
        assertNull(ClasspathUtil.urlToClasspathEntry(new URL("https://jakarta.ee/test"), "com.example"));
    }

    // -------------------------------------------------------------------------
    // locateClasspathEntries
    // -------------------------------------------------------------------------

    @Test
    void locateClasspathEntries_findsDirectoryOnClassLoader() throws Exception {
        Path rootDir = Files.createDirectories(tempDir.resolve("classes-dir"));
        Files.createDirectories(rootDir.resolve("com/example/api"));

        try (URLClassLoader loader = new URLClassLoader(new URL[]{rootDir.toUri().toURL()}, null)) {
            Set<File> entries = ClasspathUtil.locateClasspathEntries(loader, Set.of("com.example.api"));

            assertEquals(1, entries.size());
            File entry = entries.iterator().next();
            assertEquals(rootDir.toFile().getCanonicalPath(), entry.getCanonicalPath());
        }
    }

    @Test
    void locateClasspathEntries_findsJarOnClassLoader() throws Exception {
        File jarFile = createJar(tempDir.resolve("lib.jar"), "jakarta/servlet/Servlet.class");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, null)) {
            Set<File> entries = ClasspathUtil.locateClasspathEntries(loader, Set.of("jakarta.servlet"));

            assertEquals(1, entries.size());
            File entry = entries.iterator().next();
            assertEquals(jarFile.getCanonicalPath(), entry.getCanonicalPath());
        }
    }

    @Test
    void locateClasspathEntries_findsMultipleClasspathEntries() throws Exception {
        File jar1 = createJar(tempDir.resolve("lib1.jar"), "jakarta/servlet/Servlet.class");
        File jar2 = createJar(tempDir.resolve("lib2.jar"), "jakarta/json/Json.class");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar1.toURI().toURL(), jar2.toURI().toURL()}, null)) {
            Set<File> entries = ClasspathUtil.locateClasspathEntries(loader, Set.of("jakarta.servlet", "jakarta.json"));

            assertEquals(2, entries.size());
            assertTrue(entries.stream().anyMatch(f -> f.getName().equals("lib1.jar")));
            assertTrue(entries.stream().anyMatch(f -> f.getName().equals("lib2.jar")));
        }
    }

    @Test
    void locateClasspathEntries_handlesNullAndEmptyGracefully() throws Exception {
        assertTrue(ClasspathUtil.locateClasspathEntries(null, Set.of("com.example")).isEmpty());
        assertTrue(ClasspathUtil.locateClasspathEntries(ClassLoader.getSystemClassLoader(), null).isEmpty());
        assertTrue(ClasspathUtil.locateClasspathEntries(ClassLoader.getSystemClassLoader(), Collections.emptySet()).isEmpty());
        assertTrue(ClasspathUtil.locateClasspathEntries(ClassLoader.getSystemClassLoader(), Set.of("non.existent.package")).isEmpty());
    }

    // -------------------------------------------------------------------------
    // discoverPackages
    // -------------------------------------------------------------------------

    @Test
    void discoverPackages_aggregatesFromJarsAndDirectories() throws Exception {
        Path rootDir = Files.createDirectories(tempDir.resolve("dir-classes"));
        Path pkgDir = Files.createDirectories(rootDir.resolve("com/example/dir"));
        Files.write(pkgDir.resolve("DirClass.class"), new byte[]{});

        File jarFile = createJar(tempDir.resolve("jar-classes.jar"), "com/example/jar/JarClass.class");

        Set<String> packages = ClasspathUtil.discoverPackages(List.of(rootDir.toFile(), jarFile));

        assertTrue(packages.contains("com.example.dir"));
        assertTrue(packages.contains("com.example.jar"));
    }

    @Test
    void discoverPackages_handlesNullAndEmpty() throws Exception {
        assertTrue(ClasspathUtil.discoverPackages(null).isEmpty());
        assertTrue(ClasspathUtil.discoverPackages(Collections.emptyList()).isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private File createJar(Path target, String... entryNames) throws Exception {
        File jar = target.toFile();
        Set<String> dirs = new java.util.HashSet<>();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            for (String name : entryNames) {
                int idx = name.lastIndexOf('/');
                if (idx > 0) {
                    String[] parts = name.substring(0, idx).split("/");
                    String currentDir = "";
                    for (String part : parts) {
                        currentDir += part + "/";
                        if (dirs.add(currentDir)) {
                            jos.putNextEntry(new JarEntry(currentDir));
                            jos.closeEntry();
                        }
                    }
                }
                jos.putNextEntry(new JarEntry(name));
                jos.closeEntry();
            }
        }
        return jar;
    }
}
