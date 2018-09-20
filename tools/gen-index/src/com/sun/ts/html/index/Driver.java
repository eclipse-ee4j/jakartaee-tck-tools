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

package com.sun.ts.html.index;

import com.sun.cts.common.*;
import java.io.*;
import java.util.*;

public class Driver {

    public  static final String API_ASSERTIONS  = "api-assertions.html";
    public  static final String SPEC_ASSERTIONS = "spec-assertions.html";
    public  static final String EX_SUMMARY_FILE = "exec-summary.html";
    private static final String FILE_TO_INDEX   = "summary.html";
    private static final String INDEX_FILE      = "index.html";
    private static final String DOC_INDEX_FILE  = "assertion-index.html";

    private FileFinder finder;
    private FileFinder finderDoc;
    private File       rootDir;
    private File       indexFile;
    private File       indexFileDoc;

    public Driver(String[] args) throws FileNotFoundException {
	rootDir = new File(args[0]);
	if (args.length > 1) {
	    indexFile = new File(args[1]);
	    indexFileDoc = new File(args[1] + ".doc");
	} else {
	    indexFile = new File(rootDir, INDEX_FILE);
	    indexFileDoc = new File(rootDir, DOC_INDEX_FILE);
	}
	System.out.println("### Looking for summary files in \"" + rootDir + "\"");
	finder = new FileFinder(rootDir, new SummaryFilter());
	finderDoc = new FileFinder(rootDir, new SummaryFilterDoc());
    }

    public void go() {
	finder.process();
	List summaryFiles = finder.getFileList();
	System.out.println(summaryFiles);
	RelativeFileIndexer indexer = new RelativeFileIndexer(rootDir, indexFile, summaryFiles);
	System.out.println("### Building index file \"" + indexFile + "\"");
	indexer.build();
    
	finderDoc.process();
	summaryFiles = finderDoc.getFileList();
	System.out.println(summaryFiles);
	RelativeFileIndexer2 indexer2 = new RelativeFileIndexer2(rootDir, indexFileDoc, summaryFiles);
	System.out.println("### Building index file \"" + indexFileDoc + "\"");
	indexer2.build();
    }

    private static class SummaryFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
	    File newFile = new File(dir, name);
	    if (name.equalsIgnoreCase("bak")) {
		System.out.println("NOT Accepting Backup File \"" + newFile + "\"");
		return false;
	    }
	    return newFile.isDirectory() ||
		name.equals(API_ASSERTIONS) ||
		name.equals(SPEC_ASSERTIONS) ||
		name.equals(EX_SUMMARY_FILE) ||
		name.equals(FILE_TO_INDEX);
	}
    }

    private static class SummaryFilterDoc implements FilenameFilter {
	public boolean accept(File dir, String name) {
	    File newFile = new File(dir, name);
	    if (name.equalsIgnoreCase("bak")) {
		System.out.println("NOT Accepting Backup File \"" + newFile + "\"");
		return false;
	    }
	    return newFile.isDirectory() ||
		name.equals(API_ASSERTIONS) ||
		name.equals(SPEC_ASSERTIONS);
	}
    }

    public static void main(String[] args) throws Exception {
	System.out.println("### Indexer Starting...");
	Driver driver = new Driver(args);
	driver.go();
	System.out.println("### Driver done.");
    }

} // end class Driver
