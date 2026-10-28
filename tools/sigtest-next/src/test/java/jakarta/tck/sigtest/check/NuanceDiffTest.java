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

import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rigorous test suite validating that {@link SignatureGenerator} and {@link SignatureDiff}
 * comprehensively detect every Java language API change across all language nuances:
 *
 * <ul>
 *   <li>Type declarations: classes, interfaces, enums, annotations ({@code @interface})</li>
 *   <li>Nesting: top-level, static nested, member inner, protected nested</li>
 *   <li>Modifiers: access, abstract, final, static, default, varargs, transient, volatile, synchronized</li>
 *   <li>Generics: type parameters, intersection bounds, recursive bounds, wildcards, generic arrays</li>
 *   <li>Constructors &amp; Methods: overloads, varargs, checked exceptions, parameter annotations (including inner class synthetic parameter alignment)</li>
 *   <li>Fields: constants (primitive &amp; String inlined), dynamic static/instance fields, modifiers, generics</li>
 *   <li>Annotations &amp; Defaults: all primitive types, strings, classes, enums, nested annotations, arrays, default values vs mandatory</li>
 *   <li>Package metadata: {@code package-info.java} annotations</li>
 * </ul>
 */
class NuanceDiffTest {

    @TempDir
    Path tempDir;

    private SignatureRecord baselineRecord;

    // -------------------------------------------------------------------------
    // Baseline Mock API Sources
    // -------------------------------------------------------------------------

    private static final String PKG = "com.example.mockapi";

    private static final String PKG_INFO_SRC =
            "@com.example.mockapi.MockAnnotation(mandatoryVal = 0, strVal = \"pkg\")\n"
            + "package com.example.mockapi;\n";

    private static final String NESTED_ANNO_SRC =
            "package com.example.mockapi;\n"
            + "import java.lang.annotation.Retention;\n"
            + "import java.lang.annotation.RetentionPolicy;\n"
            + "@Retention(RetentionPolicy.RUNTIME)\n"
            + "public @interface NestedAnnotation {\n"
            + "    String value() default \"nestedDefault\";\n"
            + "}\n";

    private static final String MOCK_ANNO_SRC =
            "package com.example.mockapi;\n"
            + "import java.lang.annotation.ElementType;\n"
            + "import java.lang.annotation.Retention;\n"
            + "import java.lang.annotation.RetentionPolicy;\n"
            + "import java.lang.annotation.Target;\n"
            + "@Retention(RetentionPolicy.RUNTIME)\n"
            + "@Target({\n"
            + "    ElementType.TYPE,\n"
            + "    ElementType.METHOD,\n"
            + "    ElementType.FIELD,\n"
            + "    ElementType.PARAMETER,\n"
            + "    ElementType.CONSTRUCTOR,\n"
            + "    ElementType.PACKAGE\n"
            + "})\n"
            + "public @interface MockAnnotation {\n"
            + "    boolean boolVal() default true;\n"
            + "    byte byteVal() default (byte) 1;\n"
            + "    short shortVal() default (short) 2;\n"
            + "    char charVal() default 'A';\n"
            + "    int intVal() default 42;\n"
            + "    long longVal() default 100L;\n"
            + "    float floatVal() default 3.14f;\n"
            + "    double doubleVal() default 2.71828;\n"
            + "    String strVal() default \"defaultStr\";\n"
            + "    Class<?> classVal() default String.class;\n"
            + "    MockEnum enumVal() default MockEnum.ALPHA;\n"
            + "    NestedAnnotation nestedVal() default @NestedAnnotation(\"nested\");\n"
            + "    String[] arrayVal() default {\"item1\", \"item2\"};\n"
            + "    int mandatoryVal();\n"
            + "}\n";

    private static final String MOCK_ENUM_SRC =
            "package com.example.mockapi;\n"
            + "public enum MockEnum {\n"
            + "    ALPHA,\n"
            + "    BETA;\n"
            + "    public static final String ENUM_CONSTANT = \"ENUM_VAL\";\n"
            + "    public void enumMethod() {}\n"
            + "}\n";

