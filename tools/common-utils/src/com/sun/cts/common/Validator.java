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

package com.sun.cts.common;

import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;

public class Validator {

    /**
     * This class is only used to validate XML documents but since we
     * go to the trouble of parsing the document and creating the DOM
     * tree we may as well cache the doc and let the user retrieve if
     * they wish to do so.
     */
    private Document doc;

    /**
     * Describe variable <code>fileToValidate</code> here.
     */
    private File fileToValidate;

    private EntityResolver resolver;

    /**
     * If the variable <code>handleWarningAsError</code> is set to true,
     * the Validator class's <code>isValid</code> method  will return a
     * false value if the parser encounters any warnings while parsing.
     * If set to false, the <code>isValid</code> method will return true
     * if any warnings are encountered during parsing.
     */
    private boolean handleWarningAsError = true;

    /**
     * Creates a new <code>Validator</code> instance.
     *
     * @param fileToValidate a <code>File</code> value
     * @exception IOException if an error occurs
     */
    public Validator(File fileToValidate) throws IOException {
       	if (fileToValidate != null && fileToValidate.exists() && fileToValidate.isFile()) {
	    this.fileToValidate = fileToValidate;
	} else {
	    throw new IOException("File not found \"" + fileToValidate + "\"");
	}
    }

    public Validator(File fileToValidate, EntityResolver resolver) throws IOException {
	this(fileToValidate);
	this.resolver = resolver;
    }

    /**
     * Creates a new <code>Validator</code> instance.
     *
     * @param fileToValidate a <code>String</code> value
     * @exception IOException if an error occurs
     */
    public Validator(String fileToValidate) throws IOException {
	this(new File(fileToValidate));
    }

    /**
     * The <code>setHandleWarningAsError</code> method allows users to
     * treat warnings generated during parsing as errors.  If enabled
     * warnings will will be treated as errors and the <code>isValid</code>
     * method will return false if any erros occur during parsing.
     *
     * @param enabled a <code>boolean</code> value to enable parser
     *        warnings being treated as errors.
     */
    public void setHandleWarningAsError(boolean enabled) {
	handleWarningAsError = enabled;
    }

    /**
     * The <code>getDocument</code> method returns the parsed XML document.
     *
     * @return the parsed <code>Document</code> value
     */
    public Document getDocument() {
	return doc;
    }

    private SAXParseException ex;

    public SAXParseException getException() {
	return ex;
    }

    /**
     * The <code>MyHandler</code> class is used as the default error handler
     * when parsing the document to be validated.
     */
    public class MyHandler implements ErrorHandler {
	private boolean error;
	public boolean getError() { return error; }
	private boolean warning;
	public boolean getWarning() { return warning; }
	public void error(SAXParseException exception) {
	    error = true;
	    ex = exception;
	}
	public void fatalError(SAXParseException exception) {
	    error = true;
	    ex = exception;
	}
	public void warning(SAXParseException exception) {
	    warning = true;
	    ex = exception;
	}
    } // end inner-class MyHandler
    
    
    /**
     * The <code>isValid</code> method parses the specified document and returns
     * true if the document is valid.  The DTD or schema used is the one referenced
     * within the XML document.
     *
     * @return a <code>boolean</code> value of true if the prsed document
     *         is valid.
     * @exception Exception if an error occurs parsing the document.
     */
    public boolean isValid() throws Exception {
	MyHandler defaultHandler = new MyHandler();
	return isValid(defaultHandler);
    }

    private boolean isValid(MyHandler errorHandler) throws Exception {
	boolean result = true;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	//	System.out.println("dbf class is \"" + dbf.getClass().getName() + "\"");
        dbf.setNamespaceAware(true);
        dbf.setValidating(true);
	if (!fileHasDocType()) {
	    try {
		dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
				 "http://www.w3.org/2001/XMLSchema");
	    } catch (IllegalArgumentException iae) {
		iae.printStackTrace();
	    }
	}
        DocumentBuilder db = dbf.newDocumentBuilder();
	if (resolver != null) {
	    db.setEntityResolver(resolver);
	}
	//	System.out.println("db class is \"" + db.getClass().getName() + "\"");
	db.setErrorHandler(errorHandler);
	/* Parse the document and catch any warnings or errors in the defaultHandler. */
	this.doc = db.parse(fileToValidate);
	/* check for errors */
	if (errorHandler.getError()) {
	    result = false;
	} else if (errorHandler.getWarning() && handleWarningAsError) {
	    result = false;
	}
	this.doc = null;
	return result;
    }    

    private static final String DOCTYPE_ID = "<!DOCTYPE";
    private boolean fileHasDocType() {
	boolean result = false;
	BufferedReader reader = null;
	String line = null;
	try {
	    reader = new BufferedReader(new FileReader(fileToValidate));
	    while ((line = reader.readLine()) != null) {
		if (line.indexOf(DOCTYPE_ID) != -1) {
		    result = true;
		    break;
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    try { reader.close(); } catch (Exception e) {}
	}
	return result;
    }

    /**
     * The <code>usage</code> value holds the unit test driver command
     * line usage.
     */
    private static final String USAGE = "usage: run xml_file";

    /**
     * The <code>main</code> method can be used as a unit test driver.
     *
     * @param args a <code>String[]</code> value containing the command line
     *        arguments.
     */
    public static void main(String[] args) {
	if (args.length != 1) {
	    System.out.println(USAGE);
	    System.exit(1);
	}

	Validator vm = null;
	try {
	    vm = new Validator(args[0]);
	    boolean result = vm.isValid();
	    System.out.println("");
	    if (result) {
		System.out.println("File \"" + args[0] + "\" is valid.");
	    } else {
		System.out.println("File \"" + args[0] + "\" is NOT valid.");
	    }
	    System.out.println("");
	} catch (Exception ioe) {
	    System.out.println(ioe);
	    System.exit(1);
	}
    }

}
