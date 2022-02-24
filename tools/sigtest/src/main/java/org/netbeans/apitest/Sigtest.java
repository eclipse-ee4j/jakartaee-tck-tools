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
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/** Ant task to execute setup, check and strict check with the API check class.
 * @author Michal Zlamal, Jaroslav Tulach
 */
public final class Sigtest extends Task {

    File fileName;
    Path classpath;
    String version;
    String packages;
    ActionType action;
    Boolean failOnError;
    File report;
    String failureProperty;
    String release;

    public void setFileName(File f) {
        fileName = f;
    }

    public void setPackages(String s) {
        packages = s;
    }

    public void setAction(ActionType s) {
        action = s;
    }

    public void setClasspath(Path p) {
        if (classpath == null) {
            classpath = p;
        } else {
            classpath.append(p);
        }
    }

    public void setVersion(String v) {
        version = v;
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public void setFailOnError(boolean b) {
        failOnError = b;
    }

    public void setFailureProperty(String p) {
        failureProperty = p;
    }

    public void setReport(File report) {
        this.report = report;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    @Override
    public void execute() throws BuildException {
        if (fileName == null) {
            throw new BuildException("FileName has to filed", getLocation());
        }
        if (packages == null) {
            throw new BuildException("Packages has to filed", getLocation());
        }
        if (action == null) {
            throw new BuildException("Action has to filed", getLocation());
        }
        if (classpath == null) {
            throw new BuildException("Classpath has to filed", getLocation());
        }

        SigtestHandler handler = new SigtestHandler() {
            @Override
            protected String getPackages() {
                return packages;
            }

            @Override
            protected File getFileName() {
                return fileName;
            }

            @Override
            protected String getAction() {
                return action.getValue();
            }

            @Override
            protected String getVersion() {
                return version;
            }

            @Override
            protected String[] getClasspath() {
                if (classpath == null) {
                    return null;
                } else {
                    return classpath.list();
                }
            }

            @Override
            protected File getReport() {
                return report;
            }

            @Override
            protected String getMail() {
                return getProject().getProperty("sigtest.mail");
            }

            @Override
            protected Boolean isFailOnError() {
                return failOnError;
            }

            @Override
            protected void logInfo(String msg) {
                getProject().log(msg, Project.MSG_VERBOSE);
            }

            @Override
            protected void logError(String msg) {
                getProject().log(msg, Project.MSG_ERR);
            }

            @Override
            protected Integer getRelease() {
                return ListCtSym.parseReleaseInteger(release);
            }

            @Override
            protected String[] getIgnoreJDKClassEntries() {
                return new String[0];
            }
        };
        int returnCode;
        try {
            returnCode = handler.execute();
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        boolean fail;
        if (report != null) {
            fail = Boolean.TRUE.equals(failOnError);
        } else {
            fail = !Boolean.FALSE.equals(failOnError);
        }
        if (returnCode != 0) {
            if (failureProperty != null) {
                getProject().setProperty(failureProperty, "true");
            } else {
                if (fail) {
                    throw new BuildException("Signature tests return code is wrong (" + returnCode + "), check the messages above", getLocation());
                } 
            }
            log("Signature tests return code is wrong (" + returnCode + "), check the messages above");
        }
    }

    /** One of <code>generate</code>,
        <code>check</code>,
        <code>strictcheck</code>,
        <code>versioncheck</code>,
        <code>binarycheck</code>.
        */
    public static final class ActionType extends EnumeratedAttribute {
        public String[] getValues() {
            return SigtestHandler.ACTIONS;
        }
    }
}
