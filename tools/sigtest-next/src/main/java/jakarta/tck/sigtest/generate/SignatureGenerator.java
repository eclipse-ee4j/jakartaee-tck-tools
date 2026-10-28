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
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.FieldRecord;
import jakarta.tck.sigtest.model.MethodRecord;
import jakarta.tck.sigtest.model.PackageRecord;
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
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
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
        List<PackageRecord> packages = new ArrayList<>();

        for (File entry : classpath) {
            if (!entry.exists()) {
                continue;
            }
            if (entry.isDirectory()) {
                scanDirectory(entry.toPath(), classes, packages);
            } else if (entry.getName().endsWith(".jar")) {
                scanJar(entry, classes, packages);
            }
        }

        classes.sort(Comparator.comparing(c -> c.name));

        SignatureRecord record = new SignatureRecord();
        record.apiVersion = apiVersion;
        if (!packages.isEmpty()) {
            packages.sort(Comparator.comparing(p -> p.name));
            record.packages = packages;
        }
        record.classes = classes;
        return record;
    }

    // -------------------------------------------------------------------------
    // Directory scanning
    // -------------------------------------------------------------------------

    private void scanDirectory(Path root, List<ClassRecord> outClasses, List<PackageRecord> outPackages) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> classFiles = stream.filter(f -> f.toString().endsWith(".class"))
                    .collect(Collectors.toList());
            for (Path p : classFiles) {
                try (InputStream is = Files.newInputStream(p)) {
                    visitClass(is, outClasses, outPackages);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // JAR scanning
    // -------------------------------------------------------------------------

    private void scanJar(File jar, List<ClassRecord> outClasses, List<PackageRecord> outPackages) throws IOException {
        try (JarFile jf = new JarFile(jar)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream is = jf.getInputStream(e)) {
                    visitClass(is, outClasses, outPackages);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // ASM visit
    // -------------------------------------------------------------------------

    private void visitClass(InputStream is, List<ClassRecord> outClasses, List<PackageRecord> outPackages) throws IOException {
        ClassReader reader = new ClassReader(is);
        ApiClassVisitor visitor = new ApiClassVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (visitor.packageRecord != null) {
            outPackages.add(visitor.packageRecord);
        } else if (visitor.record != null) {
            outClasses.add(visitor.record);
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

    /** Render class access flags (top-level or inner) as a space-separated modifier string. */
    static String modifiersOfClass(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0)    mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0)   mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0)    mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0)     mods.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0)  mods.add("abstract");
        return String.join(" ", mods);
    }

    /** Render field access flags as a space-separated modifier string. */
    static String modifiersOfField(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0)    mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0) mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0)   mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0)    mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0)     mods.add("final");
        if ((access & Opcodes.ACC_TRANSIENT) != 0) mods.add("transient");
        if ((access & Opcodes.ACC_VOLATILE) != 0)  mods.add("volatile");
        return String.join(" ", mods);
    }

    /** Render method or constructor access flags as a space-separated modifier string. */
    static String modifiersOfMethod(int access) {
        List<String> mods = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0)       mods.add("public");
        if ((access & Opcodes.ACC_PROTECTED) != 0)    mods.add("protected");
        if ((access & Opcodes.ACC_PRIVATE) != 0)      mods.add("private");
        if ((access & Opcodes.ACC_STATIC) != 0)       mods.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0)        mods.add("final");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) mods.add("synchronized");
        if ((access & Opcodes.ACC_VARARGS) != 0)      mods.add("varargs");
        if ((access & Opcodes.ACC_NATIVE) != 0)       mods.add("native");
        if ((access & Opcodes.ACC_ABSTRACT) != 0)     mods.add("abstract");
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
        PackageRecord packageRecord;
        private int classAccess;
        private boolean isSynthetic;

        ApiClassVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {

            String binaryName = internalToBinary(name);
            String packageName = packageOf(binaryName);

            // Check package inclusion / exclusion.
            if (!isIncluded(packageName)) {
                return;
            }

            // Special-case package-info: emit a PackageRecord for package-level metadata/annotations.
            if (name.endsWith("/package-info") || name.equals("package-info")) {
                packageRecord = new PackageRecord(packageName);
                return;
            }

            // Synthetic/anonymous classes are not public API.
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                isSynthetic = true;
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
            record.modifiers = modifiersOfClass(access);
            record.interfaces = internalsToList(interfaces);
            record.genericSignature = signature;  // null for non-generic classes

            // Suppress Object superclass — it is implicit everywhere.
            String superBinary = internalToBinary(superName);
            if (superBinary != null && !superBinary.equals("java.lang.Object")
                    && !record.kind.equals("interface")) {
                record.superClass = superBinary;
            }
        }

        /** Called once per entry of the {@code PermittedSubclasses} attribute (sealed types only). */
        @Override
        public void visitPermittedSubclass(String permittedSubclass) {
            if (record == null) return;
            if (record.permittedSubclasses == null) record.permittedSubclasses = new ArrayList<>();
            record.permittedSubclasses.add(internalToBinary(permittedSubclass));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (packageRecord != null) {
                if (packageRecord.annotations == null) packageRecord.annotations = new TreeMap<>();
                return new AnnotationValueCollector(packageRecord.annotations, descriptor);
            }
            if (record == null) return null;
            if (record.annotations == null) record.annotations = new TreeMap<>();
            return new AnnotationValueCollector(record.annotations, descriptor);
        }

        /**
         * Processes one entry from the {@code InnerClasses} attribute.
         *
         * <p>ASM calls this for every entry in the attribute — not just entries that
         * belong to the current class. We handle two distinct cases:
         * <ol>
         *   <li>The entry whose {@code name} equals the current class: this gives us the
         *       correct access flags (including {@code static}, {@code protected},
         *       {@code private}) and the name of the directly enclosing class.</li>
         *   <li>Entries whose {@code outerName} equals the current class: these are
         *       directly-enclosed member types that we add to {@code innerClasses}.</li>
         * </ol>
         *
         * <p>Anonymous classes ({@code innerName == null}) and classes whose outer
         * class is a method ({@code outerName == null}) are skipped; they are not
         * named member types.
         */
        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            if (record == null) return;

            String binaryName = internalToBinary(name);

            if (binaryName.equals(record.name)) {
                // This entry describes the current class itself.
                // The access flags here are authoritative for nested classes.
                record.modifiers = modifiersOfClass(access);
            } else if (outerName != null && internalToBinary(outerName).equals(record.name)
                    && innerName != null) {
                // This entry describes a directly-enclosed member type.
                if (isPublicApi(access)) {
                    if (record.innerClasses == null) record.innerClasses = new ArrayList<>();
                    record.innerClasses.add(innerName);
                }
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            if (record == null) return null;
            if (!isPublicApi(access)) {
                // Track hidden fields for informational purposes.
                if (record.hiddenFields == null) record.hiddenFields = new ArrayList<>();
                record.hiddenFields.add(name);
                return null;
            }
            FieldRecord fr = new FieldRecord();
            fr.name = name;
            fr.type = descriptorToTypeName(descriptor);
            fr.modifiers = modifiersOfField(access);
            fr.genericSignature = signature;  // null when no generic type info
            if (value != null) {
                fr.constantValue = value.toString();
            }
            if (record.fields == null) record.fields = new ArrayList<>();
            record.fields.add(fr);
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (fr.annotations == null) fr.annotations = new TreeMap<>();
                    return new AnnotationValueCollector(fr.annotations, desc);
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
            mr.modifiers = modifiersOfMethod(access);
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
                    if (mr.annotations == null) mr.annotations = new TreeMap<>();
                    return new AnnotationValueCollector(mr.annotations, desc);
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    // The default arrives as a single unnamed element value.
                    return new AnnotationValueCollector((ignored, v) -> mr.defaultValue = v, null);
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
                    if (pr.annotations == null) pr.annotations = new TreeMap<>();
                    return new AnnotationValueCollector(pr.annotations, desc);
                }
            };
        }

        @Override
        public void visitEnd() {
            if (record != null) {
                // Sort members for stable output.
                if (record.fields != null)        record.fields.sort(Comparator.comparing(f -> f.name));
                if (record.methods != null)       record.methods.sort(
                        Comparator.comparing((MethodRecord m) -> m.name)
                                .thenComparing(m -> m.parameters == null ? ""
                                        : m.parameters.stream().map(p -> p.type).collect(Collectors.joining(",")))
                                .thenComparing(m -> m.returnType == null ? "" : m.returnType));
                if (record.constructors != null)  record.constructors.sort(
                        Comparator.comparing(c -> c.parameters == null ? ""
                                : c.parameters.stream().map(p -> p.type).collect(Collectors.joining(","))));
                if (record.hiddenFields != null) Collections.sort(record.hiddenFields);
                if (record.innerClasses != null)  Collections.sort(record.innerClasses);
                if (record.permittedSubclasses != null) Collections.sort(record.permittedSubclasses);
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

    /**
     * Collects annotation element values into a map entry keyed by annotation type.
     *
     * <p>The entry is a list of strings: the {@code value} element is rendered bare
     * (arrays flattened to one entry per item), every other element as
     * {@code name=value}. Values are rendered in a typed, self-describing form (see
     * {@link #formatValue(Object)}); enum constants, class literals, nested
     * annotations and arrays of any of these are all preserved.
     */
    private static class AnnotationValueCollector extends AnnotationVisitor {

        private final BiConsumer<String, String> sink;
        private final Runnable onEnd;
        private final boolean topLevel;

        AnnotationValueCollector(Map<String, List<String>> target, String descriptor) {
            this(topLevelSink(target, descriptor), null, true);
        }

        private AnnotationValueCollector(BiConsumer<String, String> sink, Runnable onEnd) {
            this(sink, onEnd, false);
        }

        private AnnotationValueCollector(BiConsumer<String, String> sink, Runnable onEnd, boolean topLevel) {
            super(Opcodes.ASM9);
            this.sink = sink;
            this.onEnd = onEnd;
            this.topLevel = topLevel;
        }

        private static BiConsumer<String, String> topLevelSink(Map<String, List<String>> target,
                                                              String descriptor) {
            List<String> elements = new ArrayList<>();
            target.put(descriptorToTypeName(descriptor), elements);
            return (name, value) -> elements.add("value".equals(name) ? value : name + "=" + value);
        }

        @Override
        public void visit(String name, Object value) {
            sink.accept(name, formatValue(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            sink.accept(name, formatEnum(descriptor, value));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if (topLevel && "value".equals(name)) {
                // Flatten the main element: one list entry per item.
                return new AnnotationValueCollector((ignored, v) -> sink.accept(name, v), null);
            }
            List<String> items = new ArrayList<>();
            // Array elements arrive with a null name; collect them in order.
            return new AnnotationValueCollector((ignored, v) -> items.add(v),
                    () -> sink.accept(name, "[" + String.join(", ", items) + "]"));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            Map<String, String> elements = new LinkedHashMap<>();
            return new AnnotationValueCollector(elements::put, () -> sink.accept(name,
                    formatAnnotation(descriptor, elements)));
        }

        @Override
        public void visitEnd() {
            if (onEnd != null) onEnd.run();
        }

        private static String formatEnum(String descriptor, String value) {
            return descriptorToTypeName(descriptor) + "." + value;
        }

        private static String formatAnnotation(String descriptor, Map<String, String> elements) {
            String body = elements.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
            return "@" + descriptorToTypeName(descriptor) + "(" + body + ")";
        }

        /**
         * Render a constant annotation element value so that its type is
         * recoverable: strings are quoted, chars are single-quoted, class
         * literals end in {@code .class}, and non-int numeric types carry a
         * suffix or cast ({@code 1L}, {@code 1.0f}, {@code (byte)1}, ...).
         */
        static String formatValue(Object value) {
            if (value instanceof String) {
                return "\"" + escape((String) value) + "\"";
            }
            if (value instanceof Type) {
                return ((Type) value).getClassName() + ".class";
            }
            if (value instanceof Character) {
                return "'" + escape(value.toString()) + "'";
            }
            if (value instanceof Long)  return value + "L";
            if (value instanceof Float) return value + "f";
            if (value instanceof Byte)  return "(byte)" + value;
            if (value instanceof Short) return "(short)" + value;
            if (value != null && value.getClass().isArray()) {
                // ASM passes arrays of primitives to visit() as a single value.
                int length = Array.getLength(value);
                List<String> items = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    items.add(formatValue(Array.get(value, i)));
                }
                return "[" + String.join(", ", items) + "]";
            }
            return String.valueOf(value);  // Integer, Boolean, Double
        }

        private static String escape(String s) {
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
    }
}
