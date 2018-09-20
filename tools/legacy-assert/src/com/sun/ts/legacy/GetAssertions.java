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

package com.sun.ts.legacy;

import com.sun.javatest.*;
import com.sun.javatest.finder.ChameleonTestFinder;
import com.sun.ts.lib.util.*;
import java.io.*;
import java.util.*;

public class GetAssertions extends ChameleonTestFinder {

    private static final String TS_HOME              = "ts.home";
    private static final String TS_TOOLS_HOME        = "ts.tools.home";
    private static final String INITIAL_DIR          = "initial.dir";
    private static final String TEST_FINDER_MAP_FILE = "map.jtc";
    private static final String RELATIVE_TOOLS_DIR   = "/tools/legacy-assert/";
    private static final String RELATIVE_TEST_DIR    = "/src/com/sun/ts/tests";
    private static final String FILE_FLAG            = "-f";
    private static final String NL                   = 
	System.getProperty("line.separator");

    static String lastTestName;
    static String lastClassName;

    boolean xmlformat           = true;
    String  currentArea;
    String  lastArea;
    String  sClass;

    public GetAssertions() {
        super();
    }

    private static String getRootDirectory() {
	return System.getProperty(TS_HOME) + RELATIVE_TEST_DIR;
    }

    private static String getInitialDir() {
	String initialDir = System.getProperty(INITIAL_DIR);
	if (initialDir == null ) {
	    initialDir = getRootDirectory();
	} else {
	    initialDir = getRootDirectory() + initialDir;
	}
	return initialDir;
    }

    private static String getTestFinderMapFile() {
	return System.getProperty(TS_TOOLS_HOME) + RELATIVE_TOOLS_DIR + TEST_FINDER_MAP_FILE;
    }

    private static TestFinderQueue initTestFinder(String rootDir, String initDir) throws Exception {
        TestFinderQueue tfq  = new TestFinderQueue();
        GetAssertions tf     = new GetAssertions();
        String[] sArgs       = {FILE_FLAG, getTestFinderMapFile()};
	File fRoot           = new File(rootDir);
	File[] fInitialFiles = new File[] { new File(initDir) };
	TestEnvironment env  = new TestEnvironment("assertion grabber",
						   new Hashtable(),
						   "assertions");
	env.putUrlAndFile("testSuiteRoot", fRoot);
	env.putUrlAndFile("testSuiteRootDir", fRoot);
	tf.init(sArgs, fRoot, env);
	tfq.setTestFinder(tf);
	/*
	 * This setInitialFiles method was removed from the JavaTest
	 * public API from version 3.0.2 to version 3.1.  The
	 * setTests(String[]) method appears to be the functionally
	 * equivalent new method.
	 */
	//	tfq.setInitialFiles(fInitialFiles);
	tfq.setTests(new String[] {initDir});
	return tfq;
    }

    public static void main(String[] argv) {

	String initialDir = getInitialDir();
	String rootDir    = getRootDirectory();
	System.out.println("initialDir: " + initialDir);
	System.out.println("rootDir   : " + rootDir);
	TestFinderQueue tfq = null;
	try {
	    tfq = initTestFinder(rootDir, initialDir);
	} catch (Exception e) {
	    System.err.println("Exception initializing the TSTestFinder");
            e.printStackTrace();
	    System.exit(1);
	}

	QueueHandler handler = QueueHandlerFactory.instance().getHandler();
	try {
	    handler.handle(tfq);
	} catch (Exception e) {
	    System.err.println("Exception in queue handler");
            e.printStackTrace();
	    System.exit(1);
	}
    }

} // end class GetAssertions
