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
 * This class produces an output file that matches the CTS API
 * assertion DTD.  Most of the elements are left empty except the
 * assertion ID, description (pulled from the strategy tag) and
 * the comment (a combination of the fully qualified test class
 * name and the test name).  Spec assertions are ignored.  If users
 * wish to produce a spec assertion file then use the test queue
 * handler named DefaultSpecHandler.
 */
public class DefaultAPIHandler extends QueueHandler {

    public DefaultAPIHandler(){
    }

    protected String getDocHeader() {
        StringBuffer sb = new StringBuffer(75);
	sb.append("<?xml version=\"1.0\"?>").append(NL).append(NL);
	sb.append("<?xml-stylesheet type=\"text/xsl\"").append(NL);
	sb.append("href=\"http://invalid.domain.com/CTS/XMLassertions/xsl/javadoc_assertions.xsl\"?>").append(NL);
	sb.append("<!DOCTYPE spec SYSTEM \"http://invalid.domain.com/CTS/XMLassertions/dtd/javadoc_assertions.dtd\">").append(NL);
	sb.append(NL).append(NL);
	sb.append("<javadoc>" + NL);
	sb.append("\t<next_available_id></next_available_id>" + NL);
	sb.append("\t<previous_id></previous_id>" + NL);
	sb.append("\t<technology></technology>" + NL);
	sb.append("\t<id></id>" + NL);
	sb.append("\t<name></name>" + NL);
	sb.append("\t<version></version>" + NL);
	sb.append("\t<assertions>" + NL);
        return sb.toString();
    }

    protected String getDocTail() {
        return "<\t/assertions>"+NL+NL+"</javadoc>";
    }

    protected void writeAssertionToFile(Writer fw, TestDescription td) throws IOException {
         String assertionId  = td.getParameter("assertion_ids");
         String testName     = td.getParameter("testName");
         String testStrategy = td.getParameter("test_Strategy");
         String testClass    = td.getParameter("classname");
	 //	 testClass = testClass.substring(0, testClass.lastIndexOf(".")); // remove package
 
	 String   comment      = testClass + "-" + testName; // comment element
	 String[] assertionIDs = parseAssertionIDs(assertionId); // assertion ID elements
	 String   description  = escapeStringForXML(testStrategy);

	 for (int i = 0; i < assertionIDs.length; i++) {
	     String assertionKey = comment + "-" + assertionIDs[i];
	     if ( (isAPIAssertion(assertionIDs[i])) &&
		  (!assertionKeys.contains(assertionKey)) ) {
		 String formattedAssertion =
		     formatAssertion(comment, description, assertionIDs[i]);
		 fw.write(formattedAssertion);
		 System.out.println("$$$ PROCESSED: " + comment);
		 assertionKeys.add(assertionKey);
	     }
	 }
    }

    private String formatAssertion(String comment, String description, String id) {
	StringBuffer buf = new StringBuffer();
	buf.append("\t\t<assertion required=\"\" impl-spec=\"\" "
		   + "status=\"\" testable=\"\">" + NL);
	buf.append("\t\t\t<id>" + id + "</id>" + NL);
	buf.append("\t\t\t<description>" + description + "</description>" + NL);
	buf.append("\t\t\t<keywords>" + NL);
	buf.append("\t\t\t\t<keyword></keyword>" + NL);
	buf.append("\t\t\t</keywords>" + NL);
	buf.append("\t\t\t<package></package>" + NL);
	buf.append("\t\t\t<class-interface></class-interface>" + NL);
	buf.append("\t\t\t<method name=\"\" return-type=\"\">" + NL);
	buf.append("\t\t\t\t<parameters>" + NL);
	buf.append("\t\t\t\t\t<parameter></parameter>" + NL);
	buf.append("\t\t\t\t</parameters>" + NL);
	buf.append("\t\t\t\t<throw></throw>" + NL);
	buf.append("\t\t\t</method>" + NL);
	buf.append("\t\t\t<field name=\"\" type=\"\">" + NL);
	buf.append("\t\t\t</field>" + NL);
	buf.append("\t\t\t<comment>" + comment + "</comment>" + NL);
	buf.append("\t\t\t<depends>" + NL);
	buf.append("\t\t\t\t<depend></depend>" + NL);
	buf.append("\t\t\t</depends>" + NL);
	buf.append("\t\t</assertion>" + NL);
	return buf.toString();
    }

} // end class DefaultAPIHandler
