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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import java.io.File;
import java.util.List;

/**
 * Resolves a single API artifact to a file and publishes its absolute path in the project
 * property {@value #PROPERTY}, without adding it to the project or test classpath.
 *
 * <p>This supports APIs that cannot be on the test classpath (for example JSON-P and JSON-B,
 * which the signature framework itself uses to read baselines). Only the requested artifact is
 * resolved, with no transitive dependencies, because signature scanning reads bytecode with ASM
 * and never loads the scanned classes.
 *
 * <p>Surefire does not forward project properties set by other plugins, so the property must
 * be passed explicitly to the test JVM:
 * <pre>{@code
 * <plugin>
 *   <groupId>jakarta.tck</groupId>
 *   <artifactId>sigtest-next-plugin</artifactId>
 *   <version>1.0-SNAPSHOT</version>
 *   <executions>
 *     <execution>
 *       <goals><goal>resolve</goal></goals>
 *       <configuration>
 *         <groupId>jakarta.json</groupId>
 *         <artifactId>jakarta.json-api</artifactId>
 *         <version>${jsonp.version}</version>
 *       </configuration>
 *     </execution>
 *   </executions>
 * </plugin>
 * <plugin>
 *   <artifactId>maven-surefire-plugin</artifactId>
 *   <configuration>
 *     <systemPropertyVariables>
 *       <sigtest-next.resolve.api>${sigtest-next.resolve.api}</sigtest-next.resolve.api>
 *     </systemPropertyVariables>
 *   </configuration>
 * </plugin>
 * }</pre>
 *
 * <p>Only one resolved API per module is supported; the property name is fixed.
 */
@Mojo(
        name = "resolve",
        defaultPhase = LifecyclePhase.PROCESS_TEST_RESOURCES,
        requiresDependencyResolution = ResolutionScope.NONE
)
public class ResolveMojo extends AbstractMojo {

    /** Name of the project property that receives the resolved jar path. */
    static final String PROPERTY = "sigtest-next.resolve.api";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> remoteRepositories;

    /** {@code groupId} of the artifact to resolve. */
    @Parameter(required = true)
    private String groupId;

    /** {@code artifactId} of the artifact to resolve. */
    @Parameter(required = true)
    private String artifactId;

    /** {@code version} of the artifact to resolve. */
    @Parameter(required = true)
    private String version;

    /** Optional classifier of the artifact to resolve. */
    @Parameter
    private String classifier;

    /** Packaging type (file extension) of the artifact to resolve. */
    @Parameter(defaultValue = "jar")
    private String type;

    @Override
    public void execute() throws MojoExecutionException {
        Artifact requested = new DefaultArtifact(groupId, artifactId, classifier, type, version);
        ArtifactRequest request = new ArtifactRequest(requested, remoteRepositories, null);

        File file;
        try {
            ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request);
            file = result.getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(
                    "Failed to resolve " + requested + ": " + e.getMessage(), e);
        }

        if (file == null || !file.isFile()) {
            throw new MojoExecutionException("Resolved artifact " + requested + " has no associated file.");
        }

        String path = file.getAbsolutePath();
        project.getProperties().setProperty(PROPERTY, path);
        getLog().info("sigtest-next:resolve " + requested + " -> " + path);
    }
}
