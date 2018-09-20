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

package javasoft.sqe.apiCheck;

import java.util.*;
import java.io.*;

public class BatchSetup {

    private static String MAP_FILE_PROP_NAME = "map.file.path";
    private static String SIG_REP_DIR        = "signature.repository.dir";
    private static String SIG_EXT            = ".sig_";
    private static String PACKAGE_FLAG       = "-package";
    private static String OUTPUT_FILE_FLAG   = "-out";

    private List       defaultArgs   = new LinkedList();
    private String     repositoryDir;
    private String     mapFile;
    private Properties recordPackages;

    private void addDefaultArgs(String[] args) {
	int numArgs = (args != null) ? args.length : 0;
	for (int i = 0; i < numArgs; i++) {
	    defaultArgs.add(args[i]);
	}
    }

    public BatchSetup(String[] args) throws Exception {
	addDefaultArgs(args);
	this.repositoryDir = System.getProperty(SIG_REP_DIR);
	if (this.repositoryDir == null) {
	    throw new Exception("Error, the property \"" +
				SIG_REP_DIR + "\" is not defined.");
	}
	if (!this.repositoryDir.endsWith(File.separator)) {
	    this.repositoryDir = this.repositoryDir + File.separator;
	}
	this.mapFile = System.getProperty(MAP_FILE_PROP_NAME);
	if (this.mapFile == null) {
	    throw new Exception("Error, the property \"" +
				MAP_FILE_PROP_NAME + "\" is not defined.");
	}
	InputStream in = new FileInputStream(this.mapFile);
	this.recordPackages = new Properties();
	this.recordPackages.load(in);
    }

    private String createOutputFile(String packageName, String version) {
	return repositoryDir + packageName + SIG_EXT + version;
    }

    private String[] createArgs(String packageName, String outFile) {
	List args = new LinkedList(defaultArgs);
	args.add(PACKAGE_FLAG);
	args.add(packageName);
	args.add(OUTPUT_FILE_FLAG);
	args.add(outFile);
	return (String[])(args.toArray(new String[args.size()]));
    }

    private void invokeSetup(String[] args) {
	System.out.println("Invoking javasoft.sqe.apiCheck.Setup with args:");
	System.out.println("\t" + Arrays.asList(args));
	Setup.main(args);
    }

    public void go() throws Exception {
	for (Enumeration e = recordPackages.propertyNames();
	     e.hasMoreElements(); )
	{
	    String packageName = (String)(e.nextElement());
	    String version = recordPackages.getProperty(packageName);
	    String sigOutputFile = createOutputFile(packageName, version);
	    String[] args = createArgs(packageName, sigOutputFile);
	    invokeSetup(args);
	}
    }

    public static void main(String[] args) {
	try {
	    BatchSetup bs = new BatchSetup(args);
	    bs.go();
	} catch(Exception e) {
	    System.err.println("Error: " + e);
	}
    }

} // end class BatchSetup
