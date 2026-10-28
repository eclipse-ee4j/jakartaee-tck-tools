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
import jakarta.tck.sigtest.model.FieldRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.ParameterRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SignatureDiff}.
 *
 * <p>Each test compiles two variants of a small Java snippet — a "baseline"
 * and a "live" version — into separate temp directories, generates a
 * {@link SignatureRecord} for each, then asserts that
 * {@link SignatureDiff#diff} reports (or does not report) the expected
 * violations.
 */
class SignatureDiffTest {

    // @TempDir(cleanup = CleanupMode.NEVER)
    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper — compile source into a sub-directory, return a SignatureRecord
    // -------------------------------------------------------------------------

    private SignatureRecord generate(String subdir, String source) throws Exception {
        Path dir = tempDir.resolve(subdir);
        Files.createDirectories(dir);
        compile(dir, source);
        String pkg = extractPackageName(source);
        Set<String> packages = pkg.isEmpty() ? Set.of() : Set.of(pkg);
        SignatureGenerator gen = new SignatureGenerator(List.of(dir.toFile()), packages, Set.of());
        return gen.generate("1.0");
    }

    private void compile(Path outDir, String source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available");

        String className = extractPublicClassName(source);
        String pkg = extractPackageName(source);

        Path srcDir = outDir.resolve("src");
        if (!pkg.isEmpty()) {
            srcDir = srcDir.resolve(pkg.replace('.', File.separatorChar));
        }
        Files.createDirectories(srcDir);
        Path srcFile = srcDir.resolve(className + ".java");
        Files.writeString(srcFile, source);

        int rc = compiler.run(null, null, null,
                "-d", outDir.toString(),
                "-cp", outDir.toString(),
                "--release", "11",
                srcFile.toString());
        assertTrue(rc == 0, "Compilation failed: " + srcFile);
    }

    // -------------------------------------------------------------------------
    // Identical API → no violations
    // -------------------------------------------------------------------------

    @Test
    void identicalApiProducesNoViolations() throws Exception {
        String src = "package com.example;\n"
                   + "public class Service {\n"
                   + "    public String greet(String name) { return name; }\n"
                   + "    public static final int VERSION = 1;\n"
                   + "}\n";

        SignatureRecord baseline = generate("base", src);
        SignatureRecord live     = generate("live", src);

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertTrue(violations.isEmpty(), "Identical APIs should produce no violations: " + violations);
    }

    // -------------------------------------------------------------------------
    // Class-level violations
    // -------------------------------------------------------------------------

    @Test
    void missingClassIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\npublic class Gone {}\n");

        // Live is an empty record (no classes in that package).
        SignatureRecord live = new SignatureRecord();
        live.classes = List.of();

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_CLASS, "com.example.Gone");
    }

    @Test
    void addedClassIsReported() throws Exception {
        SignatureRecord baseline = new SignatureRecord();
        baseline.classes = List.of();

        SignatureRecord live = generate("live",
                "package com.example;\npublic class NewClass {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.ADDED_CLASS, "com.example.NewClass");
    }

    @Test
    void classKindChangedIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\npublic class Widget {}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic interface Widget {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.CLASS_KIND_CHANGED, "com.example.Widget");
    }

    @Test
    void classModifiersChangedIsReported() throws Exception {
        // abstract added
        SignatureRecord baseline = generate("base",
                "package com.example;\npublic class Widget {}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic abstract class Widget {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.CLASS_MODIFIERS_CHANGED, "com.example.Widget");
    }

    @Test
    void superclassChangedIsReported() throws Exception {
        compile(tempDir.resolve("base"), "package com.example;\npublic class Animal {}\n");
        compile(tempDir.resolve("base"), "package com.example;\npublic class Dog extends Animal {}\n");
        compile(tempDir.resolve("live"), "package com.example;\npublic class Cat {}\n");
        compile(tempDir.resolve("live"), "package com.example;\npublic class Dog extends Cat {}\n");

        SignatureGenerator baseGen = new SignatureGenerator(
                List.of(tempDir.resolve("base").toFile()), Set.of("com.example"), Set.of());
        SignatureGenerator liveGen = new SignatureGenerator(
                List.of(tempDir.resolve("live").toFile()), Set.of("com.example"), Set.of());

        List<Violation> violations = SignatureDiff.diff(baseGen.generate("1.0"), liveGen.generate("1.0"));
        assertHas(violations, Violation.Kind.SUPERCLASS_CHANGED, "com.example.Dog");
    }

    @Test
    void missingInterfaceIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\nimport java.io.Serializable;\n"
                + "public class Token implements Serializable {\n"
                + "    private static final long serialVersionUID = 1L;\n}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic class Token {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_INTERFACE, "com.example.Token");
    }

    // -------------------------------------------------------------------------
    // Method violations
    // -------------------------------------------------------------------------

    @Test
    void missingMethodIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Service {\n"
                + "    public void execute() {}\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic class Service {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_METHOD, "com.example.Service");
    }

    @Test
    void methodReturnTypeChangedIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Converter {\n"
                + "    public String convert(int v) { return \"\"; }\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Converter {\n"
                + "    public int convert(int v) { return v; }\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.METHOD_RETURN_TYPE_CHANGED, "com.example.Converter");
    }

    @Test
    void methodModifiersChangedIsReported() throws Exception {
        // final added to a method
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Service {\n"
                + "    public void execute() {}\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Service {\n"
                + "    public final void execute() {}\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.METHOD_MODIFIERS_CHANGED, "com.example.Service");
    }

    @Test
    void methodExceptionsChangedIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Service {\n"
                + "    public void execute() throws java.io.IOException {}\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Service {\n"
                + "    public void execute() {}\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.METHOD_EXCEPTIONS_CHANGED, "com.example.Service");
    }

    // -------------------------------------------------------------------------
    // Constructor violations
    // -------------------------------------------------------------------------

    @Test
    void missingConstructorIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Widget {\n"
                + "    public Widget(String name) {}\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic class Widget {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_CONSTRUCTOR, "com.example.Widget");
    }

    @Test
    void constructorModifiersChangedIsReported() throws Exception {
        // We change the constructor from public to protected.
        // ASM records the modifiers string, so we build the records manually
        // to avoid fighting javac access rules.
        SignatureRecord baseline = recordWith(classWithCtor("public"));
        SignatureRecord live     = recordWith(classWithCtor("protected"));

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.CONSTRUCTOR_MODIFIERS_CHANGED, "com.example.Widget");
    }

    // -------------------------------------------------------------------------
    // Field violations
    // -------------------------------------------------------------------------

    @Test
    void missingFieldIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static final String API_VERSION = \"1.0\";\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\npublic class Constants {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_FIELD, "com.example.Constants");
    }

    @Test
    void fieldTypeChangedIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static int VERSION = 1;\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static String VERSION = \"1\";\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.FIELD_TYPE_CHANGED, "com.example.Constants");
    }

    @Test
    void fieldConstantValueChangedIsReported() throws Exception {
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static final int VERSION = 1;\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static final int VERSION = 2;\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.FIELD_CONSTANT_VALUE_CHANGED, "com.example.Constants");
    }

    @Test
    void fieldModifiersChangedIsReported() throws Exception {
        // final removed
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static final int MAX = 100;\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class Constants {\n"
                + "    public static int MAX = 100;\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.FIELD_MODIFIERS_CHANGED, "com.example.Constants");
    }

    // -------------------------------------------------------------------------
    // Generics violations
    // -------------------------------------------------------------------------

    @Test
    void methodGenericSignatureChangedIsReported() throws Exception {
        // Baseline: get() returns T (generic); live: get() returns Object (raw).
        // After erasure both descriptors are "()" → "Ljava/lang/Object;" so the
        // generic signature is the only artefact that captures the change.
        compile(tempDir.resolve("base"),
                "package com.example;\n"
                + "public class Box<T> {\n"
                + "    private T value;\n"
                + "    public T get() { return value; }\n"
                + "}\n");
        compile(tempDir.resolve("live"),
                "package com.example;\n"
                + "public class Box {\n"
                + "    private Object value;\n"
                + "    public Object get() { return value; }\n"
                + "}\n");

        SignatureGenerator baseGen = new SignatureGenerator(
                List.of(tempDir.resolve("base").toFile()), Set.of("com.example"), Set.of());
        SignatureGenerator liveGen = new SignatureGenerator(
                List.of(tempDir.resolve("live").toFile()), Set.of("com.example"), Set.of());

        List<Violation> violations = SignatureDiff.diff(baseGen.generate("1.0"), liveGen.generate("1.0"));
        assertHas(violations, Violation.Kind.METHOD_GENERIC_SIGNATURE_CHANGED, "com.example.Box");
    }

    @Test
    void fieldGenericSignatureChangedIsReported() throws Exception {
        // Baseline: items : List<T>; live: items : List (raw).
        compile(tempDir.resolve("base"),
                "package com.example;\n"
                + "import java.util.List;\n"
                + "public class Box<T> {\n"
                + "    public List<T> items;\n"
                + "}\n");
        compile(tempDir.resolve("live"),
                "package com.example;\n"
                + "import java.util.List;\n"
                + "public class Box {\n"
                + "    public List items;\n"
                + "}\n");

        SignatureGenerator baseGen = new SignatureGenerator(
                List.of(tempDir.resolve("base").toFile()), Set.of("com.example"), Set.of());
        SignatureGenerator liveGen = new SignatureGenerator(
                List.of(tempDir.resolve("live").toFile()), Set.of("com.example"), Set.of());

        List<Violation> violations = SignatureDiff.diff(baseGen.generate("1.0"), liveGen.generate("1.0"));
        assertHas(violations, Violation.Kind.FIELD_GENERIC_SIGNATURE_CHANGED, "com.example.Box");
    }

    // -------------------------------------------------------------------------
    // Annotation-related behaviour
    // -------------------------------------------------------------------------

    @Test
    void annotationChangesAreNotCheckedByDiff() throws Exception {
        // SignatureDiff does not currently enforce annotation contracts —
        // it only captures annotations for informational purposes.
        // This test documents that an annotation being removed does NOT
        // produce a violation so that any future implementation of annotation
        // checking will know to add a new Violation.Kind and update this test.
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "@Deprecated\n"
                + "public class OldApi {}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public class OldApi {}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        // No violation expected — annotation enforcement is not yet implemented.
        assertTrue(violations.isEmpty(),
                "Annotation removal should not yet produce a violation: " + violations);
    }

    // -------------------------------------------------------------------------
    // Nested / inner class violations
    // -------------------------------------------------------------------------

    @Test
    void missingPublicNestedClassIsReported() throws Exception {
        // The generator records nested public static classes as top-level entries
        // in the SignatureRecord (each gets its own ClassRecord). Removing one
        // is therefore a MISSING_CLASS violation.
        compile(tempDir.resolve("base"),
                "package com.example;\n"
                + "public class Outer {\n"
                + "    public static class Inner {}\n"
                + "}\n");
        compile(tempDir.resolve("live"),
                "package com.example;\n"
                + "public class Outer {}\n");

        SignatureGenerator baseGen = new SignatureGenerator(
                List.of(tempDir.resolve("base").toFile()), Set.of("com.example"), Set.of());
        SignatureGenerator liveGen = new SignatureGenerator(
                List.of(tempDir.resolve("live").toFile()), Set.of("com.example"), Set.of());

        List<Violation> violations = SignatureDiff.diff(baseGen.generate("1.0"), liveGen.generate("1.0"));
        assertHas(violations, Violation.Kind.MISSING_CLASS, "com.example.Outer$Inner");
    }

    @Test
    void addedPublicNestedClassIsReported() throws Exception {
        compile(tempDir.resolve("base"),
                "package com.example;\n"
                + "public class Outer {}\n");
        compile(tempDir.resolve("live"),
                "package com.example;\n"
                + "public class Outer {\n"
                + "    public static class Inner {}\n"
                + "}\n");

        SignatureGenerator baseGen = new SignatureGenerator(
                List.of(tempDir.resolve("base").toFile()), Set.of("com.example"), Set.of());
        SignatureGenerator liveGen = new SignatureGenerator(
                List.of(tempDir.resolve("live").toFile()), Set.of("com.example"), Set.of());

        List<Violation> violations = SignatureDiff.diff(baseGen.generate("1.0"), liveGen.generate("1.0"));
        assertHas(violations, Violation.Kind.ADDED_CLASS, "com.example.Outer$Inner");
    }

    // -------------------------------------------------------------------------
    // Enum violations
    // -------------------------------------------------------------------------

    @Test
    void missingEnumConstantFieldIsReported() throws Exception {
        // Enum constants are recorded as public static final fields of the enum
        // type. Removing a constant is therefore a MISSING_FIELD violation.
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public enum Status { ACTIVE, INACTIVE, PENDING }\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public enum Status { ACTIVE, INACTIVE }\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_FIELD, "com.example.Status");
    }

    // -------------------------------------------------------------------------
    // Default interface method violations
    // -------------------------------------------------------------------------

    @Test
    void missingDefaultMethodIsReported() throws Exception {
        // Removing a default method is a binary incompatibility for callers
        // that override it: the override becomes invalid.
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public interface Greeter {\n"
                + "    String greet(String name);\n"
                + "    default void sayHi() {}\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public interface Greeter {\n"
                + "    String greet(String name);\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        assertHas(violations, Violation.Kind.MISSING_METHOD, "com.example.Greeter");
    }

    @Test
    void addedDefaultMethodToInterfaceIsReported() throws Exception {
        // A newly added method (default or abstract) on an interface is an
        // ADDED_CLASS-style informational entry only when it concerns a whole class;
        // for methods the diff reports nothing about additions — only removals.
        // This test documents that behaviour: adding a default method does NOT
        // produce a violation in the current implementation.
        SignatureRecord baseline = generate("base",
                "package com.example;\n"
                + "public interface Greeter {\n"
                + "    String greet(String name);\n"
                + "}\n");
        SignatureRecord live = generate("live",
                "package com.example;\n"
                + "public interface Greeter {\n"
                + "    String greet(String name);\n"
                + "    default void sayHi() {}\n"
                + "}\n");

        List<Violation> violations = SignatureDiff.diff(baseline, live);
        // Method additions are not flagged — only removals are incompatible in
        // binary-check mode. A strictcheck mode would catch this.
        assertNone(violations, Violation.Kind.MISSING_METHOD);
        assertTrue(violations.isEmpty(),
                "Adding a default method should not produce a violation in binary mode: " + violations);
    }

    // -------------------------------------------------------------------------
    // Method key helper tests
    // -------------------------------------------------------------------------

    @Test
    void methodKeyIncludesNameAndParameters() {
        MethodRecord m = new MethodRecord();
        m.name = "doWork";
        m.parameters = List.of(new ParameterRecord("java.lang.String"), new ParameterRecord("int"));
        assertEquals("doWork(java.lang.String,int)", SignatureDiff.methodKey(m));
    }

    @Test
    void methodKeyHandlesNullParameters() {
        MethodRecord m = new MethodRecord();
        m.name = "noArgs";
        m.parameters = null;
        assertEquals("noArgs()", SignatureDiff.methodKey(m));
    }

    // -------------------------------------------------------------------------
    // Assertion helpers
    // -------------------------------------------------------------------------

    private static void assertHas(List<Violation> violations, Violation.Kind kind, String className) {
        Optional<Violation> found = violations.stream()
                .filter(v -> v.kind == kind && v.className.equals(className))
                .findFirst();
        assertTrue(found.isPresent(),
                "Expected " + kind + " for " + className + " but got: " + violations);
    }

    private static void assertNone(List<Violation> violations, Violation.Kind kind) {
        assertFalse(violations.stream().anyMatch(v -> v.kind == kind),
                "Did not expect any " + kind + " violation but got: " + violations);
    }

    // -------------------------------------------------------------------------
    // Manual record builders (for cases where javac access rules get in the way)
    // -------------------------------------------------------------------------

    private static ClassRecord classWithCtor(String modifiers) {
        MethodRecord ctor = new MethodRecord();
        ctor.name = "Widget";
        ctor.isConstructor = true;
        ctor.modifiers = modifiers;
        ctor.parameters = List.of(new ParameterRecord("java.lang.String"));

        ClassRecord cls = new ClassRecord();
        cls.name = "com.example.Widget";
        cls.kind = "class";
        cls.modifiers = "public";
        cls.constructors = List.of(ctor);
        return cls;
    }

    private static SignatureRecord recordWith(ClassRecord cls) {
        SignatureRecord r = new SignatureRecord();
        r.classes = List.of(cls);
        return r;
    }

    // -------------------------------------------------------------------------
    // Source parsing helpers (duplicated from SignatureGeneratorTest for isolation)
    // -------------------------------------------------------------------------

    private static String extractPublicClassName(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("public ")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    String kw = parts[i];
                    if (kw.equals("class") || kw.equals("interface")
                            || kw.equals("enum") || kw.equals("record")
                            || kw.equals("@interface")) {
                        return parts[i + 1].replaceAll("[{(<].*", "").trim();
                    }
                }
            }
        }
        throw new IllegalArgumentException("Cannot determine public class name from source");
    }

    private static String extractPackageName(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring("package ".length()).replace(";", "").trim();
            }
        }
        return "";
    }
}
