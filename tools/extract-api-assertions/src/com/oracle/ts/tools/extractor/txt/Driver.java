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

package com.oracle.ts.tools.extractor.txt;

import java.util.regex.*;
import java.io.*;
import java.util.*;
import com.sun.cts.common.FileFinder;
import com.sun.cts.common.FileAcceptor;

public class Driver implements FileAcceptor, FilenameFilter {

	private static final String NL = System.getProperty("line.separator", "\n");
	private static final String DATE = new Date().toString();
	private static final String REGEX = "[0-9]+";
	private static final String TEXT_REGEX = "(.*\\.html$)";
	private boolean verbose = Boolean.getBoolean("verbose");
	private File inDir;
	private File outFile;
	private String prefix = "";
	private String textFilterExp = TEXT_REGEX;
	private BufferedWriter bw = null;
	private int fileCounter = 1;
	private Map<String, String> IDs = new HashMap<String, String>();
	private File currentFileBeingProcessed;
	private String relativePath;
	private Pattern idPattern;
	private Pattern filenamePattern;

	Driver(String pre, String in, String out, String textFilter)
			throws FileNotFoundException, IOException {
		prefix = pre;
		inDir = new File(in);
		if (!inDir.isDirectory()) {
			throw new IOException("Error, \"" + in + "\" is not a directory.");
		}
		outFile = new File(out);
		textFilterExp = textFilter;
		System.err.println("Prefix          : " + "\"" + prefix + "\"");
		System.err.println("Input Directory : " + "\"" + inDir + "\"");
		System.err.println("Output File     : " + "\"" + outFile + "\"");
		System.err.println("File Name Filter: " + "\"" + textFilterExp + "\"");
	}

	private String getDocHeader() {
		StringBuffer sb = new StringBuffer(75);
		sb.append("<?xml version=\"1.0\"?>").append(NL).append(NL);
		sb.append("<?xml-stylesheet type=\"text/xsl\"").append(NL);
		sb.append(
				"href=\"http://invalid.domain.com/CTS/XMLassertions/xsl/javadoc_assertions.xsl\"?>")
				.append(NL);
		sb.append(
				"<!DOCTYPE javadoc SYSTEM \"http://invalid.domain.com/CTS/XMLassertions/dtd/javadoc_assertions.dtd\">")
				.append(NL);
		sb.append(NL).append("<!-- Generated on " + DATE + " -->").append(NL);
		sb.append("<javadoc>" + NL);
		sb.append("\t<next-available-id></next-available-id>" + NL);
		sb.append("\t<previous-id></previous-id>" + NL);
		sb.append("\t<technology></technology>" + NL);
		sb.append("\t<id></id>" + NL);
		sb.append("\t<name></name>" + NL);
		sb.append("\t<version></version>" + NL);
		sb.append("\t<assertions>" + NL);
		return sb.toString();
	}

	protected String getDocTail() {
		return "\t</assertions>" + NL + NL + "</javadoc>";
	}

	private String formatAssertion(String comment, String description, String id) {
		StringBuffer buf = new StringBuffer();
		buf.append("\t\t<assertion required=\"true\" impl-spec=\"false\" "
				+ "status=\"active\" testable=\"true\">" + NL);
		buf.append("\t\t\t<id>" + id + "</id>" + NL);
		buf.append("\t\t\t<description>" + description + "</description>" + NL);
		buf.append("\t\t\t<package></package>" + NL);
		buf.append("\t\t\t<class-interface></class-interface>" + NL);
		buf.append("\t\t\t<method name=\"\" return-type=\"\"/>" + NL);
		buf.append("\t\t</assertion>" + NL);
		return buf.toString();
	}

	private boolean dupeIDs(String id) {
		boolean result = false;
		Set<String> keys = IDs.keySet();
		if (keys.contains(id)) {
			result = true;
		} else {
			IDs.put(id, currentFileBeingProcessed.getPath());
		}
		return result;
	}

	private String getFileName(String id) {
		return IDs.get(id);
	}

	public void startOutputFile() throws Exception {
		bw = new BufferedWriter(new FileWriter(outFile));
		bw.write(getDocHeader() + NL);
	}

	private void writeFileAssertions(Matcher m) throws Exception {
		String id = null;
		// for each assertion ID, write an assertion element to the output file
		// or display a warning message if the assertion is a dupe within the
		// set of files specified by the user
		while (m.find()) {
			id = m.group();
			if (dupeIDs(id)) {
				System.err.println(NL + "WARNING: ID \"" + id + "\" in file \""
						+ currentFileBeingProcessed + "\" is NOT Unique.");
				System.err.println("it also occurs in file \""
						+ getFileName(id) + "\"" + NL);
			} else {
				bw.write(formatAssertion("", "ID pulled from file: \""
						+ currentFileBeingProcessed + "\"", id));
			}
		}
	}

	public void endOutputFile() throws Exception {
		bw.write(getDocTail() + NL);
		bw.close();
	}

	private void go() throws Exception {
		// regex used to match assertion IDs
		idPattern = Pattern.compile(prefix + REGEX);
		// regex used to match file names
		filenamePattern = Pattern.compile(textFilterExp);
		startOutputFile();
		// recursively traverse input directory looking for file names that
		// match the user supplied regular expression
		FileFinder ff = new FileFinder(inDir, this, this);
		ff.process();
		endOutputFile();
		System.err.println("DONE - wrote file: " + "\"" + outFile + "\"");
	}

	/**
	 * Implementation of the FileAcceptor interface. Called by the file finder
	 * for each file where the file name matches the user supplied regular
	 * expression. Look for assertion IDs that match the regular expression
	 * based on the user supplied assertion ID prefix.
	 */
	public void acceptFile(File aFile) {
		currentFileBeingProcessed = aFile;
		relativePath = aFile.getPath().substring(inDir.getPath().length() + 1);
		StringBuffer sb = new StringBuffer();
		String line = null;
		BufferedReader br = null;
		if (verbose) {
			System.err.println(fileCounter++ + ". Processing File \"" + currentFileBeingProcessed
					+ "\"");
		}
		try {
			br = new BufferedReader(new FileReader(aFile));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			Matcher matcher = idPattern.matcher(sb.toString());
			writeFileAssertions(matcher);
		} catch (Exception e) {
			System.err.println("Could not write assertions to output file.");
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Implementation of the FilenameFilter interface. Return true for
	 * directories, so they are recursively processed, and filenames that
	 * match the user supplied regular expression.
	 */
	public boolean accept(File dir, String name) {
		boolean result = false;
		File f = new File(dir, name);
		if (f.isDirectory()) {
			result = true;
		} else if (f.isFile()) {
			Matcher m = filenamePattern.matcher(name);
			result = m.matches();
		}
		return result;
	}

	private static void usage() {
		System.err
				.println("USAGE: java Driver prefix input-file output-file text-filter");
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			Driver.usage();
			System.exit(-1);
		}
		Driver driver = new Driver(args[0], args[1], args[2], args[3]);
		driver.go();
	}

}
