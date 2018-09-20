/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2000, 2001 The Apache Software Foundation.  All rights
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

import  java.io.*;
import  java.util.*;
import  javax.xml.parsers.SAXParser;
import  javax.xml.parsers.SAXParserFactory;
import  javax.xml.parsers.ParserConfigurationException;
import  org.xml.sax.SAXException;
import  org.apache.tools.ant.BuildException;
import  org.apache.tools.ant.Project;
import  org.apache.tools.ant.DirectoryScanner;
import  org.apache.tools.ant.taskdefs.MatchingTask;
import  org.apache.tools.ant.types.*;
import  com.sun.ant.taskdefs.common.*;

public class WebWar extends Packager {
    public void addcontentFileset (FileSet set) {
        config.contentFileSets.add(set);
    }

    public void addAllContentFileSets (List list) {
        config.contentFileSets.addAll(list);
    }

    public void execute () throws BuildException {
        validateConfig();
        WebTool riTool = new WebTool();
        riTool.setTask(this);
        //	riTool.setReplace(this.getReplace());
        riTool.configure(config);
        riTool.validateConfigured();
        riTool.setClasspath(config.classpath);
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            riTool.processDescriptor(config.descriptor.getName(), saxParser);
        } catch (SAXException se) {
            String msg = "SAXException while creating parser." + "  Details: " +
                    se.getMessage();
            throw  new BuildException(msg, se);
        } catch (ParserConfigurationException pce) {
            String msg = "ParserConfigurationException while creating parser. " +
                    "Details: " + pce.getMessage();
            throw  new BuildException(msg, pce);
        }
    }
}



