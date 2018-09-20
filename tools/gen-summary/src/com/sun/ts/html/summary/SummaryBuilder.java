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

package com.sun.ts.html.summary;

import org.apache.ecs.*;
import org.apache.ecs.html.*;
import org.apache.ecs.xhtml.*;
import java.io.*;
import java.util.*;
import java.text.DecimalFormat;

public class SummaryBuilder implements BuilderIntf {

    private static final String TITLE  = "Executive Summary of Assertion Coverage Metrics";
    private static final String INTRO  = "This page is a summary of the assertion coverage"
	+ " numbers for each technology tested by the CTS development team."
	+ "  The numbers are broken out by technology, each technology can have up to two"
	+ " coverage metrics.  One metric for API assertion coverage and another metric"
	+ " for SPEC assertion coverage.  Note, some technologies may contain"
	+ " only one of the two metrics.";
    private static final String FOOTER = " ";//TODO:insert footer
    
    private java.util.Map sortedTestFiles = new HashMap();
    private List          testFiles;
    private File          summaryFile;
    private File          rootDir;
    private String        rootDirPath;
    private Document      doc;

    public SummaryBuilder(File rootDir, File summaryFile, List testFiles) {
	this.summaryFile   = summaryFile;
	this.rootDir       = rootDir;
	this.rootDirPath   = rootDir.getPath();
	this.testFiles     = testFiles;
	groupTestFiles();
	System.out.println(sortedTestFiles);
    }

    public void groupTestFiles() {
    	int numTestFiles = (testFiles == null) ? 0 : testFiles.size();
	for (int i = 0; i < numTestFiles; i++) {
	    File testFile = (File)testFiles.get(i);
	    String path = testFile.getPath();
	    String relativePath = path.substring(rootDirPath.length() + 1);
	    String technology = relativePath.substring(0, relativePath.indexOf(File.separator));
	    addRelativePath(relativePath, technology);
	}
    }

    private void addRelativePath(String path, String tech) {
	if (sortedTestFiles.containsKey(tech)) {
	    List l = (List)(sortedTestFiles.get(tech));
	    l.add(path);
	} else {
	    List l = new ArrayList();
	    l.add(path);
	    sortedTestFiles.put(tech, l);
	}
    }

