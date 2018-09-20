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

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

/**
 * rules for comapt build.xml
 * no compile target;
 * build target is optional. If present, must depend on package target;
 * package target is mandatory, and it must contain 1 package task, which in
 * turn contains 2 and only 2 copy tasks;
 * init.compat is mandatory.  no way to validate this. We cannot get here if no present.
 * compile.compat is mandatory, and must depend on init.compat;
 * either build.compat or package.compat must be present. They must depend on
 * init.compat and compile.compat.  They must contain 1 package task;
 * some elements can not appear inside build or package target.
 *
 */
class CompatValidator {
    private String msg = "";
    private boolean hasBuild;
    private boolean hasPackage;
    private boolean hasInitCompat;
    private boolean hasCompileCompat;
    private boolean hasBuildCompat;
    private boolean hasPackageCompat;
    private Element buildCompatTarget;

    public static void main(String[] args) throws Exception {
	String[] s = {
	    "D:/compat.xml", "D:/tx.xml", "D:/whitebox.xml", "D:/connection.xml"
	};
	for (int i = 0; i < s.length; i++) {
	    CompatValidator validator = new CompatValidator();
	    validator.validate(s[i]);
	    System.out.println("Result of validation:" + s[i]);
	    System.out.println(validator.getMsg());
	}
    }
    public String getMsg() {
	return msg;
    }
    public void validate(File file) {
	validate(file.getPath());
    }
    public void validate(String path) {
	File file = new File(path);
	DocumentBuilder docb = null;
	Document doc = null;
	try {
	    docb = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    doc = docb.parse(file);
	} catch (ParserConfigurationException ex) {
	    System.out.println("Failed to create a DOM parser.");
	    ex.printStackTrace();
	} catch (SAXException sax) {
	    System.out.println("Failed to parse build file:" + path);
	    sax.printStackTrace();
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
	walkNode(doc.getDocumentElement());
	if(!hasInitCompat)
	    msg += "WARN: need to define init.compat target.\n";
	if(!hasPackage)
	    msg += "WARN: need to define package target.\n";
	if(!hasCompileCompat)
	    msg += "WARN: need to define compile.compat target.\n";
	if(!hasPackageCompat && !hasBuildCompat)
	    msg += "WARN: need to define build.compat target.\n";
	if(!hasPackageCompat && hasBuildCompat) {
	    NodeList nl = buildCompatTarget.getElementsByTagName("package");
	    if(nl == null || nl.getLength() != 1) {
		msg += "WARN: build.compat target should contain 1 package task.\n";
	    }

	}
    }
    private void walkNode(Element node) {
	NodeList nl = node.getElementsByTagName("target");
	if(nl != null) {
	    for (int i = 0; i < nl.getLength(); i++) {
		walkTarget((Element) nl.item(i));
	    }
	}
//	if(node.getNodeType() == Node.ELEMENT_NODE) {
//	    String name = node.getNodeName();
//	    if(name.equals("target")) {
//		walkTarget((Element) node);
//	    }
//	}
    }
    private void checkDisallowed(String targetName, Element target) {
	String[] disallowed = {
	    "webwar", "ejb-jar", "ejbjar", "resrar", "appear", "clientjar",
	    "jar", "zip"
	};
	for (int m = 0; m < disallowed.length; m++) {
	    NodeList nl = target.getElementsByTagName(disallowed[m]);
	    if(nl != null && nl.getLength() > 0)
		msg += "WARN: " + targetName + " target should not have "
		+ disallowed[m] +" task.\n";
	}
    }
    private void walkTarget(Element target) {
	NamedNodeMap attrs = target.getAttributes();
	String name = attrs.getNamedItem("name").getNodeValue();
	if(name.equals("compile")) {
	    msg += "WARN: compile target should be removed.\n";
	    return;
	}
	ArrayList dependsList = new ArrayList();
	Node dependsNode = attrs.getNamedItem("depends");
	if(dependsNode != null) {
	    String depends = dependsNode.getNodeValue();
	    StringTokenizer st = new StringTokenizer(depends, ", \n\r\f\t");
	    while(st.hasMoreTokens()) {
		String token = st.nextToken();
		dependsList.add(token);
	    }
	}
//	System.out.println("In walkTarget: target name=" + name + " dependes="
//	    + dependsList.toString());
	if(name.equals("build")) {
	    hasBuild = true;
	    if(dependsList.size() == 1 &&
		((String) dependsList.get(0)).equals("package")
		) {
		//good
	    } else {
		msg += "WARN: build target should depend only on package target.\n";
	    }
	    checkDisallowed(name, target);
	} else if(name.equals("package")) {
	    hasPackage = true;
	    NodeList children = target.getChildNodes();
	    int count = children.getLength();
	    int countPackageTasks = 0;
	    int countCopyTasks = 0;
	    for (int i = 0; i < count; i++) {
		Node child = children.item(i);
		String childName = child.getNodeName();
		if(childName.equals("package")) {
		    countPackageTasks++;
		    NodeList copyTasks = child.getChildNodes();
		    for (int j = 0; j < copyTasks.getLength(); j++) {
			Node copyTask = copyTasks.item(j);
			String copyName = copyTask.getNodeName();
			if(copyName.equals("copy")) {
			    countCopyTasks++;
			}
		    }
		}
	    }
	    if(countPackageTasks == 0)
		msg += "WARN: package target should contain 1 package task.\n";
	    else if(countPackageTasks > 1)
		msg += "WARN: package target should contain only 1 package task.\n";
	    if(countCopyTasks != 2)
		msg += "WARN: package task inside package target should contain 2 copy tasks. One for class file and the other for archives.\n";
	    checkDisallowed(name, target);
	} else if(name.equals("init.compat")) {
	    hasInitCompat = true;
	} else if(name.equals("compile.compat")) {
	    hasCompileCompat = true;
	    if(!dependsList.contains("init.compat")) {
		msg += "WARN: compile.compat should depend on init.compat.\n";
	    }
	} else if(name.equals("build.compat")) {
	    hasBuildCompat = true;
	    buildCompatTarget = target;
	    if(!dependsList.contains("init.compat")) {
		msg += "WARN: build.compat should depend on init.compat.\n";
	    }
	    if(!dependsList.contains("compile.compat")
	        && !dependsList.contains("package.compat")
	    ) {
		msg += "WARN: build.compat should depend on compile.compat or package.compat.\n";
	    }
	} else if(name.equals("package.compat")) {
	    hasPackageCompat = true;
	    if(!dependsList.contains("init.compat")) {
		msg += "WARN: package.compat should depend on init.compat.\n";
	    }
	    if(!dependsList.contains("compile.compat")) {
		msg += "WARN: package.compat should depend on compile.compat.\n";
	    }
	    NodeList nl = target.getElementsByTagName("package");
	    if(nl == null || nl.getLength() != 1) {
		msg += "WARN: package.compat target should contain 1 package task.\n";
	    }
	}

    }
}
