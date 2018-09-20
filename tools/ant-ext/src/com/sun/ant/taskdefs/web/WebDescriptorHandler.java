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


package  com.sun.ant.taskdefs.web;

import  java.util.*;
import  java.io.*;
import  java.net.*;
import  org.xml.sax.InputSource;
import  org.xml.sax.SAXException;
import  org.xml.sax.AttributeList;
import  org.apache.tools.ant.*;
import  com.sun.ant.taskdefs.common.*;


/**
 * Inner class used by EjbJar to facilitate the parsing of deployment
 * descriptors and the capture of appropriate information. Extends
 * HandlerBase so it only implements the methods needed. During parsing
 * creates a hashtable consisting of entries mapping the name it should be
 * inserted into an EJB jar as to a File representing the file on disk. This
 * list can then be accessed through the getFiles() method.
 */
public class WebDescriptorHandler extends com.sun.ant.taskdefs.common.DescriptorHandler {
    private static final int STATE_LOOKING_WEBWAR = 1;
    /**
     * Bunch of constants used for storing entries in a hashtable, and for
     * constructing the filenames of various parts of the ejb jar.
     */
    private static final String SERVLET_MAPPING = "servlet-mapping";
    private static final String SERVLET_NAME = "servlet-name";
    private static final String SERVLET_CLASS = "servlet-class";
    private static final String SERVLET_FILTER_CLASS = "filter-class";
    private static final String SERVLET_LISTENER_CLASS = "listener-class";
    private static final String JSP_FILE = "jsp-file";
    private static final String ERROR_PAGE = "error-page";
    private static final String LOCATION = "location";
    private static final String WEB_APP = "web-app";
    private static final String EJB_REF = "ejb-ref";
    private static final String SERVLET = "servlet";
    private static final String BEAN_CLASS = "ejb-class";
    private static final String HOME_INTERFACE = "home";
    private static final String REMOTE_INTERFACE = "remote";
    /**
     * Instance variable that stores the names of the classes as they will be
     * put into the jar file, mapped to File objects  Accessed by the SAX
     * parser call-back method characters().
     */
    protected Hashtable classes;

    public WebDescriptorHandler (Task task, File srcDir) {
        super(task, srcDir);
    }

    public void startDocument () throws SAXException {
        super.startDocument();
        classes = new Hashtable();
    }

    public void startElement (String name, AttributeList attrs) throws SAXException {
        this.currentElement = name;
        currentText = "";
    }

    public void endElement (String name) throws SAXException {
        processElement();
        currentText = "";
        this.currentElement = "";
    }

    protected void processElement () {
        if (currentElement.equals(SERVLET_CLASS) || currentElement.equals(SERVLET_FILTER_CLASS)
                || currentElement.equals(SERVLET_LISTENER_CLASS) || currentElement.equals(REMOTE_INTERFACE)
                || currentElement.equals(HOME_INTERFACE)) {
            // Get the filename into a String object
            File classFile = null;
            String className = currentText.trim();
            // If it's a primitive wrapper then we shouldn't try and put
            // it into the jar, so ignore it.
            if (!className.startsWith("java.") && !className.startsWith("javax.")) {
                // Translate periods into path separators, add .class to the
                // name, create the File object and add it to the Hashtable.
                className = className.replace('.', File.separatorChar);
                className += ".class";
                classFile = new File(srcDir, className);
                classes.put(className, classFile);
            }
        }
        else if (currentElement.equals(LOCATION) || currentElement.equals(JSP_FILE)) {
            // Get the filename into a String object
            File contentFile = null;
            String contentName = currentText.trim();
            //			System.out.println("contentName= " +contentName);
            if (contentName.endsWith(".jsp") || contentName.endsWith(".html") ||
                    contentName.endsWith(".htm")) {
                contentFile = new File(srcDir, contentName);
                //				System.out.println("contentFile= " + contentFile.toString());
                files.put(contentName, contentFile);
            }
        }
    }

    /**
     * Getter method that returns the set of files to include in the WEB war.
     */
    public Hashtable getClasses () {
        return  (classes == null) ? new Hashtable() : classes;
    }
}



