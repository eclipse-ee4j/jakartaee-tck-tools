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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.tck.sigtest.model.SignatureRecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes {@code .sig.json} files as {@link SignatureRecord}s.
 *
 * <p>Load operations accept a file, a path, a stream, or a classpath resource.
 * Save operations write pretty-formatted JSON to a file path or an arbitrary writer.
 */
public final class SignatureIO {

    /**
     * Shared {@link Jsonb} instance. {@code Jsonb} is thread-safe and expensive to
     * construct, so a single instance is created at class-load time and reused for
     * all read and write operations.
     */
    private static final Jsonb JSONB;

    static {
        JsonbConfig config = new JsonbConfig().withFormatting(true).withNullValues(false);
        JSONB = JsonbBuilder.create(config);
    }

    private SignatureIO() {
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    /** Loads a baseline from a file. */
    public static SignatureRecord load(File file) throws IOException {
        return load(file.toPath());
    }

    /** Loads a baseline from a path. */
    public static SignatureRecord load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return load(in);
        } catch (IOException e) {
            throw new IOException("Failed to read baseline signature file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Loads a baseline from a stream (UTF-8). The stream is read fully but not closed.
     */
    public static SignatureRecord load(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        try {
            return JSONB.fromJson(reader, SignatureRecord.class);
        } catch (Exception e) {
            throw new IOException("Failed to parse baseline signature: " + e.getMessage(), e);
        }
    }

    /**
     * Loads a baseline from a classpath resource.
     *
     * @param loader the class loader to search
     * @param name   resource name; a leading {@code /} is ignored
     * @throws IOException if the resource does not exist or cannot be parsed
     */
    public static SignatureRecord loadResource(ClassLoader loader, String name) throws IOException {
        String resource = name.startsWith("/") ? name.substring(1) : name;
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Baseline signature resource not found on classpath: " + name);
            }
            return load(in);
        }
    }

    // -------------------------------------------------------------------------
    // Write
    // -------------------------------------------------------------------------

    /**
     * Serialises {@code record} as pretty-formatted JSON to {@code path},
     * creating parent directories as needed.
     *
     * @param record the signature record to write
     * @param path   destination {@code .sig.json} file
     * @throws IOException if the file cannot be created or written
     */
    public static void save(SignatureRecord record, Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            save(record, writer);
        } catch (IOException e) {
            throw new IOException("Failed to write signature file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Serialises {@code record} as pretty-formatted JSON to {@code writer}.
     * The writer is flushed but not closed.
     *
     * @param record the signature record to write
     * @param writer destination writer (UTF-8 assumed)
     * @throws IOException if serialisation or writing fails
     */
    public static void save(SignatureRecord record, Writer writer) throws IOException {
        try {
            JSONB.toJson(record, writer);
            writer.flush();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to serialise signature record: " + e.getMessage(), e);
        }
    }
}
