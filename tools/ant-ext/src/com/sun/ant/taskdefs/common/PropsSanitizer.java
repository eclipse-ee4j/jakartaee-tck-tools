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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;

/*
 * usage examples:
 * 	<target name="jte.cleanup" >
 *		<props.sanitizer file="${deliverable.bundle.dir}/bin/ts.jte" quiet="true">
 *			<property name="webServerHost" value="" />
 *			<property name="webServerPort" value="" />
 *			<property name="webServerHome" value="" />
 *		</props.sanitizer>
 *	</target>
 *
 *	Using the above example you need to call the target after the "copyinstall" target
 *	is called from the build target of you give deliverable.
 *
 *	<target name="build" depends="init">
 *		<antcall target="copyinstall" />
 *		<antcall target="jte.cleanup" />
 *		...
 * 
 *  This custom task will read in a java properties file and change the specified properties to the 
 *  given value. The intentions here are to use this file to zero out properties in a given ts.jte file. 
 *  But a side effect of this Task will allow you to  set property(s) value(s) in a given file as well.
 *
 */

public class PropsSanitizer extends Task {
    private static final String NL = System.getProperty("line.separator", "\n");
    private static final String WARN = "Sanitize process Not run!";

    private ArrayList properties = new ArrayList();
    private boolean quiet;
    private String fileName;

    // private Properties fileProps = new Properties();

    public void setFile(String propertyFile) {
	this.fileName = propertyFile;
    }

    public void addProperty(Property prop) {
	properties.add(prop);
    }

    public void setQuiet(boolean q) {
	quiet = q;
    }

    public void execute() throws BuildException {
	Hashtable uProps = getProject().getUserProperties();
	Hashtable nProps = new Hashtable();
	String fileContents;

	if (properties.isEmpty() & uProps.isEmpty()) {
	    System.out.println("No Properties specified for replacement!");
	} else {

	    if (!properties.isEmpty()) {
		Iterator<?> it = properties.iterator();

		while (it.hasNext()) {
		    Property prop = (Property) it.next();
		    nProps.put((prop.getName()).trim(),
			    (prop.getValue()).trim());
		}
	    }

	    try {
		fileContents = this.readFile(fileName);

		// First we check nest properties.
		fileContents = this.changeProp(fileContents, nProps);

		// Now we check command line given properties.(these over ride
		// nested properties.)
		fileContents = this.changeProp(fileContents, uProps);

		// write the changes out.
		this.writeFile(fileName, fileContents);

		if (!quiet) {
		    System.out.println("DEBUG ==>  Finished Sanitize Process!");
		}

	    } catch (FileNotFoundException fnfe) {
		System.out.println("WARNING==> No such file: " + fileName + NL
			+ WARN);
	    } catch (IOException ioe) {
		System.out.println("WARNING==> Unable to read in file: "
			+ fileName + NL + "Check permissions on file." + NL
			+ WARN);
	    }
	}
    }

    // read in properties file.
    private String readFile(String fName) throws IOException,
	    FileNotFoundException {
	StringBuilder contents = new StringBuilder();
	Scanner scanner = new Scanner(new FileInputStream(fName));
	scanner.useDelimiter(NL);

	System.out.println("Reading from " + fName + "....");

	try {
	    while (scanner.hasNextLine()) {
		String nLine = scanner.nextLine();
		contents.append(nLine + NL);
	    }
	} finally {
	    scanner.close();
	}

	return contents.toString();
    }

    // write in properties file.
    private void writeFile(String fName, String contents)
	    throws FileNotFoundException, IOException {
	Writer output = new BufferedWriter(new FileWriter(fName));

	try {
	    output.write(contents);

	    System.out.println("Changes written to file: " + fName + NL);

	} finally {
	    output.close();
	}
    }

    private String changeProp(String contents, Hashtable hsProps) {
	StringBuilder output = new StringBuilder();
	Scanner scanner = new Scanner(contents);
	scanner.useDelimiter(NL);

	if (!quiet) {
	    System.out.println("DEBUG ==>  Starting Sanitize Process!");
	}

	while (scanner.hasNext()) {
	    String token = scanner.next();
	    Boolean written = false;
	    for (Enumeration keys = hsProps.keys(); keys.hasMoreElements();) {
		String uKey = ((String) keys.nextElement()).trim();
		String uVal = ((String) hsProps.get(uKey)).trim();

		if (token.startsWith(uKey)) {
		    output.append(token.replace(token, uKey + "=" + uVal + NL));

		    written = true;

		    // Remove key if found, no longer need to parse it.
		    hsProps.remove(uKey);

		    System.out.println("Replaced property " + token + " with " 
			    + uKey + "=" + uVal);

		    break;
		}
	    }

	    if (!written) {
		output.append(token + NL);
	    }
	}

	// if the property was not found and replaced then let us know!
	if (!quiet) {
	    if (!hsProps.isEmpty()) {
		for (Enumeration keys = hsProps.keys(); keys.hasMoreElements();) {
		    String uKey = ((String) keys.nextElement()).trim();
		    String uVal = ((String) hsProps.get(uKey)).trim();
		    System.out.println("WARNING==> " + uKey
			    + " Not Found In file " + fileName + NL);
		}
	    }
	}

	return output.toString();
    }

}
