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

import java.util.Properties;
import java.util.Enumeration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.transform.sax.SAXSource;

public class Trans {

    private static final String PROPS_FILE = "props.txt";
    private static final String USAGE           =
	"usage: xml-file xslt-file [output-file]";
    private static final String XSL_OUTPUT_FLAG = "output-mode";
    private static final String XSL_OUPUT_VALUE = "error-only";
    private Source  xmlSource;
    private Source  xsltSource;
    private Result  result;
    private boolean error;
    private Properties props = new Properties();

    private void setStreams(File xmlFile, File xsltFile, File propsFile) {
	this.xmlSource  = new StreamSource(xmlFile);
	this.xsltSource = new StreamSource(xsltFile);
	try {
	    props.load(new FileInputStream(propsFile));
	} catch (Exception e) {
	    System.out.println(e);
	    System.out.println("No Props Defined, Application Continuing...");
	}
    }

    private void setStreams(File xmlFile, File xsltFile) {
	this.xmlSource  = new StreamSource(xmlFile);
	this.xsltSource = new StreamSource(xsltFile);
	try {
	    props.load(getClass().getClassLoader().getResourceAsStream(PROPS_FILE));
	} catch (Exception e) {
	    System.out.println(e);
	    System.out.println("No Props Defined, Application Continuing...");
	}
    }

    public Trans(Source xml, Source xslt, Result transformed) {
	xmlSource = xml;
	xsltSource = xslt;
	result = transformed;
    }

    public Trans(String xmlFile, String xsltFile) throws IOException {
	this(new File(xmlFile), new File(xsltFile));
    }

    public Trans(File xmlFile, File xsltFile) throws IOException {
        checkInputFiles(xmlFile, xsltFile);
	setStreams(xmlFile, xsltFile);
	this.result = new StreamResult(System.out);
    }

    public Trans(String xmlFile, String xsltFile, String outputFile)
	throws IOException
    {
	this(new File(xmlFile), new File(xsltFile), new File(outputFile));
    }

    public Trans(String xmlFile, String xsltFile, String outputFile, File propsFile)
	throws IOException
    {
	File xml  = new File(xmlFile);
	File xslt = new File(xsltFile);
	File out  = new File(outputFile);
        checkInputFiles(xml, xslt);
	setStreams(xml, xslt, propsFile);
	this.result = new StreamResult(out);
    }

    public Trans(File xmlFile, File xsltFile, File outputFile)
	throws IOException 
    {
        checkInputFiles(xmlFile, xsltFile);
	setStreams(xmlFile, xsltFile);
	this.result = new StreamResult(outputFile);
    }

    private void checkInputFile(File file) throws IOException {
        if ((file == null) || !(file.exists() && file.isFile())) {
            throw new IOException("File does not exist \"" + file + "\"");
        }
    }

    private void checkInputFiles(File xml, File xsl) throws IOException {
        checkInputFile(xml);
        checkInputFile(xsl);
    }

    public void setError(boolean error) {
	this.error = error;
    }

    private void setParameters(Transformer trans) {
	Enumeration e = props.propertyNames();
	while (e.hasMoreElements()) {
	    String name  = (String)e.nextElement();
	    String value = props.getProperty(name, "");
	    System.out.println("[" + name + ", " + value + "]");
	    trans.setParameter(name, value);
	}
    }

    public void transform() throws Exception {
	TransformerFactory factory = TransformerFactory.newInstance();
	Transformer        trans   = factory.newTransformer(xsltSource);
	if (error) {
	    trans.setParameter(XSL_OUTPUT_FLAG, XSL_OUPUT_VALUE);
	}
	setParameters(trans);
	trans.setOutputProperty("encoding", "UTF-8");
	Source inSource = xmlSource;
    String localDTDPath = System.getProperty("local.dtd.path", "");
    if (localDTDPath.length() > 0) {
    	XMLReader reader = XMLReaderFactory.createXMLReader();
    	reader.setEntityResolver(new MyResolver(localDTDPath));
    	inSource = new SAXSource(reader, SAXSource.sourceToInputSource(xmlSource));
    }
	trans.transform(inSource, result);	
    }

    public static void main(String[] args) throws Exception {
	if (args.length < 2 || args.length > 4) {
	    System.out.println(USAGE);
	    System.exit(1);
	}

	Trans tr;
	if (args.length == 2) {
	    tr = new Trans(args[0], args[1]);
	} else if (args.length == 3) {
	    if (args[2].equalsIgnoreCase("true")) {
		tr = new Trans(args[0], args[1]);
		tr.setError(true);
	    } else if (args[2].equalsIgnoreCase("false")) {
		tr = new Trans(args[0], args[1]);
	    } else {
		tr = new Trans(args[0], args[1], args[2]);
	    }
	} else {
	    tr = new Trans(args[0], args[1], args[2]);
	    if (args[3].equalsIgnoreCase("true")) {
		tr.setError(true);
	    }
	}
	tr.transform();
    }
    
    
    static class MyResolver implements EntityResolver {
    	private File localDTDPath;
    	MyResolver(String localPath) throws FileNotFoundException {
    		this.localDTDPath = new File(localPath);
    		if (!localDTDPath.isDirectory()) {
    			throw new FileNotFoundException(localPath);
    		}
    	}
     	public InputSource resolveEntity (String publicId, String systemId) {
            InputSource result = null;
            String fileName = systemId.substring(systemId.lastIndexOf("/") + 1);
     	    if (fileName.endsWith(".dtd")) {
    	      String path = localDTDPath.getPath() + File.separator + fileName;
     	      result = new InputSource(path);
     	    } else {
     	    	result = new InputSource(fileName);
     	    }
            System.err.println("Entity Resolver returning \"" + result.getSystemId() + "\"");
     	    return result;
     	}
    }

}
