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

package com.oracle.ts.tools.extractor.pdf;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.util.regex.*;
import java.io.*;
import java.util.*;

public class Driver {

	private static final String NL = System.getProperty("line.separator", "\n");
	private static final String DATE = new Date().toString();
	private static final String REGEX1 = "\\[(\\s)*";
	private static final String REGEX2 = "(\\s)*.*?\\]";
	private File inFile;
	private File outFile;
	private String prefix = "";
	private String outputPrefix = "";
	Set<String> IDs = new HashSet<String>();
	private boolean legacyFormat;
	private boolean debug;

	Driver(String pre, String in, String out, String outPrefix) throws FileNotFoundException {
		legacyFormat = Boolean.getBoolean("legacy-format");
		debug = Boolean.getBoolean("debug");
		prefix = pre;
		if (outPrefix != null && outPrefix.length() > 0) {
			// if the user specifies an output prefeix use it
			outputPrefix = outPrefix;
		} else {
			// else set the output prefix to the input prefix with ":SPEC:"
			// concatenated to it
			outputPrefix = prefix + ":SPEC:";
		}
		inFile = new File(in);
		if (!inFile.isFile()) {
			throw new FileNotFoundException(in);
		}
		outFile = new File(out);
		System.err.println("Prefix        : " + "\"" + prefix + "\"");
		System.err.println("Output Prefix : " + "\"" + outputPrefix + "\"");
		System.err.println("Input File    : " + "\"" + inFile + "\"");
		System.err.println("Output File   : " + "\"" + outFile + "\"");
		System.err.println("Legacy Format : " + "\"" + legacyFormat + "\"");
		System.err.println("Debug         : " + "\"" + debug + "\"");
	}

	private String getDocHeader() {
		StringBuffer sb = new StringBuffer(75);
		sb.append("<?xml version=\"1.0\"?>").append(NL).append(NL);
		sb.append("<?xml-stylesheet type=\"text/xsl\"").append(NL);
		sb.append(
				"href=\"http://invalid.domain.com/CTS/XMLassertions/xsl/spec_assertions.xsl\"?>")
				.append(NL);
		sb.append(
				"<!DOCTYPE spec SYSTEM \"http://invalid.domain.com/CTS/XMLassertions/dtd/spec_assertions.dtd\">")
				.append(NL);
		sb.append(NL).append("<!-- Generated on " + DATE + " -->").append(NL);
		sb.append("<spec>" + NL);
		sb.append("\t<next-available-id></next-available-id>" + NL);
		sb.append("\t<previous-id></previous-id>" + NL);
		sb.append("\t<technology></technology>" + NL);
		sb.append("\t<id></id>" + NL);
		sb.append("\t<name></name>" + NL);
		sb.append("\t<version></version>" + NL);
		sb.append("\t<location-names>" + NL);
		sb.append("\t\t<chapters>" + NL);
		sb.append("\t\t\t<chapter id=\"\" name=\"\">" + NL);
		sb.append("\t\t\t\t<sections>" + NL);
		sb.append("\t\t\t\t\t<section id=\"\" name=\"\"/>" + NL);
		sb.append("\t\t\t\t</sections>" + NL);
		sb.append("\t\t\t</chapter>" + NL);
		sb.append("\t\t</chapters>" + NL);
		sb.append("\t</location-names>" + NL);
		sb.append("\t<assertions>" + NL);
		return sb.toString();
	}

	protected String getDocTail() {
		return "\t</assertions>" + NL + NL + "</spec>";
	}

