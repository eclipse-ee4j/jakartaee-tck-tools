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

package com.sun.cts.util;

import java.io.*;
import java.util.*;

public class NumberDoc {

    private static final int    DEFAULT_START_NUM     = 1;
    private static final int    DEFAULT_INCREMENT_NUM = 1;
    private static final String DEFAULT_PREFIX        = "";
    private static final String DEFAULT_SUFFIX        = "";
    private static final String DEFAULT_REPLACE_TAG   = "__NUMBER__";
    private static final String OUTPUT_EXT            = ".out";

    private static final String START_NUM_FLAG       = "-start_num";
    private static final String INCREMENT_NUM_FLAG   = "-inc_num";
    private static final String REPLACE_TAG_FLAG     = "-replace_tag";
    private static final String INPUT_FILE_FLAG      = "-input_file";
    private static final String OUTPUT_FILE_FLAG     = "-output_file";
    private static final String PREFIX_FLAG          = "-prefix";
    private static final String SUFFIX_FLAG          = "-suffix";
    private static final String CALLED_FROM_ANT_FLAG = "-called_from_ant";
    private static final String[] FLAGS = {START_NUM_FLAG,
					   INCREMENT_NUM_FLAG,
					   REPLACE_TAG_FLAG,
					   INPUT_FILE_FLAG,
					   OUTPUT_FILE_FLAG,
					   PREFIX_FLAG,
					   SUFFIX_FLAG,
					   CALLED_FROM_ANT_FLAG};

    private static final String USAGE ="run -input_file file"
	+ " [-output_file file] [-replace_tag tag] [-inc_num number]"
	+ " [-start_num number] [-prefix_flag prefix_string]"
	+ " [-suffix_flag suffix_string]";

    private static final String ANT_USAGE ="ant run -Dinput_file=file"
	+ " [-Doutput_file=file] [-Dreplace_tag=tag] [-Dstart_num=number]"
	+ " [-Dinc_num=number] [-Dprefix_flag=prefix_string]"
	+ " [-Dsuffix_flag=suffix_string]";


    private int     startNum;
    private int     increment;
    private String  replaceString;
    private File    inputFile;
    private File    outputFile;
    private String  prefix;
    private String  suffix;
    private boolean calledFromAnt;

    public NumberDoc() {
	startNum      = DEFAULT_START_NUM ;
	increment     = DEFAULT_INCREMENT_NUM;
	replaceString = DEFAULT_REPLACE_TAG;
	prefix        = DEFAULT_PREFIX;
	suffix        = DEFAULT_SUFFIX;
    }

    private void setStartNum(int num) {
	startNum = num;
    }

    private void setIncrement(int num) {
	increment = num;
    }

    private void setReplaceString(String str) {
	replaceString = str;
    }

    private void setInputFile(String file) throws IOException {
	File inputFile = new File(file);
	if (!inputFile.exists() || !inputFile.isFile()) {
	    throw new IOException("No such file \"" + file + "\"");
	}
	this.inputFile = inputFile;
	this.outputFile =  new File(file + OUTPUT_EXT);
    }

    private void setOutputFile(String file) throws IOException {
	outputFile =  new File(file);
	if (outputFile.exists()) {
	    outputFile.delete();
	}
	outputFile.createNewFile();
    }

    private void setPrefix(String prefix) {
	this.prefix = prefix;
    }

    private void setSuffix(String suffix) {
	this.suffix = suffix;
    }

