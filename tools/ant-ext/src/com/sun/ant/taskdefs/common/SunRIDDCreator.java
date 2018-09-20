/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import  org.apache.tools.ant.*;
import  org.apache.tools.ant.taskdefs.*;
import  org.apache.tools.ant.types.*;
import  java.io.File;


public class SunRIDDCreator extends DDCreator {
    private String sPropsFile = "";
    private String sName = "";

    /*public void execute () throws BuildException {
        if (descriptorDirectory == null || !descriptorDirectory.isDirectory()) {
            throw  new BuildException("descriptors directory " + descriptorDirectory.getPath()
                    + " is not valid");
        }
        if (generatedFilesDirectory == null || !generatedFilesDirectory.isDirectory()) {
            throw  new BuildException("dest directory " + generatedFilesDirectory.getPath()
                    + " is not valid");
        }
        String args = descriptorDirectory + " " + generatedFilesDirectory + " " +
                sName;
        // get all the files in the descriptor directory
        //DirectoryScanner ds = super.getDirectoryScanner(descriptorDirectory);
        //String[] files = ds.getIncludedFiles();
        //for (int i = 0; i < files.length; ++i) {
        args += " " + sPropsFile;
        //}
        String systemClassPath = System.getProperty("java.class.path");
        String execClassPath = project.translatePath(systemClassPath + ":" + classpath);
        Java ddCreatorTask = (Java)project.createTask("java");
        ddCreatorTask.setTaskName(getTaskName());
        ddCreatorTask.setFork(true);
        ddCreatorTask.setClassname("com.sun.ant.taskdefs.common.Props2Xml");
        Commandline.Argument arguments = ddCreatorTask.createArg();
        arguments.setLine(args);
        ddCreatorTask.setClasspath(new Path(project, execClassPath));
        if (ddCreatorTask.executeJava() != 0) {
            throw  new BuildException("Execution of Props2Xml failed");
        }
    }*/

    public void setPropsFile (String props) {
        this.sPropsFile = props;
    }

    public void setProject (Project p) {
        this.project = p;
    }

    public void setName (String name) {
        this.sName = name;
    }
}



