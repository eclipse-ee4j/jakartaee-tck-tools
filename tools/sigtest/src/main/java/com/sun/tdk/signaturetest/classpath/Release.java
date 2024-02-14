package com.sun.tdk.signaturetest.classpath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Release {
    public static final Release BOOT_CLASS_PATH = new Release('0', null, null);

    private final char version;
    private final FileSystem zipFs;
    private final String[] prefixes;

    private Release(char version, FileSystem zipFs, String... prefixes) {
        this.version = version;
        this.zipFs = zipFs;
        this.prefixes =prefixes;
    }

    /**
     * Finds version of the JDK API.
     * @param version 6, 7, 8, 9, ..., 11, ... 15
     * @return found version or null
     */
    public static Release find(int version) {
        if (version > 21) {
            return null;
        }
        char ch = (char) (version < 10 ? '0' + version : 'A' + (version - 10));
        return RELEASES.get(ch);
    }

    InputStream findClass(String name) {
        if (prefixes == null) {
            final String resourceName = name.replace('.', '/') + ".class";
            return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
        } else {
            for (String p : prefixes) {
                InputStream is = null;
                // We're using the local ct.sym file
                if (zipFs != null) {
                    if (p.endsWith("system-modules")) {
                        // The current release does not container sig files we need to just return the class resource
                        // from our current class path.
                        final String resourceName = name.replace('.', '/') + ".class";
                        return ClassLoader.getSystemClassLoader().getResourceAsStream(resourceName);
                    }
                    try {
                        // Look for the signature file in the ct.sym archive.
                        final Path sigFile = zipFs.getPath(p + "/" + name.replace('.', '/') + ".sig");
                        if (Files.exists(sigFile)) {
                            is = Files.newInputStream(sigFile);
                        }
                    } catch (IOException e) {
                        // This is unlikely, but it seems we should fail early on this as an indicator to what might
                        // be wrong.
                        throw new UncheckedIOException(e);
                    }
                } else {
                    // Attempt to use signature files that may be on the class path
                    final String resourceName = "/META-INF/sigtest/" + p + "/" + name.replace('.', '/') + ".sig";
                    is = Release.class.getResourceAsStream(resourceName);
                }
                if (is != null) {
                    return is;
                }
            }
            return null;
        }

    }

    private static final Map<Character, Release> RELEASES;
    static {
        List<String> lines = new ArrayList<>();
        final FileSystem zipFs;
        final Path ctSym = Paths.get(System.getProperty("java.home"), "lib", "ct.sym");
        // If the ct.sym file exists, use it. This is useful when testing with a JVM which is newer than the used to
        // create the library that embeds the signature files.
        if (Files.exists(ctSym)) {
            try {
                zipFs = zipFs(ctSym);
                // We need to shut down the FileSystem. This is not ideal in some environments, but it's better than
                // leaving resources open.
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        zipFs.close();
                    } catch (IOException ignore) {
                    }
                }));
                // Walk the root of the ZIP file to build the prefixes. This is akin to what
                // org.netbeans.apitest.ListCtSym does.
                Files.walkFileTree(zipFs.getPath("/"), Set.of(), 2, new SimpleFileVisitor<>() {
                    private String prefix;

                    @Override
                    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
                        prefix = dir.getFileName() == null ? null : dir.getFileName().toString();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                        final String fileName = file.getFileName().toString();
                        String newPrefix = prefix == null ? fileName : prefix + "/" + fileName;
                        if (prefix != null) {
                            lines.add(newPrefix);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) {
                        prefix = null;
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new InternalError(e);
            }
        } else {
            // The ct.sym file does not exist. Use the sigtest.ls file which should be on the class path
            zipFs = null;
            try {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(Release.class.getResourceAsStream("/META-INF/sigtest.ls")))) {
                    for (; ; ) {
                        String l = r.readLine();
                        if (l == null) {
                            break;
                        }
                        lines.add(l);
                    }
                }
            } catch (IOException ex) {
                throw new InternalError(ex);
            }
        }

        Map<Character,List<String>> prefixes = new HashMap<>();
        for (String l : lines) {
            String[] versionsDir = l.split("/");
            String versions = versionsDir[0];
            for (int i = 0; i < versions.length(); i++) {
                Character ch = versions.charAt(i);
                List<String> arr = prefixes.get(ch);
                if (arr == null) {
                    arr = new ArrayList<>();
                    prefixes.put(ch, arr);
                }
                arr.add(l);
            }
        }

        Map<Character,Release> releases = new HashMap<>();
        for (Map.Entry<Character, List<String>> entry : prefixes.entrySet()) {
            Character key = entry.getKey();
            List<String> value = entry.getValue();
            releases.put(key, new Release(key, zipFs, value.toArray(new String[0])));
        }

        RELEASES = releases;
    }

    private static FileSystem zipFs(final Path path) throws IOException {
        // locate file system by using the syntax defined in java.net.JarURLConnection
        URI uri = URI.create("jar:" + path.toUri());
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException ignore) {
        }
        return FileSystems.newFileSystem(uri, Map.of());
    }
}
