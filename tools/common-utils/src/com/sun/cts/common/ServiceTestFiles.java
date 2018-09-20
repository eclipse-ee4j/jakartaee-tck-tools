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

/**
 * $Id$
 */

package com.sun.cts.common;

import java.io.*;
import java.util.*;

/*
 * to run it:
 *~/TS_ws/src/com/sun/ts/tests >java -cp $HOME/tools_ws/tools/common-utils/classes com.sun.cts.common.ServiceTestFiles .
 */

public class ServiceTestFiles {
    private File startDir;
    private File output = new File("/tmp/servicetest.list");
    private PrintWriter writer;
    private Set serviceTestDirs = new HashSet();
    private static Set excludes = new HashSet();
    private static FileFilter subdirFilter;
    private static FileFilter javaFilter;
    static {
	excludes.add("SCCS");
	excludes.add("CVS");
	excludes.add("tmp");
	excludes.add("temp");
	excludes.add("cts_dep");
	excludes.add("ts_dep");
	excludes.add("contentRoot");
	excludes.add("goldenfiles");
	excludes.add("bak");
	excludes.add("workDir");
	excludes.add("JTwork");
	excludes.add("JTreport");

	javaFilter = new FileFilter() {
	    public boolean accept(File pathname) {
		String name = pathname.getName();
		if(!pathname.isFile()) {
		    return false;
		}
		return name.endsWith(".java");
	    }
	};
	subdirFilter = new FileFilter() {
	    public boolean accept(File pathname) {
		String name = pathname.getName();
		for(Iterator it = excludes.iterator(); it.hasNext();) {
		    if(name.equalsIgnoreCase((String) it.next())) {
			return false;
		    }
		}
		if(pathname.isFile()) {
		    return false;
		}
		return true;
	    }
	};
    }
    /**
     * starting from ts.home/src/com/sun/ts/tests, find all service test
     * java source files
     *
     * fails on all exceptions
     */
    public static void main(String[] args) throws Exception {
	ServiceTestFiles finder = new ServiceTestFiles();
	finder.startMain(args);
    }

    public void startMain(String[] args) throws Exception {
	if(args.length == 0) {
	    throw new IllegalArgumentException("start dir not specified.");
	} else if(args.length > 1) {
	    System.out.println("only one arg (start dir) is needed.");
	}
	startDir = new File(args[0]);
	try {
	    initOutput();
	    visit(startDir);
        writer.println();
        writer.println("=============================================");
        writer.println("The following dirs contain service tests:");
        List dirList = new ArrayList(serviceTestDirs);
        Collections.sort(dirList);
        for(int i = 0, n = dirList.size(); i < n; i++) {
            writer.println((String) dirList.get(i));
        }
	} finally {
	    if(writer != null) {
		writer.close();
	    }
	}
    }

    private void initOutput() throws Exception {
	if(output.exists() && output.length() > 0) {
	    if(!output.delete()) {
		System.out.println("Cannot delete output file:"
		+ output.getPath());
	    }
	}
	writer = new PrintWriter(new BufferedWriter(new FileWriter(output)));
    }

    private void visit(File dir) throws Exception {
	File[] subdirs = dir.listFiles(subdirFilter);
	if(subdirs.length == 0) {
	    doLeaf(dir);
	} else {
	    for (int i = 0; i < subdirs.length; i++) {
		visit(subdirs[i]);
	    }
	}
    }

    private void doLeaf(File leafDir) throws Exception {
	File[] javaFiles = leafDir.listFiles(javaFilter);
	for (int i = 0; i < javaFiles.length; i++) {
	    handleJavaFile(javaFiles[i]);
	}
    }

    private void handleJavaFile(File javaFile) throws Exception {
        if(isServiceTest(javaFile)) {
	    writer.println(javaFile.getPath());
        serviceTestDirs.add(javaFile.getParent());
        }
    }

    protected boolean isServiceTest( File tfile ) {
    	boolean bServiceTest=false;
    	boolean bFileContainsTestTags=false;
	String line=null;
        BufferedReader br =null;
    	StringBuffer buffer = new StringBuffer();
    	try {
        	br = new BufferedReader(new FileReader(tfile));
    		while ( ( line=br.readLine()) != null)
    		{
       			line = line.trim();

				if (line.length()  > 0 &&
				((line.indexOf("extends ServiceEETest") != -1) ||
				(line.indexOf("extends MultiTest") != -1) ||
                                (line.indexOf("extends JAXRCommonClient") != -1) ||

				(line.indexOf("extends SAXBaseTest") != -1) ||
				(line.indexOf("extends SAXParserFactoryTest") != -1) ||
				(line.indexOf("extends SAXParserTest") != -1) ||
				(line.indexOf("extends DocumentTypeTest") != -1) ||
				(line.indexOf("extends NodeTest") != -1) ||
				(line.indexOf("extends DocumentTest") != -1) ||
				(line.indexOf("extends NamedNodeMapTest") != -1) ||
				(line.indexOf("extends TextTest") != -1) ||
				(line.indexOf("extends ElementTest") != -1) ||
				(line.indexOf("extends XmlMultiTest") != -1) ||
				(line.indexOf("extends DocumentBuilderFactoryTest") != -1) ||
				(line.indexOf("extends com.sun.ts.lib.harness.ServiceEETest") != -1)))
       			{
					bServiceTest = true;
       			}

				//assume that a testname tag will always come after the public class declaration
				if (line.length()  > 0 &&
					((line.indexOf("@testName") != -1) &&
					(line.indexOf(":") != -1)))
				{
					bFileContainsTestTags = true;
					break;
				}
    		}
    	} catch ( Exception e) {
       		e.printStackTrace();
    	}
		finally
		{
			try
			{
				if(br != null)
					br.close();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
	//double check for j2eetools
	//tfile must be a file not a directory
	if(bFileContainsTestTags) {
	    if(bServiceTest) {
		return true;
	    }
	    String filePath = tfile.getPath();
	    String toFind = "/j2eetools/";
	    int pos = filePath.replace('\\','/').indexOf(toFind);
	    return (pos != -1);
	} else {
	    return false;
	}
    }

}
