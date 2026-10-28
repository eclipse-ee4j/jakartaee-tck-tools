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
 * Describes a public or protected field.
 */
@JsonbPropertyOrder({
    "modifiers",
    "type",
    "name",
    "constantValue",
    "genericSignature",
    "annotations"
})
public class FieldRecord {

    /** Simple field name. */
    public String name;

    /** Field type descriptor, e.g. {@code "java.lang.String"} or {@code "int"}. */
    public String type;

    /** Space-separated modifier keywords, e.g. {@code "public static final"}. */
    public String modifiers;

    /**
     * Constant value for {@code static final} primitive / String fields, or
     * {@code null} when the value is not a compile-time constant.
     */
    public String constantValue;

    /**
     * Full JVM generic signature of this field (e.g. {@code "Ljava/util/List<TT;>;"})
     * as emitted by {@code javac} for fields whose type involves a type variable or
     * parameterised type. {@code null} for fields with no generic type information.
     */
    public String genericSignature;

    /** Annotation descriptors applied to this field. */
    public List<AnnotationRecord> annotations;
}
