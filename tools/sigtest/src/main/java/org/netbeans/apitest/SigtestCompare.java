/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package org.netbeans.apitest;

import java.io.File;
import java.util.List;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 *
 * @author Jaroslav Tulach
 */
@Mojo(
    name="compare",
    requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase= LifecyclePhase.PACKAGE
)
public final class SigtestCompare extends AbstractMojo {
    @Component
    private MavenProject prj;
    @Component
    private LegacySupport legacySupport;
    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepos;
    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File classes;
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sigfile")
    private File sigfile;
    @Parameter(defaultValue = "")
    private String packages;

    @Parameter(property = "sigtest.releaseVersion")
    private String releaseVersion;
    @Parameter(defaultValue = "check", property = "sigtest.check")
    private String action;
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports/sigtest/TEST-${project.build.finalName}.xml")
    private File report;
    @Parameter(defaultValue = "true")
    private boolean failOnError;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packages == null) {
            throw new MojoExecutionException("Specify <packages>your.pkg1:your.pkg2</packages> in plugin config section!");
        }
        if (releaseVersion == null) {
            throw new MojoExecutionException("Specify <releaseVersion in plugin config section or use -Dsigtest.releaseVersion!");
        }
        if (sigfile == null) {
            throw new MojoExecutionException("Specify <sigfile>path-to-file-generated-before</sigfile> in plugin config section!");
        }
        if (classes == null || !classes.exists()) {
            throw new MojoExecutionException("Point <classes>to-directory-with-classfiles-to-test</classes> in plugin config section!");
        }

        ArtifactRequest artifactRequest = new ArtifactRequest();
        final DefaultArtifact defaultArtifact = new DefaultArtifact(prj.getGroupId(), prj.getArtifactId(), "jar", releaseVersion);
        artifactRequest.setArtifact(defaultArtifact);
        List<RemoteRepository> repositories = RepositoryUtils.toRepos(remoteRepos);
        artifactRequest.setRepositories(repositories);
        Artifact artifact;
        try {
            ArtifactResult result = repoSystem.resolveArtifact(legacySupport.getSession().getRepositorySession(), artifactRequest);
            artifact = result.getArtifact();
        } catch (ArtifactResolutionException ex) {
            throw new MojoExecutionException("Cannot resolve artifact" + defaultArtifact, ex);
        }

        SigtestGenerate generate = new SigtestGenerate(prj, artifact.getFile(), sigfile, packages, releaseVersion);
        generate.execute();

        SigtestCheck check = new SigtestCheck(prj, classes, sigfile, action, packages, report, failOnError);
        check.execute();
    }
}