    private void processCommandLineArgs(String[] args) throws Exception {
	CommandLineArgProc clp = new CommandLineArgProc(FLAGS, args);
	clp.processArgs();
	
	if (clp.hasErrors()) {
	    System.out.println("Errors found on command line, Errant flags are:");
	    List errors = clp.getErrantFlags();
	    for (int i = 0; i < errors.size(); i++) {
		System.out.println("\t" + (String)(errors.get(i)));
	    }
	    throw new Exception("Error, errant flags");
	}
	
	String temp = null;

	if (clp.flagPresent(CALLED_FROM_ANT_FLAG)) {
	    calledFromAnt = true;
	}

	if (clp.flagPresent(INPUT_FILE_FLAG)) {
	    if ((temp = clp.getFlagValue(INPUT_FILE_FLAG)) != null) {
		setInputFile(temp);
	    } else {
		throw new Exception("Required value missing for flag " +
				    INPUT_FILE_FLAG + "\"");
	    }
	} else {
	    throw new Exception("Required Flag Missing " + INPUT_FILE_FLAG + "\"");
	}

	if (clp.flagPresent(OUTPUT_FILE_FLAG)) {
	    if ((temp = clp.getFlagValue(OUTPUT_FILE_FLAG)) != null) {
		setOutputFile(temp);
	    } else {
		throw new Exception("Required value missing for flag " +
				    OUTPUT_FILE_FLAG + "\"");
	    }
	}

	if (clp.flagPresent(REPLACE_TAG_FLAG)) {
	    if ((temp = clp.getFlagValue(REPLACE_TAG_FLAG)) != null) {
		setReplaceString(temp);
	    } else {
		throw new Exception("Required value missing for flag " +
				    REPLACE_TAG_FLAG + "\"");
	    }
	}

	if (clp.flagPresent(INCREMENT_NUM_FLAG)) {
	    if ((temp = clp.getFlagValue(INCREMENT_NUM_FLAG)) != null) {
		setIncrement( (Integer.valueOf(temp)).intValue() );
	    } else {
		throw new Exception("Required value missing for flag " +
				    INCREMENT_NUM_FLAG + "\"");
	    }
	}
	
	if (clp.flagPresent(PREFIX_FLAG)) {
	    if ((temp = clp.getFlagValue(PREFIX_FLAG)) != null) {
		setPrefix(temp);
	    } else {
		throw new Exception("Required value missing for flag " +
				    PREFIX_FLAG + "\"");
	    }
	}
	
	if (clp.flagPresent(SUFFIX_FLAG)) {
	    if ((temp = clp.getFlagValue(SUFFIX_FLAG)) != null) {
		setSuffix(temp);
	    } else {
		throw new Exception("Required value missing for flag " +
				    SUFFIX_FLAG + "\"");
	    }
	}
	
	if (clp.flagPresent(START_NUM_FLAG)) {
	    if ( (temp = clp.getFlagValue(START_NUM_FLAG)) != null) {
		setStartNum( (Integer.valueOf(temp)).intValue() );
	    } else {
		throw new Exception("Required value missing for flag " +
				    START_NUM_FLAG + "\"");
	    }
	}
    }
    

    private int[] getLineIndices(String line) {
	List indices = new ArrayList();
	int index = 0;
	while ((index = line.indexOf(replaceString, index)) > -1) {
	    indices.add(new Integer(index));
	    index++;
	}
	int numElements = indices.size();
	int[] result = new int[numElements];
	for (int i = 0; i < numElements; i++) {
	    result[i] = ((Integer)(indices.get(i))).intValue();
	}
	return result;
    }

    private String numberLine(String line) {
	StringBuffer buf = new StringBuffer(line);
	if (updateStartNum) setNumbers(buf); // hook into the number update code
	int[] indices = getLineIndices(line);
	for (int i = 0; i < indices.length; i++) {
	    int replaceIndexStart = indices[i];
	    int replaceIndexEnd = replaceIndexStart + replaceString.length();
	    String number = this.prefix + String.valueOf(startNum) + this.suffix;
	    startNum += increment;
	    buf.replace(replaceIndexStart, replaceIndexEnd, number);
	}
	return buf.toString();
    }
    
    private void go() throws Exception {
	BufferedReader reader  = new BufferedReader(new FileReader(inputFile));
	BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
	String line = null;
	try {
	    while ((line = reader.readLine()) != null) {
		writer.write(numberLine(line) + "\n");
	    }
	    System.out.println("Input read from file  : \"" + inputFile + "\"");
	    System.out.println("Output written to file: \"" + outputFile + "\"");
	} finally {
	    try {
		reader.close();
		writer.close();
	    } catch (Exception e) {}
	}
    }

    /////// START Number Scan Code

