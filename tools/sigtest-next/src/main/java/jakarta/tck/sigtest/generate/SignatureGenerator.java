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

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.tck.sigtest.model.AnnotationRecord;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.FieldRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.ParameterRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans compiled {@code .class} files and produces a {@link SignatureRecord}
 * describing the public API of the requested packages.
 *
 * <p>The scanner uses ASM to read bytecode directly, avoiding any class-loading
 * of the scanned classes into the plugin's own JVM.
 */
public class SignatureGenerator {

    private final List<File> classpath;
    private final Set<String> rootPackages;
    private final Set<String> excludePackages;

    /**
     * @param classpath       directories and/or JAR files to scan
     * @param rootPackages    dot-separated package prefixes to include
     *                        (a package matches if its name equals or starts with one of these)
     * @param excludePackages dot-separated package prefixes to exclude
     *                        (checked after {@code rootPackages})
     */
    public SignatureGenerator(List<File> classpath, Set<String> rootPackages, Set<String> excludePackages) {
        this.classpath = classpath;
        this.rootPackages = rootPackages;
        this.excludePackages = excludePackages;
    }

    /**
     * Runs the scan, writes the result as pretty JSON to {@code output}, and
     * returns the populated {@link SignatureRecord}.
     *
     * @param apiVersion version string to embed in the record (e.g. {@code "11.0.0"})
     * @param output     path of the {@code .sig.json} file to create or overwrite
     * @return populated signature record, never {@code null}
     * @throws IOException if any classpath entry cannot be read or the output cannot be written
     */
    public SignatureRecord generate(String apiVersion, Path output) throws IOException {
        SignatureRecord record = generate(apiVersion);
        output.getParent().toFile().mkdirs();
        JsonbConfig config = new JsonbConfig().withFormatting(true).withNullValues(false);
        try (Jsonb jsonb = JsonbBuilder.create(config);
             Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            jsonb.toJson(record, writer);
        } catch (Exception e) {
            throw new IOException("Failed to write signature file: " + output, e);
        }
        return record;
    }

    /**
     * Runs the scan and returns the populated {@link SignatureRecord} without
     * writing any file. Use {@link #generate(String, Path)} to also persist the result.
     *
     * @param apiVersion version string to embed in the record (e.g. {@code "11.0.0"})
     * @return populated signature record, never {@code null}
     * @throws IOException if any classpath entry cannot be read
     */
    public SignatureRecord generate(String apiVersion) throws IOException {
        List<ClassRecord> classes = new ArrayList<>();

        for (File entry : classpath) {
            if (!entry.exists()) {
                continue;
            }
            if (entry.isDirectory()) {
                scanDirectory(entry.toPath(), classes);
            } else if (entry.getName().endsWith(".jar")) {
                scanJar(entry, classes);
            }
        }

        classes.sort(Comparator.comparing(c -> c.name));

        SignatureRecord record = new SignatureRecord();
        record.apiVersion = apiVersion;
        record.packages = rootPackages.stream().sorted().collect(Collectors.joining(","));
        record.classes = classes;
        return record;
    }

    // -------------------------------------------------------------------------
    // Directory scanning
    // -------------------------------------------------------------------------

