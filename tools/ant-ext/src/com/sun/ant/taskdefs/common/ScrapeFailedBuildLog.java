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
public class ScrapeFailedBuildLog extends Task {

    protected static final String NL = System.getProperty("line.separator",
	    "\n");

    protected File buildlog;
    protected File secondarybuildlog;
    protected String proprietaryerrors;
    protected String proprietaryerrorsexist;
    protected String faileddir;   // return propname
    protected String buildpassed; //return propname
    protected boolean inBuildFailed = false;
    protected boolean buildFailed = false;
    protected boolean proprietaryErrors = false;
	StringBuffer proprietaryErrorList = new StringBuffer();

    public void setBuildlog(File buildlog) {
    	this.buildlog = buildlog;
    }

    public void setSecondaryBuildlog(File secondarybuildlog) {
    	this.secondarybuildlog = secondarybuildlog;
    }

    public void setBuildpassed(String buildpassed) {
	this.buildpassed = buildpassed;
    }

    public void setFaileddir(String faileddir) {
	this.faileddir = faileddir;
    }

    public void setProprietaryerrorsexist(String proprietaryerrorsexist) {
    	this.proprietaryerrorsexist = proprietaryerrorsexist;
    }

    public void setProprietaryerrors(String proprietaryerrors) {
    	this.proprietaryerrors = proprietaryerrors;
    }

    public void execute() throws BuildException {
	checkPreconditions();
	processLog();
	processSecondaryBuildLog();
    }

    protected void processSecondaryBuildLog() {
    	if (secondarybuildlog == null) {
            System.err.println("Secondary build log is null, skipping the " +
    		"check for proprietary APIs in the second build log.");
            return;
    	}
    	if (!secondarybuildlog.isFile()) {
            System.err.println("Secondary build log [\"" + secondarybuildlog
              + "\"] does not exist, skipping the " +
    		  "check for proprietary APIs in the second build log.");
            return;
    	}
    	BufferedReader in = null;
    	String line = null;
    	try {
    	    in = new BufferedReader(new FileReader(secondarybuildlog));
    	    while ((line = in.readLine()) != null) {
    		    String proprietaryResult = checkLine0(line);
                if (proprietaryResult != null) {
                    proprietaryErrorList.append(proprietaryResult + NL + NL);
                }
    	    }
    	    if (proprietaryErrors) {
    		    project.setProperty(proprietaryerrorsexist, "true");
    	        project.setProperty(proprietaryerrors, proprietaryErrorList.toString());
    	    } else {
    		    project.setProperty(proprietaryerrorsexist, "false");
    	    }
    	} catch (Exception e) {
    	    throw new BuildException(e);
    	} finally {
    	    try {
    		in.close();
    	    } catch (Exception ee) { /*do nothing*/ }
    	}
    }
    
    protected void checkPreconditions() throws BuildException {
	if (buildlog == null) {
	    throw new BuildException(
		    "Error, \"buildlog\" attribute must be specified");
	}
	if (!buildlog.isFile()) {
	    throw new BuildException("Error, build log file \"" + buildlog
		    + "\" does not exist");
	}
	if (buildpassed == null || buildpassed.length() == 0) {
	    throw new BuildException(
		    "Error, \"buildpassed\" attribute must be specified");
	}
	if (faileddir == null || faileddir.length() == 0) {
	    throw new BuildException(
		    "Error, \"faileddir\" attribute must be specified");
	}
	if (proprietaryerrors == null || proprietaryerrors.length() == 0) {
	    throw new BuildException(
		    "Error, \"proprietaryerrors\" attribute must be specified");
	}
	if (proprietaryerrorsexist == null || proprietaryerrorsexist.length() == 0) {
	    throw new BuildException(
		    "Error, \"proprietaryerrorsexist\" attribute must be specified");
	}
    }

    protected void processLog() throws BuildException {
	BufferedReader in = null;
	String line = null;
	StringBuffer failedDirList = new StringBuffer();
	try {
	    in = new BufferedReader(new FileReader(buildlog));
	    while ((line = in.readLine()) != null) {
		    String result = checkLine(line);
		    String proprietaryResult = checkLine0(line);
		    if (result != null) {
		        failedDirList.append(result + NL);
	        }
            if (proprietaryResult != null) {
                proprietaryErrorList.append(proprietaryResult + NL + NL);
            }
	    }
	    if (buildFailed) {
		    project.setProperty(buildpassed, "false");
	        project.setProperty(faileddir, failedDirList.toString());
	    } else {
		    project.setProperty(buildpassed, "true");		
	    }
	    if (proprietaryErrors) {
		    project.setProperty(proprietaryerrorsexist, "true");
	        project.setProperty(proprietaryerrors, proprietaryErrorList.toString());
	    } else {
		    project.setProperty(proprietaryerrorsexist, "false");
	    }
	    
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try {
		in.close();
	    } catch (Exception ee) { /*do nothing*/ }
	}
    }

    protected String checkLine0(String line) {
    	String result = null;
    	String searchString = "[ts.javac]";
    	String searchString2 = "proprietary API";
    	int index = line.indexOf(searchString);
    	int index2 = line.indexOf(searchString2);

    	if (index != -1 && index2 != -1) {
    		proprietaryErrors = true;
    	    result = line;
    	}
    	return result;
    }

    protected String checkLine(String line) {
	String result = null;
	String searchString = "[dosubdirs]";
	String searchString2 = "BUILD FAILED";
	int index = line.indexOf(searchString);
	int index2 = line.indexOf(searchString2);

	if (index != -1 && index2 != -1) {
	    inBuildFailed = true;
	    buildFailed =  true;
	    result = line;
	} else if (inBuildFailed && index != -1) {
	    result = line;
	} else {
	    inBuildFailed = false;
	}
	return result;
    }

}