    private static final String NEXT_ID_BEGIN_TAG     = "<next-available-id>";
    private static final String NEXT_ID_END_TAG       = "</next-available-id>";
    private static final String PREVIOUS_ID_BEGIN_TAG = "<previous-id>";
    private static final String PREVIOUS_ID_END_TAG   = "</previous-id>";

    private boolean updateStartNum;
    private int     nextStartNumber;
    private int     previousStartNumber;

    private void setNumbers(StringBuffer buf) {
	String str = buf.toString();
	//	System.out.println("setNumbers() str = " + str);
	if (containsNextID(str.trim())) {
	    String value = String.valueOf(nextStartNumber);
	    int startIndex = str.indexOf(NEXT_ID_BEGIN_TAG) + NEXT_ID_BEGIN_TAG.length();
	    int endIndex = str.indexOf(NEXT_ID_END_TAG);
	    buf.replace(startIndex, endIndex, value);
	} else if (containsPreviousID(str.trim())) {
	    String value = String.valueOf(previousStartNumber);
	    int startIndex = str.indexOf(PREVIOUS_ID_BEGIN_TAG) + PREVIOUS_ID_BEGIN_TAG.length();
	    int endIndex = str.indexOf(PREVIOUS_ID_END_TAG);
	    buf.replace(startIndex, endIndex, value);
	}
    }

    private boolean containsNextID(String line) {
	return line.startsWith(NEXT_ID_BEGIN_TAG)
	    && line.endsWith(NEXT_ID_END_TAG);
    }
    
    private boolean containsPreviousID(String line) {
	return line.startsWith(PREVIOUS_ID_BEGIN_TAG)
	    && line.endsWith(PREVIOUS_ID_END_TAG);
    }
    
    private int extractStartNum(String line) {
	int beginIndex = NEXT_ID_BEGIN_TAG.length();
	int endIndex = line.indexOf(NEXT_ID_END_TAG);
	if (beginIndex >= endIndex) return DEFAULT_START_NUM;
	String number = line.substring(beginIndex, endIndex);
	int result = Integer.parseInt(number);
	return result;
    }
    
    private int countSubstitutions(BufferedReader reader) {
	int result = 0;
	String line = null;
	try {
	    while ((line = reader.readLine()) != null) {
		if (line.indexOf(replaceString) != -1) {
		    result += 1;
		}
	    }
	} catch (Exception e) {
	    return 0;	
	}
	return result;
    }

    private void scanForStartID() throws Exception {
	BufferedReader reader  = new BufferedReader(new FileReader(inputFile));
	reader.mark(4096);
	String line = null;
	String trimmedLine = null;
	try {
	    while ((line = reader.readLine()) != null) {
		trimmedLine = line.trim();
		if (containsNextID(trimmedLine)) {
		    int startNum = extractStartNum(trimmedLine);
		    setStartNum(startNum);
		    this.previousStartNumber = startNum;
		    reader.reset();
		    this.nextStartNumber = countSubstitutions(reader) + startNum;
		    this.updateStartNum = true;
// 		    System.out.println("startNum=" + startNum + " previous=" + previousStartNumber
// 				       + " next=" + nextStartNumber + " update=" + updateStartNum);
		    break;
		}
	    }
	} finally {
	    try {
		reader.close();
	    } catch (Exception e) {}
	}
    }

    /////// END Number Scan Code

    private String getUsage() {
	if (calledFromAnt) {
	    return ANT_USAGE;
	} else {
	    return USAGE;
	}
    }

    public static void main(String[] args) {
	if (args.length == 0) {
	    System.out.println(USAGE);
	    System.exit(1);
	}
	NumberDoc numberDoc = null;
	try {
	    numberDoc = new NumberDoc();
	    numberDoc.processCommandLineArgs(args);
	    numberDoc.scanForStartID();
	    numberDoc.go();
	} catch (Exception e) {
	    System.out.println("Error: " + e);
	    System.out.println();
	    System.out.println();
	    e.printStackTrace();
	    System.out.println();
	    System.out.println();
	    System.out.println("USAGE: " + numberDoc.getUsage());
	}
    }

} // end class NumberDoc
