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
 * This class produces an two output files one for spec assertions and one
 * for API assertions.
 */
public class SplitCoverageHandler extends QueueHandler {


    protected static final String API_PREFIX  = "api-";
    protected static final String SPEC_PREFIX = "spec-";

    private static final String PENDING_ID        = "pending";
    private static final String PENDING_ID_UPCASE = PENDING_ID.toUpperCase();

    // counts for final report
    private int apiCount;
    private int specCount;
    private int malformedCount;
    private int pendingCount;
    private int testsNoIDsCount;

    protected Filter assertionFilter;
    public static class Filter {
	private static final String FILTER_PROP = "assertion.filter";
	private String filterText;
	private String filterTextUpcase;
	private String filterTextLowcase;
	public Filter() {
	    filterText = System.getProperty(FILTER_PROP, "");
	    System.out.println("%%%% filterText = \"" + filterText + "\"");
	    if (filterText != "") {
		filterTextUpcase = filterText.toUpperCase();
		filterTextLowcase = filterText.toLowerCase();
	    }
	}
	public boolean accepts(String assertionID) {
	    //System.out.println("%%%% assertionID = \"" + assertionID + "\"");
	    if (filterText == "") { return true; }
	    if (assertionID == null) { return false; }
	    return assertionID.startsWith(filterText) ||
		assertionID.startsWith(filterTextUpcase) ||
		assertionID.startsWith(filterTextLowcase);
	}
    }
    

    public SplitCoverageHandler(){
	assertionFilter = new Filter();
    }

    private String prependFile(String prefix, String path) {
	int    index    = path.lastIndexOf(File.separator);
	String filename = path.substring(index + 1);
	String dirs     = path.substring(0, index + 1);
	String result   = dirs + prefix + filename;
	return result;
    }

    /**
     * This class overrides the handle method provided by QueueHandler.
     * This must be done since we want two ouput files instead of one.
     */
    public void handle(TestFinderQueue tfq) throws Exception {
	String          fileName   = System.getProperty("filename");
	String          specFile   = prependFile(SPEC_PREFIX, fileName);
	String          apiFile    = prependFile(API_PREFIX, fileName);
	FileWriter      apiWriter  = new FileWriter(apiFile);
	FileWriter      specWriter = new FileWriter(specFile);
	TestDescription td         = null;
	boolean         foundTest  = false;
	try {
	    apiWriter.write(getDocHeader() + NL);
	    specWriter.write(getDocHeader() + NL);
	    while ((td = tfq.next()) != null) {
		writeAssertionToFile(apiWriter, specWriter, td);
		foundTest = true;
	    }
	    /*
	     * Why is this code here.  The code should live in the GetAssertion class
	     * and use the TestFinderQueue API to find the number of tests found but the
	     * methods getTestsDoneCount, getTestsFoundCount and getTestsRemainingCount
	     * all return 0.  So I tried the getFilesDoneCount, getFilesFoundCount and
	     * getFilesRemainingCount APIs, these do not seem to work any better.  The
	     * first call to next() must do some intialization and the values we are after
	     * must not be available until after the next method has been called, that is why
	     * the cheesy check is done here instead of in GetAssertion.
	     */
	    if (!foundTest) {
		System.err.println();
		System.err.println("**** Error, no tests found ****");
		System.err.println("\tTry running setup for the technology area you are");
		System.err.println("\tattempting to process.");
		System.err.println();
		System.err.println("**** Tool Exiting ****");
		System.exit(1);
	    }
	    apiWriter.write(getDocTail() + NL);
	    specWriter.write(getDocTail() + NL);
	    reportStats();
	} catch (Exception e) {
	    System.err.println("@@@@ Error in SplitCoverageHandler.handle()");
	    throw e;
	} finally {
	    try { 
		apiWriter.close();
		specWriter.close();
	    } catch (Exception e) {
	    }
	}
    }    

    private boolean isPending(String assertionID) {
	if (assertionID == null) { return false; }
	return (assertionID.indexOf(PENDING_ID) != -1) ||
	    (assertionID.indexOf(PENDING_ID_UPCASE) != -1);
    }

    private void reportStats() {
	int total = apiCount + specCount + malformedCount + pendingCount;
	System.err.println();
	System.err.println("********************************************************");
	System.err.println("\t" + apiCount + "\t API assertions found, filter match.");
	System.err.println("\t" + specCount + "\t Specification assertions found, filter match.");
	System.err.println("\t" + malformedCount + "\t Malformed assertions found.");
	System.err.println("\t" + testsNoIDsCount + "\t Tests with No Assertion IDs found.");
	System.err.println("\t" + pendingCount + "\t Pending assertions found.");
	System.err.println("\t" + total + "\t Total assertions found.");
	System.err.println("********************************************************");
	System.err.println();
    }

