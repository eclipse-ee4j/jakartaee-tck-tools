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
 * $URL$ $LastChangedDate$
 */

package com.sun.ts.assertion.coverage;

import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import com.sun.cts.api.data.Globals;

public class Driver {

    private static final String USAGE =
	"Usage: ant run -Dall-assertions-file=<filename> "
	+ "-Dtested-assertions-file=<filename> "
	+ "-Dcoverage-dir=<dirname>";

    /* User specified files */
    private File assertionFile;
    private File testedAssertionFile;
    private File coverageDir;

    private void checkFile(File file) throws Exception {
	if (!file.exists() || !file.isFile() ) {
	    throw new Exception("Error the file \"" +
				file + "\" does not exist " +
				"or is not a valid file.");
	}
    }

    private void checkDir(File dir) throws Exception {
	if (!dir.exists() || !dir.isDirectory() ) {
	    throw new Exception("Error the directory \"" +
				dir + "\" does not exist " +
				"or is not a directory.");
	}
    }

    public Driver(String definedAssertions,
		  String testedAssertions,
		  String outputDir) throws Exception {
	assertionFile       = new File(definedAssertions);
	testedAssertionFile = new File(testedAssertions);
	coverageDir         = new File(outputDir);
	checkFile(assertionFile);
	checkFile(testedAssertionFile);
	checkDir(coverageDir);
    }

    private Document parseDoc(File file) throws Exception {
	SAXBuilder builder = new SAXBuilder(false);
    String localDTDPath = System.getProperty("local.dtd.path", "");
    if (localDTDPath.length() > 0) {
        builder.setEntityResolver(new MyResolver(localDTDPath));
    }
	Document doc = builder.build(file);
	return doc;
    }

    private AssertionDoc createDoc(Document assertionDoc, Document testedDoc, File outDir) {
	AssertionDoc result = null;
	if (assertionDoc.getRootElement().getName().equals
	    (Globals.JAVADOC_TAG)) {
	    result = new APIAssertionDoc(assertionDoc, testedDoc, outDir);
	} else { // must be a spec assertion doc
	    result = new SpecAssertionDoc(assertionDoc, testedDoc, outDir);
	}
	return result;
    }

    public void go() throws Exception {
	Document testedAssertionDoc = parseDoc(this.testedAssertionFile);
	Document assertionDoc       = parseDoc(this.assertionFile);
	AssertionDoc doc = createDoc(assertionDoc, testedAssertionDoc, coverageDir);
	doc.createAssertionReports();
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
