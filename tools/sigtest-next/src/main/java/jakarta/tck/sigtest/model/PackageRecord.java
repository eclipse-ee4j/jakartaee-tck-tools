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
import java.util.Map;

import jakarta.json.bind.annotation.JsonbPropertyOrder;

/**
 * Describes a package in the scanned API, including package-level annotations.
 */
@JsonbPropertyOrder({
    "name",
    "annotations"
})
public class PackageRecord {

    /** Fully-qualified package name, e.g. {@code "jakarta.data.metamodel.impl"}. */
    public String name;

    /** Annotations applied to this package; format as in {@link ClassRecord#annotations}. */
    public Map<String, List<String>> annotations;

    public PackageRecord() {
    }

    public PackageRecord(String name) {
        this.name = name;
    }
}
