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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.ant.TSBuildListener;
import com.sun.ant.taskdefs.common.Packager;
import com.sun.ant.taskdefs.common.SunRITool;


/**
 * A deployment tool which creates generic EJB jars. Generic jars contains
 * only those classes and META-INF entries specified in the EJB 1.1 standard
 *
 * This class is also used as a framework for the creation of vendor specific
 * deployment tools. A number of template methods are provided through which the
 * vendor specific tool can hook into the EJB creation process.
 */
public class WebTool extends SunRITool {

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String ejbJarSuffix = "_web.war";
    private static File webEmpty;
    private String descriptorXmlFileName = "";
    private String descriptorRuntimeXmlFileName = "";
    private Hashtable webClasses;

    public WebTool () {
        if (webEmpty == null) {
            webEmpty = new File(TSBuildListener.tsHome, "src/web/empty");
        }
    }

    protected WebDescriptorHandler getWebDescriptorHandler (File srcDir) {
        handler = new WebDescriptorHandler(task, srcDir);
        registerKnownDTDs(handler);
        // register any DTDs supplied by the user
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            Packager.DTDLocation dtdLocation = (Packager.DTDLocation)i.next();
            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return  (WebDescriptorHandler)handler;
    }

    protected void registerKnownDTDs (WebDescriptorHandler handler) {
        handler.registerDTD(PUBLICID_WEB, TSBuildListener.getDtdDir(getTask().getProject()) 
        + File.separator + DEFAULT_SUNRI13_WEB_DTD_LOCATION);
    }

    protected void parseServletClass (String descriptorFileName, SAXParser saxParser) throws IOException,
            SAXException {
        FileInputStream descriptorStream = null;
        try {
            descriptorStream = new FileInputStream(new File(config.descriptorDir,
                    descriptorFileName));
            saxParser.parse(new InputSource(descriptorStream), handler);
            webClasses = ((WebDescriptorHandler)handler).getClasses();
        } finally {
            if (descriptorStream != null) {
                try {
                    descriptorStream.close();
                } catch (IOException closeException) {}
            }
        }
    }

