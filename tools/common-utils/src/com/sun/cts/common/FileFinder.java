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

package com.sun.cts.common;

import java.io.*;
import java.util.*;

public class FileFinder {

    private File            rootDir;   // root dir where processing begins
    private FileAcceptor    handler;   // callback handler
    private FilenameFilter  filter;    // filter files before callback
    private boolean         batchMode; // place all files in a list when true
    private List            files;     // list to collect files in batch mode

    private static class DefaultFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
	    return true;
	}
    } // end class DefaultFilter

    private void checkRoot(File root) throws FileNotFoundException {
	if (root == null || !root.exists() || !root.isDirectory()) {
	    throw new FileNotFoundException("\"" + root + "\" does not exist.");
	}
    }

    public FileFinder(File rootDir, FileAcceptor handler)
	throws FileNotFoundException 
    {
	this(rootDir, handler, new DefaultFilter());
    }

    public FileFinder(File rootDir, FileAcceptor handler, FilenameFilter filter)
	throws FileNotFoundException 
    {
	checkRoot(rootDir);
	this.rootDir = rootDir;
	this.handler = handler;
	this.filter  = filter;
    }

    public FileFinder(File rootDir, FilenameFilter filter)
	throws FileNotFoundException 
    {
	this(rootDir, null, filter);
	setBatchMode(true);
    }

    public void setBatchMode(boolean enable) {
	batchMode = enable;
	files = new ArrayList();
    }

    public List getFileList() {
	return files;
    }

    private void processFile(File file) {
	if (batchMode) {
	    files.add(file);
	} else {
	    handler.acceptFile(file);
	}
    }

    private void processDir(File file) {
	if (batchMode) {
	    files.add(file);
	} else {
	    handler.acceptFile(file);
	}
    }

    private void traverse(File dir, boolean processDirs) {
	File[] files = dir.listFiles(filter);
	int numFiles = (files == null) ? 0 : files.length;
	for (int i = 0; i < numFiles; i++) {
	    File file = files[i];
	    if (file.isFile()) {
		processFile(file);
	    } else if (file.isDirectory()) {
		if (processDirs) {
		    processDir(file);
		} else {
		    traverse(file, false);
		}
	    } else {
		System.err.println("Neither a file nor directory \"" +
				   file + "\"" + " skipping...");
	    }
	}
    }

    public void process() {
	traverse(rootDir, false);
    }

    public void processWithDirs() {
	traverse(rootDir, true);
    }

} // end class FileFinder
