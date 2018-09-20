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


package com.sun.ts.diff.spec;

import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import com.sun.cts.spec.data.*;

public class Driver {

    private static final String USAGE =
	"Usage: ant run -Dprev-assertion-file=<filename> " +
	"-Dnew-assertion-file=<filename> " +
	"-Ddiff-file=<filename>";
    
    /* User specified files */
    private File prevFile;
    private File newFile;
    private File diffFile;

    /* Spec Assertion Lists */
    private SpecAssertions prevAssertions;
    private SpecAssertions newAssertions;


    private void checkFile(File file) throws Exception {
	if (!file.exists() || !file.isFile() ) {
	    throw new Exception("Error the file \"" +
				file + "\" does not exist " +
				"or is not a valid file.");
	}
    }

    public Driver(String prevAssertions,
		  String newAssertions,
		  String diffAssertions) throws Exception {
	prevFile = new File(prevAssertions);
	newFile  = new File(newAssertions);
	diffFile = new File(diffAssertions);
	checkFile(prevFile);
	checkFile(newFile);
    }

    private Document parseDoc(File file) throws Exception {
	SAXBuilder builder = new SAXBuilder(true); // validating
	Document doc = builder.build(file);
	return doc;
    }

    private void writeDiffFile(Document doc) {
	XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
	try {
	    System.out.println("**** Writing Output to file \"" + diffFile + "\"");
	    Writer outWriter = new FileWriter(diffFile);
	    out.output(doc, outWriter);
	} catch(IOException e) {
	    System.err.println("Error writing diff file \"" + diffFile + "\"");
	    System.err.println();
	    System.err.println("Exception is " + e);
	    System.exit(1);
	}
    }

    public void go() throws Exception {
	System.out.println("**** Parsing assertion document \"" + prevFile + "\"");
	this.prevAssertions = new SpecAssertions(parseDoc(this.prevFile));
	System.out.println("**** Parsing assertion document \"" + newFile + "\"");
	this.newAssertions  = new SpecAssertions(parseDoc(this.newFile));
	SpecDiff differ = new SpecDiff(this.prevAssertions, this.newAssertions);
	differ.createDiffDoc();
	writeDiffFile(differ.getDoc());
    }

    public static void main(String[] args) {
	if (args.length != 3) {
	    System.err.println(USAGE);
	    System.exit(1);
	}
	try {
	    Driver driver = new Driver(args[0], args[1], args[2]);
	    driver.go();
	} catch (Exception e) {
	    System.err.println(e);
	    System.err.println();
	    System.err.println();
	    e.printStackTrace(System.err);
	}
    }

} // end class Driver
