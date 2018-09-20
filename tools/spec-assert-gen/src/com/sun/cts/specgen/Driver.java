/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All Rights Reserved.
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

package com.sun.cts.specgen;

import java.io.*;
import org.jdom.*;
import org.jdom.input.*;
import com.sun.cts.spec.data.*;

public class Driver {

    private File inFile;

    public Driver(String file) throws FileNotFoundException {
	inFile = new File(file);
	if ( !(inFile.exists() && inFile.isFile()) ) {
	    throw new FileNotFoundException(file);
	}
    }

    public void go() throws Exception {
	SAXBuilder builder = new SAXBuilder(true); // validate
	Document doc = builder.build(inFile);
	SpecAssertions assertions =
	    new SpecAssertions(doc.getRootElement());
    }

    public static void main(String[] args) {
	if (args.length != 1) {
	    System.out.println
		("Usage: ant driver -Dfile=spec_assertion_file");
	    System.exit(1);
	}
	try {
	    Driver driver = new Driver(args[0]);
	    driver.go();
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }
    
} // end class Driver
