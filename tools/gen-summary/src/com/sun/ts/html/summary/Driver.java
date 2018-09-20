/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ts.html.summary;

import com.sun.cts.common.*;
import java.io.*;
import java.util.*;

public class Driver {

    private static final String SUMMARY_FILE  = "exec-summary.html";
    public  static final String TESTED_FILE   = "tested.xml";
    public  static final String UNTESTED_FILE = "untested.xml";
    public  static final String COV_SUM_FILE  = "summary.html";

    private File       rootDir;
    private FileFinder finder;
    private File       summaryFile;

    public Driver(String[] args) throws FileNotFoundException {
	rootDir = new File(args[0]);
	if (args.length > 1) {
	    summaryFile = new File(args[1]);
	} else {
	    summaryFile = new File(rootDir, SUMMARY_FILE);
	}
	System.out.println("### Looking for tested and untested XML files in \""
			   + rootDir + "\"");
	finder = new FileFinder(rootDir, new FileFilter());
    }

    public void go() {
	finder.process();
	List allTestFiles = finder.getFileList();
	System.out.println(allTestFiles);
	//	BuilderIntf sb = new SummaryBuilder(rootDir, summaryFile, allTestFiles);
	BuilderIntf sb = new SummaryBuilderHTML(rootDir, summaryFile, allTestFiles);
	System.out.println("### Building executive summary file \"" + summaryFile + "\"");
	sb.build();	
    }

    private static class FileFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
	    File newFile = new File(dir, name);
	    if (name.equalsIgnoreCase("bak")) {
		System.out.println("NOT Accepting Backup File \"" + newFile + "\"");
		return false;
	    }
	    return newFile.isDirectory() ||
		name.equals(COV_SUM_FILE);
	    //		name.equals(TESTED_FILE) ||
	    //		name.equals(UNTESTED_FILE);
	}
    }

    public static void main(String[] args) throws Exception {
	System.out.println("### Summary Creator Starting...");
	Driver driver = new Driver(args);
	driver.go();
	System.out.println("### Summary Creator Done.");
    }

} // end class Driver
