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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

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
    private MavenSession session;
    @Component
    private ArtifactResolver artifactResolver;
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classes;
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sigfile")
    private File sigfile;
    @Parameter(defaultValue = "")
    private String packages;

    @Parameter(property = "maven.compiler.release")
    private String release;
    @Parameter(property = "sigtest.releaseVersion")
    private String releaseVersion;
    @Parameter(defaultValue = "check", property = "sigtest.check")
    private String action;
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports/sigtest/TEST-${project.build.finalName}.xml")
    private File report;
    @Parameter(defaultValue = "true", property = "sigtest.fail")
    private boolean failOnError;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packages == null) {
            getLog().info("No packages specified, skipping sigtest:compare " + action);
            return;
        }
        if (releaseVersion == null) {
            throw new MojoExecutionException("Specify <releaseVersion in plugin config section or use -Dsigtest.releaseVersion!");
        }
        if (classes == null || !classes.exists()) {
            throw new MojoExecutionException("Point <classes>to-directory-with-classfiles-to-test</classes> in plugin config section!");
        }

        final DefaultArtifact artifact = new DefaultArtifact(prj.getGroupId(), prj.getArtifactId(), releaseVersion, null, "jar", "", new DefaultArtifactHandler("jar"));
        try {
            artifactResolver.resolve(artifact, session.getProjectBuildingRequest().getRemoteRepositories(), session.getLocalRepository());
        } catch (AbstractArtifactResolutionException ex) {
            throw new MojoExecutionException("Cannot resolve " + artifact, ex);
        }

        SigtestGenerate generate = new SigtestGenerate(prj, artifact.getFile(), sigfile, packages, releaseVersion, release);
        generate.execute();

        SigtestCheck check = new SigtestCheck(prj, classes, sigfile, action, packages, report, failOnError);
        check.execute();
    }
}
