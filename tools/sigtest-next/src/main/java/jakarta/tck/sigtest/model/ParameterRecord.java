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
 * Describes a single method or constructor parameter.
 */
@JsonbPropertyOrder({"type", "annotations"})
public class ParameterRecord {

    /** Erased parameter type name, e.g. {@code "java.lang.String"} or {@code "int"}. */
    public String type;

    /** Annotations applied to this parameter. Absent when the parameter is not annotated. */
    public List<AnnotationRecord> annotations;

    public ParameterRecord() {
    }

    public ParameterRecord(String type) {
        this.type = type;
    }
}
