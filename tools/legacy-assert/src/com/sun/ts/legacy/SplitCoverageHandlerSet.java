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
import java.util.*;
import java.io.*;

/**
 * This class produces an two output files one for spec assertions and one
 * for API assertions.  Each file is ordered by assertion IDs and all duplicate
 * assertion IDs have been removed.
 */
public class SplitCoverageHandlerSet extends SplitCoverageHandler {

    /**
     * A simple class to hold an assertion.  Instances of this class will
     * be placed in alist and sorted by assertion ID.
     */
    public class Assertion implements Comparable {
	public String id;
	public String comment;
	public String description;
	public Assertion(String id, String comment, String description) {
	    this.id = id;
	    this.comment = comment;
	    this.description = description;
	}
	private int getIntID(String id) {
	    int idNum = -1;
	    try {
		int index = id.lastIndexOf(":");
		idNum = Integer.parseInt(id.substring(index + 1));
	    } catch (Exception e) {
		System.out.println("Error getting assertion ID integer \"" +
				   id + "\", returning -1");
	    }
	    return idNum;
	}
	public int compareTo(Object o) {
	    Assertion assertion = (Assertion)o;
	    int thisID = getIntID(id);
	    int thatID = getIntID(assertion.id);
	    return thisID - thatID;
	}
	public boolean equals(Object o) {
	    if (o == null || this.getClass() != o.getClass()) {
		return false;
	    }
	    Assertion that = (Assertion)o;
	    return id.equals(that.id);
	}
    } // end class Assertion


    private static final String UNIQUE_PROP     = "unique.assertions.only";
    private static final String ALL_FIELDS_PROP = "all.assertion.fields";

    private List    specAssertions   = new ArrayList(); // Assertion instances
    private List    apiAssertions    = new ArrayList(); // Assertion instances
    private boolean uniqueIDs;
    private boolean includeAllFields;
    private final boolean includeAllFieldsDef = false;
    private final boolean uniqueIDsDef = true;
    
    public SplitCoverageHandlerSet() {
	String temp = null;
	try {
	    if ((temp = System.getProperty(UNIQUE_PROP)) == null) {
		this.uniqueIDs = uniqueIDsDef;
	    } else {
		this.uniqueIDs = (Boolean.valueOf(temp)).booleanValue();
	    }
	} catch(Exception e) {
	    System.err.println("@@@ Error setting member uniqueIDs, " +
			       "setting it to a default value of " + uniqueIDsDef);
	    this.uniqueIDs = uniqueIDsDef;
	}
	
	try {
	    if ((temp = System.getProperty(ALL_FIELDS_PROP)) == null) {
		this.includeAllFields = includeAllFieldsDef;
	    } else {
		this.includeAllFields = (Boolean.valueOf(temp)).booleanValue();
	    }
	} catch(Exception e) {
	    System.err.println("@@@ Error setting member includeAllFields, " +
			       "setting it to a default value of " + includeAllFieldsDef);
	    this.includeAllFields = includeAllFieldsDef;
	}
    }
    
    protected void writeAssertionToFile(Writer fw, TestDescription td)
	throws IOException
    {
	/*
	 * Do nothing since we are writing writeAssertionToFile to two files.
	 * This method is here to simply add an implementation for the
	 * the abstract method declared in QueueHandler.  It will not be
	 * called since we overrode the handle template method below.
	 */
    }
    
    /**
     * Override handle so we can process the assertions and place them
     * in lists so they can be sorted and then written to a file.
     */ 
    public void handle(TestFinderQueue tfq) throws Exception {
	String          fileName   = System.getProperty("filename");
	String          specFile   = SPEC_PREFIX + fileName;
	String          apiFile    = API_PREFIX + fileName;
	FileWriter      apiWriter  = new FileWriter(apiFile);
	FileWriter      specWriter = new FileWriter(specFile);
	TestDescription td      = null;
	
	while ((td = tfq.next()) != null) {
	    processAssertions(td);
	}
	Collections.sort(apiAssertions);
	Collections.sort(specAssertions);
	System.out.println("$$$ API and SPEC assertion lists are sorted");
	writeAssertions(apiWriter, apiAssertions);
	writeAssertions(specWriter, specAssertions);
	System.out.println("$$$ API and SPEC assertions files written to \"" +
			   apiFile + "\" and \"" + specFile +"\" respectively");	
    }
    
    /**
     * Creates an Assertion instance for each assertion  ID and puts the instace
     * in the appropriate list (spec or api assertion list).  Note the lists
     * contain unique IDs only, no dupes are allowed.
     */
    protected void processAssertions(TestDescription td) {
	String assertionId  = td.getParameter("assertion_ids");
	String testName     = td.getParameter("testName");
	String testStrategy = td.getParameter("test_Strategy");
	String testClass    = td.getParameter("classname");
	
	String   comment      = testClass + "-" + testName;     // comment element
	String[] assertionIDs = parseAssertionIDs(assertionId); // assertion ID elements
	String   description  = escapeStringForXML(testStrategy);
	
	for (int i = 0; i < assertionIDs.length; i++) {
	    if (isAPIAssertion(assertionIDs[i])) {
		if ( (uniqueIDs && !assertionInList(assertionIDs[i], apiAssertions))
		     || !uniqueIDs) { 
		    apiAssertions.add
			(new Assertion(assertionIDs[i], comment, description));
		}
	    } else if (isSpecAssertion(assertionIDs[i])) {
		if ( (uniqueIDs && !assertionInList(assertionIDs[i], specAssertions))
		     || !uniqueIDs) { 
		    specAssertions.add
			(new Assertion(assertionIDs[i], comment, description));
		}
	    }
	    System.out.println("$$$ PROCESSED: " + comment);
	}
    }

    /**
     * Returns true if the specified id is in the specified list of IDs.
     */
    private boolean assertionInList(String id, List assertionList){
	Assertion lookFor = new Assertion(id, null, null);
	return assertionList.contains(lookFor);
    }
    
    /**
     * Writes the sorted unique assertion IDs to the specified writer.
     */
    private void writeAssertions(Writer writer, List assertions) throws Exception {
	try {
	    writer.write(getDocHeader() + NL);
	    int numAssertions = (assertions == null) ? 0 : assertions.size();
	    for (int i = 0; i < numAssertions; i++) {
		Assertion assertion = (Assertion)assertions.get(i);
		writer.write(formatAssertion(assertion.comment,
					     assertion.description,
					     assertion.id));
	    }
	    writer.write(getDocTail() + NL);
	} catch (Exception e) {
	    throw e;
	} finally {
	    try { 
		writer.close();
	    } catch (Exception e) {
	    }
	}
    }

    /**
     * Overrides formatAssertion from super class.  We only care about the
     * assertion IDs so we ignore the comment and description.
     */
    protected String formatAssertion(String comment, String description, String id) {
	String result = null;
	if (includeAllFields) {
	    result = super.formatAssertion(comment, description, id);
	} else {
	    StringBuffer buf = new StringBuffer();
	    buf.append("\t<assertion>" + NL);
	    buf.append("\t\t<id>" + id + "</id>" + NL);
	    buf.append("\t</assertion>" + NL);
	    result = buf.toString();
	}
	return result;
    }
    
} // end class SplitCoverageHandlerSet
