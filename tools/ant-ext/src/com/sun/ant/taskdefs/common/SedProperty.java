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
public class SedProperty extends Sed {

    private String property;
    private String currentProp;
    private Properties props = new Properties();

    public void setProperty(String prop) {
	property = prop;
    }

    public void execute() throws BuildException {
	InputStream in = null;
	try {
	    in = new FileInputStream(thefile);
	    props.load(in);
	    super.execute();
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { in.close(); } catch(Exception ee) {} // do nothing
	}
    }

    protected void checkPreconditions0() throws BuildException {
	if (property == null || property.length() == 0) {
	    throw new BuildException("Error property not set, must set to a valid property name");
	}
    }

    private boolean isValidProp(String aProp) {
	boolean result = false;
	for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
	    String prop = (String)e.nextElement();
	    if (aProp.equals(prop)) {
		result = true;
		break;
	    }
	}
	return result;
    }

    protected String filterString(String line) {
	String result = line;
	int equalsIndex = line.indexOf("=");
	if (equalsIndex != -1) {
	    String temp = line.substring(0, equalsIndex).trim();
	    if (isValidProp(temp)) {
		log("Changing prop state from \"" + currentProp + "\" to \"" + temp + "\"",
		    project.MSG_VERBOSE);
		currentProp = temp;
	    }
	}
	if (property.equalsIgnoreCase(currentProp)) {
	    result = line.replaceAll(findthis, replacewith);
	}
	return result;
    }

} // end class SedProperty
