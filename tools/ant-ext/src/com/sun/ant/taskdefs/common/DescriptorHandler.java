/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */


package  com.sun.ant.taskdefs.common;

import  java.util.*;
import  java.io.*;
import  java.net.*;
import  org.xml.sax.InputSource;
import  org.xml.sax.SAXException;
import  org.xml.sax.AttributeList;
import  org.apache.tools.ant.*;


/**
 * Inner class used by EjbJar to facilitate the parsing of deployment
 * descriptors and the capture of appropriate information. Extends
 * HandlerBase so it only implements the methods needed. During parsing
 * creates a hashtable consisting of entries mapping the name it should be
 * inserted into an EJB jar as to a File representing the file on disk. This
 * list can then be accessed through the getFiles() method.
 */
public class DescriptorHandler extends org.xml.sax.HandlerBase {
    protected Task owningTask;
    protected String publicId = null;
    /**
     * The state of the parsing
     */
    //private int parseState = STATE_LOOKING_EJBJAR;
    /**
     * Instance variable used to store the name of the current element being
     * processed by the SAX parser.  Accessed by the SAX parser call-back methods
     * startElement() and endElement().
     */
    protected String currentElement = null;
    /**
     * The state of the parsing
     */
    protected int parseState = 0;
    /**
     * The text of the current element
     */
    protected String currentText = null;
    protected Hashtable fileDTDs = new Hashtable();
    protected Hashtable resourceDTDs = new Hashtable();
    protected Hashtable urlDTDs = new Hashtable();
    /**
     * The directory containing the bean classes and interfaces. This is
     * used for performing dependency file lookups.
     */
    protected File srcDir;
    /**
     * Instance variable that stores the value found in <servlet-name> element
     */
    protected String name = null;
    /**
     * Instance variable that stores the names of the files as they will be
     * put into the jar file, mapped to File objects  Accessed by the SAX
     * parser call-back method characters().
     */
    protected Hashtable files = null;

    public DescriptorHandler (Task task, File srcDir) {
        this.owningTask = task;
        this.srcDir = srcDir;
    }

    /**
     * SAX parser call-back method that is used to initialize the values of some
     * instance variables to ensure safe operation.
     */
    public void startDocument () throws SAXException {
        this.files = new Hashtable(10, 1);
        this.currentElement = null;
    }

    /**
     * SAX parser call-back method that is invoked when a new element is entered
     * into.  Used to store the context (attribute name) in the currentAttribute
     * instance variable.
     * @param name The name of the element being entered.
     * @param attrs Attributes associated to the element.
     */
    public void startElement (String name, AttributeList attrs) throws SAXException {
        this.currentElement = name;
        currentText = "";
    }

    public void registerDTD (String publicId, String location) {
        if (location == null) {
            return;
        }
        File fileDTD = new File(location);
        if (fileDTD.exists()) {
            if (publicId != null) {
                fileDTDs.put(publicId, fileDTD);
                owningTask.log("Mapped publicId " + publicId + " to file " + fileDTD,
                        Project.MSG_VERBOSE);
            }
            return;
        }
        if (getClass().getResource(location) != null) {
            if (publicId != null) {
                resourceDTDs.put(publicId, location);
                owningTask.log("Mapped publicId " + publicId + " to resource " +
                        location, Project.MSG_VERBOSE);
            }
        }
        try {
            if (publicId != null) {
                URL urldtd = new URL(location);
                urlDTDs.put(publicId, urldtd);
            }
        } catch (java.net.MalformedURLException e) {
        //ignored
        }
    }

    public InputSource resolveEntity (String publicId, String systemId) throws SAXException {
        this.publicId = publicId;
        File dtdFile = (File)fileDTDs.get(publicId);
        if (dtdFile != null) {
            try {
                owningTask.log("Resolved " + publicId + " to local file " + dtdFile,
                        Project.MSG_VERBOSE);
                return  new InputSource(new FileInputStream(dtdFile));
            } catch (FileNotFoundException ex) {
            // ignore
            }
        }
        String dtdResourceName = (String)resourceDTDs.get(publicId);
        if (dtdResourceName != null) {
            InputStream is = this.getClass().getResourceAsStream(dtdResourceName);
            if (is != null) {
                owningTask.log("Resolved " + publicId + " to local resource " +
                        dtdResourceName, Project.MSG_VERBOSE);
                return  new InputSource(is);
            }
        }
        URL dtdUrl = (URL)urlDTDs.get(publicId);
        if (dtdUrl != null) {
            try {
                InputStream is = dtdUrl.openStream();
                owningTask.log("Resolved " + publicId + " to url " + dtdUrl, Project.MSG_VERBOSE);
                return  new InputSource(is);
            } catch (IOException ioe) {
            //ignore
            }
        }
        owningTask.log("Could not resolve ( publicId: " + publicId + ", systemId: "
                + systemId + ") to a local entity", Project.MSG_INFO);
        return  null;
    }

    /**
     * Get the publicId of the DTD
     */
    public String getPublicId () {
        return  publicId;
    }

    /**
     * SAX parser call-back method invoked whenever characters are located within
     * an element.  currentAttribute (modified by startElement and endElement)
     * tells us whether we are in an interesting element (one of the up to four
     * classes of an EJB).  If so then converts the classname from the format
     * org.apache.tools.ant.Parser to the convention for storing such a class,
     * org/apache/tools/ant/Parser.class.  This is then resolved into a file
     * object under the srcdir which is stored in a Hashtable.
     * @param ch A character array containing all the characters in
     *        the element, and maybe others that should be ignored.
     * @param start An integer marking the position in the char
     *        array to start reading from.
     * @param length An integer representing an offset into the
     *        char array where the current data terminates.
     */
    public void characters (char[] ch, int start, int length) throws SAXException {
        currentText += new String(ch, start, length);
    }

    /**
     * Getter method that returns the set of files to include in the WEB war.
     */
    public Hashtable getFiles () {
        return  (files == null) ? new Hashtable() : files;
    }

    /**
     * Getter method that returns the value of the <servlet-name> element.
     */
    public String getName () {
        return  name;
    }
}



