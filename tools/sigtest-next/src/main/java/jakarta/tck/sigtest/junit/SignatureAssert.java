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
import jakarta.tck.sigtest.model.SignatureRecord;
import jakarta.tck.sigtest.util.SignatureIO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * JUnit 5 assertions for API signature compatibility.
 *
 * <p>Use {@link #assertCompatible(String)} (or the builder returned by
 * {@link #forResource(String)}) inside a JUnit 5 {@code @Test} method to verify that
 * the live API on the test classpath matches a pre-generated baseline {@code .sig.json}
 * file stored as a classpath resource.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Test
 * void apiMatchesBaseline() {
 *     SignatureAssert.assertCompatible("/jakarta.servlet-api.sig.json");
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
     * Fluent builder that configures and executes a single signature compatibility assertion.
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