    private void writeFile() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(summaryFile);
            fw.write(doc.toString());
        } catch (IOException ioe) {
            System.err.println("Error writing file \"" + summaryFile + "\"");
        } finally {
            try { fw.close(); } catch (IOException e) {}
        }
    }

    private String formatTech(String tech) {
	String techName = null;
	String cleanedVersion = null;
	int index = tech.indexOf("_");
	if (index != -1) {
	    techName = tech.substring(0, index);
	    String version = tech.substring(index + 1);
	    cleanedVersion = version.replace('_','.');
	} else { // default if expected TECH_vid1_vid2_vid3 format not used
	    techName = tech;
	    cleanedVersion = "0.0.0";
	}
	return techName.toUpperCase() + " " + cleanedVersion;
    }

    private TBody createTable() {
	TBody tbody = new TBody()
	    .addElement(new TR()
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Technology"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Spec Coverage"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("API Coverage")))));
	doc.appendBody(new Table(1).setCellPadding(2)
	    .setCellSpacing(2).setWidth("100%")
	    .addElement(tbody));
	return tbody;
    }

    private void addRow(String tech, double specCoverage, double apiCoverage, TBody tbody) {
	DecimalFormat formatter = new DecimalFormat("###.##");
	String specCoverageStr  = "N/A";
	String apiCoverageStr   = "N/A";
	if (!Double.isNaN(specCoverage)) {
	    specCoverageStr = formatter.format(specCoverage) + "%";
	}
	if (!Double.isNaN(apiCoverage)) {
	    apiCoverageStr = formatter.format(apiCoverage) + "%";
	}
	tbody.addElement(new TR()
	    .addElement(new TD().setAlign("Top")
		.addElement(tech))
	    .addElement(new TD().setAlign("Top")
		.addElement((specCoverageStr)))
	    .addElement(new TD().setAlign("Top")
		.addElement(apiCoverageStr)));
    }

    private double calcCoverage(int tested, int untested) {
	if (tested == -1 || untested == -1) {
	    return Double.NaN;
	}
	int total = tested + untested;
	if (total <= 0) {
	    System.err.println("ERROR: calcCoverage() says (tested + untested <= 0)");
	    return Double.NaN;
	}
	return ((double)tested / total) * 100.0;
    }

    private org.jdom.Document parseDoc(File file) throws Exception {
	org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder(false);
	org.jdom.Document doc = builder.build(file);
	return doc;
    }

    private int leafAssertionCount(List assertions) {
	int count = 0;
	int numAssertions = (assertions == null) ? 0 : assertions.size();
	for (int i = 0; i < numAssertions; i++) {
	    org.jdom.Element el = (org.jdom.Element)assertions.get(i);
	    List children = el.getChildren("assertion");
	    if (children != null && children.size() > 0) {
		count = count + leafAssertionCount(assertions);
	    } else {
		count++;
	    }
	}
	return count;
    }
    
    private int getAssertionCount(String file) {
	int assertionCount = 0;
	File absoluteFile = new File(rootDir, file);
	System.out.println("getAssertionCount() processing file \"" + absoluteFile + "\"");
	org.jdom.Document doc = null;
	try {
	    doc = parseDoc(absoluteFile);
	} catch (Exception e) {
	    System.err.println("Error parsing file \"" + absoluteFile + "\"");
	    System.err.println("\tApplication Continuing.");
	    return -1;
	}
	org.jdom.Element assertionsElement = doc.getRootElement().getChild("assertions");
	if (assertionsElement != null) {
	    List assertionElements = assertionsElement.getChildren("assertion");
	    assertionCount = (assertionElements == null)
		? 0 : leafAssertionCount(assertionElements);
	}
	doc = null;
	return assertionCount;
    }

    private void createTechSummary(String tech, List testFiles, TBody tbody) {
	String formattedTech = formatTech(tech);
	int apiTested        = -1;
	int apiUntested      = -1;
	int specTested       = -1;
	int specUntested     = -1;
	int numFiles = (testFiles == null) ? 0 : testFiles.size();
	for (int i = 0; i < numFiles; i++) {
	    String file     = (String)testFiles.get(i);
	    int    index    = file.lastIndexOf("/");
	    String filename = file.substring(index + 1);
	    if (file.indexOf("api") != -1) {
		if (filename.equalsIgnoreCase(Driver.TESTED_FILE)) {
		    apiTested = getAssertionCount(file);
		    System.out.println("### apiTested " + apiTested);
		} else if (filename.equalsIgnoreCase(Driver.UNTESTED_FILE)) {
		    apiUntested = getAssertionCount(file);
		    System.out.println("### apiUntested " + apiUntested);
		} else {
		    System.err.println("API file that is not tested or untested, skipping \""
				       + file + "\"");		    
		}
	    } else if (file.indexOf("spec") != -1) {	
		if (filename.equalsIgnoreCase(Driver.TESTED_FILE)) {
		    specTested = getAssertionCount(file);
		    System.out.println("### specTested " + specTested);
		} else if (filename.equalsIgnoreCase(Driver.UNTESTED_FILE)) {
		    specUntested = getAssertionCount(file);
		    System.out.println("### specUntested " + specUntested);
		} else {
		    System.err.println("SPEC file that is not tested or untested, skipping \""
				       + file + "\"");		    
		}	
	    } else {
		System.err.println("Not an API or SPEC test file, skipping \""
				   + file + "\"");
	    }
	}
	double apiCoverage  = calcCoverage(apiTested, apiUntested);
	double specCoverage = calcCoverage(specTested, specUntested);
	addRow(formattedTech, specCoverage, apiCoverage, tbody);
    }

    private void createSummary() {
	TBody tbody = createTable();
	Iterator iter = sortedTestFiles.keySet().iterator();
	while (iter.hasNext()) {
	    String key = (String)iter.next();
	    List links = (List)sortedTestFiles.get(key);
	    createTechSummary(key, links, tbody);
	}
    }

    public void build() {
	doc = new Document();
        doc.appendTitle(TITLE);
        doc.appendBody(new center(new u(new H1(TITLE))));
	doc.appendBody(new Font().setSize("+0").addElement(INTRO));
	doc.appendBody(new HR("100%", "Center", 2));
	doc.appendBody(new br());
	createSummary();
	doc.appendBody(new br());
	doc.appendBody(new HR("100%", "Center", 2));
	doc.appendBody(new Font().setSize("+0").addElement(FOOTER));	
	writeFile();
    }

} // end class SummaryBuilder
