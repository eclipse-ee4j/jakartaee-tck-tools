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
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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

/** Mojo to check {@code .class} files against an existing {@code .sigtest}
 * file.
 * <pre>
&lt;plugin&gt;
  &lt;groupId&gt;org.netbeans.tools&lt;/groupId&gt;
  &lt;artifactId&gt;sigtest-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;1.3&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;goals&gt;
        &lt;goal&gt;check&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
  &lt;configuration&gt;
    &lt;packages&gt;org.yourcompany.app.api,org.yourcompany.help.api&lt;/packages&gt;
    &lt;releaseVersion&gt;1.3&lt;/releaseVersion&gt;
    &lt;release&gt;8&lt;/release&gt; &lt;!-- specify version of JDK API to use 6,7,8,...15 --&gt;
  &lt;/configuration&gt;
&lt;/plugin&gt;
 * </pre>
 *
 * @author Jaroslav Tulach
 */
@Mojo(
    name="check",
    requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase= LifecyclePhase.TEST
)
public final class SigtestCheck extends AbstractMojo {
    @Component
    private MavenProject prj;
    @Component
    private MavenSession session;
    @Component
    private ArtifactResolver artifactResolver;

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File classes;
    @Parameter()
    private File sigfile;
    @Parameter(property = "maven.compiler.release")
    private String release;
    @Parameter(property = "sigtest.releaseVersion")
    private String releaseVersion;
    @Parameter(defaultValue = "check", property = "sigtest.check")
    private String action;
    @Parameter(defaultValue = "")
    private String packages;
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports/sigtest/TEST-${project.build.finalName}.xml")
    private File report;
    @Parameter(defaultValue = "true", property = "sigtest.fail")
    private boolean failOnError;
    /**
     * ignore JDK classes entries
     */
    @Parameter
    private String[] ignoreJDKClasses;

    public SigtestCheck() {
    }

    SigtestCheck(MavenProject prj, File classes, File sigfile, String action, String packages, File report, boolean failOnError) {
        this.prj = prj;
        this.classes = classes;
        this.sigfile = sigfile;
        this.action = action;
        this.packages = packages;
        this.report = report;
        this.failOnError = failOnError;
    }



    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packages == null) {
            getLog().info("No packages specified, skipping sigtest:check " + action);
            return;
        }
        if (sigfile == null) {
            if (releaseVersion == null) {
                throw new MojoExecutionException(
                    "Specify <sigfile>path-to-file-generated-before</sigfile> in plugin config section!\n"
                  + "Or specify <releaseVersion>version-to-compare to</releaseVersion> to download sigfile generated and attached by 'generate' target previously"
                );
            }
        }
        if (classes == null || !classes.exists()) {
            throw new MojoExecutionException("Point <classes>to-directory-with-classfiles-to-test</classes> in plugin config section!");
        }
        SigtestHandler handler = new SigtestHandler() {
            @Override
            protected String getPackages() {
                return packages;
            }

            @Override
            protected File getFileName() {
                return sigfile;
            }

            @Override
            protected String getAction() {
                return action;
            }

            @Override
            protected String getVersion() {
                return prj.getVersion();
            }

            @Override
            protected String[] getClasspath() {
                return projectClassPath(prj, classes);
            }

            @Override
            protected File getReport() {
                return report;
            }

            @Override
            protected String getMail() {
                return null;
            }

            @Override
            protected Boolean isFailOnError() {
                return failOnError;
            }

            @Override
            protected void logInfo(String message) {
                getLog().info(message);
            }

            @Override
            protected void logError(String message) {
                getLog().error(message);
            }

            @Override
            protected Integer getRelease() {
                return ListCtSym.parseReleaseInteger(release);
            }

            @Override
            protected String[] getIgnoreJDKClassEntries() {
                return ignoreJDKClasses;
            }
        };
        try {
            if (sigfile == null) {
                Artifact artifact = new DefaultArtifact(prj.getGroupId(), prj.getArtifactId(), releaseVersion, null, "sigfile", "", new DefaultArtifactHandler("sigfile"));
                try {
                    artifactResolver.resolve(artifact, session.getProjectBuildingRequest().getRemoteRepositories(), session.getLocalRepository());
                    sigfile = artifact.getFile();
                } catch (AbstractArtifactResolutionException ex) {
                    throw new MojoExecutionException("Cannot download " + artifact, ex);
                }
            }

            int returnCode = handler.execute();
            if (returnCode != 0) {
                throw new MojoFailureException("Signature check for " + sigfile + " failed with " + returnCode);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    static String[] projectClassPath(MavenProject project, File classes) {
        Set<String> path = new LinkedHashSet<String>();
        path.add(classes.getAbsolutePath());
        path.add(project.getBuild().getOutputDirectory());
        for (Artifact a : project.getArtifacts()) {
            if (a.getFile() != null && a.getFile().exists()) {
                path.add(a.getFile().getAbsolutePath());
            }
        }
        return path.toArray(new String[0]);
    }


}
