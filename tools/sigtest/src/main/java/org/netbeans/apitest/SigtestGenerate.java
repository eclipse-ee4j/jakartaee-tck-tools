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
import java.util.ArrayList;
import java.util.List;
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

/**
 *
 * @author Jaroslav Tulach
 */
@Mojo(
    name="generate",
    requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase= LifecyclePhase.PACKAGE
)
public final class SigtestGenerate extends AbstractMojo {
    @Component
    private MavenProject prj;

    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File classes;
    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.sigfile")
    private File sigfile;
    @Parameter(defaultValue = "")
    private String packages;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packages == null) {
            throw new MojoExecutionException("Specify <packages>your.pkg1:your.pkg2</packages> in plugin config section!");
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
                List<String> path = new ArrayList<String>();
                path.add(classes.getAbsolutePath());
                for (Artifact a : prj.getArtifacts()) {
                    if (a.getFile() != null && a.getFile().exists()) {
                        path.add(a.getFile().getAbsolutePath());
                    }
                }
                return path.toArray(new String[0]);
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
        };
        try {
            int returnCode = handler.execute();
            if (returnCode != 0) {
                throw new MojoFailureException("Signature check for " + sigfile + " failed with " + returnCode);
            }
            getLog().info("Signature snapshot generated at " + sigfile);
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }
}
