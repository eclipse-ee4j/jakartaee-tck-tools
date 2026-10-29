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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility methods for locating classpath entries (JAR files and directories)
 * and discovering live packages via {@link ClassLoader} resource lookups.
 */
public final class ClasspathUtil {

    private ClasspathUtil() {
    }

    /**
     * Locates the root classpath entries (JAR files or directory roots) that provide
     * the specified package names within the given {@link ClassLoader}.
     *
     * @param classLoader  the class loader to query
     * @param packageNames the dot-separated package names to locate
     * @return a set of existing {@link File} entries (JARs or directories), never {@code null}
     * @throws IOException if an error occurs querying class loader resources
     */
    public static Set<File> locateClasspathEntries(ClassLoader classLoader, Set<String> packageNames) throws IOException {
        if (classLoader == null || packageNames == null || packageNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<File> entries = new LinkedHashSet<>();
        for (String pkg : packageNames) {
            if (pkg == null || pkg.trim().isEmpty()) {
                continue;
            }
            String resourcePath = pkg.trim().replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                File entry = urlToClasspathEntry(url, pkg.trim());
                if (entry != null && entry.exists()) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    /**
     * Converts a package resource {@link URL} back to its root directory or JAR file.
     *
     * @param url         resource URL pointing to a package directory or jar entry
     * @param packageName dot-separated package name corresponding to the URL
     * @return the resolved {@link File} for the classpath entry, or {@code null} if unresolvable
     */
    public static File urlToClasspathEntry(URL url, String packageName) {
        if (url == null) {
            return null;
        }
        String protocol = url.getProtocol();
        try {
            if ("file".equalsIgnoreCase(protocol)) {
                Path path = Paths.get(url.toURI());
                if (path.toString().endsWith(".jar") && Files.isRegularFile(path)) {
                    return path.toFile();
                }
                if (packageName != null && !packageName.trim().isEmpty()) {
                    String[] segments = packageName.trim().split("\\.");
                    Path current = path;
                    for (int i = 0; i < segments.length; i++) {
                        if (current != null) {
                            current = current.getParent();
                        }
                    }
                    if (current != null && Files.isDirectory(current)) {
                        return current.toFile();
                    }
                } else if (Files.isDirectory(path)) {
                    return path.toFile();
                }
            } else if ("jar".equalsIgnoreCase(protocol)) {
                try {
                    URLConnection conn = url.openConnection();
                    if (conn instanceof JarURLConnection) {
                        JarURLConnection juc = (JarURLConnection) conn;
                        URL jarUrl = juc.getJarFileURL();
                        if ("file".equalsIgnoreCase(jarUrl.getProtocol())) {
                            return Paths.get(jarUrl.toURI()).toFile();
                        }
                    }
                } catch (Exception ignored) {
                }
                String spec = url.getFile();
                if (spec == null) {
                    spec = url.getPath();
                }
                if (spec != null) {
                    int bang = spec.indexOf("!/");
                    String fileUrlStr = bang >= 0 ? spec.substring(0, bang) : spec;
                    URI jarUri = URI.create(fileUrlStr);
                    if ("file".equalsIgnoreCase(jarUri.getScheme())) {
                        return Paths.get(jarUri).toFile();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Discovers all package names containing at least one {@code .class} file across
     * the specified classpath entries (directories or JAR files).
     *
     * @param classpathEntries the JAR files and directories to inspect
     * @return set of all package names discovered, never {@code null}
     * @throws IOException if any JAR file or directory cannot be read
     */
    public static Set<String> discoverPackages(Collection<File> classpathEntries) throws IOException {
        if (classpathEntries == null || classpathEntries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> packages = new HashSet<>();
        for (File entry : classpathEntries) {
            if (entry == null || !entry.exists()) {
                continue;
            }
            if (entry.isDirectory()) {
                packages.addAll(PackageUtil.discoverPackagesFromDirectory(entry));
            } else if (entry.getName().endsWith(".jar")) {
                packages.addAll(PackageUtil.discoverPackages(entry));
            }
        }
        return packages;
    }
}
