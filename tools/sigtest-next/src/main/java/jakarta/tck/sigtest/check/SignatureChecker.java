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
import jakarta.tck.sigtest.model.PackageRecord;
import jakarta.tck.sigtest.model.SignatureRecord;
import jakarta.tck.sigtest.util.ClasspathUtil;
import jakarta.tck.sigtest.util.SignatureIO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared signature checking facade that encapsulates baseline loading, live API scanning,
 * and computing diff violations.
 *
 * <p>Supports checking live APIs via a {@link ClassLoader} (locating API classpath entries
 * providing baseline packages and discovering all live packages present within those entries)
 * or via explicit target files/directories and classpath lists.
 */
public final class SignatureChecker {

    private SignatureChecker() {
    }

    /**
     * Checks a live API accessible through the specified {@link ClassLoader} against a baseline
     * {@link SignatureRecord}.
     *
     * <p>Package discovery scans the entire JAR or directory roots located for the baseline
     * packages so that any packages or types added to the live API are captured and reported.
     *
     * @param baseline    the baseline signature record to check against
     * @param classLoader class loader containing the live API on its classpath
     * @param excludes    package prefixes to exclude; if {@code null} or empty, defaults to
     *                    {@link SignatureRecord#excludes} from the baseline if present
     * @return unmodifiable list of violations found, or an empty list if compliant
     * @throws IOException if an error occurs while discovering resources or scanning bytecode
     */
    public static List<Violation> check(SignatureRecord baseline, ClassLoader classLoader, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");

        Set<String> baselinePackages = extractPackages(baseline);
        Set<String> effectiveExcludes = resolveExcludes(baseline, excludes);

        if (baselinePackages.isEmpty()) {
            SignatureRecord live = new SignatureRecord();
            live.apiVersion = baseline.apiVersion;
            live.classes = Collections.emptyList();
            return SignatureDiff.diff(baseline, live);
        }

        SignatureGenerator generator = SignatureGenerator.forClassLoader(classLoader, baselinePackages, effectiveExcludes);
        SignatureRecord live = generator.generate(baseline.apiVersion);
        return SignatureDiff.diff(baseline, live);
    }

    /**
     * Convenience overload for {@link #check(SignatureRecord, ClassLoader, Set)} defaulting
     * to baseline excludes if present.
     */
    public static List<Violation> check(SignatureRecord baseline, ClassLoader classLoader) throws IOException {
        return check(baseline, classLoader, null);
    }

    /**
     * Checks a live API located at a target JAR file or directory against a baseline
     * {@link SignatureRecord}, using an optional classpath for dependency type resolution.
     *
     * @param baseline  the baseline signature record to check against
     * @param target    target JAR file or directory containing the live API
     * @param classpath optional list of additional classpath JARs/directories
     * @param excludes  package prefixes to exclude; if {@code null} or empty, defaults to
     *                  {@link SignatureRecord#excludes} from the baseline if present
     * @return unmodifiable list of violations found, or an empty list if compliant
     * @throws IOException if an error occurs reading the target or classpath
     */
    public static List<Violation> check(SignatureRecord baseline, File target, List<File> classpath, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(target, "target must not be null");

        Set<String> rootPackages = ClasspathUtil.discoverPackages(Collections.singleton(target));
        Set<String> effectiveExcludes = resolveExcludes(baseline, excludes);

        List<File> cp = new ArrayList<>();
        if (target.exists()) {
            cp.add(target);
        }
        if (classpath != null) {
            for (File entry : classpath) {
                if (entry != null && !cp.contains(entry) && entry.exists()) {
                    cp.add(entry);
                }
            }
        }

        SignatureGenerator generator = new SignatureGenerator(cp, rootPackages, effectiveExcludes);
        SignatureRecord live = generator.generate(baseline.apiVersion);
        return SignatureDiff.diff(baseline, live);
    }

    /**
     * Checks a live API located at a target JAR file or directory against a baseline
     * {@link SignatureRecord}.
     *
     * @param baseline the baseline signature record to check against
     * @param target   target JAR file or directory containing the live API
     * @param excludes package prefixes to exclude; if {@code null} or empty, defaults to baseline excludes
     * @return unmodifiable list of violations found, or an empty list if compliant
     * @throws IOException if an error occurs reading the target
     */
    public static List<Violation> check(SignatureRecord baseline, File target, Set<String> excludes) throws IOException {
        return check(baseline, target, null, excludes);
    }

    /**
     * Convenience overload for {@link #check(SignatureRecord, File, Set)} with no explicit excludes.
     */
    public static List<Violation> check(SignatureRecord baseline, File target) throws IOException {
        return check(baseline, target, null, null);
    }

    /**
     * Checks a live API located at a target path (JAR or directory) against a baseline
     * {@link SignatureRecord}.
     *
     * @param baseline target baseline record
     * @param target   target path (JAR or directory)
     * @param excludes package prefixes to exclude; if {@code null} or empty, defaults to baseline excludes
     * @return unmodifiable list of violations found, or an empty list if compliant
     * @throws IOException if an error occurs reading the target
     */
    public static List<Violation> check(SignatureRecord baseline, Path target, Set<String> excludes) throws IOException {
        Objects.requireNonNull(target, "target must not be null");
        return check(baseline, target.toFile(), null, excludes);
    }

