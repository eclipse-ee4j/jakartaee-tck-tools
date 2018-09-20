/*
 * Copyright (c) 2008, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * $Id$
 */


package  com.sun.ant.taskdefs.common;

import  java.io.*;
import  java.util.*;
import  org.apache.tools.ant.*;
import  org.apache.tools.ant.taskdefs.*;
import  com.sun.ant.TSLogger;


public class DoSubdirs extends Task {
    private String todo;
    private String srcdir;
    private String includes;
    private String excludes;
    private boolean failonerror=false;
    //arrays used to set ds.  They are not attributes
    private String[] includesArray;
    private String[] excludesArray;
    
    private static List failedDirs;
    
    public void setIncludes (String inc) {
        includes = inc;
    }

    public void setExcludes (String exc) {
        excludes = exc;
    }

    private void myinit () {
        if (excludes != null && excludes.length() != 0) {
            StringTokenizer st1 = new StringTokenizer(excludes, ", \t\n\r\f");
            int tokens1 = st1.countTokens();
            excludesArray = new String[tokens1];
            for (int i = 0; i < tokens1; i++) {
                String item = st1.nextToken();
		if(item.endsWith("build.xml")) {
		    excludesArray[i] = item;
		    continue;
		}
                if (!item.endsWith("/") && !item.endsWith("\\")) {
                    item += "/";
                }
                excludesArray[i] = item;
            }
        }
        if (includes == null || includes.length() == 0) {
            includesArray = new String[1];
            includesArray[0] = "**/build.xml";
        }
        else {
            StringTokenizer st2 = new StringTokenizer(includes, ", \t\n\r\f");
            int tokens2 = st2.countTokens();
            includesArray = new String[tokens2];
            for (int i = 0; i < tokens2; i++) {
                String item = st2.nextToken();
                if (item.endsWith("build.xml")) {
                    includesArray[i] = item;
                    continue;
                }
                if (!item.endsWith("/") && !item.endsWith("\\")) {
                    item += "/";
                }
                item += "**/build.xml";
                includesArray[i] = item;
            }
        }       //includes else
    }

    public void execute () throws BuildException {
	File currentDir = null;  //used only for logging
        String[] buildfiles = null;
	//failedDirs = null;
        try {
            myinit();
            DirectoryScanner ds = new DirectoryScanner();
            ds.setIncludes(includesArray);
            ds.setExcludes(excludesArray);
            ds.setBasedir(new File(srcdir));
            ds.scan();
            buildfiles = ds.getIncludedFiles();
        } catch (Throwable th) {
	    th.printStackTrace();
	    TSLogger.addFailedDir("While initializing dosubidrs and scanning.");
	    }
        for (int i = 0; i < buildfiles.length; i++) {
            try {
                File leafDir = new File(srcdir, buildfiles[i]).getParentFile();
                currentDir = leafDir;
                Ant antTask = new Ant();
                antTask.setProject(project);
                antTask.init();
                antTask.setInheritAll(false);
                antTask.setAntfile("build.xml");
                antTask.setDir(leafDir);
                antTask.setTarget(this.todo);
                Property prop = antTask.createProperty();
                prop.setName("called.by.dosubdirs");
                prop.setValue("true");
                String msg = "Entering " + leafDir.getPath();
                log(msg);
                antTask.execute();
                antTask = null;
            } catch (Throwable th) {
                th.printStackTrace();
                addFailedDir(currentDir == null ? "Unknown" : currentDir.getPath());
            }
        }
        logFailedDirs();
    }

    public void setTodo (String todo) {
        this.todo = todo;
    }

    public void setSrcdir (String srcdir) {
        this.srcdir = srcdir;
    }

    public void setFailonerror(String foe) {
        this.failonerror = Boolean.parseBoolean(foe);
    }

    private void logFailedDirs()
    {
        StringBuffer sb = null;
        if(failedDirs != null) {
            sb = new StringBuffer();
            for (int i = 0, n = failedDirs.size(); i < n; i++) {
                sb.append(failedDirs.get(i).toString()).append('\n');
            }
	    
            System.err.println("BUILD FAILED");
            System.err.println("The following directories failed:");
            System.err.println(sb.toString());
	    }
        
        // if we have any directories that failed to compile and
        // the attribute failBuild is set to true then we
        // need to notify Ant that the build failed.
        if (failedDirs != null && failonerror==true) {
            throw new BuildException("ERROR: Some directories failed to build." +
            "  See list of failed directories above.");
        }
    }
    
    public static void addFailedDir(String s) {
	if(failedDirs == null) {
	    failedDirs = new ArrayList();
	}
	if(!failedDirs.contains(s)) {
	    failedDirs.add(s);
	}
    }
}



