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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

/** Ant task to execute setup, check and strict check with the API check class.
 * @author Michal Zlamal, Jaroslav Tulach
 */
public final class Sigtest extends Task {

    File fileName;
    Path classpath;
    String packages;
    String action;
    boolean failOnError = true;
    
    public void setFileName(File f) {
        fileName = f;
    }
    
    public void setPackages(String s) {
        packages = s;
    }

    public void setAction(String s) {
        action = s;
    }

    public void setClasspath(Path p) {
        if (classpath == null) {
            classpath = p;
        } else {
            classpath.append(p);
        }
    }
    public Path createClasspath () {
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
        
        if (packages.equals("-")) {
            log("No public packages, skipping");
            return;
        }

        List<String> arg = new ArrayList<String>();
        arg.add("-FileName");
        arg.add(fileName.getAbsolutePath());
        if (action.equals("Setup")) {
            arg.add("-setup");
        } else if (action.equals("Check")) {
            // no special arg for check
        } else if (action.equals("StrictCheck")) {
            arg.add("-maintenance");
        } else {
            throw new BuildException("Unknown action: " + action);
        }
        
        File outputFile = null;
        String s = getProject().getProperty("sigtest.output.dir");
        if (s != null) {
            File dir = getProject().resolveFile(s);
            dir.mkdirs();
            outputFile = new File(dir, fileName.getName().replace(".sig", "").replace("-", "."));
            log(outputFile.toString());
        }
        
        
        log("Packages: " + packages);
        StringTokenizer packagesTokenizer = new StringTokenizer(packages,",");
        while (packagesTokenizer.hasMoreTokens()) {
            String p = packagesTokenizer.nextToken().trim();
            String prefix = "-PackageWithoutSubpackages "; // NOI18N
            //Strip the ending ".*"
            int idx = p.lastIndexOf(".*");
            if (idx > 0) {
                p = p.substring(0, idx);
            } else {
                idx = p.lastIndexOf(".**");
                if (idx > 0) {
                    prefix = "-Package "; // NOI18N
                    p = p.substring(0, idx);
                }
            }
            arg.add(prefix.trim());
            arg.add(p);
        }
        
        if (classpath != null) {
            StringBuffer sb = new StringBuffer();
            String pref = "";
            for (String e : classpath.list()) {
                sb.append(pref);
                sb.append(e);
                pref = File.pathSeparator;
            }
            arg.add("-Classpath");
            arg.add(sb.toString());
        }
        
        int returnCode = Main.run(arg.toArray(new String[0])).getType();
        
        if (returnCode != 0) {
            if (failOnError && outputFile == null) {
                throw new BuildException("Signature tests return code is wrong (" + returnCode + "), check the messages above", getLocation());
            }
            else {
                log("Signature tests return code is wrong (" + returnCode + "), check the messages above");
            }
        } else {
            if (outputFile != null) {
                outputFile.delete();
            }
        }
    }

}
