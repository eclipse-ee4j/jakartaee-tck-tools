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
import java.io.File;
import java.io.FileInputStream;

public class RunCTSPostProcessor implements RunCTSProcIntf {

    private boolean inited = false;
    private File tsHome;
    private File j2eeHome;
    private File spsHome;
    private File jcovPropsFile;
    private File jcovJar;
    private File resultDir;
    private boolean prependTestData = true;
    private List grabbers = new ArrayList();

    private void init(Properties props, Project project) throws Exception {
	FileInputStream f = null;
	try {
	    tsHome = new File(props.getProperty("ts.home"));
	    j2eeHome = new File(props.getProperty("j2ee.home"));
	    spsHome = new File(props.getProperty("sps.home"));
	    jcovPropsFile = new File(tsHome, "internal" + File.separator + "coverage"
				     + File.separator + "jcov.properties");
	    jcovJar = new File(spsHome, "codecoverage" + File.separator + "jcov" +
			       File.separator + "lib" + File.separator + "jcov.jar");
	    Properties jcovProps = new Properties();
	    f = new FileInputStream(jcovPropsFile);
	    jcovProps.load(f);
	    resultDir = new File(jcovProps.getProperty("coverage.results.dir"));
	    if (resultDir.isDirectory() && resultDir.list().length > 0) {
		throw new Exception("Results directory exists and is not empty \"" + resultDir.getPath() + "\"");
	    } else if (resultDir.isFile()) {
		throw new Exception("Results directory is a file \"" + resultDir.getPath() + "\"");
	    } else if (!resultDir.exists()) {
		resultDir.mkdirs();
	    }
	    prependTestData = Boolean.valueOf
		(jcovProps.getProperty("append.test.area.info.to.result.file")).booleanValue();
	    
	    Enumeration propNames = jcovProps.propertyNames();
	    while(propNames.hasMoreElements()) {
		String propName = (String)(propNames.nextElement());
		if (propName.equals("coverage.results.dir") ||
		    propName.equals("append.test.area.info.to.result.file")) {
		    continue;
		} else {
		    grabbers.add(new JCOVGrabber(propName, jcovProps.getProperty(propName),
						 resultDir, prependTestData, project, jcovJar.getPath()));
		}
	    }
	} finally {
	    try { f.close(); } catch(Exception e) {} // do nothing
	}
    }
    
    public boolean execute(String currentTestDir, Project project, Properties props) throws Exception {
	project.log("***** POST PROCESSOR starting test area \"" + currentTestDir + "\"");
	if (!inited) {
	    init(props, project);
	    inited = true;
	    project.log("***** Init Called");
	}
	String testArea = removeTSHome(currentTestDir);
	int numGrabbers = grabbers.size();
	for (int i = 0; i < numGrabbers; i++) {
	    JCOVGrabber grabber = (JCOVGrabber)grabbers.get(i);
	    grabber.grab(testArea);
	}
	project.log("***** POST PROCESSSOR done test area \"" + currentTestDir + "\"");	
	return true;
    }

    private String removeTSHome(String testarea) {
	int length = tsHome.getPath().length() + "/src/com/sun/ts/tests/".length();
	if (length >= testarea.length()) {
	    return testarea;
	}
	return testarea.substring(length);
    }

}
