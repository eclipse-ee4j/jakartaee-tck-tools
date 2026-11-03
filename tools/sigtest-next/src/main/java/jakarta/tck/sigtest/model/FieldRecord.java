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

    /**
     * Annotations applied to this field, keyed by fully-qualified annotation type.
     * Each value lists the explicitly written elements, in declaration order:
     * <ul>
     * <li>the element named {@code value} is rendered bare (arrays are flattened
     *     into one entry per item), e.g. {@code "java.lang.annotation.ElementType.METHOD"};</li>
     * <li>any other element is rendered as {@code name=value};</li>
     * <li>values are typed: strings are quoted ({@code "1"}), chars single-quoted,
     *     {@code long}/{@code float} suffixed ({@code 1L}, {@code 1.0f}),
     *     {@code byte}/{@code short} cast ({@code (byte)1}), class literals end in
     *     {@code .class}, enum constants are {@code pkg.Enum.CONSTANT}, and nested
     *     annotations are {@code @pkg.Anno(name=value, ...)}.</li>
     * </ul>
     * A marker annotation maps to an empty list.
     */
    public Map<String, List<String>> annotations;
}