    private static final String MOCK_INTERFACE_SRC =
            "package com.example.mockapi;\n"
            + "import java.io.Serializable;\n"
            + "public interface MockInterface<T extends Number & Comparable<T>> extends Serializable {\n"
            + "    String INTERFACE_CONST = \"CONST_VAL\";\n"
            + "    void abstractMethod(T value) throws Exception;\n"
            + "    default String defaultMethod(String input) { return input; }\n"
            + "    static <E extends Exception> void staticGenericMethod() throws E {}\n"
            + "}\n";

    private static final String SECOND_INTERFACE_SRC =
            "package com.example.mockapi;\n"
            + "public interface SecondInterface {\n"
            + "    void secondMethod();\n"
            + "}\n";

    private static final String MOCK_SUPER_CLASS_SRC =
            "package com.example.mockapi;\n"
            + "public class MockSuperClass {\n"
            + "    public void superMethod() {}\n"
            + "}\n";

    private static final String MOCK_CLASS_SRC =
            "package com.example.mockapi;\n"
            + "import java.io.IOException;\n"
            + "import java.util.Collections;\n"
            + "import java.util.List;\n"
            + "import java.util.Map;\n"
            + "import java.util.NoSuchElementException;\n"
            + "@MockAnnotation(mandatoryVal = 100, strVal = \"classAnno\")\n"
            + "public class MockClass<T extends Number & Comparable<T>, R> extends MockSuperClass implements MockInterface<T> {\n"
            + "    public static final int INT_CONSTANT = 12345;\n"
            + "    public static final String STR_CONSTANT = \"compileTimeConstant\";\n"
            + "    public static String nonConstStatic = \"dynamic\";\n"
            + "    public volatile transient Object volatileTransientField;\n"
            + "    public List<String> genericListField;\n"
            + "    @MockAnnotation(mandatoryVal = 1)\n"
            + "    public MockClass(@MockAnnotation(mandatoryVal = 2) String param) throws IllegalArgumentException {}\n"
            + "    protected <C> MockClass(C genericC, int count) throws IOException {}\n"
            + "    @Override\n"
            + "    public void abstractMethod(T value) throws Exception {}\n"
            + "    @MockAnnotation(mandatoryVal = 10)\n"
            + "    public final synchronized Map<String, List<? extends R>> complexGenericMethod(\n"
            + "            @MockAnnotation(mandatoryVal = 20) List<? super T> inputList,\n"
            + "            String... varargsParams) throws NoSuchElementException {\n"
            + "        return Collections.emptyMap();\n"
            + "    }\n"
            + "    public static class StaticNestedClass<E extends Enum<E>> {\n"
            + "        protected int nestedField;\n"
            + "        public void nestedMethod(E enumVal) {}\n"
            + "    }\n"
            + "    public class MemberInnerClass {\n"
            + "        public MemberInnerClass(@MockAnnotation(mandatoryVal = 30) int innerParam) {}\n"
            + "        public void innerMethod() {}\n"
            + "    }\n"
            + "    protected static class ProtectedNestedClass {\n"
            + "        public void protectedInnerMethod() {}\n"
            + "    }\n"
            + "}\n";

    private static Map<String, String> createBaselineSources() {
        Map<String, String> map = new HashMap<>();
        map.put("package-info.java", PKG_INFO_SRC);
        map.put("NestedAnnotation.java", NESTED_ANNO_SRC);
        map.put("MockAnnotation.java", MOCK_ANNO_SRC);
        map.put("MockEnum.java", MOCK_ENUM_SRC);
        map.put("MockInterface.java", MOCK_INTERFACE_SRC);
        map.put("SecondInterface.java", SECOND_INTERFACE_SRC);
        map.put("MockSuperClass.java", MOCK_SUPER_CLASS_SRC);
        map.put("MockClass.java", MOCK_CLASS_SRC);
        return map;
    }

