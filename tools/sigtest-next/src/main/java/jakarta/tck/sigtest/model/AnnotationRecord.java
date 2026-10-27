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

import java.util.Map;

/**
 * Describes an annotation applied to a class, method, field, or parameter.
 */
public class AnnotationRecord {

    /**
     * Fully-qualified annotation type name, e.g.
     * {@code "java.lang.Deprecated"}.
     */
    public String type;

    /**
     * Annotation element values keyed by element name.  Values are rendered
     * as their {@link Object#toString()} representation.  Array values are
     * rendered as {@code "[v1, v2, ...]"}.  Absent when the annotation has
     * no elements.
     */
    public Map<String, String> values;
}
