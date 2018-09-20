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

import org.apache.ecs.*;
import org.apache.ecs.html.*;
import org.apache.ecs.xhtml.*;
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;
import com.sun.cts.common.DateUtil;

public class HTMLUtils {

    private static final String FOOTER = "";//TODO:add the footer
    private static HTMLUtils instance = new HTMLUtils();

    public static HTMLUtils instance() {
	return instance;
    }

    private HTMLUtils(){} // create no instances outside this class


    private Document createDoc(String title, String header) {
	Document doc = new Document();
	doc.appendTitle(title);
	doc.appendBody(new center(new u(new H1(header))));
	return doc;
    }

    private void addTestToIDs(Document doc, java.util.Map tests) {
	doc.appendBody(new HR("100%", "Center", 2));
	Iterator iter = tests.keySet().iterator();
	while (iter.hasNext()) {
	    String testComment = (String)iter.next();
	    doc.appendBody(new Font().setSize("+1")
		.addElement(new U())
		.addElement("Test Name-Method: " + testComment)
		.addElement(new BR()));
	    List testNames = (List)tests.get(testComment);
	    int numTests = (testNames == null) ? 0 : testNames.size();
	    TBody tbody = new TBody()
		.addElement(new TR()
		    .addElement(new TD().setAlign("Top")
			.addElement(new center(new b("Assertion ID"))))
		    .addElement(new TD().setAlign("Top")
			.addElement(new center(new b("Strategy")))));
	    doc.appendBody(new Table(1).setCellPadding(2)
		.setCellSpacing(2).setWidth("100%")
		.addElement(tbody));
	    for (int i = 0; i < numTests; i++) {
		TestedAssertion ta = (TestedAssertion)testNames.get(i);
		tbody.addElement(new TR()
		    .addElement(new TD().setAlign("Top")
			.addElement(ta.getID()))
		    .addElement(new TD().setAlign("Top")
			.addElement(ta.getDescription())));
	    }
	    doc.appendBody(new BR()).appendBody(new BR());
	}
    }

    private int getID(String id) {
	int result = 0;
	String idStr = null;
	StringTokenizer tokens = new StringTokenizer(id, ":");
	if (tokens.countTokens() == 3) {
	    tokens.nextToken();
	    tokens.nextToken();
	    idStr = tokens.nextToken();
	} else if (tokens.countTokens() > 0) {
	    idStr = tokens.nextToken();
	} else {
	    idStr = "0";
	}
	try {
	    result = Integer.valueOf(idStr).intValue();
	} catch(Exception e) {
	    result = 0;
	}
	return result;
    }

    private int[] parseInts(String ints) {
	int index = ints.lastIndexOf(":");
	String nums = ints.substring(index + 1);
	String numbers = nums.replace('.', ':');
	String[] strSect = numbers.split(":");
	int[] sects = new int[strSect.length];
       	for (int i = 0; i < strSect.length; i++) {
	    try {
		sects[i] = Integer.parseInt(strSect[i]);
	    } catch (NumberFormatException nfe) {
		sects[i] = 0;
	    }
	}
	return sects;
    }

    private boolean greaterThan(String a, String b) {
	int[] aInts  = parseInts(a);
	int[] bInts  = parseInts(b);
	int   result = -1;
	for (int i = 0; i < aInts.length; i++) {
	    if (i < bInts.length) {
		result = aInts[i] - bInts[i];
		if (result != 0) {
		    break;
		}
	    } else {
		result = 1;
	    }
	}
	return result > 0;
    }

    private void swap(String[] arr, int i, int j) {
	String temp = arr[i];
	arr[i] = arr[j];
	arr[j] = temp;
    }

    private List getSortedIDs(Set set) {
	String[] result = new String[set.size()];
	result = (String[])(set.toArray(result));
	/* bubble sort it */
	for (int i = result.length - 1; i >= 0; i--) {
	    for (int j = 0; j < i; j++) {
		if (greaterThan(result[j], result[j + 1])) {
		    swap(result, j, j+1);
		}
	    }
	}
//  	for (int i = 0; i < result.length; i++) {
//  	    System.out.println("result[" + i + "] = " + result[i]);
//  	}
	return Arrays.asList(result);
    }