    private void scanDirectory(Path root, List<ClassRecord> out) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> classFiles = stream.filter(f -> f.toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path p : classFiles) {
                try (InputStream is = Files.newInputStream(p)) {
                    visitClass(is, out);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // JAR scanning
    // -------------------------------------------------------------------------

    private void scanJar(File jar, List<ClassRecord> out) throws IOException {
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream is = jf.getInputStream(e)) {
                    visitClass(is, out);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ASM visit
    // -------------------------------------------------------------------------

    private void visitClass(InputStream is, List<ClassRecord> out) throws IOException {
        ClassReader reader = new ClassReader(is);
        ApiClassVisitor visitor = new ApiClassVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (visitor.record != null) {
            out.add(visitor.record);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Convert a JVM internal name ({@code java/lang/String}) to a binary name. */
    static String internalToBinary(String internalName) {
        if (internalName == null) {
            return null;
        }
        return internalName.replace('/', '.');
    }

    /** Convert a JVM type descriptor to a human-readable name. */
    static String descriptorToTypeName(String descriptor) {
        return Type.getType(descriptor).getClassName();
    }

    /** Convert an array of JVM internal names to a list of binary names. */
    static List<String> internalsToList(String[] names) {
        if (names == null || names.length == 0) {
            return List.of();
        }
        return Arrays.stream(names)
                .map(SignatureGenerator::internalToBinary)
                .collect(Collectors.toList());
    }

    /** Render access flags as a sorted, space-separated modifier string. */
    static String modifiersOf(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0)       mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0)    mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0)      mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0)       mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0)        mods.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0)     mods.add("abstract");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) mods.add("synchronized");
        if ((access & Opcodes.ACC_NATIVE) != 0)       mods.add("native");
        if ((access & Opcodes.ACC_TRANSIENT) != 0)    mods.add("transient");
        if ((access & Opcodes.ACC_VOLATILE) != 0)     mods.add("volatile");
        if ((access & Opcodes.ACC_STRICT) != 0)       mods.add("strictfp");
        return String.join(" ", mods);
    }

    /** Return {@code true} if the access flags indicate a public or protected member. */
    static boolean isPublicApi(int access) {
        return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
    }

    // =========================================================================
    // Inner ASM visitor implementations
    // =========================================================================

    /**
     * Top-level visitor for a single class file. Populates a {@link ClassRecord}
     * only when the class belongs to the requested packages and is public/protected.
     */
    private class ApiClassVisitor extends ClassVisitor {

        ClassRecord record;
        private int classAccess;
        private boolean isSynthetic;

        ApiClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {

            // Synthetic/anonymous classes are not public API.
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                isSynthetic = true;
                return;
            }

            String binaryName = internalToBinary(name);
            String packageName = packageOf(binaryName);

            // Check package inclusion / exclusion.
            if (!isIncluded(packageName)) {
                return;
            }

            // Only public or (for member classes) protected classes.
            if (!isPublicApi(access)) {
                return;
            }

            classAccess = access;
            record = new ClassRecord();
            record.name = binaryName;
            record.kind = kindOf(access);
            record.modifiers = modifiersOf(access);
            record.interfaces = internalsToList(interfaces);
            record.genericSignature = signature;  // null for non-generic classes

            // Suppress Object superclass — it is implicit everywhere.
            String superBinary = internalToBinary(superName);
            if (superBinary != null && !superBinary.equals("java.lang.Object")
                    && !record.kind.equals("interface")) {
                record.superClass = superBinary;
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (record == null) return null;
            if (record.annotations == null) record.annotations = new ArrayList<>();
            AnnotationRecord ar = new AnnotationRecord();
            ar.type = descriptorToTypeName(descriptor);
            record.annotations.add(ar);
            return new AnnotationValueCollector(ar);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if (record == null) return null;
            if (!isPublicApi(access)) {
                // Track hidden fields for informational purposes.
                if (record.hiddenClasses == null) record.hiddenClasses = new ArrayList<>();
                record.hiddenClasses.add(name);
                return null;
            }
            FieldRecord fr = new FieldRecord();
            fr.name = name;
            fr.type = descriptorToTypeName(descriptor);
            fr.modifiers = modifiersOf(access);
            fr.genericSignature = signature;  // null when no generic type info
            if (value != null) {
                fr.constantValue = value.toString();
            }
            if (record.fields == null) record.fields = new ArrayList<>();
            record.fields.add(fr);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (fr.annotations == null) fr.annotations = new ArrayList<>();
                    AnnotationRecord ar = new AnnotationRecord();
                    ar.type = descriptorToTypeName(desc);
                    fr.annotations.add(ar);
                    return new AnnotationValueCollector(ar);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (record == null) return null;
            // Skip synthetic bridge methods and private members.
            if (!isPublicApi(access) || (access & Opcodes.ACC_SYNTHETIC) != 0) return null;

            boolean isCtor = "<init>".equals(name);
            // Skip default constructor on enums — the compiler-generated one
            // has a misleading signature.
            if (isCtor && record.kind.equals("enum")) return null;

            MethodRecord mr = new MethodRecord();
            mr.name = isCtor ? simpleNameOf(record.name) : name;
            mr.isConstructor = isCtor;
            mr.modifiers = modifiersOf(access);
            mr.genericSignature = signature;  // full JVM generic sig; null for non-generic methods

            Type methodType = Type.getMethodType(descriptor);
            if (!isCtor) {
                mr.returnType = methodType.getReturnType().getClassName();
            }
            mr.parameters = Arrays.stream(methodType.getArgumentTypes())
                    .map(t -> new ParameterRecord(t.getClassName()))
                    .collect(Collectors.toList());
            mr.exceptions = internalsToList(exceptions);

            if (isCtor) {
                if (record.constructors == null) record.constructors = new ArrayList<>();
                record.constructors.add(mr);
            } else {
                if (record.methods == null) record.methods = new ArrayList<>();
                record.methods.add(mr);
            }

            return new MethodVisitor(Opcodes.ASM9) {
                // Number of parameters covered by the RuntimeVisible/InvisibleParameterAnnotations
                // attributes; -1 when unspecified (equals the descriptor parameter count).
                // javac omits synthetic/mandated leading parameters (e.g. the outer instance of
                // an inner-class constructor), so attribute indices must be shifted.
                private int visibleCount = -1;
                private int invisibleCount = -1;

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (mr.annotations == null) mr.annotations = new ArrayList<>();
                    AnnotationRecord ar = new AnnotationRecord();
                    ar.type = descriptorToTypeName(desc);
                    mr.annotations.add(ar);
                    return new AnnotationValueCollector(ar);
                }

                @Override
                public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
                    if (visible) visibleCount = parameterCount;
                    else invisibleCount = parameterCount;
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                    int declared = mr.parameters.size();
                    int count = visible ? visibleCount : invisibleCount;
                    int index = parameter + (count < 0 ? 0 : declared - count);
                    if (index < 0 || index >= declared) return null;

                    ParameterRecord pr = mr.parameters.get(index);
                    if (pr.annotations == null) pr.annotations = new ArrayList<>();
                    AnnotationRecord ar = new AnnotationRecord();
                    ar.type = descriptorToTypeName(desc);
                    pr.annotations.add(ar);
                    return new AnnotationValueCollector(ar);
                }
            };
        }

        @Override
        public void visitEnd() {
            if (record != null) {
                // Sort members for stable output.
                if (record.fields != null)       record.fields.sort(Comparator.comparing(f -> f.name));
                if (record.methods != null)      record.methods.sort(Comparator.comparing(m -> m.name));
                if (record.constructors != null) record.constructors.sort(
                        Comparator.comparing(c -> c.parameters == null ? ""
                                : c.parameters.stream().map(p -> p.type).collect(Collectors.joining(","))));
            }
        }

        // ------------------------------------------------------------------

        private String packageOf(String binaryName) {
            int dot = binaryName.lastIndexOf('.');
            return dot < 0 ? "" : binaryName.substring(0, dot);
        }

        private boolean isIncluded(String pkg) {
            boolean included = rootPackages.stream().anyMatch(
                    root -> pkg.equals(root) || pkg.startsWith(root + "."));
            if (!included) return false;
            return excludePackages.stream().noneMatch(
                    ex -> pkg.equals(ex) || pkg.startsWith(ex + "."));
        }

        private String simpleNameOf(String binaryName) {
            // Handle inner classes: "com.example.Outer$Inner" → "Inner"
            String simple = binaryName;
            int dollar = simple.lastIndexOf('$');
            if (dollar >= 0) simple = simple.substring(dollar + 1);
            int dot = simple.lastIndexOf('.');
            if (dot >= 0) simple = simple.substring(dot + 1);
            return simple;
        }

        private String kindOf(int access) {
            if ((access & Opcodes.ACC_ANNOTATION) != 0) return "annotation";
            if ((access & Opcodes.ACC_ENUM) != 0)       return "enum";
            if ((access & Opcodes.ACC_RECORD) != 0)     return "record";
            if ((access & Opcodes.ACC_INTERFACE) != 0)  return "interface";
            return "class";
        }
    }

    // =========================================================================
    // Annotation value collector
    // =========================================================================

    /** Collects annotation element values into an {@link AnnotationRecord}. */
    private static class AnnotationValueCollector extends AnnotationVisitor {

        private final AnnotationRecord target;

        AnnotationValueCollector(AnnotationRecord target) {
            super(Opcodes.ASM9);
            this.target = target;
        }

        @Override
        public void visit(String name, Object value) {
            put(name, value.toString());
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            put(name, descriptorToTypeName(descriptor) + "." + value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            List<String> items = new ArrayList<>();
            return new AnnotationVisitor(Opcodes.ASM9) {
                @Override
                public void visit(String ignoredName, Object value) {
                    items.add(value.toString());
                }

                @Override
                public void visitEnd() {
                    put(name, items.toString());
                }
            };
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            // Nested annotation — record the type only.
            put(name, "@" + descriptorToTypeName(descriptor));
            return null;
        }

        private void put(String name, String value) {
            if (target.values == null) target.values = new LinkedHashMap<>();
            target.values.put(name, value);
        }
    }
}
