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

/**
 * Describes a single public or protected class / interface / enum / annotation
 * found in the scanned packages.
 */
public class ClassRecord {

    /** Fully-qualified class name, e.g. {@code "jakarta.servlet.Servlet"}. */
    public String name;

    /**
     * Kind of type: {@code "class"}, {@code "interface"}, {@code "enum"},
     * {@code "annotation"}, or {@code "record"}.
     */
    public String kind;

    /** Space-separated modifier keywords, e.g. {@code "public abstract"}. */
    public String modifiers;

    /** Fully-qualified superclass name, or {@code null} for interfaces / {@code java.lang.Object}. */
    public String superClass;

    /** Fully-qualified names of directly implemented interfaces. */
    public List<String> interfaces;

    /** Annotation descriptors applied to this class. */
    public List<AnnotationRecord> annotations;

    /** Constructors declared by this class (empty for interfaces/annotations). */
    public List<MethodRecord> constructors;

    /** Public and protected methods. */
    public List<MethodRecord> methods;

    /** Public and protected fields. */
    public List<FieldRecord> fields;

    /**
     * Full JVM generic signature of this class (e.g. {@code "<T:Ljava/lang/Object;>Ljava/lang/Object;"})
     * as emitted by {@code javac} for generic classes. {@code null} for non-generic classes.
     */
    public String genericSignature;

    /**
     * Simple names of non-public member classes (hidden fields / inner classes)
     * carried for informational purposes only — these are not part of the
     * checked API contract.
     */
    public List<String> hiddenClasses;
}
