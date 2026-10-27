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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Generates a structured JSON signature file describing the public API of a
 * dependency artifact.
 *
 * <p>The target artifact is identified by its Maven coordinates
 * ({@code groupId:artifactId}); it must already be listed as a
 * {@code compile}-scope dependency of the consuming project. All packages
 * present in that JAR are scanned by default; individual packages can be
 * excluded via {@code <excludes>}.
 *
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
     */
    @Parameter(required = true)
    private String groupId;

    /**
     * {@code artifactId} of the dependency artifact to scan.
     */
    @Parameter(required = true)
    private String artifactId;

    /**
     * Output JSON signature file.
     */
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sig.json", required = true)
    private File sigfile;

    /**
     * Comma-separated list of package prefixes to exclude from the scan.
     * Supports the same {@code .*} / {@code .**} suffixes as the legacy
     * sigtest plugin.
     */
    @Parameter(defaultValue = "")
    private String excludes;

    /**
     * Version string embedded in the generated file.
     * Defaults to the resolved version of the scanned artifact.
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
        Artifact target = resolveArtifact();

        File jar = target.getFile();
        if (jar == null || !jar.isFile()) {
            throw new MojoExecutionException(
                    "Artifact " + groupId + ":" + artifactId + " has no associated file. "
                    + "Ensure it is listed as a compile-scope dependency.");
        }

        String resolvedVersion = apiVersion != null ? apiVersion : target.getVersion();

        Set<String> rootPackages = discoverPackages(jar);
        if (rootPackages.isEmpty()) {
            getLog().warn("sigtest-next:generate — no packages found in " + jar.getName() + ", skipping.");
            return;
        }

        Set<String> excludePackages = parsePackages(excludes);

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

    /**
     * Reads the top-level package names directly present in the JAR.
     * Only packages that contain at least one {@code .class} file are returned;
     * sub-packages are not enumerated separately — the generator handles
     * sub-package traversal internally.
     */
    static Set<String> discoverPackages(File jar) throws MojoExecutionException {
        Set<String> packages = new HashSet<>();
        try (JarFile jf = new JarFile(jar)) {
            java.util.Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.startsWith("module-info")
                        || name.contains("package-info")) {
                    continue;
                }
                int lastSlash = name.lastIndexOf('/');
                if (lastSlash > 0) {
                    // Convert "jakarta/servlet/http/HttpServlet.class" → "jakarta.servlet.http"
                    packages.add(name.substring(0, lastSlash).replace('/', '.'));
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read JAR: " + jar, e);
        }
        return packages;
    }

    /**
     * Parse a comma/colon/semicolon-separated list of package tokens into a set
     * of plain package names, stripping trailing {@code .*} / {@code .**}.
     */
    static Set<String> parsePackages(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        StringTokenizer st = new StringTokenizer(raw, ",:;");
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.endsWith(".**")) {
                token = token.substring(0, token.length() - 3);
            } else if (token.endsWith(".*")) {
                token = token.substring(0, token.length() - 2);
            }
            if (!token.trim().isEmpty()) {
                result.add(token);
            }
        }
        return result;
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
