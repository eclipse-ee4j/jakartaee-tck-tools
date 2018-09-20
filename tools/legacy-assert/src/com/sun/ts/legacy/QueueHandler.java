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
import java.io.IOException;
import java.io.Writer;
import java.io.FileWriter;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;

public abstract class QueueHandler {

    protected static final String SPEC_ID       = "SPEC";
    protected static final String API_ID        = "JAVADOC";
    protected static final String FILENAME_PROP = "filename";
    protected static final String NL            =
	System.getProperty("line.separator", "\n");

    protected List assertionKeys = new ArrayList();

    /*
     * User must extend this class and define the following abstract
     * methods.  These methods are called from the handle template method
     * defined below.  Note this base class is really only useful if
     * you want to take a list of test descriptions and write them
     * into some output file with a specialized format.
     */
    protected abstract String getDocHeader();
    protected abstract String getDocTail();
    protected abstract void   writeAssertionToFile(Writer fw,
						   TestDescription td)
	throws IOException;

    /**
     * Template method for producing an output file based on the specified
     * test description in the specified test finder queue.
     */
    public void handle(TestFinderQueue tfq) throws Exception {
	String          outFile = System.getProperty("filename");
	FileWriter      fw      = new FileWriter(outFile);
	TestDescription td      = null;
	try {
	    fw.write(getDocHeader() + NL);
	    while ((td = tfq.next()) != null) {
		writeAssertionToFile(fw,td);
	    }
	    fw.write(getDocTail());
	} catch (Exception e) {
	    throw e;
	} finally {
	    try { 
		fw.close();
	    } catch (Exception e) {
	    }
	}
    }    

    /*
     * The methods defined below are utility methods useful to any class
     * that extends this class.
     */

    /**
     * Pulls multiple assertion IDs from an assertion_id tag that has
     * multiple assertions.  This method also cuts the URL information off
     * the front of the assertion ID string.
     */
    protected String[] parseAssertionIDs(String str) {
	if (str == null) { return new String[] {}; }
	StringTokenizer idTokens = new StringTokenizer(str, NL + ";");
	String[] ids = new String[idTokens.countTokens()];
	int i = 0;
	while(idTokens.hasMoreTokens()) {
	    String id = idTokens.nextToken().trim();
	    ids[i++] = id;
	}
	return ids;
    }

    /**
     * Replaces any XML characters that need to be escaped.
     */
    protected String escapeStringForXML(String s) {
	if(s == null) {
	    return "";
	}
	char[] array = s.toCharArray();
	StringBuffer sb = new StringBuffer();
	for(int i=0; i < array.length; i++) {
	    switch(array[i]) {
	    case '&':
		sb.append("&amp;");
		break;
	    case '<':
		sb.append("&lt;");
		break;
	    case '>':
		sb.append("&gt;");
		break;
	    case '"':
		sb.append("&quot;");
		break;
	    case '\'':
		sb.append("&apos;");
		break;
	    default:
		sb.append(array[i]);
	    }
	}
	return sb.toString();
    }

    protected boolean wellFormedAssertion(String assertion) {
    // If we are skipping the well formed ID check we are dealing with
    // assertion IDs that do not conform to the standard format.  These
    // IDs will be coming from text based specs that have assertions within
    // the spec document itself.  They will have the format <TECH-ID-STRING>integer.
    // For now we will just skip the ID check since placing the IDs in the spec
    // is a proof of concept effort.  Eventually we can modify the tools to support
    // multiple assertion ID formats.
    boolean skipCheck = Boolean.getBoolean("skip.well.formed.id.check");
    if (skipCheck) return true; 
	StringTokenizer tokens = new StringTokenizer(assertion, ":");
	if (tokens.countTokens() != 3) {
	    return false;
	}
	String token = tokens.nextToken();
	token = tokens.nextToken();
	return (token.equalsIgnoreCase("javadoc") || token.equalsIgnoreCase("spec"));
    }

    protected boolean isAPIAssertion(String str) {
	boolean result = wellFormedAssertion(str) && ((str.indexOf(API_ID) != -1) ||
						      (str.indexOf(API_ID.toLowerCase()) != -1));
	return result;
    }

    protected boolean isSpecAssertion(String str) {
    // If set to true assume these are spec IDs that are defined in the spec iteslf.
    // See comment above.
    boolean skipCheck = Boolean.getBoolean("skip.well.formed.id.check");
    if (skipCheck) return true; 
	boolean result = wellFormedAssertion(str) && ((str.indexOf(SPEC_ID) != -1) ||
						      (str.indexOf(SPEC_ID.toLowerCase()) != -1));
	return result;
    }


} // end class QueueHandler
