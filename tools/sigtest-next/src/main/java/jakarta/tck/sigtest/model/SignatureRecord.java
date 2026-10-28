/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.model;

import java.util.List;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * Root record for a generated signature file.
 *
 * <p>This is the top-level object serialised to JSON. It captures the
 * scanned packages, the generator version, and the ordered list of public
 * API classes found on those packages.
 */
@JsonbPropertyOrder({
    "formatVersion",
    "apiVersion",
    "packages",
    "classes"
})
public class SignatureRecord {

    /** Version of the sigtest-next format; increment when the schema changes. */
    public static final String FORMAT_VERSION = "1.0";

    /** Format version written into every generated file. */
    public String formatVersion = FORMAT_VERSION;

    /** Version of the API artifact being described (e.g. {@code "11.0.0"}). */
    public String apiVersion;

    /**
     * Comma-separated list of root packages that were scanned (e.g.
     * {@code "jakarta.servlet,jakarta.servlet.http"}).
     */
    public String packages;

    /** All public / protected classes found in the scanned packages, sorted by qualified name. */
    public List<ClassRecord> classes;
}
