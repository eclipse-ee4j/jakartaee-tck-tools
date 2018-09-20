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

package com.sun.ant.taskdefs.common;

import java.util.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import java.io.*;

public class JCOVGrabber {

    private static final String FILE_EXT = ".jcov";
    private int counter = 1;
    private int  port;
    private File coverageFile;
    private File resultDir;
    private String fileName;
    private boolean prependTestData;
    private Project project;
    private String jcovClasspath;

    public JCOVGrabber(String fileName, String data,
		       File resultDir, boolean prependTestdata,
		       Project project, String jcovClasspath) throws Exception {
	this.project = project;
	this.jcovClasspath = jcovClasspath;
	this.resultDir = resultDir;
	this.fileName = fileName;
	this.prependTestData = prependTestdata;
	StringTokenizer datum = new StringTokenizer(data, " \t\n\r\f:");
	if (datum.countTokens() == 1) { // must be a port
	    port = Integer.parseInt(datum.nextToken());
	} else if (datum.countTokens() == 2) {
	    coverageFile = new File(datum.nextToken());
	    port = Integer.parseInt(datum.nextToken());
	} else { //error
	    throw new Exception("Badly formed data \"" + data + "\"");
	}
	project.log("JCOVGrabber created [" + coverageFile + ", " + port + "]");
    }

    private String normalizeData(String data) {
	return data.replace(File.separatorChar, '-');
    }

    public void grab(String testArea) throws Exception {
	File resultFile = null;
	if (prependTestData) {
	   resultFile = new File(resultDir, fileName + "-" + normalizeData(testArea) + "-" + counter++ + FILE_EXT);
	} else {
	   resultFile = new File(resultDir, fileName + "-" + counter++ + FILE_EXT);
	}
	if (coverageFile != null) {
	    if (coverageFile.isFile()) {
		// copy coverage file to result file
		copy(coverageFile, resultFile);
		project.log("Coverage results written to \"" + resultFile.getPath() + "\"");
	    } else {
		project.log("Coverage file not found \"" + resultFile.getPath() + "\"");
	    }
	} else {
	    grabData(resultFile);
	}
    }

    private void copy(File source, File destination) throws FileNotFoundException, IOException {
	final int MAX_BUFFER_SIZE = 1024;
	byte[] buffer = new byte[MAX_BUFFER_SIZE];
	int bytesRead = 0;

	if (destination.isFile()) {
	    throw new FileNotFoundException("Destination file \"" + destination + "\"exists");
	}
	BufferedInputStream sourceStream = new BufferedInputStream(new FileInputStream(source));
	BufferedOutputStream destinationStream = new BufferedOutputStream(new FileOutputStream(destination));
	try {
	    while ((bytesRead = sourceStream.read(buffer, 0, buffer.length)) > 0) {
		destinationStream.write(buffer, 0, bytesRead);
	    }
	} finally {
	    sourceStream.close();
	    destinationStream.close();
	}
    }

    private void grabData(File resultFile) {
   	String outputFile = resultFile.getPath();
	Java command = (Java)(project.createTask("java"));
	command.setTaskName("Coverage_Grabber");
	command.setClasspath(new Path(project, jcovClasspath));
	command.setClassname("com.sun.tdk.jcov.grabber.Main");
	command.setFork(true);
 	Commandline.Argument args = command.createArg();
 	args.setLine("-port=" + port + " -once -verbose -output=" + outputFile);
	command.execute();
	project.log("Coverage results written to \"" + resultFile.getPath() + "\" from port " + port);
    }

}