    protected String getDocHeader() {
        StringBuffer sb = new StringBuffer();
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
	/*
	 * Do nothing since we are writing writeAssertionToFile to two files.
	 * This method is here to simply add an implementation for the
	 * the abstract method declared in QueueHandler.  It will not be
	 * called since we overrode the handle template method.
	 */
    }

    protected void writeAssertionToFile(Writer api, Writer spec, TestDescription td)
	throws IOException
    {
	String assertionId  = td.getParameter("assertion_ids");
	String testName     = td.getParameter("testName");
	String testStrategy = td.getParameter("test_Strategy");
	String testClass    = td.getParameter("classname");
	
//  	System.err.println("asertionId = \"" + assertionId + "\"");
//  	System.err.println("testName = \"" + testName + "\"");
//  	System.err.println("testStrategy = \"" + testStrategy + "\"");
//  	System.err.println("testClass = \"" + testClass + "\"" + NL);

 	String   comment      = testClass + "-" + testName;     // comment element
 	String[] assertionIDs = new String[0];
 	try {
 	    assertionIDs = parseAssertionIDs(assertionId); // assertion ID elements
	    if (assertionIDs.length == 0) {
		System.err.println("@@@@ No assertion ID(s) found for test:");
		System.err.println("\t\"" + comment + "\"");
		System.err.println();
		testsNoIDsCount++;
	    }
 	} catch (Throwable t) {
 	    System.err.println("@@@@ Error parsing assertion ID(s) " + t);
 	    System.err.println("\t@@ Check assertion_ids tag for test: \"" + comment + "\"");
 	    System.err.println("\t@@ Continuing to next test description...");
 	}
 	String   description  = escapeStringForXML(testStrategy);
	
// 	// DEBUG
// // 	System.out.println("^^^^^^^  td.getName() " + td.getName());
// // 	System.out.println("^^^^^^^ Number of assertions " + assertionIDs.length);
// // 	System.out.print("^^^^^^^ IDs: " );
// // 	for (int i = 0; i < assertionIDs.length; i++) {
// // 	    System.out.print(assertionIDs[i]);
// // 	    if (i <  assertionIDs.length - 1) {
// // 		System.out.print(", ");
// // 	    }
// // 	}
// // 	System.out.println();
// 	// END DEBUG

 	/* Iterate over each assertion covered by this test */
	for (int i = 0; i < assertionIDs.length; i++) {
	    /*
	     * Since service tests are run from 4 vehicles the test finder reports
	     * 4 test descriptions for each service test.  This tool should process
	     * each assertion once and only once, so we keep a list of assertion
	     * keys and make sure we only process each key once.  The assertion key 
	     * is the concatenation of the fully qualified class name plus the test
	     * method name plus the assertion ID.  This will be unique for each test
	     * method but identical for each test method from a different vehicle.
	     */
	    String assertionKey = comment + "-" + assertionIDs[i];
	    if (!assertionKeys.contains(assertionKey)) {
		String formattedAssertion =
		    formatAssertion(comment, description, assertionIDs[i]);
		if (isAPIAssertion(assertionIDs[i]) && assertionFilter.accepts(assertionIDs[i])) {
		    api.write(formattedAssertion);
		    apiCount++;
		} else if (isSpecAssertion(assertionIDs[i]) && assertionFilter.accepts(assertionIDs[i])) {
		    spec.write(formattedAssertion);
		    specCount++;
		} else if (!wellFormedAssertion(assertionIDs[i])) {
		    if (isPending(assertionIDs[i])) {
			//System.err.println("@@@@ PENDING Assertion [" + assertionIDs[i] + "]" + NL);
			pendingCount++;
		    } else {
   			System.err.println("@@@@ Error Assertion is neither SPEC or API assertion"
   					   + NL + "\t@@ assertion location = \"" + comment + "()"
   					   + NL + "\t@@ assertion text = \"" + assertionIDs[i] + "\""
   					   + NL + "\tContinuing..." + NL);
			malformedCount++;
		    }
		}
		//		System.out.println("$$$ PROCESSED: " + comment);
		assertionKeys.add(assertionKey);
	    }
	}
    }

    protected String formatAssertion(String comment, String description, String id) {
	StringBuffer buf = new StringBuffer();
	buf.append("\t<assertion>" + NL);
	buf.append("\t\t<id>" + id + "</id>" + NL);
	buf.append("\t\t<description>" + description + "</description>" + NL);
	buf.append("\t\t<comment>" + comment + "</comment>" + NL);
	buf.append("\t</assertion>" + NL);
	return buf.toString();
    }


} // end class SplitCoverageHandler
