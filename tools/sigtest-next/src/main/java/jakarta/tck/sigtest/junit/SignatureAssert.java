/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.junit;

import jakarta.tck.sigtest.check.SignatureChecker;
import jakarta.tck.sigtest.check.Violation;
import jakarta.tck.sigtest.model.ClassRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import jakarta.tck.sigtest.util.SignatureIO;
import org.junit.jupiter.api.DynamicTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * JUnit 5 assertions and dynamic test factory for API signature compatibility.
 *
 * <p>Verifies that the live API on the test classpath (or resolved via configuration)
 * matches a pre-generated baseline {@code .sig.json} file stored as a classpath resource.
 *
 * <h2>Single assertion mode ({@code @Test})</h2>
 * <p>Use {@link #assertCompatible(String)} (or {@link #forResource(String)}{@code .check()})
 * inside a standard {@code @Test} method to fail on any signature violation:
 * <pre>{@code
 * @Test
 * void apiMatchesBaseline() {
 *     SignatureAssert.assertCompatible("/jakarta.servlet-api.sig.json");
 * }
 * }</pre>
 *
 * <h2>Dynamic tests mode ({@code @TestFactory})</h2>
 * <p>Use {@link #forResource(String)}{@code .dynamicTests()} inside a {@link org.junit.jupiter.api.TestFactory @TestFactory}
 * method to emit one {@link DynamicTest} per baseline class, plus additional checks for added
 * classes and package annotations. This gives isolated, class-by-class test reporting in Surefire
 * and IDEs without requiring inheritance from an abstract base class:
 * <pre>{@code
 * @TestFactory
 * Stream<DynamicTest> apiSignatures() {
 *     return SignatureAssert.forResource("/jakarta.servlet-api.sig.json")
 *             .dynamicTests();
 * }
 * }</pre>
 *
 * <p>The check is <em>strict</em>: both removals and additions are violations.
 *
 * <p>Live-API selection order:
 * <ol>
 *   <li>An explicit path supplied via {@link Options#liveApi(Path)}.</li>
 *   <li>The system property {@code sigtest-next.resolve.api} when set to a non-placeholder
 *       value (propagated from the {@code sigtest-next:resolve} Maven goal via
 *       Surefire {@code systemPropertyVariables}).</li>
 *   <li>Classpath scanning through the current thread's context class loader
 *       (or the class loader of this class as a fallback).</li>
 * </ol>
 */
public final class SignatureAssert {

    /** System property set by the {@code sigtest-next:resolve} mojo. */
    static final String RESOLVE_PROPERTY = "sigtest-next.resolve.api";

    private SignatureAssert() {
    }

    // -------------------------------------------------------------------------
    // Public entry points
    // -------------------------------------------------------------------------

    /**
     * Asserts that the live API matches the baseline loaded from the given classpath resource.
     *
     * <p>Uses the calling class's class loader to resolve the resource and to locate the live API.
     * Delegates to {@link Options#check()}.
     *
     * @param resourceName classpath resource path to the {@code .sig.json} baseline file
     *                     (e.g. {@code "/jakarta.servlet-api.sig.json"})
     * @throws AssertionError if any violations are found
     * @throws IllegalStateException if the baseline resource cannot be read
     */
    public static void assertCompatible(String resourceName) {
        forResource(resourceName).check();
    }

    /**
     * Returns a fluent {@link Options} builder for the specified baseline resource.
     *
     * @param resourceName classpath resource path to the {@code .sig.json} baseline file
     * @return a new {@code Options} instance
     */
    public static Options forResource(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName must not be null");
        return new Options(resourceName);
    }

    // -------------------------------------------------------------------------
    // Options builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder that configures and executes signature compatibility assertions or dynamic tests.
     */
    public static final class Options {

        private final String resourceName;
        private Path liveApiPath;
        private Set<String> excludes;

        private Options(String resourceName) {
            this.resourceName = resourceName;
        }

        /**
         * Overrides the package prefixes excluded during the live API scan.
         * When not set, excludes are read from the baseline's stored {@code excludes} field.
         *
         * @param excludes package prefixes to exclude
         * @return this builder
         */
        public Options excludes(Collection<String> excludes) {
            this.excludes = (excludes == null) ? null : new HashSet<>(excludes);
            return this;
        }

        /**
         * Overrides the package prefixes excluded during the live API scan.
         *
         * @param excludes package prefixes to exclude
         * @return this builder
         */
        public Options excludes(String... excludes) {
            this.excludes = (excludes == null) ? null : new HashSet<>(Arrays.asList(excludes));
            return this;
        }

        /**
         * Provides an explicit JAR or directory path containing the live API.
         * When set, this takes priority over system property and classpath scanning.
         *
         * @param liveApi path to a JAR file or directory containing the live API classes
         * @return this builder
         */
        public Options liveApi(Path liveApi) {
            this.liveApiPath = liveApi;
            return this;
        }

        /**
         * Executes the signature compatibility assertion.
         *
         * @throws AssertionError    if any violations are found
         * @throws IllegalStateException if the baseline resource cannot be read or the live API
         *                               resolved via system property cannot be accessed
         */
        public void check() {
            ClassLoader cl = classLoader();
            SignatureRecord baseline = loadBaseline(cl);
            List<Violation> violations = runCheck(baseline, cl);
            if (!violations.isEmpty()) {
                throw new AssertionError(formatViolations(violations));
            }
        }

        /**
         * Returns a stream of {@link DynamicTest} instances: one per baseline class, plus
         * tests for added classes and package annotations.
         *
         * <p>Use this inside a JUnit 5 {@link org.junit.jupiter.api.TestFactory @TestFactory}
         * method to get isolated per-class failure reporting.
         *
         * <p>Example usage:
         * <pre>{@code
         * @TestFactory
         * Stream<DynamicTest> apiSignatures() {
         *     return SignatureAssert.forResource("/jakarta.servlet-api.sig.json")
         *             .dynamicTests();
         * }
         * }</pre>
         *
         * @return stream of dynamic tests
         * @throws IllegalStateException if the baseline resource cannot be read or the live API
         *                               resolved via system property cannot be accessed
         */
        public Stream<DynamicTest> dynamicTests() {
            ClassLoader cl = classLoader();
            SignatureRecord baseline = loadBaseline(cl);
            List<Violation> violations = runCheck(baseline, cl);

            // Map violations by class name
            Map<String, List<Violation>> violationsByClass = new HashMap<>();
            List<Violation> addedClassViolations = new ArrayList<>();
            List<Violation> packageViolations = new ArrayList<>();

            for (Violation v : violations) {
                if (v.kind == Violation.Kind.ADDED_CLASS) {
                    addedClassViolations.add(v);
                } else if (v.kind == Violation.Kind.PACKAGE_ANNOTATIONS_CHANGED) {
                    packageViolations.add(v);
                } else {
                    violationsByClass.computeIfAbsent(v.className, k -> new ArrayList<>()).add(v);
                }
            }

            Set<String> baselineClassNames = new TreeSet<>();
            if (baseline.classes != null) {
                for (ClassRecord c : baseline.classes) {
                    if (c != null && c.name != null) {
                        baselineClassNames.add(c.name);
                    }
                }
            }

            List<DynamicTest> tests = new ArrayList<>();

            // 1. One test per baseline class (sorted alphabetically)
            for (String className : baselineClassNames) {
                List<Violation> classViolations = violationsByClass.getOrDefault(className, Collections.emptyList());
                tests.add(DynamicTest.dynamicTest(className, () -> {
                    if (!classViolations.isEmpty()) {
                        throw new AssertionError(formatViolations(classViolations));
                    }
                }));
            }

            // 2. Added / unexpected classes test
            List<Violation> extraViolations = new ArrayList<>(addedClassViolations);
            for (Map.Entry<String, List<Violation>> entry : violationsByClass.entrySet()) {
                if (!baselineClassNames.contains(entry.getKey())) {
                    extraViolations.addAll(entry.getValue());
                }
            }
            tests.add(DynamicTest.dynamicTest("[added classes]", () -> {
                if (!extraViolations.isEmpty()) {
                    throw new AssertionError(formatViolations(extraViolations));
                }
            }));

            // 3. Package annotations test (if baseline declares packages or package violations exist)
            boolean hasPackages = baseline.packages != null && !baseline.packages.isEmpty();
            if (hasPackages || !packageViolations.isEmpty()) {
                tests.add(DynamicTest.dynamicTest("[package annotations]", () -> {
                    if (!packageViolations.isEmpty()) {
                        throw new AssertionError(formatViolations(packageViolations));
                    }
                }));
            }

            return tests.stream();
        }

        // -------------------------------------------------------------------------
        // Internal helpers
        // -------------------------------------------------------------------------

        private ClassLoader classLoader() {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = SignatureAssert.class.getClassLoader();
            }
            return cl;
        }

        private SignatureRecord loadBaseline(ClassLoader cl) {
            try {
                return SignatureIO.loadResource(cl, resourceName);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot load baseline resource '" + resourceName + "': " + e.getMessage(), e);
            }
        }

        private List<Violation> runCheck(SignatureRecord baseline, ClassLoader cl) {
            // Priority 1: explicit liveApi path
            if (liveApiPath != null) {
                return checkViaPath(baseline, liveApiPath);
            }

            // Priority 2: system property set by sigtest-next:resolve mojo
            String resolveProperty = System.getProperty(RESOLVE_PROPERTY);
            if (resolveProperty != null && !resolveProperty.startsWith("${")) {
                Path jarPath = Paths.get(resolveProperty);
                if (!Files.exists(jarPath)) {
                    throw new IllegalStateException(
                            "System property '" + RESOLVE_PROPERTY + "' points to a file that does not exist: "
                                    + resolveProperty);
                }
                return checkViaPath(baseline, jarPath);
            }

            // Priority 3: classpath scanning
            return checkViaClassLoader(baseline, cl);
        }

        private List<Violation> checkViaPath(SignatureRecord baseline, Path path) {
            try {
                return SignatureChecker.check(baseline, path, excludes);
            } catch (IOException e) {
                throw new IllegalStateException("Error scanning live API at '" + path + "': " + e.getMessage(), e);
            }
        }

        private List<Violation> checkViaClassLoader(SignatureRecord baseline, ClassLoader cl) {
            try {
                return SignatureChecker.check(baseline, cl, excludes);
            } catch (IOException e) {
                throw new IllegalStateException("Error scanning live API from classpath: " + e.getMessage(), e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Violation formatting
    // -------------------------------------------------------------------------

    /**
     * Formats a non-empty list of violations into a human-readable {@link AssertionError} message.
     * Violations are grouped by class name for readability.
     *
     * @param violations non-empty list of violations
     * @return formatted message string
     */
    static String formatViolations(List<Violation> violations) {
        // Group violations by class name, preserving insertion order within each class
        Map<String, List<Violation>> byClass = new LinkedHashMap<>();
        for (Violation v : violations) {
            byClass.computeIfAbsent(v.className, k -> new java.util.ArrayList<>()).add(v);
        }

        // Sort classes alphabetically for stable output
        Map<String, List<Violation>> sorted = new TreeMap<>(byClass);

        StringBuilder sb = new StringBuilder();
        sb.append("API signature check failed: ")
          .append(violations.size())
          .append(violations.size() == 1 ? " violation" : " violations")
          .append(" found\n");

        for (Map.Entry<String, List<Violation>> entry : sorted.entrySet()) {
            sb.append("\n  ").append(entry.getKey()).append(":\n");
            for (Violation v : entry.getValue()) {
                sb.append("    [").append(v.kind).append("] ").append(v.message).append("\n");
            }
        }

        return sb.toString();
    }
}