	private String formatAssertion(String comment, String description, String id) {
		StringBuffer buf = new StringBuffer();
		buf.append("\t\t<assertion required=\"true\" impl-spec=\"false\" "
				+ "defined-by=\"technology\" status=\"active\" testable=\"true\">"
				+ NL);
		buf.append("\t\t\t<id>" + id + "</id>" + NL);
		buf.append("\t\t\t<description>" + description + "</description>" + NL);
		buf.append("\t\t\t<keywords>" + NL);
		buf.append("\t\t\t\t<keyword></keyword>" + NL);
		buf.append("\t\t\t</keywords>" + NL);
		buf.append("\t\t\t<location chapter=\"\" section=\"\"/>" + NL);
		buf.append("\t\t\t<comment>" + comment + "</comment>" + NL);
		buf.append("\t\t\t<depends order=\"\">" + NL);
		buf.append("\t\t\t\t<depend></depend>" + NL);
		buf.append("\t\t\t</depends>" + NL);
		buf.append("\t\t</assertion>" + NL);
		return buf.toString();
	}

	private boolean dupeIDs (String id) {
		return !(IDs.add(id));
	}
	
	private void writeXmlFile(Matcher m) throws Exception {
		BufferedWriter bw = null;
		String rawid = null;
		String id = null;
		try {
			bw = new BufferedWriter(new FileWriter(outFile));
			bw.write(getDocHeader() + NL);
			while (m.find()) {
				rawid =  m.group();

				// Clean up white space
				id = rawid.replaceAll(NL, "").replaceAll(" ", "").replaceAll("\t", "");
				
				// remove leading and trailing bracket
				// ex.  [prefix <number>]  ->  prefix <number>]
				id = id.substring(1);
				// ex.  prefix <number>]  ->  prefix <number>
				id = id.substring(0, id.length() -1);
				
				// remove the user specified prefix for the IDs found in the pdf
                id = id.substring(prefix.length());
                if (id.startsWith("-")) {
                	// remove leading slash if it is there, implying IDs in the spec
                	// look like this SSSSSS-D.D.D
                	id = id.substring(1);
                }
				
				if (legacyFormat) {
					// this is the way the tool worked initially, basically passing
					// IDs found in the spec as is to the output XML document replacing
					// "-"s with "."s in the number portion.
					// NOT RECOMMENDED, DO NOT USE THIS MODE
					id = id.replaceAll("-", ".");
					id = prefix + "-" + id;
				}
				
				// prepend the output prefix to properly format the assertion
				// ex.  [java] raw [WSC 2.2.5-2]  normed WSC:SPEC:2.2.5-2
				// ex.  [java] raw [WSC-3.1-1]  normed WSC:SPEC:3.1-1
				// If the legacy format is being used the output would look like this
				// ex.  [java] raw [WSC 2.2.5-2]  normed WSC:SPEC:WSC-2.2.5.2
				// ex.  [java] raw [WSC-3.1-1]  normed WSC:SPEC:WSC-3.1.1
				id = outputPrefix + id;
				
				if (debug) {
					System.err.println("*** raw ID \"" + rawid + "\"  normed ID \"" + id + "\"");
				}
				
				if (dupeIDs(id)) {
					System.err.println("WARNING: ID \"" + id + "\" is NOT Unique.");
				} else {
					bw.write(formatAssertion("", "", id));
					System.err.println("Adding assertion ID: \"" + id + "\"");
				}
			}
			bw.write(getDocTail() + NL);
			System.err.println("*** Total Unique Assertions Found: " + IDs.size());
		} finally {
			if (bw != null)
				bw.close();
		}
	}

	private void go() throws Exception {
		PDFTextStripper stripper = new PDFTextStripper();
		PDDocument doc = PDDocument.load(inFile);
		String text = stripper.getText(doc);
		String regEx = REGEX1 + prefix + REGEX2;
		Pattern p = Pattern.compile(regEx, Pattern.DOTALL);
		Matcher m = p.matcher(text);
		writeXmlFile(m);
		System.err.println("DONE - wrote file: " + "\"" + outFile + "\"");
	}

	private static void usage() {
		System.err.println("USAGE: java Driver prefix input-file output-file, output-prefix");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			Driver.usage();
			System.exit(-1);
		}
		Driver driver = new Driver(args[0].trim(), args[1].trim(), args[2].trim(), args[3].trim());
		driver.go();
	}

}
