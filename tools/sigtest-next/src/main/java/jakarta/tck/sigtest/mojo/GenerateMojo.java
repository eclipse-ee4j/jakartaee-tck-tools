/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package jakarta.tck.sigtest.mojo;

import jakarta.tck.sigtest.generate.SignatureGenerator;
import jakarta.tck.sigtest.model.SignatureRecord;
import jakarta.tck.sigtest.util.PackageUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates a structured JSON signature file describing the public API of either:
 * <ul>
 *   <li>a <b>dependency artifact</b> — identified by {@code <groupId>} and
 *       {@code <artifactId>}, resolved from the project's compile-scope dependencies, or</li>
 *   <li>the <b>project's own compiled classes</b> — when {@code <classes>} is
 *       pointed at a directory (e.g. {@code ${project.build.outputDirectory}}).
 *       In this mode {@code <groupId>} and {@code <artifactId>} are not required.</li>
 * </ul>
 *
 * <p>All packages present in the scan target are included by default; individual
 * packages can be excluded via {@code <excludes>}.
 *
 * <h2>Scanning a dependency JAR</h2>
 * <pre>{@code
 * <plugin>
 *   <groupId>jakarta.tck</groupId>
 *   <artifactId>sigtest-next-plugin</artifactId>
 *   <version>1.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals><goal>generate</goal></goals>
 *     </execution>
 *   </executions>
 *   <configuration>
 *     <!-- coordinates of the dependency JAR to scan -->
 *     <groupId>jakarta.servlet</groupId>
 *     <artifactId>jakarta.servlet-api</artifactId>
 *     <!-- optional: exclude internal sub-packages -->
 *     <excludes>jakarta.servlet.internal</excludes>
 *   </configuration>
 * </plugin>
 * }</pre>
 *
 * <h2>Scanning the project's own output directory</h2>
 * <pre>{@code
 * <plugin>
 *   <groupId>jakarta.tck</groupId>
 *   <artifactId>sigtest-next-plugin</artifactId>
 *   <version>1.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals><goal>generate</goal></goals>
 *     </execution>
 *   </executions>
 *   <configuration>
 *     <!-- scan the module being built, not a dependency -->
 *     <classes>${project.build.outputDirectory}</classes>
 *     <packages>com.example.api</packages>
 *   </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(
        name = "generate",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class GenerateMojo extends AbstractMojo {

    @Component
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * {@code groupId} of the dependency artifact to scan.
     * Not required when {@link #classes} is set.
     */
    @Parameter
    private String groupId;

    /**
     * {@code artifactId} of the dependency artifact to scan.
     * Not required when {@link #classes} is set.
     */
    @Parameter
    private String artifactId;

    /**
     * Directory of compiled {@code .class} files to scan instead of resolving a
     * dependency artifact. When set, {@link #groupId} and {@link #artifactId} are
     * ignored. Typically {@code ${project.build.outputDirectory}}.
     *
     * <p>When scanning a classes directory the package set is <em>not</em>
     * auto-discovered from a JAR manifest; use {@link #packages} to list the
     * root packages explicitly, or rely on the directory walk to pick up all
     * {@code .class} files.
     */
    @Parameter
    private File classes;

    /**
     * Output JSON signature file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sig.json", required = true)
    private File sigfile;

    /**
     * Optional directory to write the signature file into, keeping the file name of
     * {@link #sigfile}. When set, it replaces the parent directory of {@link #sigfile}.
     *
     * <p>Use {@code ${project.build.testOutputDirectory}} to put the baseline on the test
     * resource classpath (for {@code SignatureAssert}), or
     * {@code ${project.build.outputDirectory}} to package it with the main artifact.
     */
    @Parameter
    private File outputDirectory;

    /**
     * Comma-separated list of package prefixes to <em>include</em> in the scan.
     * When scanning a classes directory this acts as a positive filter; when
     * scanning a JAR it is used as an override over the auto-discovered package set.
     * Supports the same {@code .*} / {@code .**} suffixes as the legacy sigtest plugin.
     * If empty, all packages discovered in the JAR (or directory) are included.
     */
    @Parameter(defaultValue = "")
    private String packages;

    /**
     * Comma-separated list of package prefixes to exclude from the scan.
     * Supports the same {@code .*} / {@code .**} suffixes as the legacy
     * sigtest plugin.
     */
    @Parameter(defaultValue = "")
    private String excludes;

    /**
     * Version string embedded in the generated file.
     * Defaults to {@code ${project.version}} when scanning a classes directory,
     * or to the resolved version of the scanned artifact otherwise.
     */
    @Parameter
    private String apiVersion;

    /**
     * Attach the generated {@code .sig.json} file to the project for deployment.
     */
    @Parameter(defaultValue = "true")
    private boolean attach;

    // -------------------------------------------------------------------------

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (outputDirectory != null) {
            sigfile = new File(outputDirectory, sigfile.getName());
        }
        if (classes != null) {
            executeDirectory();
        } else {
            executeArtifact();
        }
    }

    /** Scan a pre-compiled classes directory (the module being built). */
    private void executeDirectory() throws MojoExecutionException, MojoFailureException {
        if (!classes.isDirectory()) {
            throw new MojoExecutionException(
                    "<classes> directory does not exist or is not a directory: "
                    + classes.getAbsolutePath()
                    + ". Ensure the compile phase has run before generate.");
        }

        String resolvedVersion = apiVersion != null ? apiVersion : project.getVersion();

        Set<String> rootPackages = PackageUtil.parsePackages(packages);
        if (rootPackages.isEmpty()) {
            // Fall back to discovering packages from the directory.
            try {
                rootPackages = PackageUtil.discoverPackagesFromDirectory(classes);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to discover packages in " + classes + ": " + e.getMessage(), e);
            }
        }
        if (rootPackages.isEmpty()) {
            getLog().warn("sigtest-next:generate — no packages found in " + classes + ", skipping.");
            return;
        }

        Set<String> excludePackages = PackageUtil.parsePackages(excludes);

        getLog().info("sigtest-next:generate scanning directory: " + classes);
        getLog().info("sigtest-next:generate discovered packages: " + rootPackages.stream().sorted()
                .collect(java.util.stream.Collectors.toList()));
        if (!excludePackages.isEmpty()) {
            getLog().info("sigtest-next:generate excluding packages: " + excludePackages);
        }
        getLog().info("sigtest-next:generate output: " + sigfile);

        List<File> classpath = buildClasspath();
        // Prepend the classes directory itself so the generator finds all types.
        if (!classpath.contains(classes)) {
            classpath.add(0, classes);
        }

        SignatureGenerator generator = new SignatureGenerator(classpath, rootPackages, excludePackages);

        SignatureRecord record;
        try {
            record = generator.generate(resolvedVersion, sigfile.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan " + classes + ": " + e.getMessage(), e);
        }

        getLog().info("sigtest-next:generate found " + record.classes.size() + " API classes.");

        if (attach && sigfile.exists()) {
            projectHelper.attachArtifact(project, "sig.json", sigfile);
            getLog().info("sigtest-next:generate attached artifact: " + sigfile.getName());
        }
    }

    /** Scan a dependency artifact JAR resolved from the project's dependencies. */
    private void executeArtifact() throws MojoExecutionException, MojoFailureException {
        if (groupId == null || artifactId == null) {
            throw new MojoExecutionException(
                    "Either <classes> (directory) or both <groupId> and <artifactId> must be configured.");
        }

        Artifact target = resolveArtifact();

        File jar = target.getFile();
        if (jar == null || !jar.isFile()) {
            throw new MojoExecutionException(
                    "Artifact " + groupId + ":" + artifactId + " has no associated file. "
                    + "Ensure it is listed as a compile-scope dependency.");
        }

        String resolvedVersion = apiVersion != null ? apiVersion : target.getVersion();

        // Allow an explicit <packages> list to override auto-discovery.
        Set<String> rootPackages = PackageUtil.parsePackages(packages);
        if (rootPackages.isEmpty()) {
            try {
                rootPackages = PackageUtil.discoverPackages(jar);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot read JAR: " + jar, e);
            }
        }
        if (rootPackages.isEmpty()) {
            getLog().warn("sigtest-next:generate — no packages found in " + jar.getName() + ", skipping.");
            return;
        }

        Set<String> excludePackages = PackageUtil.parsePackages(excludes);

        getLog().info("sigtest-next:generate scanning artifact: " + jar.getName());
        getLog().info("sigtest-next:generate discovered packages: " + rootPackages.stream().sorted()
                .collect(java.util.stream.Collectors.toList()));
        if (!excludePackages.isEmpty()) {
            getLog().info("sigtest-next:generate excluding packages: " + excludePackages);
        }
        getLog().info("sigtest-next:generate output: " + sigfile);

        // Classpath: the scanned JAR plus all other compile dependencies for
        // type resolution (SignatureGenerator uses ASM, so loading is not needed,
        // but keeping the list available for future reference / extension).
        List<File> classpath = buildClasspath();

        SignatureGenerator generator = new SignatureGenerator(classpath, rootPackages, excludePackages);

        SignatureRecord record;
        try {
            record = generator.generate(resolvedVersion, sigfile.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to scan " + jar + ": " + e.getMessage(), e);
        }

        getLog().info("sigtest-next:generate found " + record.classes.size() + " API classes.");

        if (attach && sigfile.exists()) {
            projectHelper.attachArtifact(project, "sig.json", sigfile);
            getLog().info("sigtest-next:generate attached artifact: " + sigfile.getName());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Find the compile-scope artifact matching {@link #groupId}/{@link #artifactId}. */
    private Artifact resolveArtifact() throws MojoExecutionException {
        for (Artifact a : project.getArtifacts()) {
            if (groupId.equals(a.getGroupId()) && artifactId.equals(a.getArtifactId())) {
                return a;
            }
        }
        throw new MojoExecutionException(
                "Could not find artifact " + groupId + ":" + artifactId + " in the project's "
                + "resolved dependencies. Add it as a <dependency> with scope 'compile'.");
    }

    /** Build the full compile classpath: scanned artifact + all other compile artifacts. */
    private List<File> buildClasspath() {
        List<File> cp = new ArrayList<>();
        for (Artifact a : project.getArtifacts()) {
            if (a.getFile() != null && a.getFile().exists()) {
                cp.add(a.getFile());
            }
        }
        return cp;
    }

}
