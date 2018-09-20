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
import java.util.zip.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

public class GenerateBOM extends Task  {
    private String zipFile=null;
    private String bomFile="bom.list";
    private Enumeration enu = null;

    public void execute() throws BuildException {
     log("Scan for files...");
	try {
		ZipFile zf = new ZipFile(zipFile);
		enu = zf.entries();	
	} catch (Exception e) {
		throw new BuildException("Error with Zip File: \"" +
					 zipFile + "\"" + e); 
	}


	try {
		FileWriter fw = new FileWriter(bomFile);
		for (;enu.hasMoreElements();){ 
			fw.write(enu.nextElement()+"\n");
		}
		fw.flush();
		fw.close();
	} catch (IOException ioe) {
		throw new BuildException("Creation of "+bomFile+" failed");
	}
    }

    public void setZipFile(String zipFile) {
	this.zipFile = zipFile;
    }
    public void setOutputFile(String bomFile) {
	this.bomFile = bomFile;
    }
}
