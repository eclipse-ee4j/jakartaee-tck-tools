/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.generate;

import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.FieldRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.PackageRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SignatureGenerator}.
 *
 * <p>Each test compiles a small Java source snippet into a temporary directory,
 * then runs the generator and asserts properties of the resulting
 * {@link SignatureRecord}.
 */
class SignatureGeneratorTest {

    // @TempDir(cleanup = CleanupMode.NEVER)
    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SignatureRecord generate(String source, Set<String> packages) throws Exception {
        return generate(source, packages, Set.of());
    }

    private SignatureRecord generate(String source, Set<String> packages, Set<String> excludes) throws Exception {
        compile(source);
        SignatureGenerator gen = new SignatureGenerator(List.of(tempDir.toFile()), packages, excludes);
        Path output = tempDir.resolve("signature.sig.json");
        SignatureRecord record = gen.generate("1.0", output);
        return record;
    }

    /** Compiles Java source text into {@link #tempDir}. */
    private void compile(String source) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler not available — run tests with a JDK");

        String className = extractPublicClassName(source);
        String pkg = extractPackageName(source);
        Path srcDir = tempDir.resolve("src");
        if (!pkg.isEmpty()) {
            srcDir = srcDir.resolve(pkg.replace('.', File.separatorChar));
        }
        Files.createDirectories(srcDir);
        Path srcFile = srcDir.resolve(className + ".java");
        Files.writeString(srcFile, source);

