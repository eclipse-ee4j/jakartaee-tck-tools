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
        System.out.println("  -> written: " + output.toAbsolutePath());
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
        assertEquals(List.of("java.lang.String"), greet.get().parameters);

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
        assertEquals(List.of("java.lang.String", "int"), ctor.parameters);
        assertNull(ctor.returnType);
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
        assertTrue(cls.annotations.stream()
                .anyMatch(a -> a.type.equals("java.lang.Deprecated")),
                "Expected @Deprecated on class");
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
