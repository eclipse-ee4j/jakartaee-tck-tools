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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

/*
 * usage examples:
 * <target name="check.props">
 *   <checkprops quiet="true">
 *     <property name="user.name" value="${user.name}"/>
 *     <property name="password" value="${password}"/>
 *   </checkprops>
 * </target>
 *
 * If either user.name or password or both is not set, ant will
 * throw BuildException and exit.  Therefore, this task should
 * only be used for mandatory properties.
 *
 * If quiet attribute is set to "false" or not set, ant will
 * only print names of unset properties.  If quiet is set to
 * "true", ant will print all property names and values, along
 * with the failure message, if any.
 *
 */
public class CheckProps extends Task  {
    private ArrayList properties = new ArrayList();
    private boolean quiet;
    /*
    public static class PropVal {
	private String val;
	public String getVal() {
	    return val;
	}
	public void setVal(String newVal) {
	    val = newVal;
	}
    }
    */

    public void addProperty(Property prop) {
	    properties.add(prop);
    }

    public void setQuiet(boolean q) {
	    quiet = q;
    }

    public void execute() throws BuildException {
	String notSet = "";
	for(Iterator it = properties.iterator(); it.hasNext();) {
	    Property p = (Property) it.next();
	    String key = (p.getName()).trim();
	    String val = (p.getValue()).trim();
	    if(val.length() == 0
		|| val.equals("${" + key + "}")
		|| val.equals("$" + key)) {
		notSet = notSet + " " + key;
	    }
	    if(!quiet) {
	        System.out.println(key + "=" + val);
	    }
	}
	if(notSet.trim().length() > 0) {
	    throw new BuildException("The following properties are not set:"
		+ System.getProperty("line.separator")
		+ notSet);
	}
    }
}
