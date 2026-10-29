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
import jakarta.tck.sigtest.model.SignatureRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SignatureIO}.
 */
class SignatureIOTest {

    @TempDir
    Path tempDir;

    private static String json() throws Exception {
        SignatureRecord record = new SignatureRecord();
        record.apiVersion = "1.2.3";
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(record);
        }
    }

    // -------------------------------------------------------------------------
    // Load tests
    // -------------------------------------------------------------------------

    @Test
    void load_fromPath() throws Exception {
        Path file = tempDir.resolve("api.sig.json");
        Files.writeString(file, json());

        assertEquals("1.2.3", SignatureIO.load(file).apiVersion);
        assertEquals("1.2.3", SignatureIO.load(file.toFile()).apiVersion);
    }

    @Test
    void load_fromInputStream() throws Exception {
        byte[] bytes = json().getBytes(StandardCharsets.UTF_8);

        assertEquals("1.2.3", SignatureIO.load(new ByteArrayInputStream(bytes)).apiVersion);
    }

    @Test
    void load_missingFileThrowsIOException() {
        assertThrows(IOException.class, () -> SignatureIO.load(tempDir.resolve("missing.sig.json")));
    }

    @Test
    void load_invalidJsonThrowsIOException() {
        assertThrows(IOException.class,
                () -> SignatureIO.load(new ByteArrayInputStream("not json".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void loadResource_findsResourceWithAndWithoutLeadingSlash() throws Exception {
        Files.writeString(tempDir.resolve("api.sig.json"), json());
        try (URLClassLoader loader = new URLClassLoader(new URL[] {tempDir.toUri().toURL()}, null)) {
            assertEquals("1.2.3", SignatureIO.loadResource(loader, "/api.sig.json").apiVersion);
            assertEquals("1.2.3", SignatureIO.loadResource(loader, "api.sig.json").apiVersion);
        }
    }

    @Test
    void loadResource_missingResourceThrowsIOException() {
        IOException e = assertThrows(IOException.class,
                () -> SignatureIO.loadResource(getClass().getClassLoader(), "/does-not-exist.sig.json"));
        assertEquals("Baseline signature resource not found on classpath: /does-not-exist.sig.json",
                e.getMessage());
    }

    // -------------------------------------------------------------------------
    // Save tests
    // -------------------------------------------------------------------------

    @Test
    void save_toPath_roundtrips() throws Exception {
        SignatureRecord original = new SignatureRecord();
        original.apiVersion = "2.0.0";

        Path file = tempDir.resolve("out.sig.json");
        SignatureIO.save(original, file);

        assertEquals("2.0.0", SignatureIO.load(file).apiVersion);
    }

    @Test
    void save_toPath_createsParentDirectories() throws Exception {
        SignatureRecord record = new SignatureRecord();
        record.apiVersion = "3.0.0";

        Path nested = tempDir.resolve("a/b/c/out.sig.json");
        SignatureIO.save(record, nested);

        assertEquals("3.0.0", SignatureIO.load(nested).apiVersion);
    }

    @Test
    void save_toWriter_producesValidJson() throws Exception {
        SignatureRecord record = new SignatureRecord();
        record.apiVersion = "4.0.0";

        StringWriter sw = new StringWriter();
        SignatureIO.save(record, sw);

        byte[] bytes = sw.toString().getBytes(StandardCharsets.UTF_8);
        assertEquals("4.0.0", SignatureIO.load(new ByteArrayInputStream(bytes)).apiVersion);
    }

    @Test
    void save_toPath_outputIsFormatted() throws Exception {
        SignatureRecord record = new SignatureRecord();
        record.apiVersion = "5.0.0";

        Path file = tempDir.resolve("formatted.sig.json");
        SignatureIO.save(record, file);

        String content = Files.readString(file);
        // Pretty-formatted JSON contains newlines.
        assertTrue(content.contains("\n"), "Expected pretty-formatted (multi-line) JSON output");
    }
}
