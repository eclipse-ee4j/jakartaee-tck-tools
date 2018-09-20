/*
 * Copyright (c) 2004, 2018 Oracle and/or its affiliates. All rights reserved.
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

/*
 * $Id$
 */


package  com.sun.ant.taskdefs.common;

import  java.io.*;
import  java.util.*;
import  org.apache.tools.ant.*;
import  org.apache.tools.ant.taskdefs.*;
import  com.sun.ant.TSLogger;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

public class DoTarget extends Task {
    private boolean foundTarget=false;
    private String srcdir;
    private String todo;
    private String includes;
    private String excludes;
    //arrays used to set ds.  They are not attributes
    private String[] includesArray;
    private String[] excludesArray;

    // use the inner case below as the handler
    DoTargetHandler handler = null;
    // Use the default (non-validating) parser
    SAXParserFactory factory = null;
    SAXParser saxParser = null;

    public void setIncludes (String inc) {
        includes = inc;
    }

    public void setExcludes (String exc) {
        excludes = exc;
    }

    private void setupParser () throws 
         javax.xml.parsers.ParserConfigurationException, 
         org.xml.sax.SAXException {
           // Use an instance of ourselves as the SAX event handler
           handler = new DoTargetHandler();
           handler.setTarget(todo);

           // Use the default (non-validating) parser
           factory = SAXParserFactory.newInstance();
           saxParser = factory.newSAXParser();
    }

    private void myinit () {
        if (excludes != null && excludes.length() != 0) {
            StringTokenizer st1 = new StringTokenizer(excludes, ", \t\n\r\f");
            int tokens1 = st1.countTokens();
            excludesArray = new String[tokens1];
            for (int i = 0; i < tokens1; i++) {
                String item = st1.nextToken();
		if(item.endsWith("build.xml")) {
		    excludesArray[i] = item;
		    continue;
		}
                if (!item.endsWith("/") && !item.endsWith("\\")) {
                    item += "/";
                }
                excludesArray[i] = item;
            }
        }
        if (includes == null || includes.length() == 0) {
            includesArray = new String[1];
            includesArray[0] = "**/build.xml";
        }
        else {
            StringTokenizer st2 = new StringTokenizer(includes, ", \t\n\r\f");
            int tokens2 = st2.countTokens();
            includesArray = new String[tokens2];
            for (int i = 0; i < tokens2; i++) {
                String item = st2.nextToken();
                if (item.endsWith("build.xml")) {
                    includesArray[i] = item;
                    continue;
                }
                if (!item.endsWith("/") && !item.endsWith("\\")) {
                    item += "/";
                }
                item += "**/build.xml";
                includesArray[i] = item;
            }
        }       //includes else
    }

    public void execute () throws BuildException {
	File currentDir = null;  //used only for logging
        String[] buildfiles = null;
	try {
            myinit();
            setupParser();
            DirectoryScanner ds = new DirectoryScanner();
            ds.setIncludes(includesArray);
            ds.setExcludes(excludesArray);
            ds.setBasedir(new File(srcdir));
            ds.scan();
            buildfiles = ds.getIncludedFiles();
        } catch (Throwable th) {
	    th.printStackTrace();
	    TSLogger.addFailedDir("While initializing doTarget and scanning.");
	   }
        // Scan throught the list of build files and look for those that 
        // have the target we want
        for (int i = 0; i < buildfiles.length; i++) {
            try {
                File leafBuildFile = new File(srcdir, buildfiles[i]);
                handler.initResult();
                saxParser.parse( leafBuildFile, handler);
                if (handler.getResult()) {
                    File leafDir = new File(srcdir, buildfiles[i]).getParentFile();
                    currentDir = leafDir;
                    Ant antTask = new Ant();
                    antTask.setProject(project);
                    antTask.init();
                    antTask.setInheritAll(false);
                    antTask.setAntfile("build.xml");
                    antTask.setDir(leafDir);
                    antTask.setTarget(this.todo);
                    String msg = "Entering " + leafDir.getPath();
                    log(msg);
                    antTask.execute();
                    antTask = null;
               }
            } catch (Throwable th) {
                th.printStackTrace();
                TSLogger.addFailedDir(currentDir == null ? "Unknown" : currentDir.getPath());
            }
        }
    }

    public void setSrcdir (String srcdir) {
        this.srcdir = srcdir;
    }
    public void setTodo (String todo) {
        this.todo = todo;
    }
}

// this is the inner class
class DoTargetHandler extends DefaultHandler {

    private boolean foundTarget=false;
    private String todo=null;

    public DoTargetHandler(){}

    public void setTarget(String todo){
        this.todo = todo;
    }
    public boolean getResult(){
        return this.foundTarget;
    }
    public void initResult(){
        this.foundTarget=false;
    }

    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================

    public void startDocument() throws SAXException { }

    public void endDocument() throws SAXException { }

    public void startElement(String namespaceURI,
                             String lName, // local name
                             String qName, // qualified name
                             Attributes attrs)
    throws SAXException
    {
        String eName = lName; // element name
        if ("".equals(eName)) eName = qName; // namespaceAware = false
        if (eName.equals("target")){
           if (attrs != null) {
               for (int i = 0; i < attrs.getLength(); i++) {
                   String aName = attrs.getLocalName(i); // Attr name 
                   if ("".equals(aName)) aName = attrs.getQName(i);
                   if (aName.equals("name")){
                      String value=attrs.getValue(i);
                      if (value.equals(todo)){
                         foundTarget=true;
                      }
                   }
               }
           }
        }
    }

    public void endElement(String namespaceURI,
                           String sName, // simple name
                           String qName  // qualified name
                          ) throws SAXException { }
}
