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


package com.sun.ts.legacy;

import com.sun.javatest.TestFinderQueue;
import com.sun.javatest.TestDescription;
import java.util.StringTokenizer;
import java.io.*;

/**
 * This class produces an output file that is used as input to the
 * CTS coverage analysis tool.
 */
public class CoverageHandler extends QueueHandler {

    public CoverageHandler(){
    }

    protected String getDocHeader() {
        StringBuffer sb = new StringBuffer(75);
	sb.append("<?xml version=\"1.0\"?>" + NL + NL);
	sb.append("<assertions>" + NL);
        return sb.toString();
    }

    protected String getDocTail() {
        return "</assertions>" + NL;
    }

    protected void writeAssertionToFile(Writer fw, TestDescription td)
	throws IOException
    {
	String assertionId  = td.getParameter("assertion_ids");
	String testName     = td.getParameter("testName");
	String testStrategy = td.getParameter("test_Strategy");
	String testClass    = td.getParameter("classname");
	//	 testClass = testClass.substring(0, testClass.lastIndexOf(".")); // remove package
	
	String   comment      = testClass + "-" + testName;     // comment element
	String[] assertionIDs = parseAssertionIDs(assertionId); // assertion ID elements
	String   description  = escapeStringForXML(testStrategy);
	
	for (int i = 0; i < assertionIDs.length; i++) {
	    String assertionKey = comment + "-" + assertionIDs[i];
	    if (!assertionKeys.contains(assertionKey)) {
		String formattedAssertion =
		    formatAssertion(comment, description, assertionIDs[i]);
		fw.write(formattedAssertion);
		System.out.println("$$$ PROCESSED: " + comment);
		assertionKeys.add(assertionKey);
	    }
	}
    }
    
    private String formatAssertion(String comment, String description, String id) {
	StringBuffer buf = new StringBuffer();
	buf.append("\t<assertion>" + NL);
	buf.append("\t\t<id>" + id + "</id>" + NL);
	buf.append("\t\t<description>" + description + "</description>" + NL);
	buf.append("\t\t<comment>" + comment + "</comment>" + NL);
	buf.append("\t</assertion>" + NL);
	return buf.toString();
    }


} // end class CoverageHandler
