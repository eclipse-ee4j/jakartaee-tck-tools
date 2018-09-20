/*
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ant.taskdefs.common;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;

public class GetPortingClasses extends Task  {
    private String property = "porting.classes";
    private String defaultIncludes = "src/com/sun/ts/lib/**/porting/**/*.java";
    private FileSet fileSet;  //currently only one fileset

    public void setProperty(String propName) {
	property = propName;
    }
    public FileSet createFileSet() {
	fileSet = new FileSet();
	return fileSet;
    }

    public void execute() {
	String tsHome = project.getProperty("ts.home");
	File fTsHome = new File(tsHome);
	if(fileSet == null) {
	    fileSet = new FileSet();
	    fileSet.setDir(fTsHome);
	    fileSet.setIncludes(defaultIncludes);
	}
	String[] sFiles = fileSet.getDirectoryScanner(project).getIncludedFiles();
	if(sFiles == null) {
	    throw new BuildException("No source file found.");
	}
	String val = "";
	//ant has bug that ignores sourcepath.
	String baseDir = fileSet.getDir(project).getPath();
	for (int i = 0; i < sFiles.length; i++) {
	    val = val + baseDir + "/" + sFiles[i] + ",";
	}
	if(val.endsWith(",")) {
	    val = val.substring(0, val.length() - 1);
	}
        project.setProperty(property, val);
        //after ant 1.4.1, getProperties returns a defensive copy.
	//Hashtable p = project.getProperties();
	//if(p.containsKey(property)) {
	//    project.log("WARN: " + property + " exits. Will overide it");
	//}
	//p.put(property, val);
//	String ath = null;
//	try {
//	    ath = fTsHome.getCanonicalPath();
//	} catch (IOException ex) {
//	    ex.printStackTrace();
//	    throw new BuildException("Failed to get canonical path for ts.home");
//	}
//	if(ath != null) {
//	    p.put("absolute.ts.home", ath);
//	}
    }
}
