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

import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.FieldRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.PackageRecord;
import jakarta.tck.sigtest.model.ParameterRecord;
import jakarta.tck.sigtest.model.SignatureRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Compares a baseline {@link SignatureRecord} against a live one and returns
 * every compatibility violation found.
 *
 * <p>The rules applied are binary-compatibility-oriented: anything that
 * removes or changes an existing public/protected API member is a violation.
 * Additions (new classes, methods, fields) are reported as informational
 * {@link Violation.Kind#ADDED_CLASS} entries but do not constitute failures
 * unless the caller chooses to treat them as such.
 *
 * <h2>Matching strategy</h2>
 * <ul>
 *   <li><b>Classes</b> — matched by fully-qualified name.</li>
 *   <li><b>Methods</b> — matched by name <em>and</em> erased parameter list.
 *       This mirrors the JVM method descriptor used for binary compatibility.</li>
 *   <li><b>Constructors</b> — matched by erased parameter list.</li>
 *   <li><b>Fields</b> — matched by name.</li>
 * </ul>
 */
public final class SignatureDiff {

    private SignatureDiff() {
    }

    /**
     * Compares {@code baseline} against {@code live} and returns all violations.
     *
     * @param baseline the previously recorded "golden" signature
     * @param live     the freshly scanned signature to validate
     * @return unmodifiable list of violations; empty when the APIs are compatible
     */
    public static List<Violation> diff(SignatureRecord baseline, SignatureRecord live) {
        List<Violation> violations = new ArrayList<>();

        diffPackages(baseline, live, violations);

        Map<String, ClassRecord> liveByName = index(live.classes);

        for (ClassRecord baseClass : nullToEmpty(baseline.classes)) {
            ClassRecord liveClass = liveByName.get(baseClass.name);

            if (liveClass == null) {
                violations.add(Violation.of(
                        Violation.Kind.MISSING_CLASS, baseClass.name,
                        "Class was present in baseline but is absent from the live API"));
                continue;
            }

            diffClass(baseClass, liveClass, violations);
        }

        // Report additions (informational only).
        Map<String, ClassRecord> baseByName = index(baseline.classes);
        for (ClassRecord liveClass : nullToEmpty(live.classes)) {
            if (!baseByName.containsKey(liveClass.name)) {
                violations.add(Violation.of(
                        Violation.Kind.ADDED_CLASS, liveClass.name,
                        "Class is new in the live API (not in baseline)"));
            }
        }

        return Collections.unmodifiableList(violations);
    }

    // -------------------------------------------------------------------------
    // Package-level diff
    // -------------------------------------------------------------------------

    private static void diffPackages(SignatureRecord baseline, SignatureRecord live, List<Violation> out) {
        List<PackageRecord> basePkgs = nullToEmpty(baseline.packages);
        List<PackageRecord> livePkgs = nullToEmpty(live.packages);

        Map<String, PackageRecord> livePkgMap = new LinkedHashMap<>();
        for (PackageRecord p : livePkgs) {
            livePkgMap.put(p.name, p);
        }

        for (PackageRecord basePkg : basePkgs) {
            PackageRecord livePkg = livePkgMap.get(basePkg.name);
            if (livePkg == null) {
                if (basePkg.annotations != null && !basePkg.annotations.isEmpty()) {
                    out.add(Violation.of(Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED, basePkg.name,
                            "package annotations removed: " + basePkg.annotations));
                }
            } else if (!Objects.equals(nullToEmpty(basePkg.annotations), nullToEmpty(livePkg.annotations))) {
                out.add(Violation.of(Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED, basePkg.name,
                        "package annotations changed: " + basePkg.annotations + " → " + livePkg.annotations));
            }
        }

        Map<String, PackageRecord> basePkgMap = new LinkedHashMap<>();
        for (PackageRecord p : basePkgs) {
            basePkgMap.put(p.name, p);
        }

        for (PackageRecord livePkg : livePkgs) {
            if (!basePkgMap.containsKey(livePkg.name)) {
                if (livePkg.annotations != null && !livePkg.annotations.isEmpty()) {
                    out.add(Violation.of(Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED, livePkg.name,
                            "package annotations added: " + livePkg.annotations));
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Class-level diff
    // -------------------------------------------------------------------------

    private static void diffClass(ClassRecord base, ClassRecord live, List<Violation> out) {
        String cls = base.name;

        if (!Objects.equals(base.kind, live.kind)) {
            out.add(Violation.of(Violation.Kind.CLASS_KIND_CHANGED, cls,
                    "kind changed: " + base.kind + " → " + live.kind));
        }

        if (!Objects.equals(base.modifiers, live.modifiers)) {
            out.add(Violation.of(Violation.Kind.CLASS_MODIFIERS_CHANGED, cls,
                    "modifiers changed: '" + base.modifiers + "' → '" + live.modifiers + "'"));
        }

        if (!Objects.equals(base.superClass, live.superClass)) {
            out.add(Violation.of(Violation.Kind.SUPERCLASS_CHANGED, cls,
                    "superclass changed: " + base.superClass + " → " + live.superClass));
        }

        if (!Objects.equals(base.genericSignature, live.genericSignature)) {
            out.add(Violation.of(Violation.Kind.CLASS_GENERIC_SIGNATURE_CHANGED, cls,
                    "generic signature changed: " + base.genericSignature + " → " + live.genericSignature));
        }

        if (!Objects.equals(nullToEmpty(base.annotations), nullToEmpty(live.annotations))) {
            out.add(Violation.of(Violation.Kind.CLASS_ANNOTATIONS_CHANGED, cls,
                    "annotations changed: " + base.annotations + " → " + live.annotations));
        }

        if (!Objects.equals(nullToEmpty(base.innerClasses), nullToEmpty(live.innerClasses))) {
            out.add(Violation.of(Violation.Kind.INNER_CLASSES_CHANGED, cls,
                    "inner classes changed: " + base.innerClasses + " → " + live.innerClasses));
        }

        diffInterfaces(base, live, out);
        diffMethods(base, live, out);
        diffConstructors(base, live, out);
        diffFields(base, live, out);
    }

    private static void diffInterfaces(ClassRecord base, ClassRecord live, List<Violation> out) {
        List<String> baseIfaces = nullToEmpty(base.interfaces);
        List<String> liveIfaces = nullToEmpty(live.interfaces);
        for (String iface : baseIfaces) {
            if (!liveIfaces.contains(iface)) {
                out.add(Violation.of(Violation.Kind.MISSING_INTERFACE, base.name,
                        "interface no longer implemented: " + iface));
            }
        }
        for (String iface : liveIfaces) {
            if (!baseIfaces.contains(iface)) {
                out.add(Violation.of(Violation.Kind.ADDED_INTERFACE, base.name,
                        "new interface implemented: " + iface));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Method diff
    // -------------------------------------------------------------------------

    private static void diffMethods(ClassRecord base, ClassRecord live, List<Violation> out) {
        List<MethodRecord> baseMethods = nullToEmpty(base.methods);
        List<MethodRecord> liveMethods = nullToEmpty(live.methods);

        for (MethodRecord baseMethod : baseMethods) {
            String key = methodKey(baseMethod);
            MethodRecord liveMethod = findByKey(liveMethods, key);

            if (liveMethod == null) {
                // Check whether a method with the same name but different params exists —
                // that is still a removal of the baseline overload.
                out.add(Violation.of(Violation.Kind.MISSING_METHOD, base.name,
                        "method missing: " + methodSignature(baseMethod)));
                continue;
            }

            if (!Objects.equals(baseMethod.returnType, liveMethod.returnType)) {
                out.add(Violation.of(Violation.Kind.METHOD_RETURN_TYPE_CHANGED, base.name,
                        "return type of " + methodSignature(baseMethod)
                        + " changed: " + baseMethod.returnType + " → " + liveMethod.returnType));
            }

            if (!Objects.equals(baseMethod.modifiers, liveMethod.modifiers)) {
                out.add(Violation.of(Violation.Kind.METHOD_MODIFIERS_CHANGED, base.name,
                        "modifiers of " + methodSignature(baseMethod)
                        + " changed: '" + baseMethod.modifiers + "' → '" + liveMethod.modifiers + "'"));
            }

            if (!Objects.equals(nullToEmpty(baseMethod.exceptions), nullToEmpty(liveMethod.exceptions))) {
                out.add(Violation.of(Violation.Kind.METHOD_EXCEPTIONS_CHANGED, base.name,
                        "exceptions of " + methodSignature(baseMethod)
                        + " changed: " + baseMethod.exceptions + " → " + liveMethod.exceptions));
            }

            if (!Objects.equals(baseMethod.genericSignature, liveMethod.genericSignature)) {
                out.add(Violation.of(Violation.Kind.METHOD_GENERIC_SIGNATURE_CHANGED, base.name,
                        "generic signature of " + methodSignature(baseMethod)
                        + " changed: " + baseMethod.genericSignature + " → " + liveMethod.genericSignature));
            }

            if (!Objects.equals(nullToEmpty(baseMethod.annotations), nullToEmpty(liveMethod.annotations))) {
                out.add(Violation.of(Violation.Kind.METHOD_ANNOTATIONS_CHANGED, base.name,
                        "annotations of " + methodSignature(baseMethod)
                        + " changed: " + baseMethod.annotations + " → " + liveMethod.annotations));
            }

            if (!Objects.equals(baseMethod.defaultValue, liveMethod.defaultValue)) {
                out.add(Violation.of(Violation.Kind.ANNOTATION_DEFAULT_VALUE_CHANGED, base.name,
                        "default value of annotation element " + methodSignature(baseMethod)
                        + " changed: " + baseMethod.defaultValue + " → " + liveMethod.defaultValue));
            }

            diffParameters(base.name, baseMethod, liveMethod, out);
        }

        // Report added methods (both abstract and non-abstract).
        for (MethodRecord liveMethod : liveMethods) {
            String key = methodKey(liveMethod);
            if (findByKey(baseMethods, key) == null) {
                if (liveMethod.modifiers != null && liveMethod.modifiers.contains("abstract")) {
                    out.add(Violation.of(Violation.Kind.ADDED_ABSTRACT_METHOD, base.name,
                            "new abstract method added: " + methodSignature(liveMethod)));
                } else {
                    out.add(Violation.of(Violation.Kind.ADDED_METHOD, base.name,
                            "new method added: " + methodSignature(liveMethod)));
                }
            }
        }
    }

    private static void diffParameters(String className, MethodRecord baseMethod, MethodRecord liveMethod, List<Violation> out) {
        List<ParameterRecord> baseParams = nullToEmpty(baseMethod.parameters);
        List<ParameterRecord> liveParams = nullToEmpty(liveMethod.parameters);
        int count = Math.min(baseParams.size(), liveParams.size());
        for (int i = 0; i < count; i++) {
            ParameterRecord baseP = baseParams.get(i);
            ParameterRecord liveP = liveParams.get(i);
            if (!Objects.equals(nullToEmpty(baseP.annotations), nullToEmpty(liveP.annotations))) {
                String memberDesc = baseMethod.isConstructor
                        ? "constructor (" + paramKey(baseMethod) + ")"
                        : methodSignature(baseMethod);
                out.add(Violation.of(Violation.Kind.PARAMETER_ANNOTATIONS_CHANGED, className,
                        "annotations of parameter " + (i + 1) + " of " + memberDesc
                        + " changed: " + baseP.annotations + " → " + liveP.annotations));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Constructor diff
    // -------------------------------------------------------------------------

    private static void diffConstructors(ClassRecord base, ClassRecord live, List<Violation> out) {
        List<MethodRecord> baseCtors = nullToEmpty(base.constructors);
        List<MethodRecord> liveCtors = nullToEmpty(live.constructors);

        for (MethodRecord baseCtor : baseCtors) {
            String key = paramKey(baseCtor);
            MethodRecord liveCtor = findCtorByKey(liveCtors, key);

            if (liveCtor == null) {
                out.add(Violation.of(Violation.Kind.MISSING_CONSTRUCTOR, base.name,
                        "constructor missing: " + base.name + "(" + paramKey(baseCtor) + ")"));
                continue;
            }

            if (!Objects.equals(baseCtor.modifiers, liveCtor.modifiers)) {
                out.add(Violation.of(Violation.Kind.CONSTRUCTOR_MODIFIERS_CHANGED, base.name,
                        "constructor modifiers changed: '" + baseCtor.modifiers
                        + "' → '" + liveCtor.modifiers + "' for (" + paramKey(baseCtor) + ")"));
            }

            if (!Objects.equals(nullToEmpty(baseCtor.exceptions), nullToEmpty(liveCtor.exceptions))) {
                out.add(Violation.of(Violation.Kind.CONSTRUCTOR_EXCEPTIONS_CHANGED, base.name,
                        "constructor exceptions changed for (" + paramKey(baseCtor) + "): "
                        + baseCtor.exceptions + " → " + liveCtor.exceptions));
            }

            if (!Objects.equals(baseCtor.genericSignature, liveCtor.genericSignature)) {
                out.add(Violation.of(Violation.Kind.CONSTRUCTOR_GENERIC_SIGNATURE_CHANGED, base.name,
                        "generic signature of constructor (" + paramKey(baseCtor) + ") changed: "
                        + baseCtor.genericSignature + " → " + liveCtor.genericSignature));
            }

            if (!Objects.equals(nullToEmpty(baseCtor.annotations), nullToEmpty(liveCtor.annotations))) {
                out.add(Violation.of(Violation.Kind.CONSTRUCTOR_ANNOTATIONS_CHANGED, base.name,
                        "constructor annotations changed for (" + paramKey(baseCtor) + "): "
                        + baseCtor.annotations + " → " + liveCtor.annotations));
            }

            diffParameters(base.name, baseCtor, liveCtor, out);
        }

        // Report added constructors.
        for (MethodRecord liveCtor : liveCtors) {
            String key = paramKey(liveCtor);
            if (findCtorByKey(baseCtors, key) == null) {
                out.add(Violation.of(Violation.Kind.ADDED_CONSTRUCTOR, base.name,
                        "new constructor added: " + base.name + "(" + paramKey(liveCtor) + ")"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Field diff
    // -------------------------------------------------------------------------

    private static void diffFields(ClassRecord base, ClassRecord live, List<Violation> out) {
        List<FieldRecord> baseFields = nullToEmpty(base.fields);
        Map<String, FieldRecord> liveFieldMap = new LinkedHashMap<>();
        for (FieldRecord f : nullToEmpty(live.fields)) {
            liveFieldMap.put(f.name, f);
        }

        for (FieldRecord baseField : baseFields) {
            FieldRecord liveField = liveFieldMap.get(baseField.name);

            if (liveField == null) {
                out.add(Violation.of(Violation.Kind.MISSING_FIELD, base.name,
                        "field missing: " + baseField.name));
                continue;
            }

            if (!Objects.equals(baseField.type, liveField.type)) {
                out.add(Violation.of(Violation.Kind.FIELD_TYPE_CHANGED, base.name,
                        "type of field '" + baseField.name + "' changed: "
                        + baseField.type + " → " + liveField.type));
            }

            if (!Objects.equals(baseField.modifiers, liveField.modifiers)) {
                out.add(Violation.of(Violation.Kind.FIELD_MODIFIERS_CHANGED, base.name,
                        "modifiers of field '" + baseField.name + "' changed: '"
                        + baseField.modifiers + "' → '" + liveField.modifiers + "'"));
            }

            if (!Objects.equals(baseField.constantValue, liveField.constantValue)) {
                out.add(Violation.of(Violation.Kind.FIELD_CONSTANT_VALUE_CHANGED, base.name,
                        "constant value of field '" + baseField.name + "' changed: "
                        + baseField.constantValue + " → " + liveField.constantValue));
            }

            if (!Objects.equals(baseField.genericSignature, liveField.genericSignature)) {
                out.add(Violation.of(Violation.Kind.FIELD_GENERIC_SIGNATURE_CHANGED, base.name,
                        "generic signature of field '" + baseField.name + "' changed: "
                        + baseField.genericSignature + " → " + liveField.genericSignature));
            }

            if (!Objects.equals(nullToEmpty(baseField.annotations), nullToEmpty(liveField.annotations))) {
                out.add(Violation.of(Violation.Kind.FIELD_ANNOTATIONS_CHANGED, base.name,
                        "annotations of field '" + baseField.name + "' changed: "
                        + baseField.annotations + " → " + liveField.annotations));
            }
        }

        // Report added fields.
        Map<String, FieldRecord> baseFieldMap = new LinkedHashMap<>();
        for (FieldRecord f : baseFields) {
            baseFieldMap.put(f.name, f);
        }
        for (FieldRecord liveField : nullToEmpty(live.fields)) {
            if (!baseFieldMap.containsKey(liveField.name)) {
                out.add(Violation.of(Violation.Kind.ADDED_FIELD, base.name,
                        "new field added: " + liveField.name));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Map<String, ClassRecord> index(List<ClassRecord> classes) {
        Map<String, ClassRecord> map = new LinkedHashMap<>();
        if (classes != null) {
            for (ClassRecord c : classes) {
                map.put(c.name, c);
            }
        }
        return map;
    }

    /**
     * Unique key for a method: {@code name(param1,param2,...)}. The parameter
     * list uses the erased type names (what the generator records in
     * {@link MethodRecord#parameters}), which matches the JVM's binary method
     * descriptor and is the correct identity for binary compatibility.
     */
    static String methodKey(MethodRecord m) {
        return m.name + "(" + paramKey(m) + ")";
    }

    /**
     * Key for a constructor: its erased parameter list only (constructors have
     * no name other than the class name, which is already implied by context).
     */
    static String paramKey(MethodRecord m) {
        return String.join(",", parameterTypes(m));
    }

    private static List<String> parameterTypes(MethodRecord m) {
        return nullToEmpty(m.parameters).stream().map(p -> p.type).collect(Collectors.toList());
    }

    private static <T extends MethodRecord> T findByKey(List<T> list, String key) {
        for (T m : list) {
            if (methodKey(m).equals(key)) return m;
        }
        return null;
    }

    /** Find a constructor by its erased parameter key (not by method key, which includes name). */
    private static <T extends MethodRecord> T findCtorByKey(List<T> list, String key) {
        for (T m : list) {
            if (paramKey(m).equals(key)) return m;
        }
        return null;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private static <K, V> Map<K, V> nullToEmpty(Map<K, V> map) {
        return map == null ? Collections.emptyMap() : map;
    }

    private static String methodSignature(MethodRecord m) {
        return m.name + "(" + String.join(", ", parameterTypes(m)) + ")";
    }
}
