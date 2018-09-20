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

public class GetClient extends Task  {
    private String property = "test.client";
    private File srcdir;
    public void setProperty(String propName) {
	property = propName;
    }
    public void setSrcdir(File f) {
	srcdir = f;
    }

    public void execute() throws BuildException {
//	if(property == null || property.length() == 0) {
//	    throw new BuildException("Specify the name of the property to store output.");
//	}
	if(srcdir == null || !srcdir.exists()) {
	    throw new BuildException("Specify src dir where to search for client file.");
	}
	File[] clientFiles = srcdir.listFiles(ArchiveInfo.clientJavaFilter);
	if(clientFiles.length != 0) {
	    project.setProperty(property, clientFiles[0].getName());
	} else {
	    throw new BuildException("Could not figure out test.client. Use 'tsant -Dtest.client=FooClient.java -Dtest=test7 runclient'");
	}
    }
}
