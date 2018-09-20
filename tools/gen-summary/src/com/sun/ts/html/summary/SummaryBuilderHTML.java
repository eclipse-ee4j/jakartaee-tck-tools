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
import com.sun.cts.common.DateUtil;

public class SummaryBuilderHTML implements BuilderIntf {

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
    private int           testedAPIAssertions;
    private int           testedSpecAssertions;
    private int           untestedAPIAssertions;
    private int           untestedSpecAssertions;

    public SummaryBuilderHTML(File rootDir, File summaryFile, List testFiles) {
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

    private void addRow(String tech, a specCoverage, a apiCoverage, TBody tbody) {
	TR tr = new TR();
	tbody.addElement(tr);
	tr.addElement(new TD().setAlign("Top").addElement(tech));
	if (specCoverage == null) {
	    tr.addElement(new TD().setAlign("Top").addElement("N/A"));
	} else {
	    tr.addElement(new TD().setAlign("Top").addElement(specCoverage));
	}
	if (apiCoverage == null) {
	    tr.addElement(new TD().setAlign("Top").addElement("N/A"));
	} else {
	    tr.addElement(new TD().setAlign("Top").addElement(apiCoverage));
	}
    }

    private void updateCounters(String tested, String untested, File sourceFile) {
        if (sourceFile.getParent().endsWith("api")) {
	    testedAPIAssertions      += Integer.parseInt(tested);
	    untestedAPIAssertions    += Integer.parseInt(untested);
	} else if (sourceFile.getParent().endsWith("spec")) {
              testedSpecAssertions   += Integer.parseInt(tested);
	      untestedSpecAssertions += Integer.parseInt(untested);
	} else {
	    System.err.println("ERROR, the specified file \"" + sourceFile
			       + "\" is neither in a spec nor api directory. "
			       + "Global tested/untested assertion counters will not be updated.");
	}
	System.err.println("&&&&&&&& TOTAL API  TESTED   : " + testedAPIAssertions);
	System.err.println("&&&&&&&& TOTAL API  UNTESTED : " + untestedAPIAssertions);
	System.err.println("&&&&&&&& TOTAL SPEC TESTED   : " + testedSpecAssertions);
	System.err.println("&&&&&&&& TOTAL SPEC UNTESTED : " + untestedSpecAssertions);
    }

    private String scrapeCoverage(String file) {
	File fullPath  = new File(rootDir, file);
	String marker  = "% of testable assertions are tested [";
        String marker2 = "<input name=\"tested\" type=\"HIDDEN\" value=\"";
        String marker3 = "\"><input name=\"untested\" type=\"HIDDEN\" value=\"";
        String marker4 = "\"></form></body></html>";
	StringBuffer buf = new StringBuffer();
	BufferedReader in = null;
	final int MAX_CHARS= 6;
	char[] chars = new char[MAX_CHARS];
	String coverageString = "000.00";
	try {
	    in = new BufferedReader(new FileReader(fullPath));
	    String line = null;
	    while((line = in.readLine()) != null) {
		buf.append(line);
	    }
	    String bufStr = buf.toString();
	    int index = bufStr.indexOf(marker);
	    if (index != -1) {
	        for (int i = 1; i <= MAX_CHARS; i++) {
		    char a = bufStr.charAt(index - i);
		    if (!Character.isDigit(a) && a != '.') {
			coverageString = new String(chars, MAX_CHARS - i, i);
			break;
		    } else {
			chars[MAX_CHARS - i] = a;
		    }
		}
	    }

            int index4 =  bufStr.indexOf(marker4);
	    if (index4 != -1) {
		int index2 =  bufStr.indexOf(marker2);
		int index3 = bufStr.indexOf(marker3);
		String testedCount = bufStr.substring(index2 + marker2.length(), index3);
		String untestedCount = bufStr.substring(index3 + marker3.length(), index4);
		System.err.println("*** " + fullPath);
                System.err.println("&&&&&&&& TESTED COUNT  : " + testedCount);
                System.err.println("&&&&&&&& UNTESTED COUNT: " + untestedCount);
                System.err.println("***");
                System.err.println();
		updateCounters(testedCount, untestedCount, fullPath);
	    }
	} catch(Exception e) {
	    System.out.println(e);
	    System.err.println("Error processing file \"" + fullPath + "\", continuing...");
	} finally {
	    try { in.close(); } catch(Exception e) {}
	}
	return coverageString;
    }

    private void createTechSummary(String tech, List testFiles, TBody tbody) {
	String formattedTech = formatTech(tech);
	a apiCoverageA = null;
	a specCoverageA = null;
	int numFiles = (testFiles == null) ? 0 : testFiles.size();
	for (int i = 0; i < numFiles; i++) {
	    String file     = (String)testFiles.get(i);
	    int    index    = file.lastIndexOf("/");
	    String filename = file.substring(index + 1);
	    if (file.indexOf("api") != -1) {
		String apiCoverage = scrapeCoverage(file);
		apiCoverageA = new a(file, apiCoverage + "%");
	    } else if (file.indexOf("spec") != -1) {	
		String specCoverage = scrapeCoverage(file);
		specCoverageA = new a(file, specCoverage + "%");
	    } else {
		System.err.println("Not an API or SPEC summary file, skipping \""
				   + file + "\"");
	    }
	}
	addRow(formattedTech, specCoverageA, apiCoverageA, tbody);
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

    private void addAssertionSummary() {
	DecimalFormat formatter = new DecimalFormat("###.##");
	TBody tbody = new TBody()
	    .addElement(new TR()
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("&nbsp;"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Tested"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Untested"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Totals"))))
		.addElement(new TD().setAlign("Top")
		    .addElement(new center(new b("Percent Tested")))));
        
        int totalSpecAssertions = testedSpecAssertions + untestedSpecAssertions;
	int totalAPIAssertions  = untestedAPIAssertions + testedAPIAssertions;
        int testedAssertions    = testedAPIAssertions + testedSpecAssertions;
        int untestedAssertions  = untestedAPIAssertions + untestedSpecAssertions;
	int totalAssertions     = testedAssertions + untestedAssertions;
	double percentageSpec   = ((double)testedSpecAssertions / (double)totalSpecAssertions) * 100.0;
	double percentageAPI    = ((double)testedAPIAssertions / (double)totalAPIAssertions) * 100.0;
	double percentageTotal  = ((double)testedAssertions / (double)totalAssertions) * 100.0;

	TR tr = new TR();
	tbody.addElement(tr);
	tr.addElement(new TD().setAlign("Top").addElement("Spec Assertions"));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(testedSpecAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(untestedSpecAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(totalSpecAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(formatter.format(percentageSpec) + "%"));

	tr = new TR();
	tbody.addElement(tr);
	tr.addElement(new TD().setAlign("Top").addElement("API Assertions"));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(testedAPIAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(untestedAPIAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(totalAPIAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(formatter.format(percentageAPI) + "%"));

	tr = new TR();
	tbody.addElement(tr);
	tr.addElement(new TD().setAlign("Top").addElement("Totals"));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(testedAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(untestedAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(String.valueOf(totalAssertions)));
	tr.addElement(new TD().setAlign("Top").addElement(formatter.format(percentageTotal) + "%"));

	doc.appendBody(new Table(1).setCellPadding(2)
	    .setCellSpacing(2).setWidth("100%")
	    .addElement(tbody));
    }

    private void timeStamp() {
	doc.appendBody(new BR());
	String date = "Page last updated on " + DateUtil.instance().getFullDate();
	doc.appendBody(new Font().setSize("+0").addElement(date));
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
        doc.appendBody(new u(new H3("Assertion Totals")));
        addAssertionSummary();
	doc.appendBody(new br());
	doc.appendBody(new HR("100%", "Center", 2));
	doc.appendBody(new Font().setSize("+0").addElement(FOOTER));
	timeStamp();	
	writeFile();
    }

} // end class SummaryBuilder
