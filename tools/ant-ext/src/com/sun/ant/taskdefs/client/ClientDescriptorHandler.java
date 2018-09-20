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


package  com.sun.ant.taskdefs.client;

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
public class ClientDescriptorHandler extends com.sun.ant.taskdefs.common.DescriptorHandler {
    /**
     * Bunch of constants used for storing entries in a hashtable, and for
     * constructing the filenames of various parts of the ejb jar.
     */
    private static final String APP_CLIENT = "application-client";
    private static final String EJB_REF = "ejb-ref";
    private static final String BEAN_CLASS = "ejb-class";
    private static final String HOME_INTERFACE = "home";
    private static final String REMOTE_INTERFACE = "remote";
    protected boolean inEJBRef = false;

    public ClientDescriptorHandler (Task task, File srcDir) {
        super(task, srcDir);
    }

    /**
     * SAX parser call-back method that is used to initialize the values of some
     * instance variables to ensure safe operation.
     */
    public void startDocument () throws SAXException {
        super.startDocument();
        inEJBRef = false;
    }

    /**
     * SAX parser call-back method that is invoked when a new element is entered
     * into.  Used to store the context (attribute name) in the currentAttribute
     * instance variable.
     * @param name The name of the element being entered.
     * @param attrs Attributes associated to the element.
     */
    public void startElement (String name, AttributeList attrs) throws SAXException {
        super.startElement(name, attrs);
        if (name.equals(EJB_REF)) {
            inEJBRef = true;
        }
    }

    /**
     * SAX parser call-back method that is invoked when an element is exited.
     * Used to blank out (set to the empty string, not nullify) the name of
     * the currentAttribute.  A better method would be to use a stack as an
     * instance variable, however since we are only interested in leaf-node
     * data this is a simpler and workable solution.
     * @param name The name of the attribute being exited. Ignored
     *        in this implementation.
     */
    public void endElement (String name) throws SAXException {
        processElement();
        currentText = "";
        this.currentElement = "";
        if (name.equals(EJB_REF)) {
            inEJBRef = false;
        }
    }

    protected void processElement () {
        if (currentElement.equals(REMOTE_INTERFACE) || currentElement.equals(HOME_INTERFACE)) {
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
                files.put(className, classFile);
            }
        }
    }
}



