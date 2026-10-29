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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Package discovery and parsing helpers shared by the Maven mojos and the
 * JUnit assertion support.
 */
public final class PackageUtil {

    private PackageUtil() {
    }

    /**
     * Reads the package names present in the JAR. Only packages that contain at
     * least one {@code .class} file are returned; {@code module-info} and
     * {@code package-info} entries are ignored, as are classes in the default package.
     *
     * @throws IOException if the JAR cannot be read
     */
    public static Set<String> discoverPackages(File jar) throws IOException {
        Set<String> packages = new HashSet<>();
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.startsWith("module-info")
                        || name.contains("package-info")) {
                    continue;
                }
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash > 0) {
                    // Convert "jakarta/servlet/http/HttpServlet.class" -> "jakarta.servlet.http"
                    packages.add(name.substring(0, lastSlash).replace('/', '.'));
                }
            }
        }
        return packages;
    }

    /**
     * Discovers all package names that contain at least one {@code .class} file
     * under the given directory tree.
     *
     * @throws IOException if the directory cannot be walked
     */
    public static Set<String> discoverPackagesFromDirectory(File dir) throws IOException {
        Set<String> packages = new HashSet<>();
        Path root = dir.toPath();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(p -> p.toString().endsWith(".class"))
                  .forEach(p -> {
                      Path parent = root.relativize(p).getParent();
                      if (parent != null) {
                          packages.add(parent.toString().replace(File.separatorChar, '.'));
                      }
                  });
        }
        return packages;
    }

    /**
     * Parse a comma/colon/semicolon-separated list of package tokens into a set
     * of plain package names, stripping trailing {@code .*} / {@code .**}.
     */
    public static Set<String> parsePackages(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        StringTokenizer st = new StringTokenizer(raw, ",:;");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.endsWith(".**")) {
                token = token.substring(0, token.length() - 3);
            } else if (token.endsWith(".*")) {
                token = token.substring(0, token.length() - 2);
            }
            if (!token.trim().isEmpty()) {
                result.add(token);
            }
        }
        return result;
    }
}