    private void addIDsToTests(Document doc, java.util.Map tests) {
	doc.appendBody(new HR("100%", "Center", 2));
	Iterator iter = getSortedIDs(tests.keySet()).iterator();
	while (iter.hasNext()) {
	    String id = (String)iter.next();
	    doc.appendBody(new Font().setSize("+1")
		.addElement(new U())
		.addElement("Assertion ID: " + id)
		.addElement(new BR()));
	    List testNames = (List)tests.get(id);
	    int numTests = (testNames == null) ? 0 : testNames.size();
	    TBody tbody = new TBody()
		.addElement(new TR()
		    .addElement(new TD().setAlign("Top")
			.addElement(new center(new b("Test Class"))))
		    .addElement(new TD().setAlign("Top")
			.addElement(new center(new b("Test Method"))))
		    .addElement(new TD().setAlign("Top")
			.addElement(new center(new b("Strategy")))));
	    doc.appendBody(new Table(1).setCellPadding(2)
		.setCellSpacing(2).setWidth("100%")
		.addElement(tbody));
	    for (int i = 0; i < numTests; i++) {
		TestedAssertion ta = (TestedAssertion)testNames.get(i);
		tbody.addElement(new TR()
		    .addElement(new TD().setAlign("Top")
			.addElement(ta.getTestClass()))
		    .addElement(new TD().setAlign("Top")
			.addElement(ta.getTestName()))
		    .addElement(new TD().setAlign("Top")
			.addElement(ta.getDescription())));
	    }
	    doc.appendBody(new BR()).appendBody(new BR());
	}
    }

    private void writeFile(File file, Document doc) {
	FileWriter fw = null;
	try {
	    fw = new FileWriter(file);
	    fw.write(doc.toString());
	} catch (IOException ioe) {
	    System.err.println("Error writing file \"" + file + "\"");
	} finally {
	    try { fw.close(); } catch (IOException e) {}
	}
    }

    public void createTestToIdMappingFile(File file, TestedAssertionList list,
					  HTMLDataProvider data) {
	Document doc = createDoc("Test to ID Mapping",
				 "Assertion Mapping from Test Classes to IDs for " +
				 data.getTechnology() + " version: " + data.getVersion());
	java.util.Map testsToIDs = list.getAssertionsByTest();
	addTestToIDs(doc, testsToIDs);
	writeFile(file, doc);
    }
    
    public void createIDToTestMappingFile(File file, TestedAssertionList list,
					  HTMLDataProvider data) {
	Document doc = createDoc("ID to Test Mapping",
				 "Assertion Mapping from IDs to Test Classes for " +
				 data.getTechnology() + " version: " + data.getVersion());
	java.util.Map idsToTests = list.getAssertionsByID();
	addIDsToTests(doc, idsToTests);
	writeFile(file, doc);
    }

