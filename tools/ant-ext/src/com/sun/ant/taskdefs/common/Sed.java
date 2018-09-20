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
public class Sed extends Task {

    protected static final String NL = System.getProperty("line.separator", "\n");

    protected File thefile;
    protected String findthis;
    protected String replacewith;

    public void setFile(File file) {
	thefile = file;
    }

    public void setFind(String find) {
	findthis = find;
    }

    public void setReplace(String replace) {
	replacewith = replace;
    }

    public void execute() throws BuildException {
	checkPreconditions();
	doIt();
    }

    protected void checkPreconditions()  throws BuildException {
	if (!thefile.exists() ) {
	    throw new BuildException("Error, \"" + thefile + "\" does not exist");
	}
	if (findthis == null || findthis.length() == 0) {
	    throw new BuildException("Find string is empty, it can NOT be.");
	}
	if (replacewith == null || replacewith.length() == 0) {
	    throw new BuildException("Replace string is empty, it can NOT be.");
	}
	checkPreconditions0();
    }

    protected void checkPreconditions0() throws BuildException {
	// can be overridden by a subclass if additional checks are necessary
    }

    protected String filterFile() throws BuildException {
	BufferedReader in = null;
	String line = null;
	StringBuffer filteredBuffer = new StringBuffer();
	try {
	    in = new BufferedReader(new FileReader(thefile));
	    while((line = in.readLine()) != null) {
		String result = filterString(line);
		filteredBuffer.append(result + NL);
	    }
	    return filteredBuffer.toString();
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { in.close(); } catch(Exception ee) {} // do nothing
	}
    }

    protected String filterString(String line) {
	return line.replaceAll(findthis, replacewith);
    }

    protected void writeFile(String output) throws BuildException {
	Writer out = null;
	try {
	    File outFile = new File(thefile.getPath() + ".temp");
	    out = new FileWriter(outFile);
	    out.write(output);
	    out.flush();
	    if(!thefile.delete()) {
		throw new BuildException("Could not delete \"" + thefile + "\"");
	    }
	    if(!outFile.renameTo(thefile)) {
		throw new BuildException("Could not rename \"" + outFile + "\" to \"" + thefile + "\"");
	    }
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { out.close(); } catch(Exception ee) {} // do nothing
	}
    }

    protected void doIt() throws BuildException {
	String buf = filterFile();
	writeFile(buf);
    }

} // end class Sed
