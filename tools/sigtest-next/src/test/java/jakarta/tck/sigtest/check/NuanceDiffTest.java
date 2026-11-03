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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jakarta.tck.sigtest.check.Violation.Kind.*;
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
        return compileAndGenerate(subdir, sources, 11, Set.of(PKG), Set.of());
    }

    private SignatureRecord compileAndGenerate(String subdir, Map<String, String> sources, int release,
                                               Set<String> rootPackages, Set<String> excludePackages) throws Exception {
        Path dir = compile(subdir, sources, release);
        SignatureGenerator gen = new SignatureGenerator(List.of(dir.toFile()), rootPackages, excludePackages);
        return gen.generate("1.0");
    }

    /**
     * Compiles the given sources (keys are file names relative to the {@code com/example/mockapi}
     * source directory and may contain sub-directories) and returns the output directory.
     * Each call starts from a clean directory so repeated use of the same name is safe.
     */
    private Path compile(String subdir, Map<String, String> sources, int release) throws Exception {
        Path dir = tempDir.resolve(subdir);
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(dir);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available");

        Path srcDir = dir.resolve("src").resolve("com").resolve("example").resolve("mockapi");
        Files.createDirectories(srcDir);

        List<String> args = new ArrayList<>(List.of(
                "-d", dir.toString(),
                "-cp", dir.toString(),
                "--release", Integer.toString(release)
        ));

        for (Map.Entry<String, String> entry : sources.entrySet()) {
            Path srcFile = srcDir.resolve(entry.getKey()).normalize();
            Files.createDirectories(srcFile.getParent());
            Files.writeString(srcFile, entry.getValue());
            args.add(srcFile.toString());
        }

        int rc = compiler.run(null, null, null, args.toArray(String[]::new));
        assertEquals(0, rc, "Compilation failed for " + subdir);
        return dir;
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

    // =========================================================================
    // Extended helpers
    // =========================================================================

    private static String pkgSrc(String body) {
        return "package " + PKG + ";\n" + body + "\n";
    }

    /** Builds a compilation unit: {@code header { members }}. */
    private static String cls(String header, String members) {
        return pkgSrc(header + " {\n" + members + "\n}");
    }

    private static Map<String, String> files(String... nameAndBody) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < nameAndBody.length; i += 2) {
            map.put(nameAndBody[i], nameAndBody[i + 1]);
        }
        return map;
    }

    private List<Violation> diffPair(String before, String after) throws Exception {
        return diffPair(files("Subject.java", before), files("Subject.java", after), 11);
    }

    private List<Violation> diffPair(Map<String, String> before, Map<String, String> after) throws Exception {
        return diffPair(before, after, 11);
    }

    private List<Violation> diffPair(Map<String, String> before, Map<String, String> after, int release) throws Exception {
        SignatureRecord b = compileAndGenerate("pair_before", before, release, Set.of(PKG), Set.of());
        SignatureRecord a = compileAndGenerate("pair_after", after, release, Set.of(PKG), Set.of());
        return SignatureDiff.diff(b, a);
    }

    private List<Violation> diffBaseline(String subdir, Map<String, String> sources) throws Exception {
        return SignatureDiff.diff(baselineRecord, compileAndGenerate(subdir, sources));
    }

    /** Asserts that the violations are exactly the expected kinds (order-insensitive, multiplicity-sensitive). */
    private static void assertKinds(List<Violation> actual, Violation.Kind... expected) {
        List<Violation.Kind> a = actual.stream().map(v -> v.kind).sorted().collect(Collectors.toList());
        List<Violation.Kind> e = Arrays.stream(expected).sorted().collect(Collectors.toList());
        assertEquals(e, a, "Unexpected violations: " + actual);
    }

    private static ClassRecord classNamed(SignatureRecord record, String name) {
        return record.classes.stream().filter(c -> c.name.equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("Class not found in record: " + name));
    }

    private static Set<String> methodNames(ClassRecord c) {
        return c.methods == null ? Set.of()
                : c.methods.stream().map(m -> m.name).collect(Collectors.toSet());
    }

    private static Arguments tc(String id, String before, String after, Violation.Kind... kinds) {
        return Arguments.of(id, before, after, kinds);
    }

    // -------------------------------------------------------------------------
    // Parameterised case providers (referenced by fully-qualified name)
    // -------------------------------------------------------------------------

    static Stream<Arguments> typeCases() {
        return Stream.of(
                tc("class becomes abstract",
                        cls("public class Subject", ""), cls("public abstract class Subject", ""),
                        CLASS_MODIFIERS_CHANGED),
                tc("class loses final",
                        cls("public final class Subject", ""), cls("public class Subject", ""),
                        CLASS_MODIFIERS_CHANGED),
                tc("type parameter added",
                        cls("public class Subject<A>", ""), cls("public class Subject<A, B>", ""),
                        CLASS_GENERIC_SIGNATURE_CHANGED),
                tc("type parameter bound added",
                        cls("public class Subject<A>", ""), cls("public class Subject<A extends Number>", ""),
                        CLASS_GENERIC_SIGNATURE_CHANGED),
                tc("implemented interface type argument changed",
                        cls("public abstract class Subject implements java.util.Comparator<String>", ""),
                        cls("public abstract class Subject implements java.util.Comparator<Integer>", ""),
                        CLASS_GENERIC_SIGNATURE_CHANGED),
                tc("superclass type argument changed",
                        cls("public class Subject extends java.util.ArrayList<String>", ""),
                        cls("public class Subject extends java.util.ArrayList<Integer>", ""),
                        CLASS_GENERIC_SIGNATURE_CHANGED),
                tc("superclass replaced by another class",
                        cls("public class Subject extends java.util.ArrayList<String>", ""),
                        cls("public class Subject extends java.util.LinkedList<String>", ""),
                        SUPERCLASS_CHANGED, CLASS_GENERIC_SIGNATURE_CHANGED),
                tc("interface gains super-interface",
                        cls("public interface Subject", ""),
                        cls("public interface Subject extends java.io.Serializable", ""),
                        ADDED_INTERFACE),
                tc("interface loses super-interface",
                        cls("public interface Subject extends java.io.Serializable", ""),
                        cls("public interface Subject", ""),
                        MISSING_INTERFACE),
                tc("@FunctionalInterface added",
                        cls("public interface Subject", "void m();"),
                        cls("@FunctionalInterface public interface Subject", "void m();"),
                        CLASS_ANNOTATIONS_CHANGED),
                tc("annotation added to class without annotations",
                        cls("public class Subject", ""), cls("@Deprecated public class Subject", ""),
                        CLASS_ANNOTATIONS_CHANGED),
                tc("annotation removed from class",
                        cls("@Deprecated public class Subject", ""), cls("public class Subject", ""),
                        CLASS_ANNOTATIONS_CHANGED),
                tc("annotation order on class is irrelevant",
                        cls("@Deprecated @FunctionalInterface public interface Subject", "void m();"),
                        cls("@FunctionalInterface @Deprecated public interface Subject", "void m();")),
                tc("member declaration order is irrelevant",
                        cls("public class Subject",
                                "public int x; public int y; public void a() {} public void b() {} "
                                        + "public Subject() {} public Subject(int i) {}"),
                        cls("public class Subject",
                                "public Subject(int i) {} public Subject() {} public void b() {} "
                                        + "public void a() {} public int y; public int x;"))
        );
    }

    static Stream<Arguments> methodCases() {
        String c = "public class Subject";
        String i = "public interface Subject";
        return Stream.of(
                tc("static added", cls(c, "public void m() {}"), cls(c, "public static void m() {}"),
                        METHOD_MODIFIERS_CHANGED),
                tc("synchronized added", cls(c, "public void m() {}"), cls(c, "public synchronized void m() {}"),
                        METHOD_MODIFIERS_CHANGED),
                tc("native added", cls(c, "public void m() {}"), cls(c, "public native void m();"),
                        METHOD_MODIFIERS_CHANGED),
                tc("strictfp added", cls(c, "public void m() {}"), cls(c, "public strictfp void m() {}"),
                        METHOD_MODIFIERS_CHANGED),
                tc("method becomes abstract",
                        cls("public abstract class Subject", "public void m() {}"),
                        cls("public abstract class Subject", "public abstract void m();"),
                        METHOD_MODIFIERS_CHANGED),
                tc("final removed", cls(c, "public final void m() {}"), cls(c, "public void m() {}"),
                        METHOD_MODIFIERS_CHANGED),
                tc("public narrowed to protected", cls(c, "public void m() {}"), cls(c, "protected void m() {}"),
                        METHOD_MODIFIERS_CHANGED),
                tc("public narrowed to package-private", cls(c, "public void m() {}"), cls(c, "void m() {}"),
                        MISSING_METHOD),
                tc("package-private widened to public", cls(c, "void m() {}"), cls(c, "public void m() {}"),
                        ADDED_METHOD),
                tc("private and package-private members added are ignored",
                        cls(c, "public void m() {}"),
                        cls(c, "public void m() {} private void p() {} void q() {} private int pf; int qf;")),
                tc("interface abstract method becomes default",
                        cls(i, "void m();"), cls(i, "default void m() {}"), METHOD_MODIFIERS_CHANGED),
                tc("interface static method becomes default",
                        cls(i, "static void m() {}"), cls(i, "default void m() {}"), METHOD_MODIFIERS_CHANGED),
                tc("interface private method added is ignored",
                        cls(i, "void m();"), cls(i, "void m(); private void p() {}")),
                tc("checked exception removed",
                        cls(c, "public void m() throws java.io.IOException {}"), cls(c, "public void m() {}"),
                        METHOD_EXCEPTIONS_CHANGED),
                tc("throws clause order is significant (reordering is reported)",
                        cls(c, "public void m() throws java.io.IOException, InterruptedException {}"),
                        cls(c, "public void m() throws InterruptedException, java.io.IOException {}"),
                        METHOD_EXCEPTIONS_CHANGED),
                tc("exception narrowed",
                        cls(c, "public void m() throws java.io.IOException {}"),
                        cls(c, "public void m() throws java.io.FileNotFoundException {}"),
                        METHOD_EXCEPTIONS_CHANGED),
                tc("exception widened",
                        cls(c, "public void m() throws java.io.IOException {}"),
                        cls(c, "public void m() throws Exception {}"),
                        METHOD_EXCEPTIONS_CHANGED),
                tc("generic return type argument changed",
                        cls(c, "public java.util.List<String> m() { return null; }"),
                        cls(c, "public java.util.List<Integer> m() { return null; }"),
                        METHOD_GENERIC_SIGNATURE_CHANGED),
                tc("generic parameter type argument changed",
                        cls(c, "public void m(java.util.List<String> a) {}"),
                        cls(c, "public void m(java.util.List<Integer> a) {}"),
                        METHOD_GENERIC_SIGNATURE_CHANGED),
                tc("type-variable bound changes erased return type",
                        cls(c, "public <T> T m() { return null; }"),
                        cls(c, "public <T extends Number> T m() { return null; }"),
                        METHOD_RETURN_TYPE_CHANGED, METHOD_GENERIC_SIGNATURE_CHANGED),
                tc("annotation added to method",
                        cls(c, "public void m() {}"), cls(c, "@Deprecated public void m() {}"),
                        METHOD_ANNOTATIONS_CHANGED),
                tc("annotation removed from method",
                        cls(c, "@Deprecated public void m() {}"), cls(c, "public void m() {}"),
                        METHOD_ANNOTATIONS_CHANGED),
                tc("annotation element value changed on method",
                        cls(c, "@Deprecated(since = \"1\") public void m() {}"),
                        cls(c, "@Deprecated(since = \"2\") public void m() {}"),
                        METHOD_ANNOTATIONS_CHANGED),
                tc("one overload replaced",
                        cls(c, "public void m(int a) {} public void m(String a) {} public void m(long a) {}"),
                        cls(c, "public void m(int a) {} public void m(Object a) {} public void m(long a) {}"),
                        MISSING_METHOD, ADDED_METHOD),
                tc("parameter annotation added",
                        cls(c, "public void m(int a) {}"), cls(c, "public void m(@Deprecated int a) {}"),
                        PARAMETER_ANNOTATIONS_CHANGED),
                tc("parameter annotation removed",
                        cls(c, "public void m(@Deprecated int a) {}"), cls(c, "public void m(int a) {}"),
                        PARAMETER_ANNOTATIONS_CHANGED),
                tc("parameter annotation moved to a different parameter",
                        cls(c, "public void m(int a, @Deprecated int b) {}"),
                        cls(c, "public void m(@Deprecated int a, int b) {}"),
                        PARAMETER_ANNOTATIONS_CHANGED, PARAMETER_ANNOTATIONS_CHANGED),
                tc("source-retention annotations are not recorded",
                        cls(c, "public int f; public void m(int a) {}"),
                        cls("@SuppressWarnings(\"all\") public class Subject",
                                "@SuppressWarnings(\"all\") public int f; "
                                        + "@SuppressWarnings(\"all\") public void m(@SuppressWarnings(\"all\") int a) {}"))
        );
    }

    static Stream<Arguments> constructorCases() {
        String c = "public class Subject";
        return Stream.of(
                tc("implicit ctor replaced by parameterised ctor",
                        cls(c, ""), cls(c, "public Subject(int x) {}"),
                        MISSING_CONSTRUCTOR, ADDED_CONSTRUCTOR),
                tc("implicit ctor replaced by private ctor",
                        cls(c, ""), cls(c, "private Subject() {}"), MISSING_CONSTRUCTOR),
                tc("implicit ctor is equivalent to explicit public no-arg ctor",
                        cls(c, ""), cls(c, "public Subject() {}")),
                tc("ctor public narrowed to protected",
                        cls(c, "public Subject() {}"), cls(c, "protected Subject() {}"),
                        CONSTRUCTOR_MODIFIERS_CHANGED),
                tc("ctor public narrowed to package-private",
                        cls(c, "public Subject() {}"), cls(c, "Subject() {}"), MISSING_CONSTRUCTOR),
                tc("ctor varargs changed to array",
                        cls(c, "public Subject(String... a) {}"), cls(c, "public Subject(String[] a) {}"),
                        CONSTRUCTOR_MODIFIERS_CHANGED),
                tc("ctor exception removed",
                        cls(c, "public Subject() throws java.io.IOException {}"), cls(c, "public Subject() {}"),
                        CONSTRUCTOR_EXCEPTIONS_CHANGED),
                tc("ctor annotation added",
                        cls(c, "public Subject() {}"), cls(c, "@Deprecated public Subject() {}"),
                        CONSTRUCTOR_ANNOTATIONS_CHANGED),
                tc("ctor generic parameter argument changed",
                        cls(c, "public Subject(java.util.List<String> a) {}"),
                        cls(c, "public Subject(java.util.List<Integer> a) {}"),
                        CONSTRUCTOR_GENERIC_SIGNATURE_CHANGED),
                tc("private and package-private ctors added are ignored",
                        cls(c, "public Subject() {}"),
                        cls(c, "public Subject() {} private Subject(int x) {} Subject(long x) {}")),
                tc("overloaded ctor added",
                        cls(c, "public Subject() {}"), cls(c, "public Subject() {} public Subject(int x) {}"),
                        ADDED_CONSTRUCTOR),
                tc("inner class ctor parameter removed",
                        cls(c, "public class Inner { public Inner(int a) {} }"),
                        cls(c, "public class Inner { }"),
                        MISSING_CONSTRUCTOR, ADDED_CONSTRUCTOR),
                tc("inner class ctor narrowed",
                        cls(c, "public class Inner { public Inner(int a) {} }"),
                        cls(c, "public class Inner { protected Inner(int a) {} }"),
                        CONSTRUCTOR_MODIFIERS_CHANGED),
                tc("inner class ctor second parameter annotated",
                        cls(c, "public class Inner { public Inner(int a, int b) {} }"),
                        cls(c, "public class Inner { public Inner(int a, @Deprecated int b) {} }"),
                        PARAMETER_ANNOTATIONS_CHANGED),
                tc("inner class ctor first parameter annotated",
                        cls(c, "public class Inner { public Inner(int a, int b) {} }"),
                        cls(c, "public class Inner { public Inner(@Deprecated int a, int b) {} }"),
                        PARAMETER_ANNOTATIONS_CHANGED)
        );
    }

    private static Arguments constantCase(String type, String before, String after) {
        String c = "public class Subject";
        return tc("constant of type " + type + " changed",
                cls(c, "public static final " + type + " X = " + before + ";"),
                cls(c, "public static final " + type + " X = " + after + ";"),
                FIELD_CONSTANT_VALUE_CHANGED);
    }

    static Stream<Arguments> fieldCases() {
        String c = "public class Subject";
        return Stream.of(
                constantCase("long", "100L", "200L"),
                constantCase("float", "1.5f", "2.5f"),
                constantCase("double", "1.5", "2.5"),
                constantCase("char", "'a'", "'b'"),
                constantCase("boolean", "true", "false"),
                constantCase("byte", "(byte) 1", "(byte) 2"),
                constantCase("short", "(short) 1", "(short) 2"),
                tc("constant becomes non-constant",
                        cls(c, "public static final int X = 1;"),
                        cls(c, "public static final int X = Integer.parseInt(\"1\");"),
                        FIELD_CONSTANT_VALUE_CHANGED),
                tc("non-constant becomes constant",
                        cls(c, "public static final int X = Integer.parseInt(\"1\");"),
                        cls(c, "public static final int X = 1;"),
                        FIELD_CONSTANT_VALUE_CHANGED),
                tc("static added to field", cls(c, "public int f;"), cls(c, "public static int f;"),
                        FIELD_MODIFIERS_CHANGED),
                tc("final removed from field",
                        cls(c, "public final Object f = new Object();"), cls(c, "public Object f = new Object();"),
                        FIELD_MODIFIERS_CHANGED),
                tc("field public narrowed to protected", cls(c, "public int f;"), cls(c, "protected int f;"),
                        FIELD_MODIFIERS_CHANGED),
                tc("field public narrowed to package-private", cls(c, "public int f;"), cls(c, "int f;"),
                        MISSING_FIELD),
                tc("field package-private widened to public", cls(c, "int f;"), cls(c, "public int f;"),
                        ADDED_FIELD),
                tc("volatile added to field", cls(c, "public int f;"), cls(c, "public volatile int f;"),
                        FIELD_MODIFIERS_CHANGED),
                tc("transient added to field", cls(c, "public int f;"), cls(c, "public transient int f;"),
                        FIELD_MODIFIERS_CHANGED),
                tc("private and package-private fields added are ignored",
                        cls(c, "public int f;"), cls(c, "public int f; private int p; int q;")),
                tc("field annotation added", cls(c, "public int f;"), cls(c, "@Deprecated public int f;"),
                        FIELD_ANNOTATIONS_CHANGED),
                tc("field annotation removed", cls(c, "@Deprecated public int f;"), cls(c, "public int f;"),
                        FIELD_ANNOTATIONS_CHANGED),
                tc("field annotation element value changed",
                        cls(c, "@Deprecated(since = \"1\") public int f;"),
                        cls(c, "@Deprecated(since = \"2\") public int f;"),
                        FIELD_ANNOTATIONS_CHANGED),
                tc("static non-constant initializer change is ignored",
                        cls(c, "public static String s = \"a\";"), cls(c, "public static String s = \"b\";"))
        );
    }

    static Stream<Arguments> nestedCases() {
        String c = "public class Subject";
        return Stream.of(
                tc("static nested class becomes inner class",
                        cls(c, "public static class N {}"), cls(c, "public class N {}"),
                        CLASS_MODIFIERS_CHANGED, MISSING_CONSTRUCTOR, ADDED_CONSTRUCTOR),
                tc("protected nested class widened to public",
                        cls(c, "protected static class N {}"), cls(c, "public static class N {}"),
                        CLASS_MODIFIERS_CHANGED, CONSTRUCTOR_MODIFIERS_CHANGED),
                tc("public nested class becomes package-private",
                        cls(c, "public static class N {}"), cls(c, "static class N {}"),
                        INNER_CLASSES_CHANGED, MISSING_CLASS),
                tc("nested class added",
                        cls(c, ""), cls(c, "public static class N {}"),
                        INNER_CLASSES_CHANGED, ADDED_CLASS),
                tc("nested class removed",
                        cls(c, "public static class N {}"), cls(c, ""),
                        INNER_CLASSES_CHANGED, MISSING_CLASS),
                tc("private and package-private nested classes added are ignored",
                        cls(c, ""), cls(c, "private static class P {} static class Q {}")),
                tc("doubly nested class removed",
                        cls(c, "public static class A { public static class B {} }"),
                        cls(c, "public static class A { }"),
                        INNER_CLASSES_CHANGED, MISSING_CLASS)
        );
    }

    static Stream<Arguments> annotationTypeCases() {
        String h = "public @interface Subject";
        String rt = "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) ";
        String cl = "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS) ";
        return Stream.of(
                tc("element default removed",
                        cls(h, "int a() default 1;"), cls(h, "int a();"), ANNOTATION_DEFAULT_VALUE_CHANGED),
                tc("element default added",
                        cls(h, "int a();"), cls(h, "int a() default 1;"), ANNOTATION_DEFAULT_VALUE_CHANGED),
                tc("element with default added",
                        cls(h, "int a() default 1;"), cls(h, "int a() default 1; int b() default 2;"),
                        ADDED_ABSTRACT_METHOD),
                tc("mandatory element added (not distinguished from defaulted)",
                        cls(h, "int a() default 1;"), cls(h, "int a() default 1; int b();"),
                        ADDED_ABSTRACT_METHOD),
                tc("element removed",
                        cls(h, "int a() default 1; int b() default 2;"), cls(h, "int a() default 1;"),
                        MISSING_METHOD),
                tc("element type changed",
                        cls(h, "int a() default 1;"), cls(h, "long a() default 1;"),
                        METHOD_RETURN_TYPE_CHANGED, ANNOTATION_DEFAULT_VALUE_CHANGED),
                tc("@Retention changed",
                        cls(rt + h, ""), cls(cl + h, ""), CLASS_ANNOTATIONS_CHANGED),
                tc("@Target widened",
                        cls("@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE) " + rt + h, ""),
                        cls("@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE, "
                                + "java.lang.annotation.ElementType.METHOD}) " + rt + h, ""),
                        CLASS_ANNOTATIONS_CHANGED),
                tc("@Documented added",
                        cls(rt + h, ""), cls(rt + "@java.lang.annotation.Documented " + h, ""),
                        CLASS_ANNOTATIONS_CHANGED)
        );
    }

    static Stream<Arguments> annotationDefaultCases() {
        return Stream.of(
                Arguments.of("boolean", "boolean boolVal() default true;", "boolean boolVal() default false;"),
                Arguments.of("byte", "byte byteVal() default (byte) 1;", "byte byteVal() default (byte) 9;"),
                Arguments.of("short", "short shortVal() default (short) 2;", "short shortVal() default (short) 9;"),
                Arguments.of("char", "char charVal() default 'A';", "char charVal() default 'B';"),
                Arguments.of("long", "long longVal() default 100L;", "long longVal() default 200L;"),
                Arguments.of("float", "float floatVal() default 3.14f;", "float floatVal() default 1.5f;"),
                Arguments.of("double", "double doubleVal() default 2.71828;", "double doubleVal() default 1.0;"),
                Arguments.of("String", "String strVal() default \"defaultStr\";", "String strVal() default \"other\";"),
                Arguments.of("Class", "Class<?> classVal() default String.class;",
                        "Class<?> classVal() default Integer.class;"),
                Arguments.of("enum", "MockEnum enumVal() default MockEnum.ALPHA;",
                        "MockEnum enumVal() default MockEnum.BETA;"),
                Arguments.of("nested annotation", "NestedAnnotation nestedVal() default @NestedAnnotation(\"nested\");",
                        "NestedAnnotation nestedVal() default @NestedAnnotation(\"changed\");"),
                Arguments.of("array", "String[] arrayVal() default {\"item1\", \"item2\"};",
                        "String[] arrayVal() default {\"item1\"};")
        );
    }

    // =========================================================================
    // 7. Parameterised mutation matrices
    // =========================================================================

    @Nested
    @DisplayName("Type-Level Matrix")
    class TypeMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#typeCases")
        void typeMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }
    }

    @Nested
    @DisplayName("Method-Level Matrix")
    class MethodMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#methodCases")
        void methodMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }
    }

    @Nested
    @DisplayName("Constructor-Level Matrix")
    class ConstructorMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#constructorCases")
        void constructorMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }

        @Test
        @DisplayName("Annotating the second declared parameter of an inner class ctor reports it as parameter 3 (outer instance is parameter 1)")
        void innerCtorParameterIndexIncludesOuterInstance() throws Exception {
            List<Violation> violations = diffPair(
                    cls("public class Subject", "public class Inner { public Inner(int a, int b) {} }"),
                    cls("public class Subject", "public class Inner { public Inner(int a, @Deprecated int b) {} }"));
            List<Violation> matches = findViolations(violations, PARAMETER_ANNOTATIONS_CHANGED);
            assertEquals(1, matches.size());
            assertEquals(PKG + ".Subject$Inner", matches.get(0).className);
            assertTrue(matches.get(0).message.contains("parameter 3"), matches.get(0).message);
        }
    }

    @Nested
    @DisplayName("Field-Level Matrix")
    class FieldMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#fieldCases")
        void fieldMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }
    }

    @Nested
    @DisplayName("Nested-Type Matrix")
    class NestedMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#nestedCases")
        void nestedTypeMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }
    }

    @Nested
    @DisplayName("Annotation-Type Matrix")
    class AnnotationTypeMatrixTests {
        @ParameterizedTest(name = "{0}")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#annotationTypeCases")
        void annotationTypeMutation(String id, String before, String after, Violation.Kind[] expected) throws Exception {
            assertKinds(diffPair(before, after), expected);
        }

        @ParameterizedTest(name = "default value of {0} element changed")
        @MethodSource("jakarta.tck.sigtest.check.NuanceDiffTest#annotationDefaultCases")
        void defaultValueOfEachElementTypeChanged(String type, String from, String to) throws Exception {
            assertTrue(MOCK_ANNO_SRC.contains(from), "Fixture does not contain: " + from);
            Map<String, String> sources = createBaselineSources();
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC.replace(from, to));

            assertKinds(diffBaseline("anno_default_" + type.replace(' ', '_'), sources),
                    ANNOTATION_DEFAULT_VALUE_CHANGED);
        }

        @Test
        @DisplayName("Annotation element removed from MockAnnotation triggers MISSING_METHOD only")
        void elementRemovedFromBaselineAnnotation() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC.replace("    int intVal() default 42;\n", ""));

            assertKinds(diffBaseline("anno_elem_removed", sources), MISSING_METHOD);
        }

        @Test
        @DisplayName("Changing @Retention of MockAnnotation only affects the annotation type, not its usages")
        void retentionChangeDoesNotAffectUsages() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC.replace(
                    "@Retention(RetentionPolicy.RUNTIME)\n@Target", "@Retention(RetentionPolicy.CLASS)\n@Target"));

            List<Violation> violations = diffBaseline("anno_retention", sources);
            assertKinds(violations, CLASS_ANNOTATIONS_CHANGED);
            assertEquals(PKG + ".MockAnnotation", violations.get(0).className);
        }

        @Test
        @DisplayName("Widening @Target of MockAnnotation triggers CLASS_ANNOTATIONS_CHANGED only")
        void targetChangeOnBaselineAnnotation() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC.replace(
                    "ElementType.PACKAGE\n", "ElementType.PACKAGE,\n    ElementType.ANNOTATION_TYPE\n"));

            assertKinds(diffBaseline("anno_target", sources), CLASS_ANNOTATIONS_CHANGED);
        }
    }

    // =========================================================================
    // 8. Enums, records and sealed types
    // =========================================================================

    @Nested
    @DisplayName("Enum Mutations")
    class EnumMutationTests {

        @Test
        @DisplayName("Baseline enum exposes constants, values(), valueOf(), and no constructors")
        void baselineEnumShape() {
            ClassRecord e = classNamed(baselineRecord, PKG + ".MockEnum");
            assertEquals("enum", e.kind);

            Set<String> fieldNames = e.fields.stream().map(f -> f.name).collect(Collectors.toSet());
            assertTrue(fieldNames.containsAll(Set.of("ALPHA", "BETA", "ENUM_CONSTANT")), fieldNames.toString());

            Set<String> methods = methodNames(e);
            assertTrue(methods.containsAll(Set.of("values", "valueOf", "enumMethod")), methods.toString());

            assertTrue(e.constructors == null || e.constructors.isEmpty());
            assertTrue(e.hiddenFields != null && e.hiddenFields.contains("$VALUES"));
        }

        @Test
        @DisplayName("Enum constant removed triggers MISSING_FIELD")
        void enumConstantRemoved() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace("ALPHA,\n    BETA;", "ALPHA;"));

            assertKinds(diffBaseline("enum_removed", sources), MISSING_FIELD);
        }

        @Test
        @DisplayName("Enum constant added triggers ADDED_FIELD")
        void enumConstantAdded() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace("BETA;", "BETA,\n    GAMMA;"));

            assertKinds(diffBaseline("enum_added", sources), ADDED_FIELD);
        }

        @Test
        @DisplayName("Reordering enum constants (ordinal change) is NOT detected - known limitation")
        void enumConstantReorderNotDetected() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace("ALPHA,\n    BETA;", "BETA,\n    ALPHA;"));

            assertKinds(diffBaseline("enum_reordered", sources));
        }

        @Test
        @DisplayName("Adding a private enum constructor is ignored")
        void privateEnumConstructorIgnored() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace(
                    "public void enumMethod() {}", "private MockEnum() {}\n    public void enumMethod() {}"));

            assertKinds(diffBaseline("enum_private_ctor", sources));
        }

        @Test
        @DisplayName("Enum method removed triggers MISSING_METHOD")
        void enumMethodRemoved() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace("    public void enumMethod() {}\n", ""));

            assertKinds(diffBaseline("enum_method_removed", sources), MISSING_METHOD);
        }

        @Test
        @DisplayName("Enum constant string field value changed triggers FIELD_CONSTANT_VALUE_CHANGED")
        void enumStringConstantChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockEnum.java", MOCK_ENUM_SRC.replace("\"ENUM_VAL\"", "\"OTHER\""));

            assertKinds(diffBaseline("enum_const_changed", sources), FIELD_CONSTANT_VALUE_CHANGED);
        }

        @Test
        @DisplayName("Interface constant value changed triggers FIELD_CONSTANT_VALUE_CHANGED")
        void interfaceConstantChanged() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockInterface.java", MOCK_INTERFACE_SRC.replace("\"CONST_VAL\"", "\"OTHER\""));

            assertKinds(diffBaseline("iface_const_changed", sources), FIELD_CONSTANT_VALUE_CHANGED);
        }
    }

    @Nested
    @EnabledForJreRange(min = JRE.JAVA_17)
    @DisplayName("Records and Sealed Types (requires JDK 17+)")
    class RecordAndSealedTests {

        private static final int RELEASE = 17;
        private static final String POINT = "public record Point(int x, int y) {}";

        private Map<String, String> point(String decl) {
            return files("Point.java", pkgSrc(decl));
        }

        @Test
        @DisplayName("Record kind, java.lang.Record superclass, canonical ctor and accessors are captured")
        void recordShapeCaptured() throws Exception {
            SignatureRecord record = compileAndGenerate("rec_shape", point(POINT), RELEASE, Set.of(PKG), Set.of());
            ClassRecord p = classNamed(record, PKG + ".Point");

            assertEquals("record", p.kind);
            assertEquals("java.lang.Record", p.superClass);
            assertEquals(1, p.constructors.size());
            assertEquals(2, p.constructors.get(0).parameters.size());
            assertTrue(methodNames(p).containsAll(Set.of("x", "y", "equals", "hashCode", "toString")),
                    methodNames(p).toString());
            assertTrue(p.hiddenFields.containsAll(List.of("x", "y")), p.hiddenFields.toString());
        }

        @Test
        @DisplayName("Record self-diff is clean")
        void recordSelfDiff() throws Exception {
            assertKinds(diffPair(point(POINT), point(POINT), RELEASE));
        }

        @Test
        @DisplayName("Record component removed triggers MISSING_METHOD and constructor replacement")
        void recordComponentRemoved() throws Exception {
            List<Violation> violations = diffPair(point(POINT), point("public record Point(int x) {}"), RELEASE);
            assertKinds(violations, MISSING_METHOD, MISSING_CONSTRUCTOR, ADDED_CONSTRUCTOR);
        }

        @Test
        @DisplayName("Record component type changed triggers return type change and constructor replacement")
        void recordComponentTypeChanged() throws Exception {
            List<Violation> violations = diffPair(point(POINT), point("public record Point(int x, long y) {}"), RELEASE);
            assertKinds(violations, METHOD_RETURN_TYPE_CHANGED, MISSING_CONSTRUCTOR, ADDED_CONSTRUCTOR);
        }

        @Test
        @DisplayName("Record converted to class triggers CLASS_KIND_CHANGED")
        void recordToClass() throws Exception {
            List<Violation> violations = diffPair(point("public record Point(int x) {}"),
                    point("public final class Point { public int x() { return 0; } }"), RELEASE);
            assertFalse(findViolations(violations, CLASS_KIND_CHANGED).isEmpty(), violations.toString());
            assertFalse(findViolations(violations, SUPERCLASS_CHANGED).isEmpty(), violations.toString());
        }

        private final String circle = pkgSrc("public final class Circle implements Shape {}");
        private final String square = pkgSrc("public final class Square implements Shape {}");

        @Test
        @DisplayName("Sealed type records its permitted subclasses; non-sealed types record none")
        void sealedTypeRecordsPermittedSubclasses() throws Exception {
            SignatureRecord record = compileAndGenerate("sealed_shape", files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Square, Circle {}"),
                    "Circle.java", circle, "Square.java", square), RELEASE, Set.of(PKG), Set.of());

            assertEquals(List.of(PKG + ".Circle", PKG + ".Square"),
                    classNamed(record, PKG + ".Shape").permittedSubclasses);
            assertEquals(null, classNamed(record, PKG + ".Circle").permittedSubclasses);
        }

        @Test
        @DisplayName("Implicit permits (subclasses in the same compilation unit) are recorded")
        void implicitPermitsRecorded() throws Exception {
            SignatureRecord record = compileAndGenerate("sealed_implicit", files(
                    "Shape.java", pkgSrc("public sealed interface Shape {}\nfinal class Hidden implements Shape {}")),
                    RELEASE, Set.of(PKG), Set.of());

            assertEquals(List.of(PKG + ".Hidden"), classNamed(record, PKG + ".Shape").permittedSubclasses);
        }

        @Test
        @DisplayName("Sealed self-diff is clean and permits order is irrelevant")
        void sealedSelfDiffAndOrder() throws Exception {
            Map<String, String> a = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle, Square {}"),
                    "Circle.java", circle, "Square.java", square);
            Map<String, String> b = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Square, Circle {}"),
                    "Circle.java", circle, "Square.java", square);

            assertKinds(diffPair(a, a, RELEASE));
            assertKinds(diffPair(a, b, RELEASE));
        }

        @Test
        @DisplayName("Sealed to non-sealed triggers PERMITTED_SUBCLASSES_CHANGED")
        void sealedToNonSealed() throws Exception {
            Map<String, String> before = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle {}"),
                    "Circle.java", circle);
            Map<String, String> after = files(
                    "Shape.java", pkgSrc("public interface Shape {}"),
                    "Circle.java", circle);

            List<Violation> violations = diffPair(before, after, RELEASE);
            assertKinds(violations, PERMITTED_SUBCLASSES_CHANGED);
            assertEquals(PKG + ".Shape", violations.get(0).className);
            assertTrue(violations.get(0).message.contains("no longer sealed"), violations.get(0).message);
        }

        @Test
        @DisplayName("Non-sealed to sealed triggers PERMITTED_SUBCLASSES_CHANGED")
        void nonSealedToSealed() throws Exception {
            Map<String, String> before = files(
                    "Shape.java", pkgSrc("public interface Shape {}"),
                    "Circle.java", circle);
            Map<String, String> after = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle {}"),
                    "Circle.java", circle);

            List<Violation> violations = diffPair(before, after, RELEASE);
            assertKinds(violations, PERMITTED_SUBCLASSES_CHANGED);
            assertTrue(violations.get(0).message.contains("now sealed"), violations.get(0).message);
        }

        @Test
        @DisplayName("Permitted subclass added triggers PERMITTED_SUBCLASSES_CHANGED")
        void permittedSubclassAdded() throws Exception {
            Map<String, String> before = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle {}"),
                    "Circle.java", circle);
            Map<String, String> after = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle, Square {}"),
                    "Circle.java", circle, "Square.java", square);

            assertKinds(diffPair(before, after, RELEASE), PERMITTED_SUBCLASSES_CHANGED, ADDED_CLASS);
        }

        @Test
        @DisplayName("Permitted subclass removed triggers PERMITTED_SUBCLASSES_CHANGED")
        void permittedSubclassRemoved() throws Exception {
            Map<String, String> before = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle, Square {}"),
                    "Circle.java", circle, "Square.java", square);
            Map<String, String> after = files(
                    "Shape.java", pkgSrc("public sealed interface Shape permits Circle {}"),
                    "Circle.java", circle);

            assertKinds(diffPair(before, after, RELEASE), PERMITTED_SUBCLASSES_CHANGED, MISSING_CLASS);
        }

        @Test
        @DisplayName("Sealed class made final triggers PERMITTED_SUBCLASSES_CHANGED and CLASS_MODIFIERS_CHANGED")
        void sealedClassMadeFinal() throws Exception {
            Map<String, String> before = files(
                    "Base.java", pkgSrc("public sealed class Base permits Leaf {}"),
                    "Leaf.java", pkgSrc("public final class Leaf extends Base {}"));
            Map<String, String> after = files(
                    "Base.java", pkgSrc("public final class Base {}"),
                    "Leaf.java", pkgSrc("public final class Leaf {}"));

            assertKinds(diffPair(before, after, RELEASE),
                    PERMITTED_SUBCLASSES_CHANGED, CLASS_MODIFIERS_CHANGED, SUPERCLASS_CHANGED);
        }

    }

    // =========================================================================
    // 9. Hierarchy behaviour
    // =========================================================================

    @Nested
    @DisplayName("Hierarchy & Bridge Behaviour")
    class HierarchyTests {

        @Test
        @DisplayName("Method moved to superclass is reported as MISSING_METHOD + ADDED_METHOD (declared members only)")
        void methodMovedToSuperclass() throws Exception {
            Map<String, String> before = files(
                    "Base.java", pkgSrc("public class Base {}"),
                    "Sub.java", pkgSrc("public class Sub extends Base { public void m() {} }"));
            Map<String, String> after = files(
                    "Base.java", pkgSrc("public class Base { public void m() {} }"),
                    "Sub.java", pkgSrc("public class Sub extends Base {}"));

            List<Violation> violations = diffPair(before, after);
            assertKinds(violations, MISSING_METHOD, ADDED_METHOD);
            assertEquals(PKG + ".Sub", findViolations(violations, MISSING_METHOD).get(0).className);
            assertEquals(PKG + ".Base", findViolations(violations, ADDED_METHOD).get(0).className);
        }

        @Test
        @DisplayName("Covariant return override reports only METHOD_RETURN_TYPE_CHANGED (bridge method ignored)")
        void covariantReturnOverride() throws Exception {
            Map<String, String> before = files(
                    "Base.java", pkgSrc("public class Base { public Object m() { return null; } }"),
                    "Sub.java", pkgSrc("public class Sub extends Base { public Object m() { return null; } }"));
            Map<String, String> after = files(
                    "Base.java", pkgSrc("public class Base { public Object m() { return null; } }"),
                    "Sub.java", pkgSrc("public class Sub extends Base { public String m() { return null; } }"));

            assertKinds(diffPair(before, after), METHOD_RETURN_TYPE_CHANGED);
        }

        @Test
        @DisplayName("Generic bridge methods are not recorded")
        void bridgeMethodsNotRecorded() throws Exception {
            SignatureRecord record = compileAndGenerate("bridge", files(
                    "Box.java", pkgSrc("public class Box implements Comparable<Box> {"
                            + " public int compareTo(Box o) { return 0; } }")), 11, Set.of(PKG), Set.of());
            ClassRecord box = classNamed(record, PKG + ".Box");

            assertEquals(1, box.methods.size(), box.methods.toString());
            assertEquals("compareTo", box.methods.get(0).name);
            assertEquals(PKG + ".Box", box.methods.get(0).parameters.get(0).type);
        }

        @Test
        @DisplayName("Annotation with CLASS retention on a method is tracked (documented behaviour)")
        void classRetentionAnnotationTracked() throws Exception {
            String anno = pkgSrc("import java.lang.annotation.*;\n"
                    + "@Retention(RetentionPolicy.CLASS) @Target(ElementType.METHOD) public @interface ClassRet {}");
            Map<String, String> before = files("ClassRet.java", anno,
                    "Subject.java", cls("public class Subject", "public void m() {}"));
            Map<String, String> after = files("ClassRet.java", anno,
                    "Subject.java", cls("public class Subject", "@ClassRet public void m() {}"));

            assertKinds(diffPair(before, after), METHOD_ANNOTATIONS_CHANGED);
        }
    }

    // =========================================================================
    // 10. Package filtering and generator input handling
    // =========================================================================

    @Nested
    @DisplayName("Package & Generator Input Handling")
    class PackageAndGeneratorTests {

        @Test
        @DisplayName("package-info.java deleted entirely triggers PACKAGE_ANNOTATIONS_CHANGED")
        void packageInfoDeleted() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.remove("package-info.java");

            assertKinds(diffBaseline("pkg_info_deleted", sources), PACKAGE_ANNOTATIONS_CHANGED);
        }

        @Test
        @DisplayName("Annotated package-info added to a package without one triggers PACKAGE_ANNOTATIONS_CHANGED")
        void packageInfoAdded() throws Exception {
            Map<String, String> before = files("Subject.java", cls("public class Subject", ""));
            Map<String, String> after = files("Subject.java", cls("public class Subject", ""),
                    "package-info.java", "@Deprecated\npackage " + PKG + ";\n");

            assertKinds(diffPair(before, after), PACKAGE_ANNOTATIONS_CHANGED);
        }

        @Test
        @DisplayName("New annotated sub-package is included via root-package prefix")
        void newAnnotatedSubPackage() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("sub/package-info.java", "@Deprecated\npackage " + PKG + ".sub;\n");

            assertKinds(diffBaseline("pkg_sub_annotated", sources), PACKAGE_ANNOTATIONS_CHANGED);
        }

        @Test
        @DisplayName("Class added in sub-package triggers ADDED_CLASS")
        void classInSubPackageIncluded() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("sub/Extra.java", "package " + PKG + ".sub;\npublic class Extra {}\n");

            List<Violation> violations = diffBaseline("pkg_sub_class", sources);
            assertKinds(violations, ADDED_CLASS);
            assertEquals(PKG + ".sub.Extra", violations.get(0).className);
        }

        @Test
        @DisplayName("excludePackages removes a sub-package from the signature")
        void excludedSubPackageIgnored() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("sub/Extra.java", "package " + PKG + ".sub;\npublic class Extra {}\n");

            SignatureRecord live = compileAndGenerate("pkg_excluded", sources, 11, Set.of(PKG), Set.of(PKG + ".sub"));
            assertTrue(live.classes.stream().noneMatch(c -> c.name.startsWith(PKG + ".sub")));
            assertKinds(SignatureDiff.diff(baselineRecord, live));
        }

        @Test
        @DisplayName("Root package matching is by package boundary, not string prefix")
        void siblingPackageWithSharedPrefixIgnored() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("../mockapifake/Fake.java", "package com.example.mockapifake;\npublic class Fake {}\n");

            SignatureRecord live = compileAndGenerate("pkg_sibling", sources);
            assertTrue(live.classes.stream().noneMatch(c -> c.name.contains("mockapifake")));
            assertKinds(SignatureDiff.diff(baselineRecord, live));
        }

        @Test
        @DisplayName("Package-private top-level classes are not part of the API")
        void packagePrivateTopLevelClassIgnored() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("Hidden.java", pkgSrc("class Hidden { public void m() {} }"));

            SignatureRecord live = compileAndGenerate("hidden_class", sources);
            assertTrue(live.classes.stream().noneMatch(c -> c.name.endsWith(".Hidden")));
            assertKinds(SignatureDiff.diff(baselineRecord, live));
        }

        @Test
        @DisplayName("Anonymous and local classes are not recorded")
        void anonymousAndLocalClassesIgnored() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("Anon.java", pkgSrc("public class Anon {\n"
                    + "    public Runnable r() {\n"
                    + "        class Local implements Runnable { public void run() {} }\n"
                    + "        return new Runnable() { public void run() {} };\n"
                    + "    }\n"
                    + "}"));

            SignatureRecord live = compileAndGenerate("anon_local", sources);
            List<String> anonRelated = live.classes.stream().map(c -> c.name)
                    .filter(n -> n.contains("Anon")).collect(Collectors.toList());
            assertEquals(List.of(PKG + ".Anon"), anonRelated);
            assertTrue(classNamed(live, PKG + ".Anon").innerClasses == null
                    || classNamed(live, PKG + ".Anon").innerClasses.isEmpty());
            assertKinds(SignatureDiff.diff(baselineRecord, live), ADDED_CLASS);
        }

        @Test
        @DisplayName("Non-API fields are tracked as hiddenFields but never produce violations")
        void hiddenFieldsTrackedButNotDiffed() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockSuperClass.java", pkgSrc(
                    "public class MockSuperClass { private int secret; int pkgField; public void superMethod() {} }"));

            SignatureRecord live = compileAndGenerate("hidden_fields", sources);
            ClassRecord sup = classNamed(live, PKG + ".MockSuperClass");
            assertTrue(sup.hiddenFields.containsAll(List.of("secret", "pkgField")), sup.hiddenFields.toString());
            assertKinds(SignatureDiff.diff(baselineRecord, live));
        }

        @Test
        @DisplayName("Signature survives a JSON write/read round-trip with zero diff in both directions")
        void jsonRoundTrip() throws Exception {
            Path dir = compile("roundtrip", createBaselineSources(), 11);
            Path out = tempDir.resolve("out").resolve("mockapi.sig.json");
            SignatureRecord generated = new SignatureGenerator(List.of(dir.toFile()), Set.of(PKG), Set.of())
                    .generate("1.0", out);

            SignatureRecord loaded;
            try (Jsonb jsonb = JsonbBuilder.create(); Reader reader = Files.newBufferedReader(out)) {
                loaded = jsonb.fromJson(reader, SignatureRecord.class);
            }

            assertKinds(SignatureDiff.diff(generated, loaded));
            assertKinds(SignatureDiff.diff(loaded, generated));
            assertKinds(SignatureDiff.diff(baselineRecord, loaded));
        }
    }

    // =========================================================================
    // 11. Cross-cutting scenarios
    // =========================================================================

    @Nested
    @DisplayName("Cross-Cutting Scenarios")
    class CrossCuttingTests {

        @Test
        @DisplayName("Several simultaneous changes in one class are all reported, and nothing else")
        void multipleSimultaneousChanges() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC
                    .replace("public class MockClass<", "public final class MockClass<")
                    .replace("    public static final int INT_CONSTANT = 12345;\n", "")
                    .replace("public static String nonConstStatic = \"dynamic\";",
                            "public static Object nonConstStatic = \"dynamic\";")
                    .replace("public static class StaticNestedClass",
                            "public void brandNew() {}\n    public static class StaticNestedClass"));

            assertKinds(diffBaseline("multi_change", sources),
                    CLASS_MODIFIERS_CHANGED, MISSING_FIELD, FIELD_TYPE_CHANGED, ADDED_METHOD);
        }

        @Test
        @DisplayName("Several classes removed are each reported once")
        void multipleClassesRemoved() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.remove("SecondInterface.java");
            sources.put("MockSuperClass.java", pkgSrc("public class MockSuperClass {}"));
            sources.remove("MockEnum.java");
            sources.put("MockAnnotation.java", MOCK_ANNO_SRC
                    .replace("    MockEnum enumVal() default MockEnum.ALPHA;\n", ""));

            List<Violation> violations = diffBaseline("multi_missing", sources);
            assertEquals(2, findViolations(violations, MISSING_CLASS).size(), violations.toString());
        }

        @Test
        @DisplayName("Reverse diff swaps MISSING_* and ADDED_* kinds")
        void reverseDiffIsSymmetric() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.remove("SecondInterface.java");
            sources.put("MockSuperClass.java", pkgSrc("public class MockSuperClass { public void superMethod() {}"
                    + " public void extra() {} }"));
            SignatureRecord live = compileAndGenerate("reverse", sources);

            assertKinds(SignatureDiff.diff(baselineRecord, live), MISSING_CLASS, ADDED_METHOD);
            assertKinds(SignatureDiff.diff(live, baselineRecord), ADDED_CLASS, MISSING_METHOD);
        }

        @Test
        @DisplayName("Self-diff of a mutated record is also clean")
        void mutatedRecordSelfDiffClean() throws Exception {
            Map<String, String> sources = createBaselineSources();
            sources.put("MockClass.java", MOCK_CLASS_SRC.replace("public class MockClass<", "public final class MockClass<"));
            SignatureRecord live = compileAndGenerate("mutated_self", sources);

            assertKinds(SignatureDiff.diff(live, live));
        }
    }

}