    @BeforeEach
    void setUpBaseline() throws Exception {
        baselineRecord = compileAndGenerate("baseline", createBaselineSources());
        assertNotNull(baselineRecord);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SignatureRecord compileAndGenerate(String subdir, Map<String, String> sources) throws Exception {
        Path dir = tempDir.resolve(subdir);
        Files.createDirectories(dir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available");

        Path srcDir = dir.resolve("src").resolve("com").resolve("example").resolve("mockapi");
        Files.createDirectories(srcDir);

        List<String> args = new ArrayList<>(List.of(
                "-d", dir.toString(),
                "-cp", dir.toString(),
                "--release", "11"
        ));

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path srcFile = srcDir.resolve(entry.getKey());
            Files.writeString(srcFile, entry.getValue());
            args.add(srcFile.toString());
        }

        int rc = compiler.run(null, null, null, args.toArray(String[]::new));
        assertEquals(0, rc, "Compilation failed for " + subdir);

        SignatureGenerator gen = new SignatureGenerator(List.of(dir.toFile()), Set.of(PKG), Set.of());
        return gen.generate("1.0");
    }

    private static List<Violation> findViolations(List<Violation> violations, Violation.Kind kind) {
        return violations.stream().filter(v -> v.kind == kind).collect(java.util.stream.Collectors.toList());
    }

    // =========================================================================
    // 1. Baseline Fidelity Verification
    // =========================================================================

    @Nested
    @DisplayName("Baseline Fidelity & Self-Diff")
    class BaselineFidelityTests {

        @Test
        @DisplayName("Identical baseline against itself produces zero violations")
        void baselineSelfDiffHasNoViolations() {
            List<Violation> violations = SignatureDiff.diff(baselineRecord, baselineRecord);
            assertTrue(violations.isEmpty(), "Expected 0 violations but got: " + violations);
        }

        @Test
        @DisplayName("Baseline captures package annotations, all class kinds, and nested classes")
        void baselineContainsAllExpectedElements() {
            assertNotNull(baselineRecord.packages);
            assertFalse(baselineRecord.packages.isEmpty());
            assertEquals("com.example.mockapi", baselineRecord.packages.get(0).name);
            assertTrue(baselineRecord.packages.get(0).annotations.containsKey("com.example.mockapi.MockAnnotation"));

            Set<String> classNames = baselineRecord.classes.stream().map(c -> c.name).collect(java.util.stream.Collectors.toSet());
            assertTrue(classNames.contains("com.example.mockapi.MockAnnotation"));
            assertTrue(classNames.contains("com.example.mockapi.MockEnum"));
            assertTrue(classNames.contains("com.example.mockapi.MockInterface"));
            assertTrue(classNames.contains("com.example.mockapi.MockClass"));
            assertTrue(classNames.contains("com.example.mockapi.MockClass$StaticNestedClass"));
            assertTrue(classNames.contains("com.example.mockapi.MockClass$MemberInnerClass"));
            assertTrue(classNames.contains("com.example.mockapi.MockClass$ProtectedNestedClass"));
        }
    }

    // =========================================================================
    // 2. Package-Level Modifications
    // =========================================================================

    @Nested
    @DisplayName("Package Metadata Mutations")
    class PackageMutationTests {

        @Test
        @DisplayName("Package annotation value changed triggers PACKAGE_ANNOTATIONS_CHANGED")
        void packageAnnotationValueChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("package-info.java",
                    "@com.example.mockapi.MockAnnotation(mandatoryVal = 999, strVal = \"changedPkg\")\n"
                    + "package com.example.mockapi;\n");

            SignatureRecord live = compileAndGenerate("pkg_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi", matches.get(0).className);
        }

        @Test
        @DisplayName("Package annotation removed triggers PACKAGE_ANNOTATIONS_CHANGED")
        void packageAnnotationRemoved() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("package-info.java", "package com.example.mockapi;\n");

            SignatureRecord live = compileAndGenerate("pkg_anno_removed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
        }
    }

    // =========================================================================
    // 3. Type-Level Modifications
    // =========================================================================

    @Nested
    @DisplayName("Type-Level Mutations")
    class TypeMutationTests {

        @Test
        @DisplayName("Missing class triggers MISSING_CLASS")
        void missingClassDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.remove("SecondInterface.java");

            SignatureRecord live = compileAndGenerate("missing_class", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.MISSING_CLASS);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.SecondInterface", matches.get(0).className);
        }

        @Test
        @DisplayName("Added class triggers ADDED_CLASS")
        void addedClassDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("AddedClass.java", "package com.example.mockapi;\npublic class AddedClass {}\n");

            SignatureRecord live = compileAndGenerate("added_class", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_CLASS);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.AddedClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Class kind changed (interface to class) triggers CLASS_KIND_CHANGED")
        void classKindChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("SecondInterface.java", "package com.example.mockapi;\npublic class SecondInterface { public void secondMethod() {} }\n");

