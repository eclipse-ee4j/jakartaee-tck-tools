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


package com.sun.ant.taskdefs.common;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;

public class BOMVerifier extends Task  {
    private File zipfile;
    private File bomfile;
    private File bomfile2;
    private List includesfiles = new ArrayList();
    private List excludesfiles = new ArrayList();
    private List bomlist1;

    public void setZipfile(File f) {
	zipfile = f;
    }
    public void setBomfile(File f) {
	bomfile = f;
    }
    public void setBomfile2(File f) {
	bomfile2 = f;
    }
    public void setIncludesfiles(String s) {
	addFileNames(includesfiles, s);
    }
    public void setExcludesfiles(String s) {
	addFileNames(excludesfiles, s);
    }
    private void addFileNames(List fl, String s) {
	if(s == null || s.length() == 0) {
	    return;
	}
	StringTokenizer st = new StringTokenizer(s, ",");
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    fl.add(token);
	}
    }
    private void validateAttrs() {

    }
    private void initItems(List fileNames, List items, File baseDir) throws IOException{
	for (int i = 0, n = fileNames.size(); i < n; i++) {
	    String fileName = (String) fileNames.get(i);
	    File file = new File(baseDir, fileName);
	    if(!file.exists()) {
		log("File does not exist:" + fileName);
	    } else {
		readFile2List(file, items);
	    }
	}

    }
    protected void verifyBomfile(File bfile) {
	String bomPath = bomfile.getPath();
	File baseDir = project.getBaseDir();
	List includesItems = new ArrayList();
	List excludesItems = new ArrayList();
	 try {
	    initItems(includesfiles, includesItems, baseDir);
	    initItems(excludesfiles, excludesItems, baseDir);
	    if(bomlist1 == null) {
		bomlist1 = new ArrayList(1000);
		readFile2List(bomfile, bomlist1);
	    }
	    } catch (Exception ex) {
		ex.printStackTrace();
		return;
	    }
	    if(bomlist1.containsAll(includesItems)) {
		log("All required files are included in " + bomPath);
	    } else {
		includesItems.removeAll(bomlist1);
		log("WARNING: missing " + includesItems.size() + " files in "
		    + bomPath + ":");
		printList(includesItems);
	    }
	    bomlist1.retainAll(excludesItems);
	    if(bomlist1.size() == 0) {
		log("All files that should be excluded are excluded in  "
		    + bomPath);
	    } else {
		log("WARNING: " + bomlist1.size() + " files are not excluded in "
		    + bomPath + ":");
		printList(bomlist1);
	    }
    }
    protected void verifyZipfile(File zfile) {
	log("Please generate BOM first.");
    }
    private void readFile2List(File file, List list) throws IOException {
	if(list == null) {
	    return;
	}
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new FileReader(file));
	    String line = null;
	    while((line = br.readLine()) != null) {
		list.add(line);
	    }
	}
	finally {
	    if(br != null) {
		try {
		    br.close();
		}
		catch (Exception ex) {
		}
	    }
	}
    }
//    private List readBom(File file, List list) throws IOException {
//	ArrayList list = new ArrayList(1000);
//	readFile2List(file, list);
//	return
//    }
    protected void compareBoms(File b1, File b2) {
	if(b2 == null) {
	    return;
	} else if(b1 == null) {
	    log("bomfile not set and cannot compare to bomfile2 " + b2.getPath());
	    return;
	}
	if(!b1.isFile() || !b2.isFile()) {
	    log("bomfile or bomfile2 is not a regular file. Abort ...");
	    return;
	}
	//compare 2 bomfiles
	List list1 = null;
	List list2 = null;
	try {
	    if(bomlist1 == null) {
	        bomlist1 = new ArrayList(1000);
		readFile2List(bomfile, bomlist1);
		list1 = bomlist1;
	    } else {
		list1 = bomlist1;
	    }
	    list2 = new ArrayList(1000);
	    readFile2List(bomfile2, list2);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    return;
	}
	int size1 = list1.size();
	int size2 = list2.size();
	List common = null;
	if(size1 < size2) {
	    common = new ArrayList(list1);
	    if(!common.retainAll(list2)) {
		log("After reading in 2 BOM files, failed to get common entries between");
		return;
	    }
	} else {
	    common = new ArrayList(list2);
	    if(!common.retainAll(list1)) {
		log("After reading in 2 BOM files, failed to get common entries between");
		return;
	    }
	}
	if(!list1.removeAll(common)) {
	    log("After reading in 2 BOM files and getting the common entries, failed to get unique entries in bomfile1");
		return;
	}
	if(!list2.removeAll(common)) {
	    log("After reading in 2 BOM files and getting the common entries, failed to get unique entries in bomfile2");
		return;
	}
	displayComparison(list1, list2);
    }
    private void displayComparison(List list1, List list2) {
	int size1 = list1.size();
	int size2 = list2.size();
	String path1 = bomfile.getPath();
	String path2 = bomfile2.getPath();
	if(size1 == 0) {
	    if(size2 == 0) {
		log("2 bomfiles are identical:\n" + path1
		    + "\n" + path2);
	    } else {
		log("All entries in " + path1 + " are in " + path2
		    + "\n\n" + size2 + " entries in " + path2 + " are not in "
		    + path1 + ":");
		printList(list2);
	    }
	} else {
	    if(size2 == 0) {
		log("All entries in " + path2 + " are in " + path1
		    + "\n\n" + size1 + " entries in " + path1 + " are not in "
		    + path2 + ":");
		printList(list1);
	    } else {
		log(size1 + " entries in " + path1 + " are not in " + path2 + ":");
		printList(list1);
		log(size2 + " entries in " + path2 + " are not in " + path1 + ":");
		printList(list2);
	    }
	}
    }
    private void printList(List list) {
	if(list == null) {
	    return;
	}
	for (int i = 0, n = list.size() ; i < n; i++) {
	    log("\t" + (String) list.get(i));
	}
	log("* * * * * * * * * * * * * * * * * *\n");
    }
    public void execute() throws BuildException {
	validateAttrs();
	if(zipfile == null) {
	    if(bomfile == null) {
		throw new BuildException("Specify zipfile or bomfile or both.");
	    } else {
		verifyBomfile(bomfile);
	    }
	} else {
	    if(bomfile == null) {
		verifyZipfile(zipfile);
	    } else {
		verifyBomfile(bomfile);
	    }
	}
        compareBoms(bomfile, bomfile2);
    }//execute
}