    public void processDescriptor (String descriptorFileName, SAXParser saxParser) {
        //String baseName = descriptorFileName.substring(0, descriptorFileName.lastIndexOf(".")) + "_web";
        String baseName = config.name + "_web";
        String jarFileName = baseName + ".war";
        //convert props file here
        if (descriptorFileName.indexOf(".xml") < 0) {
            //		    convertProps2Xml(descriptorFileName);
            descriptorXmlFileName = config.name + "_web.xml";
            descriptorRuntimeXmlFileName = config.name + "_web.runtime.xml";
        }
        else {
            descriptorXmlFileName = descriptorFileName;
        }
        checkConfiguration(descriptorXmlFileName, saxParser);
        try {
            handler = getWebDescriptorHandler(config.srcDir);
            Hashtable webFiles = new Hashtable();
            addContentFiles(webFiles);
            parseServletClass(descriptorXmlFileName, saxParser);
	    autoCheckExclude(webClasses);

            addSupportClasses(webClasses);
            checkAndAddInherited(webClasses);
            File jarFile = new File(destDir, jarFileName);
            // Check to see if we need a build and start doing the work!
            if (needToRebuild(webFiles, jarFile) || needToRebuild(webClasses, jarFile)) {
                // Log that we are going to build...
                log("building " + jarFile.getPath() + " with " + String.valueOf(webFiles.size())
                        + " content files and " + String.valueOf(webClasses.size())
                        + " class files", Project.MSG_VERBOSE);
                // Use helper method to write the jarfile
                String publicId = handler.getPublicId();
                writeWar(baseName, jarFile, webFiles, webClasses, publicId);
            }
            else {
                // Log that the file is up to date...
                log(jarFile.toString() + " is up to date.", Project.MSG_VERBOSE);
            }
        } catch (Exception e) {
            log(e.getMessage(), Project.MSG_INFO);
            throw  new BuildException(e.getMessage(), e);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    protected void writeWar (String baseName, File jarfile, Hashtable files, Hashtable classes,
            String publicId) throws BuildException {
        String sFilesToAdd = "";
        String sClassesToAdd = "";
        String sJarFilesToAdd = "";
        boolean hasContentFiles = true;

        String sJarFile = jarfile.getPath();
        //If the jarfile already exists then whack it and recreate it.
        if (jarfile.exists()) {
            jarfile.delete();
        }
        // Loop through all the files found and add them to the jar
        for (Iterator entryIterator = files.keySet().iterator(); entryIterator.hasNext();) {
            String entryName = (String)entryIterator.next();
            File entryFile = (File)files.get(entryName);
            log("adding web file '" + entryName + "'", Project.MSG_VERBOSE);
            if (entryName.charAt(0) == '/') {
                entryName = entryName.substring(1);
            }
            if (!sFilesToAdd.equals(""))
                sFilesToAdd += ":" + entryName;
            else
                sFilesToAdd += entryName;
        }
        if(sFilesToAdd.length() == 0) {
            hasContentFiles = false;
        }
        // Loop through all the class files found and add them to the jar
        for (Iterator entryIterator = classes.keySet().iterator(); entryIterator.hasNext();) {
            String entryName = (String)entryIterator.next();
            File entryFile = (File)classes.get(entryName);
            if (entryName.endsWith(".class")) {
                log("adding web class '" + entryName + "'", Project.MSG_VERBOSE);
                if (!sClassesToAdd.equals(""))
                    sClassesToAdd += ":" + entryName;
                else
                    sClassesToAdd += entryName;
                // See if there are any inner classes for this class
                com.sun.ant.taskdefs.common.InnerClassFilenameFilter flt = new com.sun.ant.taskdefs.common.InnerClassFilenameFilter(entryFile.getName());
                File entryDir = entryFile.getParentFile();
                String[] innerfiles = entryDir.list(flt);
                for (int i = 0, n = innerfiles.length; i < n; i++) {
                    //get and clean up innerclass name
                    int entryIndex = entryName.lastIndexOf(entryFile.getName()) - 1;
                    if (entryIndex < 0) {
                        entryName = innerfiles[i];
                    }
                    else {
                        entryName = entryName.substring(0, entryIndex) + File.separatorChar
                            + innerfiles[i];
                    }
                    // link the file
                    entryFile = new File(config.srcDir, entryName);
                    log("adding innerclass file '" + entryName + "'", Project.MSG_VERBOSE);
                    sClassesToAdd += ":" + entryName;
                }
            } else if (entryName.endsWith(".jar")) {
               log("adding library JAR '" + entryName + "'", Project.MSG_VERBOSE);
               if (!sJarFilesToAdd.equals(""))
                   sJarFilesToAdd += ":" + entryFile.toString();
               else
                   sJarFilesToAdd += entryFile.toString();
            } else {
                log("adding non-class, non-jar file " + entryName, Project.MSG_INFO);
                if(sFilesToAdd.length() == 0) {
                  sFilesToAdd +=  entryName;
                } else {
                    sFilesToAdd += ":" + entryName;
                }
            }
        }
        String args = "";
        args = "-webArchive ";
        if (sClassesToAdd != "") {
            args += " -classpath ";
            String CPATH = config.srcDir.getPath();
            args += CPATH + " ";
            args += "-classFiles ";
            args += sClassesToAdd + " ";
        }
        if (sJarFilesToAdd != "") {
            args += " -libraryJars ";
            args += sJarFilesToAdd + " ";
        }
        if(!hasContentFiles && sFilesToAdd.length() > 0) {  //we may have non-class, non-jar files 
            config.contentDir = new File(TSBuildListener.tsHome, "classes");
        }
        
        if (config.contentDir != null) {
            log("config.contentDir is " + config.contentDir.getPath(), Project.MSG_VERBOSE);
            if (sFilesToAdd != "") {
                args += config.contentDir.getPath() + " ";
                args += "-contentFiles ";
                args += sFilesToAdd + " ";
            }
            else {              //if no content files
                File reallyEmptyDir = new File(webEmpty, "really_empty");
                if (!reallyEmptyDir.exists()) {
                    if (!reallyEmptyDir.mkdirs()) {
                        log("WARNING:Failed to mkdirs. war will have unneeded files:"
                                + reallyEmptyDir.getPath(), Project.MSG_WARN);
                    }
                    else {
                        config.contentDir = reallyEmptyDir;
                    }
                }
                else {          //if already there
                    config.contentDir = reallyEmptyDir;
                }
                args += config.contentDir.getPath() + " ";
            }
        }
        else {
            args += project.getProperty("ts.home") + "/dev ";
        }
        args += config.descriptorDir.getPath() + File.separator + descriptorXmlFileName
                + " ";
        args += jarfile.getPath();
        invokePackager(jarfile, args);
    }           // end of writeWar

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @throws BuildException If the Deployment Tool's configuration isn't
     *                        valid
     */
    public void validateConfigured () throws BuildException {
        super.validateConfigured();
        /* don't know why contentFileSets.size() == 0. got indexOutOfBoundsException.
         because contentFileSet and content element is specific to webwar task.
         */
        checkContentDir();
    }

    /**
     * if contentDir is invalid and no content element in build.xml, use src/web/empty
     * if no contentDir and there is only one content element, use it as contentDir
     * if there are multiple content elements, use it as contentDir, possibly
     * override contentDir.
     */
    private void checkContentDir () {
        int n = config.contentFileSets.size();
        boolean noContentDir = (config.contentDir == null) || (!config.contentDir.isDirectory());
        if (n == 0 && noContentDir) {
            //	    throw new BuildException("Please specify content dir.");
            config.contentDir = webEmpty;
        }
        else if (n == 1 && noContentDir) {
            FileSet fs = (FileSet)config.contentFileSets.get(0);
            config.contentDir = fs.getDir(project);
        }
        else if (n > 1) {
            File tmpContent = new File(TSBuildListener.tsHome, "tmp/contentRoot");
            String excludes = "";
            if (tmpContent.exists()) {
                FileSet toDel = new FileSet();
                toDel.setDir(tmpContent);
                toDel.setDefaultexcludes(false);
                toDel.setIncludes("**/*");
                Delete del = new Delete();
                del.setProject(project);
                del.init();
                del.setTaskName("webwar");
                del.setIncludeEmptyDirs(true);
                del.addFileset(toDel);
                del.perform();
            }
            else {              //not there yet
                if (!tmpContent.mkdirs()) {
                    project.log("Cannot mkdir, web content will be incomplete:" +
                            tmpContent.getPath(), Project.MSG_WARN);
                }
            }
            Copy copy = new Copy();
            copy.setProject(project);
            copy.init();
            copy.setTaskName("webwar");
            copy.setTodir(tmpContent);
            for (int i = 0, m = config.contentFileSets.size(); i < m; i++) {
                copy.addFileset((FileSet)config.contentFileSets.get(i));
            }
            copy.perform();
            config.contentDir = tmpContent;
            config.contentFileSets.clear();
            FileSet newfs = new FileSet();
            newfs.setDir(tmpContent);
            config.contentFileSets.add(newfs);
        }
    }
}