            SignatureRecord live = compileAndGenerate("kind_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CLASS_KIND_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.SecondInterface", matches.get(0).className);
        }

        @Test
        @DisplayName("Class modifier changed to final triggers CLASS_MODIFIERS_CHANGED")
        void classModifiersChangedToFinal() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public class MockClass<",
                    "public final class MockClass<"));

            SignatureRecord live = compileAndGenerate("mod_final", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CLASS_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Superclass changed triggers SUPERCLASS_CHANGED")
        void superclassChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            // Change MockClass to not extend MockSuperClass (defaults to Object)
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace("extends MockSuperClass ", ""));

            SignatureRecord live = compileAndGenerate("super_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.SUPERCLASS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Class generic signature changed triggers CLASS_GENERIC_SIGNATURE_CHANGED")
        void classGenericSignatureChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            // Add bound to type parameter R
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public class MockClass<T extends Number & Comparable<T>, R>",
                    "public class MockClass<T extends Number & Comparable<T>, R extends CharSequence>"));

            SignatureRecord live = compileAndGenerate("class_generic_sig", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CLASS_GENERIC_SIGNATURE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Class annotations changed triggers CLASS_ANNOTATIONS_CHANGED")
        void classAnnotationsChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 100, strVal = \"classAnno\")",
                    "@MockAnnotation(mandatoryVal = 200, strVal = \"changedClassAnno\")"));

            SignatureRecord live = compileAndGenerate("class_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CLASS_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Implemented interface removed triggers MISSING_INTERFACE")
        void missingInterfaceDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC
                    .replace("implements MockInterface<T>", "")
                    .replace("@Override\n    public void abstractMethod(T value) throws Exception {}",
                             "public void abstractMethod(T value) throws Exception {}"));

            SignatureRecord live = compileAndGenerate("missing_iface", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.MISSING_INTERFACE);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("New implemented interface added triggers ADDED_INTERFACE")
        void addedInterfaceDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "implements MockInterface<T>",
                    "implements MockInterface<T>, SecondInterface")
                    + "\n");
            // Also supply the dummy implementation for secondMethod
            sources.put("MockClass.java", sources.get("MockClass.java").replace(
                    "public void protectedInnerMethod() {}",
                    "public void protectedInnerMethod() {}\n}\npublic void secondMethod() {}\n{"));

            // Let's ensure MockClass compiles cleanly with secondMethod
            String fixedMockClass = MOCK_CLASS_SRC.replace(
                    "implements MockInterface<T>",
                    "implements MockInterface<T>, SecondInterface")
                    .replace(
                            "public void abstractMethod(T value) throws Exception {}",
                            "public void abstractMethod(T value) throws Exception {}\n    @Override public void secondMethod() {}");
            sources.put("MockClass.java", fixedMockClass);

            SignatureRecord live = compileAndGenerate("added_iface", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_INTERFACE);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Inner classes list change triggers INNER_CLASSES_CHANGED")
        void innerClassesChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            String innerClassBlock = "    public class MemberInnerClass {\n"
                    + "        public MemberInnerClass(@MockAnnotation(mandatoryVal = 30) int innerParam) {}\n"
                    + "        public void innerMethod() {}\n"
                    + "    }\n";
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(innerClassBlock, ""));

            SignatureRecord live = compileAndGenerate("inner_cls_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> innerMatches = findViolations(violations, Violation.Kind.INNER_CLASSES_CHANGED);
            assertEquals(1, innerMatches.size());
            assertEquals("com.example.mockapi.MockClass", innerMatches.get(0).className);
        }
    }

    // =========================================================================
    // 4. Method-Level Modifications
    // =========================================================================

    @Nested
    @DisplayName("Method-Level Mutations")
    class MethodMutationTests {

        @Test
        @DisplayName("Missing method triggers MISSING_METHOD")
        void missingMethodDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java", "package com.example.mockapi;\npublic class MockSuperClass {}\n");

            SignatureRecord live = compileAndGenerate("missing_method", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.MISSING_METHOD);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Added abstract method to interface triggers ADDED_ABSTRACT_METHOD")
        void addedAbstractMethodToInterfaceDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("SecondInterface.java",
                    "package com.example.mockapi;\n"
                    + "public interface SecondInterface {\n"
                    + "    void secondMethod();\n"
                    + "    void newlyAddedAbstractMethod();\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("added_abstract_meth", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_ABSTRACT_METHOD);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.SecondInterface", matches.get(0).className);
        }

        @Test
        @DisplayName("Method return type changed triggers METHOD_RETURN_TYPE_CHANGED")
        void methodReturnTypeChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public int superMethod() { return 0; }\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("ret_type_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_RETURN_TYPE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Method modifiers changed triggers METHOD_MODIFIERS_CHANGED")
        void methodModifiersChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public final void superMethod() {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("meth_mods_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Method exceptions changed triggers METHOD_EXCEPTIONS_CHANGED")
        void methodExceptionsChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public void superMethod() throws java.io.IOException {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("meth_ex_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_EXCEPTIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Method generic signature changed triggers METHOD_GENERIC_SIGNATURE_CHANGED")
        void methodGenericSignatureChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockInterface.java", MOCK_INTERFACE_SRC.replace(
                    "static <E extends Exception> void staticGenericMethod() throws E {}",
                    "static <E extends Throwable> void staticGenericMethod() throws E {}"));

            SignatureRecord live = compileAndGenerate("meth_generic_sig", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_GENERIC_SIGNATURE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockInterface", matches.get(0).className);
        }

        @Test
        @DisplayName("Method annotations changed triggers METHOD_ANNOTATIONS_CHANGED")
        void methodAnnotationsChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 10)\n    public final synchronized Map",
                    "@MockAnnotation(mandatoryVal = 99)\n    public final synchronized Map"));

            SignatureRecord live = compileAndGenerate("meth_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Annotation default value changed triggers ANNOTATION_DEFAULT_VALUE_CHANGED")
        void annotationDefaultValueChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC.replace(
                    "int intVal() default 42;",
                    "int intVal() default 9999;"));

            SignatureRecord live = compileAndGenerate("anno_def_val_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ANNOTATION_DEFAULT_VALUE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockAnnotation", matches.get(0).className);
        }

        @Test
        @DisplayName("Method parameter annotation changed triggers PARAMETER_ANNOTATIONS_CHANGED")
        void methodParameterAnnotationChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 20) List<? super T> inputList",
                    "@MockAnnotation(mandatoryVal = 88) List<? super T> inputList"));

            SignatureRecord live = compileAndGenerate("meth_param_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.PARAMETER_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Varargs method modifier change to array triggers METHOD_MODIFIERS_CHANGED")
        void methodVarargsModifierChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "String... varargsParams",
                    "String[] varargsParams"));

            SignatureRecord live = compileAndGenerate("varargs_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Interface default method converted to abstract triggers METHOD_MODIFIERS_CHANGED")
        void interfaceDefaultMethodMadeAbstract() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockInterface.java", MOCK_INTERFACE_SRC.replace(
                    "default String defaultMethod(String input) { return input; }",
                    "String defaultMethod(String input);"));
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public void abstractMethod(T value) throws Exception {}",
                    "public void abstractMethod(T value) throws Exception {}\n    @Override public String defaultMethod(String input) { return input; }"));

            SignatureRecord live = compileAndGenerate("iface_default_to_abstract", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.METHOD_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockInterface", matches.get(0).className);
        }

        @Test
        @DisplayName("Changing parameter types of an interface method emits MISSING_METHOD and ADDED_ABSTRACT_METHOD")
        void interfaceMethodParameterTypeChangeEmitsMissingAndAddedAbstractMethod() throws Exception {
            Map<String, String> sources = createBaselineSources();
            // Change secondMethod() to secondMethod(int x) on SecondInterface
            sources.put("SecondInterface.java",
                    "package com.example.mockapi;\n"
                    + "public interface SecondInterface {\n"
                    + "    void secondMethod(int x);\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("iface_param_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> missingMatches = findViolations(violations, Violation.Kind.MISSING_METHOD);
            List<Violation> addedMatches = findViolations(violations, Violation.Kind.ADDED_ABSTRACT_METHOD);

            assertEquals(1, missingMatches.size());
            assertEquals("com.example.mockapi.SecondInterface", missingMatches.get(0).className);

            assertEquals(1, addedMatches.size());
            assertEquals("com.example.mockapi.SecondInterface", addedMatches.get(0).className);
        }

        @Test
        @DisplayName("Changing parameter types of a concrete class method emits MISSING_METHOD and ADDED_METHOD")
        void concreteMethodParameterTypeChangeEmitsMissingAndAddedMethod() throws Exception {
            Map<String, String> sources = createBaselineSources();
            // Change superMethod() to superMethod(int count)
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public void superMethod(int count) {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("concrete_param_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> missingMatches = findViolations(violations, Violation.Kind.MISSING_METHOD);
            List<Violation> addedMatches = findViolations(violations, Violation.Kind.ADDED_METHOD);

            assertEquals(1, missingMatches.size());
            assertEquals("com.example.mockapi.MockSuperClass", missingMatches.get(0).className);

            assertEquals(1, addedMatches.size());
            assertEquals("com.example.mockapi.MockSuperClass", addedMatches.get(0).className);
        }

        @Test
        @DisplayName("Added non-abstract method to class triggers ADDED_METHOD")
        void addedNonAbstractMethodDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public void superMethod() {}\n"
                    + "    public void newMethod() {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("added_meth", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_METHOD);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }
    }

    // =========================================================================
    // 5. Constructor-Level Modifications
    // =========================================================================

    @Nested
    @DisplayName("Constructor-Level Mutations")
    class ConstructorMutationTests {

        @Test
        @DisplayName("Missing constructor triggers MISSING_CONSTRUCTOR")
        void missingConstructorDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "protected <C> MockClass(C genericC, int count) throws IOException {}",
                    ""));

            SignatureRecord live = compileAndGenerate("missing_ctor", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.MISSING_CONSTRUCTOR);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Constructor modifiers changed triggers CONSTRUCTOR_MODIFIERS_CHANGED")
        void constructorModifiersChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "protected <C> MockClass(C genericC, int count) throws IOException {}",
                    "public <C> MockClass(C genericC, int count) throws IOException {}"));

            SignatureRecord live = compileAndGenerate("ctor_mods_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CONSTRUCTOR_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Constructor exceptions changed triggers CONSTRUCTOR_EXCEPTIONS_CHANGED")
        void constructorExceptionsChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public MockClass(@MockAnnotation(mandatoryVal = 2) String param) throws IllegalArgumentException {}",
                    "public MockClass(@MockAnnotation(mandatoryVal = 2) String param) throws Exception {}"));

            SignatureRecord live = compileAndGenerate("ctor_ex_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CONSTRUCTOR_EXCEPTIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Constructor generic signature changed triggers CONSTRUCTOR_GENERIC_SIGNATURE_CHANGED")
        void constructorGenericSignatureChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "protected <C> MockClass(C genericC, int count) throws IOException {}",
                    "protected <C extends Object & java.io.Serializable> MockClass(C genericC, int count) throws IOException {}"));

            SignatureRecord live = compileAndGenerate("ctor_gen_sig", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CONSTRUCTOR_GENERIC_SIGNATURE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Constructor annotations changed triggers CONSTRUCTOR_ANNOTATIONS_CHANGED")
        void constructorAnnotationsChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 1)\n    public MockClass(",
                    "@MockAnnotation(mandatoryVal = 999)\n    public MockClass("));

            SignatureRecord live = compileAndGenerate("ctor_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.CONSTRUCTOR_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Top-level constructor parameter annotation changed triggers PARAMETER_ANNOTATIONS_CHANGED")
        void topLevelConstructorParameterAnnotationChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 2) String param",
                    "@MockAnnotation(mandatoryVal = 77) String param"));

            SignatureRecord live = compileAndGenerate("ctor_param_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.PARAMETER_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Inner class constructor parameter annotation changed (testing synthetic outer-this shift) triggers PARAMETER_ANNOTATIONS_CHANGED")
        void innerClassConstructorParameterAnnotationChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "@MockAnnotation(mandatoryVal = 30) int innerParam",
                    "@MockAnnotation(mandatoryVal = 404) int innerParam"));

            SignatureRecord live = compileAndGenerate("inner_ctor_param_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.PARAMETER_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass$MemberInnerClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Changing parameter types of a constructor emits MISSING_CONSTRUCTOR and ADDED_CONSTRUCTOR")
        void constructorParameterTypeChangeEmitsMissingAndAddedConstructor() throws Exception {
            Map<String, String> sources = createBaselineSources();
            // Change MockClass(String param) to MockClass(int param)
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public MockClass(@MockAnnotation(mandatoryVal = 2) String param) throws IllegalArgumentException {}",
                    "public MockClass(int param) throws IllegalArgumentException {}"));

            SignatureRecord live = compileAndGenerate("ctor_param_type_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> missingMatches = findViolations(violations, Violation.Kind.MISSING_CONSTRUCTOR);
            List<Violation> addedMatches = findViolations(violations, Violation.Kind.ADDED_CONSTRUCTOR);

            assertEquals(1, missingMatches.size());
            assertEquals("com.example.mockapi.MockClass", missingMatches.get(0).className);

            assertEquals(1, addedMatches.size());
            assertEquals("com.example.mockapi.MockClass", addedMatches.get(0).className);
        }

        @Test
        @DisplayName("Added constructor triggers ADDED_CONSTRUCTOR")
        void addedConstructorDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public MockSuperClass() {}\n"
                    + "    public MockSuperClass(String name) {}\n"
                    + "    public void superMethod() {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("added_ctor", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_CONSTRUCTOR);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }
    }

    // =========================================================================
    // 6. Field-Level Modifications
    // =========================================================================

    @Nested
    @DisplayName("Field-Level Mutations")
    class FieldMutationTests {

        @Test
        @DisplayName("Missing field triggers MISSING_FIELD")
        void missingFieldDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public volatile transient Object volatileTransientField;",
                    ""));

            SignatureRecord live = compileAndGenerate("missing_field", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.MISSING_FIELD);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Field type changed triggers FIELD_TYPE_CHANGED")
        void fieldTypeChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public volatile transient Object volatileTransientField;",
                    "public volatile transient String volatileTransientField;"));

            SignatureRecord live = compileAndGenerate("field_type_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_TYPE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Field modifiers changed triggers FIELD_MODIFIERS_CHANGED")
        void fieldModifiersChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public volatile transient Object volatileTransientField;",
                    "public Object volatileTransientField;"));

            SignatureRecord live = compileAndGenerate("field_mods_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_MODIFIERS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Inlined int constant value changed triggers FIELD_CONSTANT_VALUE_CHANGED")
        void fieldConstantValueChangedInt() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public static final int INT_CONSTANT = 12345;",
                    "public static final int INT_CONSTANT = 99999;"));

            SignatureRecord live = compileAndGenerate("field_int_const", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_CONSTANT_VALUE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Inlined String constant value changed triggers FIELD_CONSTANT_VALUE_CHANGED")
        void fieldConstantValueChangedString() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public static final String STR_CONSTANT = \"compileTimeConstant\";",
                    "public static final String STR_CONSTANT = \"differentStringConstant\";"));

            SignatureRecord live = compileAndGenerate("field_str_const", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_CONSTANT_VALUE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Field generic signature changed triggers FIELD_GENERIC_SIGNATURE_CHANGED")
        void fieldGenericSignatureChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public List<String> genericListField;",
                    "public List<Integer> genericListField;"));

            SignatureRecord live = compileAndGenerate("field_gen_sig", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_GENERIC_SIGNATURE_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Field annotation added or changed triggers FIELD_ANNOTATIONS_CHANGED")
        void fieldAnnotationChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace(
                    "public List<String> genericListField;",
                    "@MockAnnotation(mandatoryVal = 55)\n    public List<String> genericListField;"));

            SignatureRecord live = compileAndGenerate("field_anno_changed", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.FIELD_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockClass", matches.get(0).className);
        }

        @Test
        @DisplayName("Added field triggers ADDED_FIELD")
        void addedFieldDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java",
                    "package com.example.mockapi;\n"
                    + "public class MockSuperClass {\n"
                    + "    public int extraField = 42;\n"
                    + "    public void superMethod() {}\n"
                    + "}\n");

            SignatureRecord live = compileAndGenerate("added_field", sources);
            List<Violation> violations = SignatureDiff.diff(baselineRecord, live);

            List<Violation> matches = findViolations(violations, Violation.Kind.ADDED_FIELD);
            assertEquals(1, matches.size());
            assertEquals("com.example.mockapi.MockSuperClass", matches.get(0).className);
        }
    }
}
