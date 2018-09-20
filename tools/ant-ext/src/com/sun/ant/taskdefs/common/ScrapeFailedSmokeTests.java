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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import org.apache.tools.ant.Project;

/**
 * Sed command that can be invoked from ant.
 */
public class ScrapeFailedSmokeTests extends Task {

    protected static final String NL = System.getProperty("line.separator", "\n");

    protected File   smokelog;
    protected String property;

    public void setSmokelog(File smokelog) {
	this.smokelog = smokelog;
    }

    public void setProperty(String property) {
	this.property = property;
    }

    public void execute() throws BuildException {
	checkPreconditions();
	processLog();
    }

    protected void checkPreconditions()  throws BuildException {
	if (smokelog == null) {
	    throw new BuildException("Error, \"smokelog\" attribute must be specified");
	}
	if (!smokelog.isFile()) {
	    throw new BuildException("Error, smoke log file \"" + smokelog + "\" does not exist");
	}
	if (property == null || property.length() == 0) {
	    throw new BuildException("Error, \"property\" attribute must be specified");
	}
    }

    protected void processLog() throws BuildException {
	BufferedReader in = null;
	String line = null;
	StringBuffer failedTestList = new StringBuffer();
	try {
	    in = new BufferedReader(new FileReader(smokelog));
	    while((line = in.readLine()) != null) {
		String result = checkLine(line);
		if (result != null) {
		    failedTestList.append(result + NL + NL);
		}
	    }
	    project.setProperty(property, failedTestList.toString());
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { in.close(); } catch(Exception ee) {} // do nothing
	}
    }

    protected String checkLine(String line) {
	String result = null;
	String searchString = "[java] FAILED";
	int index = line.indexOf(searchString);
	if (index != -1) {
	    result = line;
	}
	return result;
    }

}