    public void createSummaryFile(File file, HTMLDataProvider data) {
	String countParent = System.getProperty("count.parent", "true");
	int numTested   = 0;
	int numUntested = 0;
	if (countParent.equalsIgnoreCase("true")) {
	    numTested   = data.getNumTested();
	    numUntested = data.getNumUntested();
	} else {
	    numTested   = data.getNumTestedNoParent();
	    numUntested = data.getNumUntestedNoParent();
	}
	System.out.println("$$$$$$$ Tested   = " + numTested);
	System.out.println("$$$$$$$ Untested = " + numUntested);
	double coverageValue = ((double)numTested / (numTested + numUntested)) * 100.0;
	DecimalFormat formatter = new DecimalFormat("###.##");
	String title = "Assertion Coverage Summary for " + data.getTechnology() +
	    " version: " + data.getVersion();
	if (data.isSpec()) {
	    title = "Spec " + title;
	} else {
	    title = "API " + title;
	}
	Document doc = createDoc(title, title);
	doc.appendBody(new Font().setSize("+0")
	    .addElement("This page summarizes the assertion coverage for a specific " +
			"technology area.  The page is broken up into 3 sections.  The " +
			"first section contains summary counts for each type of assertion. " +
			"The assertions are grouped into 6 different catagories, tested, " +
			"untested, untestable, optional, removed and deprecated. " +
			"The table shows the total count for each category.  An overall " +
			"coverage percentage is also listed, this is calcualted by dividing " +
			"the number of tested assertions by the sum of tested and untested " +
			"assertions. This number should provide a basic coverage statistic. " +
			"Section 2 contains 6 links one link per assertion catagory. " +
			"Following a link will take the user to a complete list of all " +
			"assertions that are in that catagory. The last section contains " +
			"2 mapping files, one file maps assertion IDs to the test methods " +
			"that verify those IDs and the second file maps test methods to " +
			"the assertion IDs verified by that test method.")
	    .addElement(new BR())
	    .addElement(new BR()));
	doc.appendBody(new HR("100%", "Center", 2));
	doc.appendBody(new BR());
	doc.appendBody(new Font().setSize("+2")
	    .addElement(new U())
	    .addElement(new b("Assertion Counts, Grouped by Assertion Type:"))
	    .addElement(new BR()));
	TBody tbody = new TBody()
	    .addElement(new TR()
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Tested"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Untested"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Untestable"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Optional"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Removed"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Deprecated")))));
	doc.appendBody(new Table(1).setCellPadding(2)
	    .setCellSpacing(2).setWidth("100%")
	    .addElement(tbody));
	tbody.addElement(new TR()
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumTested())))
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumUntested())))
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumUntestable())))
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumOptional())))
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumRemoved())))
	    .addElement(new TD().setAlign("Top")
		.addElement(String.valueOf(data.getNumDeprecated()))));
	doc.appendBody(new BR());
	if (countParent.equalsIgnoreCase("true") || !data.isSpec()) {
	    doc.appendBody(new Font().setSize("+0")
		.addElement(new b(formatter.format(coverageValue) +
				  "% of testable assertions are tested [" +
				  "testAssertions / (testedAssertions + untestedAssertions)]")));
	} else { // must be a spec with countParent set to false
	    doc.appendBody(new Font().setSize("+0")
		.addElement(new b(formatter.format(coverageValue) +
				  "% of testable assertions are tested [" +
				  "testAssertions / (testedAssertions + untestedAssertions)]" +
				  " - NOT Counting Parent Assertions")));
	}
	doc.appendBody(new BR());
	doc.appendBody(new BR());
	
	doc.appendBody(new Font().setSize("+2")
	    .addElement(new b("Detailed list of assertions separated by category:"))
	    .addElement(new BR()));
	File[] assertionFiles = data.getAssertionFileNames();
	UL ul1 = new UL();
	doc.appendBody(ul1);
	for (int i = 0; i < assertionFiles.length; i++) {
	    String filename = assertionFiles[i].getName();
	    ul1.addElement(new LI(new A(filename, filename)));
	}
	doc.appendBody(new BR());
	doc.appendBody(new U());

	doc.appendBody(new Font().setSize("+2")
	    .addElement(new b("Mapping of Assertion IDs -> Test Methods / " +
			      "Test Methods -> Assertion IDs:"))
	    .addElement(new BR()));
	File[] mappingFiles = data.getTestIDMappingFileNames();
	UL ul2 = new UL();
	doc.appendBody(ul2);
	for (int i = 0; i < mappingFiles.length; i++) {
	    String filename = mappingFiles[i].getName();
	    ul2.addElement(new LI(new A(filename, filename)));
	}

	doc.appendBody(new BR());
	doc.appendBody(new HR("100%", "Center", 2));
	doc.appendBody((new Font().setSize("+0").addElement(FOOTER)));
	doc.appendBody(new BR());
	String date = "Page last updated on " + DateUtil.instance().getFullDate();
	doc.appendBody(new Font().setSize("+0").addElement(date));

	// add the tested and untested countin hidden fields so they are easy to parse later
	// but don't show up on the generated page.
        doc.appendBody(new Form()
	    .addElement(new Input(Input.HIDDEN, "tested", String.valueOf(data.getNumTested())))
	    .addElement(new Input(Input.HIDDEN, "untested", String.valueOf(data.getNumUntested()))));

	writeFile(file, doc);
    }

} // end class HTMLUtils
