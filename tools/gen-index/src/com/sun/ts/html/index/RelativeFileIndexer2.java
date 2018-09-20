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

public class RelativeFileIndexer2 extends BaseFileIndexer {

    private static final String TITLE         = "Assertion Documents";
    private static final String INTRO         = "This page provides links to the assertion"
	+ " documents for each technology area.  The assertion documents are"
	+ " separated by SPEC and API assertions.";
    private static final String FOOTER         = " ";//TODO:insert footer

    private java.util.Map groupedAssertionFiles = new HashMap();
    private List          summaryFiles;
    private File          indexFile;
    private String        rootDirPath;
    private Document      doc;

    public  RelativeFileIndexer2(File rootDir, File indexFile, List summaryFiles) {
	this.indexFile     = indexFile;
	this.rootDirPath   = rootDir.getPath();
	this.summaryFiles  = summaryFiles;
	groupFiles();
	System.out.println(groupedAssertionFiles);
    }
    
    private void groupFiles() {
	int numSumFiles = (summaryFiles == null) ? 0 : summaryFiles.size();
	for (int i = 0; i < numSumFiles; i++) {
	    File sumFile = (File)summaryFiles.get(i);
	    String path = sumFile.getPath();
	    String relativePath = path.substring(rootDirPath.length() + 1);
	    String technology = relativePath.substring
		(0, relativePath.indexOf(File.separator));
	    addRelativePath(groupedAssertionFiles, relativePath, technology);
	}
    }

    private void createLinkList(String tech, List assertionLinks) {
	String formattedTech = formatTech(tech);
	doc.appendBody(new u(new b("Assertion documents for " + formattedTech)));
	doc.appendBody(new BR());
	int numLinks = (assertionLinks == null) ? 0 : assertionLinks.size();
	UL ul2 = new UL();
	if (numLinks > 0) {
	    doc.appendBody(ul2);
	}
	for (int i = 0; i < numLinks; i++) {
	    String url = (String)assertionLinks.get(i);
	    String desc = getAssertionDesc(url);
	    ul2.addElement(new LI(new A(url, desc)));
	}
	doc.appendBody(new HR("100%", "Center", 2));
    }

    private void addLinks() {
	Iterator iter = groupedAssertionFiles.keySet().iterator();
	while (iter.hasNext()) {
	    String key = (String)iter.next();
	    List assertionLinks = (List)groupedAssertionFiles.get(key);
	    createLinkList(key, assertionLinks);
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

} // end class RelativeFileIndexer2
