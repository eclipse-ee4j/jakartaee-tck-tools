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

package com.sun.ts.html.index;

import org.apache.ecs.*;
import org.apache.ecs.html.*;
import org.apache.ecs.xhtml.*;
import java.util.*;
import java.io.*;

public class RelativeFileIndexer extends BaseFileIndexer {

    private static final String TITLE         = "Assertion Coverage Metrics";
    private static final String INTRO         = "This page provides links to the assertion coverage"
	+ " numbers for each technology tested by the CTS development team.  The assertion coverage"
	+ " numbers are broken down into two catagories: specification assertions and API"
	+ " (or javadoc) assertions.  Specification assertions are derived from a technology"
	+ " specification document, typically this document is a text document created with a"
	+ " word processing application.  API assertions are generated from a technology's"
	+ " API represented in javadoc format.";
    private static final String FOOTER         = " ";//TODO:insert footer

    private java.util.Map groupedSummaryFiles   = new HashMap();
    private java.util.Map groupedAssertionFiles = new HashMap();
    private List          summaryFiles;
    private File          indexFile;
    private String        rootDirPath;
    private Document      doc;
    private String        relativeExecSummaryPath;

    public  RelativeFileIndexer(File rootDir, File indexFile, List summaryFiles) {
	this.indexFile     = indexFile;
	this.rootDirPath   = rootDir.getPath();
	this.summaryFiles  = summaryFiles;
	groupFiles();
	System.out.println(groupedSummaryFiles);
	System.out.println(groupedAssertionFiles);
    }
    
    private void groupFiles() {
	int numSumFiles = (summaryFiles == null) ? 0 : summaryFiles.size();
	for (int i = 0; i < numSumFiles; i++) {
	    File sumFile = (File)summaryFiles.get(i);
	    String path = sumFile.getPath();
	    String relativePath = path.substring(rootDirPath.length() + 1);
	    if (path.indexOf(Driver.EX_SUMMARY_FILE) != -1) {
		relativeExecSummaryPath = relativePath;		
	    } else {
		String technology = relativePath.substring
		    (0, relativePath.indexOf(File.separator));
		if (path.indexOf(Driver.API_ASSERTIONS) != -1 ||
		    path.indexOf(Driver.SPEC_ASSERTIONS) != -1)
		    {
			addRelativePath(groupedAssertionFiles, relativePath, technology);
		    } else {
			addRelativePath(groupedSummaryFiles, relativePath, technology);
		    }
	    }
	}
    }

    private String getDesc(String url) {
	if (url.indexOf("api") != -1) {
	    return "API Coverage Metrics";
	} else if (url.indexOf("spec") != -1) {
	    return "Specification Coverage Metrics";
	} else {
	    return url;
	}
    }

    private void createLinkList(String tech, List summaryLinks, List assertionLinks) {
	String formattedTech = formatTech(tech);
	doc.appendBody(new u(new b("Assertion information for " + formattedTech)));
	doc.appendBody(new BR());
	doc.appendBody(new BR());
	doc.appendBody("Coverage Metrics:");
	UL ul = new UL();
	doc.appendBody(ul);
	int numLinks = (summaryLinks == null) ? 0 : summaryLinks.size();
	for (int i = 0; i < numLinks; i++) {
	    String url = (String)summaryLinks.get(i);
	    String desc = getDesc(url);
	    ul.addElement(new LI(new A(url, desc)));
	}
	numLinks = (assertionLinks == null) ? 0 : assertionLinks.size();
	UL ul2 = new UL();
	if (numLinks > 0) {
	    doc.appendBody("Assertion Lists:");
	    doc.appendBody(ul2);
	}
	for (int i = 0; i < numLinks; i++) {
	    String url = (String)assertionLinks.get(i);
	    String desc = getAssertionDesc(url);
	    ul2.addElement(new LI(new A(url, desc)));
	}
	doc.appendBody(new HR("100%", "Center", 2));
    }

    private void addExecSummary() {
	if (relativeExecSummaryPath != null) {
	    doc.appendBody(new BR());
	    doc.appendBody("The ");
	    doc.appendBody(new A(relativeExecSummaryPath, "Executive Summary"));
	    doc.appendBody(" contains the assertion coverage numbers for each technology");
	    doc.appendBody(" area separated by SPEC and API assertions. Each coverage");
	    doc.appendBody(" number in the table is a link to the assertion summary page");
	    doc.appendBody(" for that technology.");
	    doc.appendBody(new BR());
	    doc.appendBody(new BR());
	    doc.appendBody(new HR("100%", "Center", 2));
	}
    }

    private void addLinks() {
	addExecSummary();
	Iterator iter = groupedSummaryFiles.keySet().iterator();
	while (iter.hasNext()) {
	    String key = (String)iter.next();
	    List summaryLinks = (List)groupedSummaryFiles.get(key);
	    List assertionLinks = (List)groupedAssertionFiles.get(key);
	    createLinkList(key, summaryLinks, assertionLinks);
	}
    }

    public void build() {
	doc = new Document();
        doc.appendTitle(TITLE);
        doc.appendBody(new center(new u(new H1(TITLE))));
	doc.appendBody(new Font().setSize("+0").addElement(INTRO));
	doc.appendBody(new HR("100%", "Center", 2));
	addLinks();
	doc.appendBody(new Font().setSize("+0").addElement(FOOTER));	
	timeStamp(doc);
	writeFile(indexFile, doc);
    }

} // end class RelativeFileIndexer
