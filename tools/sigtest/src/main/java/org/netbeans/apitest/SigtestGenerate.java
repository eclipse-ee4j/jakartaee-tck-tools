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

/** Mojo to generate a {@code .sigtest} file.
 * <pre>
&lt;plugin&gt;
  &lt;groupId&gt;org.netbeans.tools&lt;/groupId&gt;
  &lt;artifactId&gt;sigtest-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;1.4&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;goals&gt;
        &lt;goal&gt;generate&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
  &lt;configuration&gt;
    &lt;release&gt;8&lt;/release&gt; &lt;!-- specify version of JDK API to use 6,7,8,...15 --&gt;
    &lt;packages&gt;org.yourcompany.app.api,org.yourcompany.help.api&lt;/packages&gt;
  &lt;/configuration&gt;
&lt;/plugin&gt;
 * </pre>
 *
 * @author Jaroslav Tulach
 */
@Mojo(
    name="generate",
    requiresDependencyResolution = ResolutionScope.COMPILE,
    defaultPhase= LifecyclePhase.PROCESS_CLASSES
)
public final class SigtestGenerate extends AbstractMojo {
    @Component
    private MavenProject prj;
    @Component 
    private MavenProjectHelper helper;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File classes;
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sigfile")
    private File sigfile;
    @Parameter(defaultValue = "")
    private String packages;
    @Parameter(property = "maven.compiler.release")
    private String release;
    /**
     * attach the generated file with extension .sigfile to the main artifact for deployment
     */
    @Parameter(defaultValue = "true")
    private boolean attach;
    
    /**
     * ignore JDK classes entries
     */
    @Parameter
    private String[] ignoreJDKClasses;

    private String version;

    public SigtestGenerate() {
    }

    SigtestGenerate(MavenProject prj, File classes, File sigfile, String packages, String version, String release) {
        this.prj = prj;
        this.classes = classes;
        this.sigfile = sigfile;
        this.packages = packages;
        this.version = version;
        this.release = release;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packages == null) {
            getLog().info("No packages specified, skipping sigtest:generate");
            return;
        }
        if (sigfile == null) {
            throw new MojoExecutionException("Specify <sigfile>path-to-file-generated-before</sigfile> in plugin config section!");
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
                return "generate";
            }

            @Override
            protected String getVersion() {
                return prj.getVersion();
            }

            @Override
            protected String[] getClasspath() {
                return SigtestCheck.projectClassPath(prj, classes);
            }

            @Override
            protected File getReport() {
                return null;
            }

            @Override
            protected String getMail() {
                return null;
            }

            @Override
            protected Boolean isFailOnError() {
                return null;
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
            int returnCode = handler.execute();
            if (returnCode != 0) {
                throw new MojoFailureException("Signature check for " + sigfile + " failed with " + returnCode);
            }
            getLog().info("Signature snapshot generated at " + sigfile);
            if (sigfile.exists() && attach) {
                helper.attachArtifact(prj, "sigfile", sigfile);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
