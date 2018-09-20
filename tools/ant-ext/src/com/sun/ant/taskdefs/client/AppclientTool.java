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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
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
public class AppclientTool extends SunRITool {
    private String descriptorXmlFileName = "";
    private String descriptorRuntimeXmlFileName = "";
    protected ClientDescriptorHandler clientHandler;

    private boolean bIncludeLastEJBFiles = false;

    protected void registerKnownDTDs (ClientDescriptorHandler handler) {
        handler.registerDTD(PUBLICID_APP_CLIENT, 
            TSBuildListener.getDtdDir(getTask().getProject()) 
            + File.separator + DEFAULT_SUNRI13_APP_CLIENT_DTD_LOCATION);
    }

    protected ClientDescriptorHandler getClientDescriptorHandler (File srcDir) {
        clientHandler = new ClientDescriptorHandler(getTask(), srcDir);
        registerKnownDTDs(clientHandler);
        // register any DTDs supplied by the user
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            Packager.DTDLocation dtdLocation = (Packager.DTDLocation)i.next();
            clientHandler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return  clientHandler;
    }
    
    //Tell the task that we want to include the classes packaged in the ejb vehicle jar 
    //in the ejb vehicle client jar as well
    public void includeLastJarredFiles(boolean bIncludeLastEJBFiles)
    {
         this.bIncludeLastEJBFiles = bIncludeLastEJBFiles;
    }

    /**
     * This method returns a list of WEB files and classes found when the
     * specified WEB descriptor is parsed and processed.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *                           descriptor to be processed
     * @param saxParser          SAXParser which may be used to parse the XML
     *                           descriptor
     * @return                   Hashtable of WAR class (and other) files to be
     *                           added to the completed WAR file
     * @throws SAXException      Any SAX exception, possibly wrapping another
     *                           exception
     * @throws IOException       An IOException from the parser, possibly from a
     *                           the byte stream or character stream
     */
    protected Hashtable parseClientFiles (String descriptorFileName, SAXParser saxParser) throws IOException,
            SAXException {
        FileInputStream descriptorStream = null;
        Hashtable clientFiles = null;
        try {
            /* Parse the web deployment descriptor.  While it may not
             * look like much, we use a SAXParser and an inner class to
             * get hold of all the classfile names for the descriptor.
             */
            descriptorStream = new FileInputStream(new File(config.descriptorDir,
                    descriptorFileName));
            saxParser.parse(new InputSource(descriptorStream), clientHandler);
            clientFiles = clientHandler.getFiles();
        } finally {
            if (descriptorStream != null) {
                try {
                    descriptorStream.close();
                } catch (IOException closeException) {}
            }
        }
        return  clientFiles;
    }

    public void processDescriptor (String descriptorFileName, SAXParser saxParser) {
        //String baseName = descriptorFileName.substring(0, descriptorFileName.lastIndexOf(".")) + "_client";
        String baseName = config.name + "_client";
        String jarFileName = baseName + ".jar";
        //		convertProps2Xml(descriptorFileName);
        descriptorXmlFileName = config.name + "_client.xml";
        descriptorRuntimeXmlFileName = config.name + "_client.runtime.xml";
        checkConfiguration(descriptorXmlFileName, saxParser);
        //We need to get all appclient classes and create the war here
        try {
            clientHandler = getClientDescriptorHandler(config.srcDir);
            // Retrive the files to be added to JAR from EJB descriptor
            Hashtable clientFiles = parseClientFiles(descriptorXmlFileName, saxParser);
	    autoCheckExclude(clientFiles);

	    //automatically include main class file
            String className = config.mainClass;
            className = className.replace('.', File.separatorChar);
            className += ".class";
            File classFile = new File(config.srcDir, className);
            clientFiles.put(className, classFile);
            // Add any support classes specified in the build file
            addSupportClasses(clientFiles);
            // add any inherited files
            checkAndAddInherited(clientFiles);
            File jarFile = new File(destDir, jarFileName);
            // Check to see if we need a build and start doing the work!
            if (needToRebuild(clientFiles, jarFile)) {
                // Log that we are going to build...
                log("building " + jarFile.getName() + " with " + String.valueOf(clientFiles.size())
                        + " files", Project.MSG_VERBOSE);
                // Use helper method to write the jarfile
                String publicId = clientHandler.getPublicId();
                writeClientJar(baseName, jarFile, clientFiles, publicId);
            }
            else {
                // Log that the file is up to date...
                log(jarFile.toString() + " is up to date.", Project.MSG_VERBOSE);
            }
        } catch (SAXException se) {
            String msg = "SAXException while parsing '" + descriptorXmlFileName.toString()
                    + "'. This probably indicates badly-formed XML." + "  Details: "
                    + se.getMessage();
            throw  new BuildException(msg, se);
        } catch (IOException ioe) {
            String msg = "IOException while parsing'" + descriptorXmlFileName.toString()
                    + "'.  This probably indicates that the descriptor" + " doesn't exist. Details: "
                    + ioe.getMessage();
            throw  new BuildException(msg, ioe);
        }
    }

    /**
     * Method used to encapsulate the writing of the JAR file. Iterates over the
     * filenames/java.io.Files in the Hashtable stored on the instance variable
     * ejbFiles.
     */
    protected void writeClientJar (String baseName, File jarfile, Hashtable files,
            String publicId) throws BuildException {
        String sFilesToAdd = "";
        String sJarFile = jarfile.getPath();
        //If the jarfile already exists then whack it and recreate it.
        if (jarfile.exists()) {
            jarfile.delete();
        }
        // Loop through all the class files found and add them to the jar
        for (Iterator entryIterator = files.keySet().iterator(); entryIterator.hasNext();) {
            String entryName = (String)entryIterator.next();
            File entryFile = (File)files.get(entryName);
            log("adding file '" + entryName + "'", Project.MSG_VERBOSE);
            if (!sFilesToAdd.equals(""))
                sFilesToAdd += ":" + entryName;
            else
                sFilesToAdd += entryName;
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
                sFilesToAdd += ":" + entryName;
            }
        }
        System.out.println("clienttool: bIncludeLastEJBFiles = " + bIncludeLastEJBFiles);
        System.out.println("clienttool: sLastFilesAdded = " + sLastFilesAdded);

        //if we're packaging an ejb vehicle appclient jar, then include all of the classes
        //that were packaged in the corresponding ejb vehicle jar
        if(bIncludeLastEJBFiles)
             sFilesToAdd += ":" + sLastFilesAdded;
        log("Adding files to appclient jar " + sFilesToAdd, Project.MSG_VERBOSE);
        String args = "";
        args = "-applicationClient ";
        args += config.srcDir.getPath() + " ";
        args += sFilesToAdd + " ";
        args += config.mainClass + " ";
        args += config.descriptorDir.getPath() + File.separator + descriptorXmlFileName
                + " ";
        args += jarfile.getPath();
        invokePackager(jarfile, args);
    }           // end of writeClientJar


}