    /**
     * Convenience overload for {@link #check(SignatureRecord, Path, Set)} with no explicit excludes.
     */
    public static List<Violation> check(SignatureRecord baseline, Path target) throws IOException {
        return check(baseline, target, null);
    }

    /**
     * Checks a live API across the specified classpath entries against a baseline
     * {@link SignatureRecord}.
     *
     * @param baseline  target baseline record
     * @param classpath list of JAR files or directories to scan
     * @param excludes  package prefixes to exclude; if {@code null} or empty, defaults to baseline excludes
     * @return unmodifiable list of violations found, or an empty list if compliant
     * @throws IOException if an error occurs reading classpath entries
     */
    public static List<Violation> check(SignatureRecord baseline, List<File> classpath, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baseline, "baseline must not be null");
        Objects.requireNonNull(classpath, "classpath must not be null");

        Set<String> rootPackages = ClasspathUtil.discoverPackages(classpath);
        Set<String> effectiveExcludes = resolveExcludes(baseline, excludes);

        SignatureGenerator generator = new SignatureGenerator(classpath, rootPackages, effectiveExcludes);
        SignatureRecord live = generator.generate(baseline.apiVersion);
        return SignatureDiff.diff(baseline, live);
    }

    /**
     * Convenience overload for {@link #check(SignatureRecord, List, Set)} with no explicit excludes.
     */
    public static List<Violation> check(SignatureRecord baseline, List<File> classpath) throws IOException {
        return check(baseline, classpath, null);
    }

    /**
     * Loads a baseline from a file and checks the live API accessible via {@link ClassLoader}.
     */
    public static List<Violation> check(File baselineFile, ClassLoader classLoader, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baselineFile, "baselineFile must not be null");
        SignatureRecord baseline = SignatureIO.load(baselineFile);
        return check(baseline, classLoader, excludes);
    }

    /**
     * Convenience overload for {@link #check(File, ClassLoader, Set)} with no explicit excludes.
     */
    public static List<Violation> check(File baselineFile, ClassLoader classLoader) throws IOException {
        return check(baselineFile, classLoader, null);
    }

    /**
     * Loads a baseline from a path and checks the live API accessible via {@link ClassLoader}.
     */
    public static List<Violation> check(Path baselinePath, ClassLoader classLoader, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baselinePath, "baselinePath must not be null");
        SignatureRecord baseline = SignatureIO.load(baselinePath);
        return check(baseline, classLoader, excludes);
    }

    /**
     * Convenience overload for {@link #check(Path, ClassLoader, Set)} with no explicit excludes.
     */
    public static List<Violation> check(Path baselinePath, ClassLoader classLoader) throws IOException {
        return check(baselinePath, classLoader, null);
    }

    /**
     * Loads a baseline from a classpath resource and checks the live API accessible via {@link ClassLoader}.
     */
    public static List<Violation> check(String baselineResource, ClassLoader classLoader, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baselineResource, "baselineResource must not be null");
        Objects.requireNonNull(classLoader, "classLoader must not be null");
        SignatureRecord baseline = SignatureIO.loadResource(classLoader, baselineResource);
        return check(baseline, classLoader, excludes);
    }

    /**
     * Convenience overload for {@link #check(String, ClassLoader, Set)} with no explicit excludes.
     */
    public static List<Violation> check(String baselineResource, ClassLoader classLoader) throws IOException {
        return check(baselineResource, classLoader, null);
    }

    /**
     * Loads a baseline from a file and checks a target JAR or directory.
     */
    public static List<Violation> check(File baselineFile, File target, List<File> classpath, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baselineFile, "baselineFile must not be null");
        SignatureRecord baseline = SignatureIO.load(baselineFile);
        return check(baseline, target, classpath, excludes);
    }

    /**
     * Loads a baseline from a path and checks a target JAR or directory.
     */
    public static List<Violation> check(Path baselinePath, File target, List<File> classpath, Set<String> excludes) throws IOException {
        Objects.requireNonNull(baselinePath, "baselinePath must not be null");
        SignatureRecord baseline = SignatureIO.load(baselinePath);
        return check(baseline, target, classpath, excludes);
    }

    /**
     * Extracts all package names declared by classes or package records in the given baseline record.
     *
     * @param record the signature record to inspect
     * @return set of dot-separated package names, never {@code null}
     */
    public static Set<String> extractPackages(SignatureRecord record) {
        if (record == null) {
            return Collections.emptySet();
        }
        Set<String> packages = new HashSet<>();
        if (record.packages != null) {
            for (PackageRecord p : record.packages) {
                if (p != null && p.name != null && !p.name.trim().isEmpty()) {
                    packages.add(p.name.trim());
                }
            }
        }
        if (record.classes != null) {
            for (ClassRecord c : record.classes) {
                if (c != null && c.name != null) {
                    int lastDot = c.name.lastIndexOf('.');
                    if (lastDot > 0) {
                        packages.add(c.name.substring(0, lastDot).trim());
                    }
                }
            }
        }
        return packages;
    }

    private static Set<String> resolveExcludes(SignatureRecord baseline, Set<String> explicitExcludes) {
        if (explicitExcludes != null && !explicitExcludes.isEmpty()) {
            return explicitExcludes;
        }
        if (baseline != null && baseline.excludes != null && !baseline.excludes.isEmpty()) {
            return new HashSet<>(baseline.excludes);
        }
        return Collections.emptySet();
    }
}
