/*
 * The Apache Software License, Version 1.1
 *
   Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
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


package  com.sun.ant.taskdefs.connector;

import  java.io.*;
import  java.util.*;
import  java.util.jar.*;
import  java.util.zip.*;
import  java.net.*;
import  javax.xml.parsers.SAXParser;
import  org.xml.sax.InputSource;
import  org.xml.sax.SAXException;
import  org.apache.tools.ant.*;
import  org.apache.tools.ant.types.*;
import  org.apache.tools.ant.taskdefs.*;
import  org.apache.tools.ant.taskdefs.optional.ejb.*;
import  com.sun.ant.taskdefs.common.*;
import  com.sun.ant.types.*;
import  com.sun.ant.TSBuildListener;


public class ConnectorTool extends SunRITool {
    private String descriptorXmlFileName = "";
    private String descriptorRuntimeXmlFileName = "";

    protected Hashtable getRarContents () {
        Hashtable htFilesToAdd = new Hashtable();
        Project project = task.getProject();
        if (config.supportFileSets.size() == 0) {
            Support defaultRarFileSet = new Support();
            defaultRarFileSet.setDir(destDir);
            defaultRarFileSet.setIncludes("*.jar, *.war");
            config.supportFileSets.add(defaultRarFileSet);
        }
        for (int i = 0, n = config.supportFileSets.size(); i < n; i++) {
            Support fileSet = (Support)config.supportFileSets.get(i);
            File baseDir = fileSet.getDir(project);
            if (baseDir.compareTo(TSBuildListener.fClassDir) == 0) {
                project.log("ConnectorTool detects the dir for a rar fileset is class.dir.  Will reset it to dist.dir",
                        Project.MSG_VERBOSE);
                fileSet.setDir(destDir);
                baseDir = destDir;
            }
            DirectoryScanner scanner = fileSet.getDirectoryScanner(project);
//            scanner.scan();
            String[] sFiles = scanner.getIncludedFiles();
            for (int jj = 0; jj < sFiles.length; jj++) {
                htFilesToAdd.put(sFiles[jj], new File(baseDir, sFiles[jj]));
            }
        }
        return  htFilesToAdd;
    }           //getRarContents

    public void processRar () {
        Project project = task.getProject();
        Hashtable rarContents = getRarContents();
        File rarFile = new File(destDir, config.name + ".rar");
        if (needToRebuild(rarContents, rarFile)) {
            project.log("building " + rarFile.getName() + " with " + String.valueOf(rarContents.size())
                    + " files", Project.MSG_VERBOSE);
            writeRar(rarFile, rarContents);
        }
        else {
            project.log(rarFile.toString() + " is up to date.");
        }
    }

    protected void writeRar (File jarfile, Hashtable htContents) {
        String sFilesToAdd = "";
        String sJarFile = jarfile.getPath();
        if (jarfile.exists()) {
            jarfile.delete();
        }
        for (Iterator entryIterator = htContents.keySet().iterator(); entryIterator.hasNext();) {
            String entryName = (String)entryIterator.next();
            File entryFile = (File)htContents.get(entryName);
            log("adding file '" + entryName + "'", Project.MSG_VERBOSE);
            if (!sFilesToAdd.equals("")) {
                sFilesToAdd += ":" + entryName;
            }
            else {
                sFilesToAdd += entryName;
            }
        }       //for iterator
        String args = "-connector " + destDir + " " + sFilesToAdd + " " + config.descriptor.getPath()
                + " " + sJarFile;
        invokePackager(jarfile, args);
    }           // end of writeEar
}



