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

package com.sun.ant.taskdefs.common;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

public class ElementAppend extends Task  {
    private String original;
    private String suffix;
    private String prefix;
    private String property;

    public void setOriginal(String ptn) {
	original = ptn;
    }
    public void setSuffix(String sfx) {
	suffix = sfx;
    }
    public void setPrefix(String pre) {
	prefix = pre;
    }
    public void setProperty(String p) {
	property = p;
    }

    private String removeSlash(String s) {
	if(s == null || s.length() == 0) {
	    return s;
	}
	if(s.startsWith("/") || s.startsWith("\\")) {
	    s = s.substring(1);
	}
	if(s.endsWith("/") || s.endsWith("\\")) {
	    s = s.substring(0, s.length() - 1);
	}
	return s;
    }
    public void execute() throws BuildException {
        if(original == null || original.length() == 0
	    || property == null || property.length() == 0) {
	    throw new BuildException("original or property cannot be empty.");
        }
	boolean prefixPresent = true;
	boolean suffixPresent = true;
	if(prefix == null || prefix.length() == 0) {
	    prefixPresent = false;
	}
	if(suffix == null || suffix.length() == 0) {
	    suffixPresent = false;
	}
	if(!prefixPresent && !suffixPresent) {
	    throw new BuildException("both suffix and prefix are empty.");
	}
	int originalLen = original.length();

	StringTokenizer st = new StringTokenizer(original, ", \t\n\r\f");
	int tokens = st.countTokens();
	StringBuffer result = new StringBuffer();
	while(st.hasMoreTokens()) {
	    String item = st.nextToken();
	    item = removeSlash(item);
	    if(prefixPresent && !item.startsWith(prefix)) {
	        result.append(prefix).append("/").append(item);
	    } else {
            result.append(item);
        }
	    if(suffixPresent && !item.endsWith(suffix)) {
		result.append("/").append(suffix);
	    }
	    result.append(",");
	}
	String returnVal = null;
	if(tokens > 0) {
	    returnVal = result.substring(0, result.length() - 1);
	} else {
	    returnVal = result.toString();
	}
//	log(property + "=" + returnVal);
	project.setProperty(property, returnVal);
    }
}
