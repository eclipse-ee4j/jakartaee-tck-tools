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
 * Describes a public or protected method or constructor.
 *
 * <p>Constructors are distinguished from regular methods by {@link #isConstructor}
 * being {@code true} and {@link #returnType} being {@code null}.
 */
@JsonbPropertyOrder({
    "modifiers",
    "returnType",
    "isConstructor",
    "name",
    "genericSignature",
    "annotations",
    "parameters",
    "exceptions"
})
public class MethodRecord {

    /** Simple method name, or the simple class name for constructors. */
    public String name;

    /** {@code true} when this record represents a constructor. */
    public boolean isConstructor;

    /** Return type descriptor, or {@code null} for constructors. */
    public String returnType;

    /** Space-separated modifier keywords, e.g. {@code "public abstract"}. */
    public String modifiers;

    /** Ordered list of parameters, each with its erased type and annotations. */
    public List<ParameterRecord> parameters;

    /** Checked exception type descriptors declared by this member. */
    public List<String> exceptions;

    /** Annotations applied to this method; format as in {@link FieldRecord#annotations}. */
    public Map<String, List<String>> annotations;

    /**
     * Full JVM generic signature of this method (e.g. {@code "<R:Ljava/lang/Object;>(TT;)TR;"})
     * as emitted by {@code javac} when the method or its parameter / return types involve
     * type variables or parameterised types. {@code null} for non-generic methods.
     */
    public String genericSignature;
}
