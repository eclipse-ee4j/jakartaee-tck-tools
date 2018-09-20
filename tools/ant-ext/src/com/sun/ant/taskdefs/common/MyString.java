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


package com.sun.ant.taskdefs.common;

import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

public class MyString extends Task  {
    private String returnVal;
    private String thisVal;
    private String methodName;
    private String arg1;
    private String arg2;
    private String arg3;
    private String arg4;
    private String arg5;

    public void setArg1(String s) {
	this.arg1 = s;
    }
    public void setArg2(String s) {
	this.arg2 = s;
    }
    public void setArg3(String s) {
	this.arg3 = s;
    }
    public void setArg4(String s) {
	this.arg4 = s;
    }
    public void setArg5(String s) {
	this.arg5 = s;
    }
    public void setReturn(String s) {
	returnVal = s;
    }
    public void setThis(String s) {
	thisVal = s;
    }
    public void setMethod(String s) {
	methodName = s;
    }
    public void execute() throws BuildException {
	if(this.thisVal == null) {
	    throw new BuildException("thisVal in string task not set.");
	}
	//why not using reflection? cannot determine type for args.
	if(methodName.equals("replace")) {
	    replace();
	} else if (methodName.equals("indexOf")) {
	    indexOf();
	} else if (methodName.equals("length")) {
	    length();
	} else if (methodName.equals("substring")) {
	    substring();
	}
    }

    private void length() {
	if(returnVal == null || returnVal.length() == 0) {
	    throw new BuildException("returnVal or arg1 in string task not set.");
	}
	int index = thisVal.length();
	String result = String.valueOf(index);
	if(project.getProperties().containsKey(returnVal)) {
	    log("Property " + returnVal + " already defined in project.");
	}
	project.setProperty(returnVal, result);
    }

    private void substring() {
	if(returnVal == null || returnVal.length() == 0
	   || arg1 == null || arg1.length() == 0 ) {
	    throw new BuildException("returnVal or arg1 in string task not set.");
	}
	String result = null;
	try {
	    int a1 = Integer.parseInt(arg1);
	    if (arg2 != null && arg2.length() > 0) {
		int a2 = Integer.parseInt(arg2);
		result = thisVal.substring(a1, a2);
	    } else {
		result = thisVal.substring(a1);
	    }
	} catch (Exception e) {
	    throw new BuildException(e);
	}
	if(project.getProperties().containsKey(returnVal)) {
	    log("Property " + returnVal + " already defined in project.");
	}
	project.setProperty(returnVal, result);
    }

    private void indexOf() {
	if(returnVal == null || returnVal.length() == 0
	   || arg1 == null || arg1.length() == 0 ) {
	    throw new BuildException("returnVal or arg1 in string task not set.");
	}
	int index = thisVal.indexOf(arg1);
	String result = String.valueOf(index);
	if(project.getProperties().containsKey(returnVal)) {
	    log("Property " + returnVal + " already defined in project.");
	}
	project.setProperty(returnVal, result);
    }

    private void replace() {
	if(returnVal == null || returnVal.length() == 0
	    || arg1 == null || arg1.length() == 0
	    || arg2 == null || arg2.length() == 0) {
	    throw new BuildException("returnVal, arg1, or arg2 in string task not set.");
	}
	String result = thisVal.replace(arg1.charAt(0), arg2.charAt(0));
	if(project.getProperties().containsKey(returnVal)) {
	    log("Property " + returnVal + " already defined in project.");
	}
	project.setProperty(returnVal, result);
    }
}
