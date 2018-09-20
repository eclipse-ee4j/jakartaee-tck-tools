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


package  com.sun.ant.taskdefs.ejb;

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
public class EJBTool extends SunRITool {

    /** Instance variable that stores the suffix for the generated jarfile. */
    private String ejbJarSuffix = "_ejb.jar";
    private String descriptorXmlFileName = "";
    private String descriptorRuntimeXmlFileName = "";
//    protected EJBDescriptorHandler ejbHandler;

    protected void registerKnownDTDs (EJBDescriptorHandler handler) {
        handler.registerDTD(PUBLICID_EJB20, TSBuildListener.getDtdDir(getTask().getProject())
        + File.separator + DEFAULT_SUNRI13_EJB20_DTD_LOCATION);
    }

    protected EJBDescriptorHandler getDescriptorHandler (File srcDir) {
        EJBDescriptorHandler handler = new EJBDescriptorHandler(getTask(), srcDir);
        registerKnownDTDs(handler);
        // register any DTDs supplied by the user
        for (Iterator i = getConfig().dtdLocations.iterator(); i.hasNext();) {
            Packager.DTDLocation dtdLocation = (Packager.DTDLocation)i.next();
            handler.registerDTD(dtdLocation.getPublicId(), dtdLocation.getLocation());
        }
        return  handler;
    }

    /**
     * This method returns a list of EJB files found when the specified EJB
     * descriptor is parsed and processed.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *                           descriptor to be processed
     * @param saxParser          SAXParser which may be used to parse the XML
     *                           descriptor
     * @return                   Hashtable of EJB class (and other) files to be
     *                           added to the completed JAR file
     * @throws SAXException      Any SAX exception, possibly wrapping another
     *                           exception
     * @throws IOException       An IOException from the parser, possibly from a
     *                           the byte stream or character stream
     */
    protected Hashtable parseEjbFiles (String descriptorFileName, SAXParser saxParser) throws IOException,
            SAXException {
        FileInputStream descriptorStream = null;
        Hashtable ejbFiles = null;
        try {
            /* Parse the ejb deployment descriptor.  While it may not
             * look like much, we use a SAXParser and an inner class to
             * get hold of all the classfile names for the descriptor.
             */
            descriptorStream = new FileInputStream(new File(config.descriptorDir,
                    descriptorFileName));
            saxParser.parse(new InputSource(descriptorStream), handler);
            ejbFiles = handler.getFiles();
        } finally {
            if (descriptorStream != null) {
                try {
                    descriptorStream.close();
                } catch (IOException closeException) {}
            }
        }
        return  ejbFiles;
    }

    public void processDescriptor (String descriptorFileName, SAXParser saxParser) {
        //String baseName = descriptorFileName.substring(0, descriptorFileName.lastIndexOf(".")) + "_ejb";
        String baseName = config.name + "_ejb";
        String jarFileName = baseName + ".jar";
        //convert props file here
        //		convertProps2Xml(descriptorFileName);
        descriptorXmlFileName = config.name + "_ejb.xml";
        descriptorRuntimeXmlFileName = config.name + "_ejb.runtime.xml";
        checkConfiguration(descriptorXmlFileName, saxParser);
        try {
            handler = getDescriptorHandler(config.srcDir);
            // Retrive the files to be added to JAR from EJB descriptor
            Hashtable ejbFiles = parseEjbFiles(descriptorXmlFileName, saxParser);
	    autoCheckExclude(ejbFiles);

	    // Add any support classes specified in the build file
            addSupportClasses(ejbFiles);
            // add any inherited files
            checkAndAddInherited(ejbFiles);
            // Lastly create File object for the Jar files. If we are using
            // a flat destination dir, then we need to redefine baseName!
            /*if (config.flatDestDir && baseName.length() != 0) {
             int startName = baseName.lastIndexOf(File.separator);
             if (startName == -1) {
             startName = 0;
             }
             int endName   = baseName.length();
             baseName = baseName.substring(startName, endName);
             }*/
            File jarFile = new File(destDir, jarFileName);
            // Check to see if we need a build and start doing the work!
            if (needToRebuild(ejbFiles, jarFile)) {
                // Log that we are going to build...
                log("building " + jarFile.getName() + " with " + String.valueOf(ejbFiles.size())
                        + " files", Project.MSG_VERBOSE);
                // Use helper method to write the jarfile
                String publicId = handler.getPublicId();
                writeJar(baseName, jarFile, ejbFiles, publicId);
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
    private void writeJar (String baseName, File jarfile, Hashtable files, String publicId) throws BuildException {
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
        //save off the files we've packaged inside this jar in case we're in a vehicledir
        //we want the ejb vehicle client to pick up these classes as well
        sLastFilesAdded = sFilesToAdd;
        log("sFilesToAdd = " + sFilesToAdd, Project.MSG_VERBOSE);
        String args = "";
        args = "-ejbJar ";
        args += config.srcDir.getPath() + " ";
        args += sFilesToAdd + " ";
        args += config.descriptorDir.getPath() + File.separator + descriptorXmlFileName
                + " ";
        args += jarfile.getPath();
        invokePackager(jarfile, args);
    }           // end of writeJar
}



