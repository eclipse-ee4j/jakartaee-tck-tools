/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.check;

/**
 * A single API compatibility violation found by {@link SignatureDiff}.
 */
public final class Violation {

    /** High-level category of incompatibility. */
    public enum Kind {
        /** A public/protected class that exists in the baseline is absent from the live API. */
        MISSING_CLASS,
        /** A class present in the live API was not in the baseline. */
        ADDED_CLASS,
        /** The kind of a type changed (e.g. {@code class} → {@code interface}). */
        CLASS_KIND_CHANGED,
        /** The modifier string of a class changed. */
        CLASS_MODIFIERS_CHANGED,
        /** The superclass of a class changed. */
        SUPERCLASS_CHANGED,
        /** A class's generic signature changed. */
        CLASS_GENERIC_SIGNATURE_CHANGED,
        /** A class's annotations changed. */
        CLASS_ANNOTATIONS_CHANGED,
        /** An interface that was on the baseline class is no longer present. */
        MISSING_INTERFACE,
        /** An interface was added to a class or interface. */
        ADDED_INTERFACE,
        /** A class's directly enclosed public/protected inner classes changed. */
        INNER_CLASSES_CHANGED,
        /** A public/protected method present in the baseline is absent from the live class. */
        MISSING_METHOD,
        /** A new abstract method was added to an interface or abstract class. */
        ADDED_ABSTRACT_METHOD,
        /** A method's return type changed. */
        METHOD_RETURN_TYPE_CHANGED,
        /** A method's parameter list changed. */
        METHOD_PARAMETERS_CHANGED,
        /** A method's modifier string changed. */
        METHOD_MODIFIERS_CHANGED,
        /** A method's declared exceptions changed. */
        METHOD_EXCEPTIONS_CHANGED,
        /** A method's generic signature changed. */
        METHOD_GENERIC_SIGNATURE_CHANGED,
        /** A method's annotations changed. */
        METHOD_ANNOTATIONS_CHANGED,
        /** An annotation element's default value changed. */
        ANNOTATION_DEFAULT_VALUE_CHANGED,
        /** A parameter's annotations changed on a method or constructor. */
        PARAMETER_ANNOTATIONS_CHANGED,
        /** A public/protected constructor present in the baseline is absent from the live class. */
        MISSING_CONSTRUCTOR,
        /** A constructor's parameter list changed (matched by index when counts are equal). */
        CONSTRUCTOR_PARAMETERS_CHANGED,
        /** A constructor's modifier string changed. */
        CONSTRUCTOR_MODIFIERS_CHANGED,
        /** A constructor's declared exceptions changed. */
        CONSTRUCTOR_EXCEPTIONS_CHANGED,
        /** A constructor's generic signature changed. */
        CONSTRUCTOR_GENERIC_SIGNATURE_CHANGED,
        /** A constructor's annotations changed. */
        CONSTRUCTOR_ANNOTATIONS_CHANGED,
        /** A public/protected field present in the baseline is absent from the live class. */
        MISSING_FIELD,
        /** A field's type changed. */
        FIELD_TYPE_CHANGED,
        /** A field's modifier string changed. */
        FIELD_MODIFIERS_CHANGED,
        /** A field's constant value changed. */
        FIELD_CONSTANT_VALUE_CHANGED,
        /** A field's generic signature changed. */
        FIELD_GENERIC_SIGNATURE_CHANGED,
        /** A field's annotations changed. */
        FIELD_ANNOTATIONS_CHANGED,
        /** A package's package-level annotations changed. */
        PACKAGE_ANNOTATIONS_CHANGED,
    }

    public final Kind kind;
    /** Fully-qualified class name the violation belongs to. */
    public final String className;
    /** Short human-readable description of the specific change. */
    public final String message;

    private Violation(Kind kind, String className, String message) {
        this.kind = kind;
        this.className = className;
        this.message = message;
    }

    /** Factory used internally by {@link SignatureDiff}. */
    static Violation of(Kind kind, String className, String message) {
        return new Violation(kind, className, message);
    }

    @Override
    public String toString() {
        return "[" + kind + "] " + className + ": " + message;
    }
}
