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


package  com.sun.ant.taskdefs.common;

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
import  org.apache.tools.ant.taskdefs.*;
import  com.sun.ant.TSBuildListener;
import  com.sun.ant.types.*;


public class Packager extends MatchingTask {

    public static class DTDLocation {
        private String publicId = null;
        private String location = null;

        public void setPublicId (String publicId) {
            this.publicId = publicId;
        }

        public void setLocation (String location) {
            this.location = location;
        }

        public String getPublicId () {
            return  publicId;
        }

        public String getLocation () {
            return  location;
        }
    }

    public static class Config {
        /* files not to automatically check in descriptro
         for example: com/sun/enterprise/util/Foo.class, com/sun/server/Bar.class
         */
        public List autocheckexcludes;

        /** Stores a handle to the directory under which to search for class files */
        public File srcDir;

        /** Stores a handle to the directory under which deployment descriptor exists */
        public File descriptorDir;

        /** Stores a handle to the deployment descriptor */
        public File descriptor;

        /** Stores a handle to the directory in which to search for web files */
        public File contentDir;

        /**
         * Instance variable that determines whether to use a package structure
         * of a flat directory as the destination for the jar files.
         */
        public boolean flatDestDir = false;
        /**
         * The classpath to use when loading classes
         */
        public Path classpath;
        /**
         * A Fileset of support classes
         */
        public List supportFileSets = new ArrayList();
        /**
         * A Fileset of support static web files
         */
        public List contentFileSets = new ArrayList();
        /**
         * A Fileset of support classes
         */
        public List earFileSets = new ArrayList();
        /**
         * The list of configured DTD locations
         */
        public ArrayList dtdLocations = new ArrayList();

        public File manifest;

        //for appclient packaging
        public String mainClass;

        public String name;
    }

    protected Config config = new Config();

    /** flag to tell whether need to replace display-name and jndi-name for xml files
     in service tests **/
    private boolean replace;

    public void setAutocheckexcludes (String s) {
        if (config.autocheckexcludes == null) {
            config.autocheckexcludes = new ArrayList();
        }
        //comman and space are used as delimiter.
        //if only comma, cannot parse "foo, bar" (space bar will be retrived)
        StringTokenizer st = new StringTokenizer(s, ", \t\n\r\f");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            //make sure all DescriptorHandlers use File.separatorChar
            //and GenericDeployTool.add* method use File.separatorChar
            token = token.replace('/', File.separatorChar);
            int pos = token.lastIndexOf(".class");
            if (pos == -1) {
                token += ".class";
            }
            config.autocheckexcludes.add(token);
        }
    }

    public void setReplace (boolean b) {
        this.replace = b;
    }

    public boolean getReplace () {
        return  this.replace;
    }

    protected File destDir;

    /**
     * creates a nested classpath element.
     *
     * This classpath is used to locate the super classes and interfaces
     * of the classes that will make up the EJB jar.
     *
     * @return the path to be configured.
     */
    public Path createClasspath () {
        if (config.classpath == null) {
            config.classpath = new Path(project);
        }
        return  config.classpath.createPath();
    }

    /**
     * Create a DTD location record. This stores the location of a DTD. The DTD is identified
     * by its public Id. The location may either be a file location or a resource location.
     */
    public DTDLocation createDTD () {
        DTDLocation dtdLocation = new DTDLocation();
        config.dtdLocations.add(dtdLocation);
        return  dtdLocation;
    }

    /**
     * Adds a set of files (nested fileset attribute) for ear packaging.
     */
    public void addFileset (FileSet set) {
        config.earFileSets.add(set);
    }

    public void addSupport (Support spt) {
        config.supportFileSets.add(spt);
    }

    public FileSet createContent () {
        FileSet contentFileSet = new FileSet();
        config.contentFileSets.add(contentFileSet);
        return  contentFileSet;
    }

    public void setManifest (File manifest) {
        config.manifest = manifest;
    }

    public void setSrcdir (File inDir) {
        config.srcDir = inDir;
    }

    public void setContentdir (File inDir) {
        config.contentDir = inDir;
    }

    public void setName (String sName) {
        config.name = sName;
    }

    public String getName () {
        return  config.name;
    }

    public void setDescriptor (File ddFile) {
        config.descriptor = ddFile;
        String ddfile = config.descriptor.getPath();
        int n = ddfile.lastIndexOf(File.separator);
        config.descriptorDir = new File(ddfile.substring(0, n));
    }

    public void setDestdir (File inDir) {
        this.destDir = inDir;
    }

    /**
     * Set the classpath to use when resolving classes for inclusion in the jar.
     *
     * @param classpath the classpath to use.
     */
    public void setClasspath (Path classpath) {
        config.classpath = classpath;
    }

    /**
     * Set the flat dest dir flag.
     *
     * This flag controls whether the destination jars are written out in the
     * destination directory with the same hierarchal structure from which
     * the deployment descriptors have been read. If this is set to true the
     * generated EJB jars are written into the root of the destination directory,
     * otherwise they are written out in the same relative position as the deployment
     * descriptors in the descriptor directory.
     *
     * @param inValue the new value of the flatdestdir flag.
     */
    public void setFlatdestdir (boolean inValue) {
        config.flatDestDir = inValue;
    }

    protected void validateConfig () {
        if (config.srcDir == null) {
            config.srcDir = new File(TSBuildListener.tsHome + File.separator + "classes");
        }
        if (config.descriptorDir == null) {
            config.descriptorDir = config.srcDir;
        }
        if (config.classpath == null) {
            config.classpath = TSBuildListener.getTsClasspath(project);
        }
    }

    public void execute () throws BuildException {}
}