        int rc = compiler.run(null, null, null,
                "-d", tempDir.toString(),
                "-cp", tempDir.toString(),
                "--release", "11",
                srcFile.toString());
        assertTrue(rc == 0, "Compilation failed for: " + srcFile);
    }

    private static String extractPublicClassName(String source) {
        for (String line : source.split("\n")) {
            line = line.trim();
            if (line.startsWith("public ")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length - 1; i++) {
                    String kw = parts[i];
                    if (kw.equals("class") || kw.equals("interface")
                            || kw.equals("enum") || kw.equals("record")) {
                        return parts[i + 1].replaceAll("[{(<].*", "").trim();
                    }
                    if (kw.equals("@interface")) {
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

    private static Optional<ClassRecord> findClass(SignatureRecord record, String name) {
        return record.classes.stream().filter(c -> c.name.equals(name)).findFirst();
    }

    private static Optional<MethodRecord> findMethod(ClassRecord cls, String name) {
        if (cls.methods == null) return Optional.empty();
        return cls.methods.stream().filter(m -> m.name.equals(name)).findFirst();
    }

    private static List<String> paramTypes(MethodRecord m) {
        return m.parameters.stream().map(p -> p.type).collect(java.util.stream.Collectors.toList());
    }

    private static Optional<FieldRecord> findField(ClassRecord cls, String name) {
        if (cls.fields == null) return Optional.empty();
        return cls.fields.stream().filter(f -> f.name.equals(name)).findFirst();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void publicClassIsIncluded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Hello {}\n",
                Set.of("com.example"));

        Optional<ClassRecord> cls = findClass(record, "com.example.Hello");
        assertTrue(cls.isPresent(), "Expected com.example.Hello in record");
        assertEquals("class", cls.get().kind);
        assertTrue(cls.get().modifiers.contains("public"));
    }

    @Test
    void packagePrivateClassIsExcluded() throws Exception {
        compileDirect(
                "package com.example;\n" +
                "class Internal {}\n",
                "Internal.java");
        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        assertTrue(record.classes.isEmpty(), "Package-private class should not appear in record");
    }

    @Test
    void classOutsideRequestedPackageIsExcluded() throws Exception {
        SignatureRecord record = generate(
                "package com.other;\n" +
                "public class Outside {}\n",
                Set.of("com.example"));

        assertTrue(record.classes.isEmpty(), "Class outside requested package should be excluded");
    }

    @Test
    void excludedSubPackageIsOmitted() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Hello {}\n",
                Set.of("com.example"),
                Set.of("com.example"));

        assertTrue(record.classes.isEmpty(), "Explicitly excluded package should produce no classes");
    }

    @Test
    void publicMethodsAreRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Service {\n" +
                "    public String greet(String name) { return name; }\n" +
                "    public void doWork() {}\n" +
                "    private void internal() {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Service").orElseThrow();
        assertNotNull(cls.methods, "Methods list should not be null");

        Optional<MethodRecord> greet = findMethod(cls, "greet");
        assertTrue(greet.isPresent());
        assertEquals("java.lang.String", greet.get().returnType);
        assertEquals(List.of("java.lang.String"), paramTypes(greet.get()));

        assertTrue(findMethod(cls, "doWork").isPresent());
        assertFalse(findMethod(cls, "internal").isPresent(), "Private method should not appear");
    }

    @Test
    void publicFieldsAreRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Constants {\n" +
                "    public static final String VERSION = \"1.0\";\n" +
                "    public int count;\n" +
                "    private String secret;\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Constants").orElseThrow();
        assertNotNull(cls.fields);

        FieldRecord version = findField(cls, "VERSION").orElseThrow();
        assertEquals("java.lang.String", version.type);
        assertTrue(version.modifiers.contains("static"));
        assertTrue(version.modifiers.contains("final"));
        assertEquals("1.0", version.constantValue);

        assertTrue(findField(cls, "count").isPresent());
        assertFalse(findField(cls, "secret").isPresent(), "Private field should not appear");
    }

    @Test
    void interfaceKindAndMethodsRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public interface Greeter {\n" +
                "    String greet(String name);\n" +
                "    default void sayHi() {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Greeter").orElseThrow();
        assertEquals("interface", cls.kind);
        assertTrue(findMethod(cls, "greet").isPresent());
        assertTrue(findMethod(cls, "sayHi").isPresent());
    }

    @Test
    void enumKindRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public enum Color { RED, GREEN, BLUE }\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Color").orElseThrow();
        assertEquals("enum", cls.kind);
    }

    @Test
    void objectSuperClassIsSuppressed() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Animal {}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Animal").orElseThrow();
        assertNull(cls.superClass, "java.lang.Object superclass should be suppressed");
    }

    @Test
    void explicitSuperClassIsRecorded() throws Exception {
        // Compile both classes into tempDir so javac can resolve the supertype.
        compileDirect(
                "package com.example;\n" +
                "public class Animal {}\n",
                "Animal.java");
        compileDirect(
                "package com.example;\n" +
                "public class Dog extends Animal {}\n",
                "Dog.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        ClassRecord dog = findClass(record, "com.example.Dog").orElseThrow();
        assertEquals("com.example.Animal", dog.superClass);
    }

    @Test
    void implementedInterfacesAreRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "import java.io.Serializable;\n" +
                "public class Token implements Serializable {\n" +
                "    private static final long serialVersionUID = 1L;\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Token").orElseThrow();
        assertNotNull(cls.interfaces);
        assertTrue(cls.interfaces.contains("java.io.Serializable"));
    }

    @Test
    void constructorIsRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Widget {\n" +
                "    public Widget(String name, int size) {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Widget").orElseThrow();
        assertNotNull(cls.constructors, "Constructors list should not be null");
        MethodRecord ctor = cls.constructors.get(0);
        assertTrue(ctor.isConstructor);
        assertEquals("Widget", ctor.name);
        assertEquals(List.of("java.lang.String", "int"), paramTypes(ctor));
        assertNull(ctor.returnType);
    }

    @Test
    void parameterAnnotationsAreRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Service {\n" +
                "    public void run(@Deprecated(since = \"1\") String a, int b, @Deprecated int c) {}\n" +
                "    public Service(int x, @Deprecated String y) {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.Service").orElseThrow();
        MethodRecord run = findMethod(cls, "run").orElseThrow();
        assertEquals(List.of("java.lang.String", "int", "int"), paramTypes(run));
        assertEquals(List.of("since=\"1\""), run.parameters.get(0).annotations.get("java.lang.Deprecated"));
        assertNull(run.parameters.get(1).annotations);
        assertEquals(List.of(), run.parameters.get(2).annotations.get("java.lang.Deprecated"));

        MethodRecord ctor = cls.constructors.get(0);
        assertNull(ctor.parameters.get(0).annotations);
        assertTrue(ctor.parameters.get(1).annotations.containsKey("java.lang.Deprecated"));
    }

    @Test
    void innerClassConstructorParameterAnnotationsAreAligned() throws Exception {
        // javac emits a synthetic outer-instance parameter that the annotation
        // attribute does not cover; indices must be shifted accordingly.
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Outer {\n" +
                "    public class Inner {\n" +
                "        public Inner(@Deprecated String s) {}\n" +
                "    }\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord inner = findClass(record, "com.example.Outer$Inner").orElseThrow();
        MethodRecord ctor = inner.constructors.get(0);
        assertEquals(List.of("com.example.Outer", "java.lang.String"), paramTypes(ctor));
        assertNull(ctor.parameters.get(0).annotations);
        assertTrue(ctor.parameters.get(1).annotations.containsKey("java.lang.Deprecated"));
    }

    @Test
    void annotationElementValuesAreTypedAndComplete() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "import java.lang.annotation.*;\n" +
                "public class Holder {\n" +
                "    public @interface Inner { String name() default \"\"; }\n" +
                "    @Repeatable(Many.class) @Retention(RetentionPolicy.RUNTIME)\n" +
                "    public @interface One { int n(); }\n" +
                "    @Retention(RetentionPolicy.RUNTIME)\n" +
                "    public @interface Many { One[] value(); }\n" +
                "    @Target({ElementType.METHOD, ElementType.FIELD})\n" +
                "    public @interface Marker {\n" +
                "        Class<?> type() default Object.class;\n" +
                "        Inner inner() default @Inner(name = \"x\");\n" +
                "        long l() default 1L;\n" +
                "        int i() default 1;\n" +
                "        String s() default \"1\";\n" +
                "        char c() default 'a';\n" +
                "        int[] nums() default {1, 2};\n" +
                "    }\n" +
                "    @Marker(type = String.class, inner = @Inner(name = \"y\"), l = 2L, i = 3, s = \"3\", c = 'b', nums = {4, 5})\n" +
                "    @Many({@One(n = 1), @One(n = 2)})\n" +
                "    public void annotated() {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord holder = findClass(record, "com.example.Holder").orElseThrow();
        MethodRecord m = findMethod(holder, "annotated").orElseThrow();
        var marker = m.annotations.get("com.example.Holder$Marker");
        assertEquals(List.of(
                "type=java.lang.String.class",
                "inner=@com.example.Holder$Inner(name=\"y\")",
                "l=2L",
                "i=3",
                "s=\"3\"",
                "c='b'",
                "nums=[4, 5]"), marker);

        assertEquals(List.of("@com.example.Holder$One(n=1)", "@com.example.Holder$One(n=2)"),
                m.annotations.get("com.example.Holder$Many"));

        ClassRecord markerCls = findClass(record, "com.example.Holder$Marker").orElseThrow();
        assertEquals(List.of("java.lang.annotation.ElementType.METHOD", "java.lang.annotation.ElementType.FIELD"),
                markerCls.annotations.get("java.lang.annotation.Target"));

        ClassRecord oneCls = findClass(record, "com.example.Holder$One").orElseThrow();
        assertEquals(List.of("com.example.Holder$Many.class"),
                oneCls.annotations.get("java.lang.annotation.Repeatable"));
        assertEquals(List.of("java.lang.annotation.RetentionPolicy.RUNTIME"),
                oneCls.annotations.get("java.lang.annotation.Retention"));
    }

    @Test
    void annotationElementDefaultsAreRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "import java.lang.annotation.*;\n" +
                "public class Holder {\n" +
                "    public @interface Inner { String name() default \"\"; }\n" +
                "    public @interface Cfg {\n" +
                "        boolean required();\n" +
                "        boolean ignoreCase() default false;\n" +
                "        String s() default \"x\";\n" +
                "        long l() default 2L;\n" +
                "        Class<?> type() default Object.class;\n" +
                "        RetentionPolicy policy() default RetentionPolicy.RUNTIME;\n" +
                "        Inner inner() default @Inner(name = \"y\");\n" +
                "        int[] nums() default {1, 2};\n" +
                "        String[] none() default {};\n" +
                "    }\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cfg = findClass(record, "com.example.Holder$Cfg").orElseThrow();
        assertNull(findMethod(cfg, "required").orElseThrow().defaultValue);
        assertEquals("false", findMethod(cfg, "ignoreCase").orElseThrow().defaultValue);
        assertEquals("\"x\"", findMethod(cfg, "s").orElseThrow().defaultValue);
        assertEquals("2L", findMethod(cfg, "l").orElseThrow().defaultValue);
        assertEquals("java.lang.Object.class", findMethod(cfg, "type").orElseThrow().defaultValue);
        assertEquals("java.lang.annotation.RetentionPolicy.RUNTIME",
                findMethod(cfg, "policy").orElseThrow().defaultValue);
        assertEquals("@com.example.Holder$Inner(name=\"y\")", findMethod(cfg, "inner").orElseThrow().defaultValue);
        assertEquals("[1, 2]", findMethod(cfg, "nums").orElseThrow().defaultValue);
        assertEquals("[]", findMethod(cfg, "none").orElseThrow().defaultValue);
    }

    @Test
    void annotationOnClassIsRecorded() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "@Deprecated\n" +
                "public class OldApi {}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.OldApi").orElseThrow();
        assertNotNull(cls.annotations, "Annotations list should not be null");
        assertTrue(cls.annotations.containsKey("java.lang.Deprecated"), "Expected @Deprecated on class");
    }

    @Test
    void formatVersionIsPresent() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class Dummy {}\n",
                Set.of("com.example"));

        assertEquals(SignatureRecord.FORMAT_VERSION, record.formatVersion);
        assertEquals("1.0", record.apiVersion);
    }

    @Test
    void classesAreSortedByName() throws Exception {
        compileDirect("package com.example;\npublic class Beta {}\n", "Beta.java");
        compileDirect("package com.example;\npublic class Alpha {}\n", "Alpha.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        List<String> names = record.classes.stream()
                .map(c -> c.name)
                .collect(java.util.stream.Collectors.toList());
        assertTrue(names.indexOf("com.example.Alpha") < names.indexOf("com.example.Beta"),
                "Classes should be sorted alphabetically");
    }

    // -------------------------------------------------------------------------
    // Generics tests
    // -------------------------------------------------------------------------

    @Test
    void genericClassAndRawClassHaveDifferentSignatures() throws Exception {
        // Box<T> and RawBox (no type param) must not serialize the same.
        compileDirect(
                "package com.example;\n" +
                "public class Box<T> {\n" +
                "    public T value;\n" +
                "}\n",
                "Box.java");
        compileDirect(
                "package com.example;\n" +
                "public class RawBox {\n" +
                "    public Object value;\n" +
                "}\n",
                "RawBox.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        ClassRecord box = findClass(record, "com.example.Box").orElseThrow();
        ClassRecord rawBox = findClass(record, "com.example.RawBox").orElseThrow();

        // The generic class must carry a class-level generic signature; raw does not.
        assertNotNull(box.genericSignature,
                "Box<T> must have a genericSignature recording its type parameter");
        assertNull(rawBox.genericSignature,
                "RawBox must have no genericSignature");
        assertNotEquals(box.genericSignature, rawBox.genericSignature);
    }

    @Test
    void genericFieldAndConcreteFieldHaveDifferentSignatures() throws Exception {
        // Box<T>.items : List<T>  vs  StringBox.items : List<String>
        // After erasure both descriptors are "Ljava/util/List;" — the generic
        // signature is the only thing that distinguishes them.
        compileDirect(
                "package com.example;\n" +
                "import java.util.List;\n" +
                "public class Box<T> {\n" +
                "    public List<T> items;\n" +
                "}\n",
                "Box.java");
        compileDirect(
                "package com.example;\n" +
                "import java.util.List;\n" +
                "public class StringBox {\n" +
                "    public List<String> items;\n" +
                "}\n",
                "StringBox.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        FieldRecord boxItems = findField(
                findClass(record, "com.example.Box").orElseThrow(), "items").orElseThrow();
        FieldRecord stringBoxItems = findField(
                findClass(record, "com.example.StringBox").orElseThrow(), "items").orElseThrow();

        // Erased type is the same — genericSignature must differ.
        assertEquals("java.util.List", boxItems.type);
        assertEquals("java.util.List", stringBoxItems.type);
        assertNotNull(boxItems.genericSignature,
                "List<T> field must carry a genericSignature");
        assertNotNull(stringBoxItems.genericSignature,
                "List<String> field must carry a genericSignature");
        assertNotEquals(boxItems.genericSignature, stringBoxItems.genericSignature,
                "List<T> and List<String> must have different genericSignatures");
    }

    @Test
    void genericMethodAndConcreteMethodHaveDifferentSignatures() throws Exception {
        // get() : T  vs  get() : String — erased return type differs, but
        // also the generic method signature must be preserved.
        compileDirect(
                "package com.example;\n" +
                "public class Box<T> {\n" +
                "    private T value;\n" +
                "    public T get() { return value; }\n" +
                "    public void set(T val) { this.value = val; }\n" +
                "}\n",
                "Box.java");
        compileDirect(
                "package com.example;\n" +
                "public class StringBox {\n" +
                "    private String value;\n" +
                "    public String get() { return value; }\n" +
                "    public void set(String val) { this.value = val; }\n" +
                "}\n",
                "StringBox.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        ClassRecord box = findClass(record, "com.example.Box").orElseThrow();
        ClassRecord stringBox = findClass(record, "com.example.StringBox").orElseThrow();

        MethodRecord boxGet = findMethod(box, "get").orElseThrow();
        MethodRecord stringBoxGet = findMethod(stringBox, "get").orElseThrow();

        // Erased return types differ already (Object vs String), but the
        // generic signature must also be recorded and differ.
        assertNotNull(boxGet.genericSignature,
                "Box.get() must carry a genericSignature");
        assertNull(stringBoxGet.genericSignature,
                "StringBox.get() has no generic signature");
        assertNotEquals(boxGet.genericSignature, stringBoxGet.genericSignature);

        MethodRecord boxSet = findMethod(box, "set").orElseThrow();
        MethodRecord stringBoxSet = findMethod(stringBox, "set").orElseThrow();

        assertNotNull(boxSet.genericSignature,
                "Box.set(T) must carry a genericSignature");
        assertNull(stringBoxSet.genericSignature,
                "StringBox.set(String) has no generic signature");
        assertNotEquals(boxSet.genericSignature, stringBoxSet.genericSignature);
    }

    @Test
    void methodWithTypeParameterDeclarationIsDistinct() throws Exception {
        // <R> R transform(Function<T,R> fn)  — the method itself declares a type param.
        compileDirect(
                "package com.example;\n" +
                "import java.util.function.Function;\n" +
                "public class Box<T> {\n" +
                "    private T value;\n" +
                "    public <R> R transform(Function<T, R> fn) { return fn.apply(value); }\n" +
                "    public Object transformRaw(Function<Object, Object> fn) { return fn.apply(value); }\n" +
                "}\n",
                "Box.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("signature.sig.json"));

        ClassRecord box = findClass(record, "com.example.Box").orElseThrow();

        MethodRecord transform = findMethod(box, "transform").orElseThrow();
        MethodRecord transformRaw = findMethod(box, "transformRaw").orElseThrow();

        assertNotNull(transform.genericSignature,
                "transform<R> must carry a genericSignature encoding <R>");
        assertNotNull(transformRaw.genericSignature,
                "transformRaw(Function<Object,Object>) still carries a generic signature for the parameterised arg");
        assertNotEquals(transform.genericSignature, transformRaw.genericSignature,
                "method-level type parameter <R> must make transform's signature distinct from transformRaw's");
        // transform's signature must contain the type-parameter declaration '<R:...>'
        assertTrue(transform.genericSignature.startsWith("<"),
                "transform<R> generic signature must open with a type-parameter declaration");
        assertFalse(transformRaw.genericSignature.startsWith("<"),
                "transformRaw has no method-level type parameter declaration");
    }

    // -------------------------------------------------------------------------
    // InnerClasses
    // -------------------------------------------------------------------------

    @Test
    void nestedStaticClassHasStaticModifier() throws Exception {
        // A static nested class must carry "static" in its modifiers.
        // The top-level class access_flags do NOT have ACC_STATIC; it is only
        // available in the InnerClasses attribute entry.
        compileDirect(
                "package com.example;\n" +
                "public class Outer {\n" +
                "    public static class Nested {}\n" +
                "}\n",
                "Outer.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord nested = findClass(record, "com.example.Outer$Nested").orElseThrow();
        assertTrue(nested.modifiers.contains("static"),
                "static nested class must have 'static' in modifiers; was: " + nested.modifiers);
        assertTrue(nested.modifiers.contains("public"),
                "public nested class must have 'public' in modifiers; was: " + nested.modifiers);
    }

    @Test
    void innerClassLacksStaticModifier() throws Exception {
        // A non-static inner class must NOT carry 'static' in its modifiers.
        compileDirect(
                "package com.example;\n" +
                "public class Outer {\n" +
                "    public class Inner {}\n" +
                "}\n",
                "Outer.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord inner = findClass(record, "com.example.Outer$Inner").orElseThrow();
        assertFalse(inner.modifiers.contains("static"),
                "non-static inner class must not have 'static' in modifiers; was: " + inner.modifiers);
    }

    @Test
    void outerClassRecordsPublicInnerClasses() throws Exception {
        // The outer class must list its public and protected member types in innerClasses.
        compileDirect(
                "package com.example;\n" +
                "public class Outer {\n" +
                "    public static class PublicNested {}\n" +
                "    protected static class ProtectedNested {}\n" +
                "    static class PackageNested {}\n" +
                "    private static class PrivateNested {}\n" +
                "}\n",
                "Outer.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord outer = findClass(record, "com.example.Outer").orElseThrow();
        assertNotNull(outer.innerClasses, "outer class must record its public/protected member types");
        assertTrue(outer.innerClasses.contains("PublicNested"),
                "public nested class must appear in innerClasses");
        assertTrue(outer.innerClasses.contains("ProtectedNested"),
                "protected nested class must appear in innerClasses");
        assertFalse(outer.innerClasses.contains("PackageNested"),
                "package-private nested class must not appear in innerClasses");
        assertFalse(outer.innerClasses.contains("PrivateNested"),
                "private nested class must not appear in innerClasses");
    }

    @Test
    void protectedNestedClassHasProtectedModifier() throws Exception {
        // A protected nested class must carry "protected" (not "public") in its modifiers.
        compileDirect(
                "package com.example;\n" +
                "public class Outer {\n" +
                "    protected static class Protected {}\n" +
                "}\n",
                "Outer.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord protectedNested = findClass(record, "com.example.Outer$Protected").orElseThrow();
        assertTrue(protectedNested.modifiers.contains("protected"),
                "protected nested class must have 'protected' in modifiers; was: " + protectedNested.modifiers);
        assertFalse(protectedNested.modifiers.contains("public"),
                "protected nested class must not have 'public' in modifiers; was: " + protectedNested.modifiers);
    }

    @Test
    void classModifiersDoNotIncludeSynchronized() throws Exception {
        compileDirect(
                "package com.example;\n" +
                "public class NormalClass {}\n",
                "NormalClass.java");
        compileDirect(
                "package com.example;\n" +
                "public final class FinalClass {}\n",
                "FinalClass.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord normal = findClass(record, "com.example.NormalClass").orElseThrow();
        assertEquals("public", normal.modifiers, "ACC_SUPER must not be interpreted as 'synchronized'");

        ClassRecord finalCls = findClass(record, "com.example.FinalClass").orElseThrow();
        assertEquals("public final", finalCls.modifiers, "ACC_SUPER must not be interpreted as 'synchronized'");
    }

    @Test
    void varargsMethodAndConstructorHaveVarargsModifier() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class VarargsClass {\n" +
                "    public VarargsClass(String... args) {}\n" +
                "    public static void by(int first, String... rest) {}\n" +
                "    public synchronized void syncMethod() {}\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.VarargsClass").orElseThrow();

        MethodRecord ctor = cls.constructors.get(0);
        assertEquals("public varargs", ctor.modifiers,
                "constructor with varargs must have 'varargs' modifier; was: " + ctor.modifiers);

        MethodRecord by = findMethod(cls, "by").orElseThrow();
        assertEquals("public static varargs", by.modifiers,
                "ACC_VARARGS must be rendered as 'varargs' (not 'transient'); was: " + by.modifiers);

        MethodRecord sync = findMethod(cls, "syncMethod").orElseThrow();
        assertEquals("public synchronized", sync.modifiers,
                "method with synchronized must have 'synchronized' modifier; was: " + sync.modifiers);
    }

    @Test
    void fieldModifiersCanBeTransientAndVolatile() throws Exception {
        SignatureRecord record = generate(
                "package com.example;\n" +
                "public class FieldModifiersClass {\n" +
                "    public transient int transientField;\n" +
                "    public volatile int volatileField;\n" +
                "}\n",
                Set.of("com.example"));

        ClassRecord cls = findClass(record, "com.example.FieldModifiersClass").orElseThrow();

        FieldRecord transientField = findField(cls, "transientField").orElseThrow();
        assertEquals("public transient", transientField.modifiers);

        FieldRecord volatileField = findField(cls, "volatileField").orElseThrow();
        assertEquals("public volatile", volatileField.modifiers);
    }

    @Test
    void modifierHelpersProduceContextSpecificStrings() {
        // Class modifier tests: ACC_SUPER (0x20) ignored, only class-valid flags emitted
        assertEquals("public", SignatureGenerator.modifiersOfClass(0x0021)); // ACC_PUBLIC | ACC_SUPER
        assertEquals("public final", SignatureGenerator.modifiersOfClass(0x0031)); // ACC_PUBLIC | ACC_FINAL | ACC_SUPER
        assertEquals("public abstract", SignatureGenerator.modifiersOfClass(0x0401)); // ACC_PUBLIC | ACC_ABSTRACT
        assertEquals("public static final", SignatureGenerator.modifiersOfClass(0x0019)); // ACC_PUBLIC | ACC_STATIC | ACC_FINAL

        // Field modifier tests: transient (0x80) and volatile (0x40) supported, no synchronized/native/etc.
        assertEquals("public transient", SignatureGenerator.modifiersOfField(0x0081)); // ACC_PUBLIC | ACC_TRANSIENT
        assertEquals("public volatile", SignatureGenerator.modifiersOfField(0x0041)); // ACC_PUBLIC | ACC_VOLATILE
        assertEquals("public static final", SignatureGenerator.modifiersOfField(0x0019)); // ACC_PUBLIC | ACC_STATIC | ACC_FINAL

        // Method modifier tests: varargs (0x80), bridge (0x40) ignored, synchronized (0x20), native, abstract, strictfp
        assertEquals("public static varargs", SignatureGenerator.modifiersOfMethod(0x0089)); // ACC_PUBLIC | ACC_STATIC | ACC_VARARGS
        assertEquals("public synchronized", SignatureGenerator.modifiersOfMethod(0x0021)); // ACC_PUBLIC | ACC_SYNCHRONIZED
        assertEquals("public native", SignatureGenerator.modifiersOfMethod(0x0101)); // ACC_PUBLIC | ACC_NATIVE
        assertEquals("public abstract", SignatureGenerator.modifiersOfMethod(0x0401)); // ACC_PUBLIC | ACC_ABSTRACT
        assertEquals("public", SignatureGenerator.modifiersOfMethod(0x0041)); // ACC_PUBLIC | ACC_BRIDGE (no volatile)
    }

    @Test
    void packageInfoAnnotationsAreRecorded() throws Exception {
        compileDirect(
                "@Deprecated(since = \"1.1\")\n" +
                "package com.example.annotated;\n",
                "package-info.java");
        compileDirect(
                "package com.example.annotated;\n" +
                "public class ServiceA {}\n",
                "ServiceA.java");
        // SOURCE-retention annotation that is legal on packages (@SuppressWarnings is not on JDK 17+).
        // Lives outside the scanned "com.example" root so it is not part of the signature.
        compileDirect(
                "package other.anno;\n" +
                "import java.lang.annotation.*;\n" +
                "@Retention(RetentionPolicy.SOURCE)\n" +
                "@Target(ElementType.PACKAGE)\n" +
                "public @interface SourceOnly {}\n",
                "SourceOnly.java");
        compileDirect(
                "@other.anno.SourceOnly\n" +
                "package com.example.plain;\n",
                "package-info.java");
        compileDirect(
                "package com.example.plain;\n" +
                "public class ServiceB {}\n",
                "ServiceB.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        assertNotNull(record.packages, "record.packages should not be null when package-info files are present");
        assertEquals(2, record.packages.size());

        PackageRecord annotatedPkg = record.packages.get(0);
        assertEquals("com.example.annotated", annotatedPkg.name);
        assertNotNull(annotatedPkg.annotations);
        assertEquals(List.of("since=\"1.1\""), annotatedPkg.annotations.get("java.lang.Deprecated"));

        PackageRecord plainPkg = record.packages.get(1);
        assertEquals("com.example.plain", plainPkg.name);
        assertNull(plainPkg.annotations, "Unannotated package should have null annotations");

        // package-info should not be emitted as a ClassRecord in classes
        assertFalse(record.classes.stream().anyMatch(c -> c.name.endsWith("package-info")),
                "package-info should not be emitted in classes[]");
        assertEquals(2, record.classes.size());
    }

    @Test
    void packageInfoInExcludedPackageIsDropped() throws Exception {
        compileDirect(
                "@Deprecated\n" +
                "package com.example.internal;\n",
                "package-info.java");
        compileDirect(
                "package com.example.internal;\n" +
                "public class InternalClass {}\n",
                "InternalClass.java");
        compileDirect(
                "@Deprecated\n" +
                "package com.example.api;\n",
                "package-info.java");
        compileDirect(
                "package com.example.api;\n" +
                "public class ApiClass {}\n",
                "ApiClass.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of("com.example.internal"));
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        assertNotNull(record.packages);
        assertEquals(1, record.packages.size());
        assertEquals("com.example.api", record.packages.get(0).name);
    }

    @Test
    void signatureWithoutPackageInfoHasNullPackages() throws Exception {
        compileDirect(
                "package com.example.onlyclasses;\n" +
                "public class OnlyClass {}\n",
                "OnlyClass.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        assertNull(record.packages, "Signature without package-info should have null packages");
    }

    @Test
    void methodOverloadsAreSortedByParameterTypes() throws Exception {
        compileDirect(
                "package com.example;\n" +
                "public class Service {\n" +
                "    public void run(String a, int b) {}\n" +
                "    public void run() {}\n" +
                "    public void run(int a) {}\n" +
                "    public void run(String a) {}\n" +
                "}\n",
                "Service.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord cls = findClass(record, "com.example.Service").orElseThrow();
        assertNotNull(cls.methods);
        assertEquals(4, cls.methods.size());

        List<String> paramSignatures = cls.methods.stream()
                .map(m -> m.parameters == null ? ""
                        : m.parameters.stream().map(p -> p.type).collect(java.util.stream.Collectors.joining(",")))
                .collect(java.util.stream.Collectors.toList());

        assertEquals(List.of("", "int", "java.lang.String", "java.lang.String,int"), paramSignatures,
                "Method overloads should be sorted deterministically by parameter types");
    }

    @Test
    void hiddenFieldsAreSortedAlphabetically() throws Exception {
        compileDirect(
                "package com.example;\n" +
                "public class Data {\n" +
                "    private int zebra;\n" +
                "    private int apple;\n" +
                "    private int mango;\n" +
                "}\n",
                "Data.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord cls = findClass(record, "com.example.Data").orElseThrow();
        assertNotNull(cls.hiddenFields);
        assertEquals(List.of("apple", "mango", "zebra"), cls.hiddenFields,
                "Hidden fields should be sorted alphabetically");
    }

    @Test
    void innerClassesAreSortedAlphabetically() throws Exception {
        compileDirect(
                "package com.example;\n" +
                "public class Container {\n" +
                "    public static class Zebra {}\n" +
                "    public static class Apple {}\n" +
                "    public static class Mango {}\n" +
                "}\n",
                "Container.java");

        SignatureGenerator gen = new SignatureGenerator(
                List.of(tempDir.toFile()), Set.of("com.example"), Set.of());
        SignatureRecord record = gen.generate("1.0", tempDir.resolve("sig.json"));

        ClassRecord cls = findClass(record, "com.example.Container").orElseThrow();
        assertNotNull(cls.innerClasses);
        assertEquals(List.of("Apple", "Mango", "Zebra"), cls.innerClasses,
                "Inner classes should be sorted alphabetically");
    }

    // -------------------------------------------------------------------------
    // Lower-level compile helper (no class-name inference needed)
    // -------------------------------------------------------------------------

    private void compileDirect(String source, String fileName) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        String pkg = extractPackageName(source);
        Path srcDir = tempDir.resolve("src");
        if (!pkg.isEmpty()) {
            srcDir = srcDir.resolve(pkg.replace('.', File.separatorChar));
        }
        Files.createDirectories(srcDir);
        Path srcFile = srcDir.resolve(fileName);
        Files.writeString(srcFile, source);
        int rc = compiler.run(null, null, null,
                "-d", tempDir.toString(),
                "-cp", tempDir.toString(),
                "--release", "11",
                srcFile.toString());
        assertTrue(rc == 0, "Compilation failed: " + srcFile);
    }
}
