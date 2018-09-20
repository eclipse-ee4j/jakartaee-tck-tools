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
import  com.sun.ant.types.*;
import  com.sun.ant.*;

public class GenericDeploymentTool {
    //flag to tell if already recompiled
    boolean recompiled;

    /** Stores a handle to the directory to put the Jar files in */
    protected File destDir;
    /** The classpath to use with this deployment tool. This is appended to
     any paths from the Packager task itself.*/
    protected Path classpath;
    protected Task task;
    protected Project project;

    /**
     * The classloader generated from the given classpath to load
     * the super classes and super interfaces.
     */
    protected ClassLoader classpathLoader = null;
    /**
     * List of files have been loaded into the EJB jar
     */
    protected List addedfiles;
    /**
     * Handler used to parse the EJB XML descriptor
     */
    protected DescriptorHandler handler;
    protected Packager.Config config;

    public void setDestdir (File inDir) {
        this.destDir = inDir;
    }

    protected File getDestDir () {
        return  destDir;
    }

    public void setTask (Task task) {
        this.task = task;
        this.project = task.getProject();
    }

    protected Task getTask () {
        return  task;
    }

    /**
     * Add the classpath for the user classes
     */
    public Path createClasspath () {
        if (classpath == null) {
            classpath = new Path(task.getProject());
        }
        return  classpath.createPath();
    }

    /**
     * Set the classpath to be used for this compilation.
     */
    public void setClasspath (Path classpath) {
        this.classpath = classpath;
    }

    /**
     * Get the classpath by combining the one from the surrounding task, if any
     * and the one from tis tool.
     */
    protected Path getCombinedClasspath () {
        Path combinedPath = classpath;
        if(config.classpath != null)
        {
            if (combinedPath == null) {
                combinedPath = config.classpath;
            }
            else {
                //added this check when moving to ANT 1.7 to be sure we don't have the same instance of Path
                //If some other class in the hierarchy overrides equals in the future, we may want to change this
                //check to use ==.
                if(!combinedPath.equals(config.classpath))
                     combinedPath.append(config.classpath);
            }
        }
        return  combinedPath;
    }

    protected void log (String message, int level) {
        getTask().log(message, level);
    }

    protected Location getLocation () {
        return  getTask().getLocation();
    }

    /**
     * Register the locations of all known DTDs.
     *
     * vendor-specific subclasses should override this method to define
     * the vendor-specific locations of the EJB DTDs
     */
    protected void registerKnownDTDs (DescriptorHandler handler) {
    // none to register for generic
    }

    /**
     * This method is called as the first step in the processDescriptor method
     * to allow vendor-specific subclasses to validate the task configuration
     * prior to processing the descriptor.  If the configuration is invalid,
     * a BuildException should be thrown.
     *
     * @param descriptorFileName String representing the file name of an EJB
     *                           descriptor to be processed
     * @param saxParser          SAXParser which may be used to parse the XML
     *                           descriptor
     * @thows BuildException     Thrown if the configuration is invalid
     */
    protected void checkConfiguration (String descriptorFileName, SAXParser saxParser) throws BuildException {    /*
     * For the GenericDeploymentTool, do nothing.  Vendor specific
     * subclasses should throw a BuildException if the configuration is
     * invalid for their server.
     */
    }

    private String replacePattern (String[] pat) {
        String result = "";
        for (int i = 0; i < pat.length; i++) {
            if (pat[i].endsWith(".class")) {
                result = result + pat[i].substring(0, pat[i].length() - 6) + ".java";
            }
            else if (pat[i].endsWith("**/*")) {
                result = result + pat[i] + ".java";
            }
            else if (pat[i].endsWith("**")) {
                result = result + pat[i] + "/*.java";
            }
            else if (pat[i].endsWith("*")) {
                result = result + pat[i] + ".java";
            }
            else {
                result = result + pat[i];
            }
            result = result + ",";
        }
        if (result.endsWith(",")) {
            result = result.substring(0, result.length() - 1);
        }
        return  result;
    }

    /**
     * We cannot assume that there is always a corresponding source file for
     * each class file.  For instance, some tie and stub classes do not have
     * corresponding source file.
     *
     * Adds any classes the user specifies using <i>support</i> nested elements
     * to the <code>ejbFiles</code> Hashtable.
     *
     * @param ejbFiles Hashtable of EJB classes (and other) files that will be
     *                 added to the completed JAR file
     */
    protected void addSupportClasses (Hashtable ejbFiles) {
        for (int i = 0, n = config.supportFileSets.size(); i < n; i++) {
            Support supportFileSet = (Support)config.supportFileSets.get(i);
            File supportBaseDir = supportFileSet.getDir(project);
            if (!supportBaseDir.exists()) {
               project.log("Directory '" + supportBaseDir.toString() + "' does not exist - skipping its processing.", Project.MSG_WARN);
               continue;
            } 
            DirectoryScanner supportScanner = supportFileSet.getDirectoryScanner(project);
            String[] supportFiles1 = supportScanner.getIncludedFiles();
            for (int j = 0; j < supportFiles1.length; ++j) {
                ejbFiles.put(supportFiles1[j], new File(supportBaseDir, supportFiles1[j]));
            }
        }
        if (TSBuildListener.skipMakeupCompile() || TSBuildListener.getAlreadyMadeup()) {
            return;
        }
        for (int i = 0, n = config.supportFileSets.size(); i < n; i++) {
            Support spt = (Support)config.supportFileSets.get(i);
            File supportBaseDir = spt.getDir(project);
            //	    File supportSrcDir = new File(TSBuildListener.tsHome, "src");
            String[] clsIncludes = spt.getOriginalIncludesArray();
            String[] clsExcludes = spt.getOriginalExcludesArray();
            //	    project.log("addSupportClass:original clsIncludes="
            //		+ Arrays.asList(clsIncludes).toString()
            //		+ "\n"
            //		+ "original clsExcludes="
            //		+ Arrays.asList(clsExcludes).toString());
            String srcIncludes = replacePattern(clsIncludes);
            String srcExcludes = replacePattern(clsExcludes);
            //	    project.log("addSupportClass:after replacePattern, srcIncludes="
            //		+ srcIncludes + "\n" + "srcExcludes=" + srcExcludes);
            FileSet shadow = new FileSet();
            shadow.setDir(TSBuildListener.fSrcDir);
            shadow.setIncludes(srcIncludes);
            shadow.setExcludes(srcExcludes);
            DirectoryScanner ds = shadow.getDirectoryScanner(project);
            String[] supportFiles = ds.getIncludedFiles();
            //	    project.log("addSupportClass:after scan shadow, supportFiles(java)="
            //		+ Arrays.asList(supportFiles).toString());
            for (int j = 0; j < supportFiles.length; ++j) {
                String clsName = null;
                if (supportFiles[j].endsWith(".java")) {
                    clsName = supportFiles[j].substring(0, supportFiles[j].length()
                            - 5) + ".class";
                }
                else {
                    clsName = supportFiles[j];
                }
                ejbFiles.put(clsName, new File(supportBaseDir, clsName));
            }
        }
    }

    protected void addContentFiles (Hashtable webFiles) {
        for (Iterator i = config.contentFileSets.iterator(); i.hasNext();) {
            FileSet contentFileSet = (FileSet)i.next();
            File contentFileSetDir = contentFileSet.getDir(task.getProject());
            //File contentDir = config.contentDir;
            DirectoryScanner contentScanner = contentFileSet.getDirectoryScanner(project);
            //DirectoryScanner contentScanner = new DirectoryScanner();
            //contentScanner.setBasedir(contentDir);
            //            contentScanner.scan();
            String[] contentFiles = contentScanner.getIncludedFiles();
            for (int j = 0; j < contentFiles.length; ++j) {
                webFiles.put(contentFiles[j], new File(contentFileSetDir, contentFiles[j]));
            }
        }
    }

    /**
     * This method checks the timestamp on each file listed in the <code>
     * ejbFiles</code> and compares them to the timestamp on the <code>jarFile
     * </code>.  If the <code>jarFile</code>'s timestamp is more recent than
     * each EJB file, <code>true</code> is returned.  Otherwise, <code>false
     * </code> is returned.
     *
     * @param ejbFiles Hashtable of EJB classes (and other) files that will be
     *                 added to the completed JAR file
     * @param jarFile  JAR file which will contain all of the EJB classes (and
     *                 other) files
     * @return         boolean indicating whether or not the <code>jarFile</code>
     *                 is up to date
     */
    protected boolean needToRebuild (Hashtable ejbFiles, File jarFile) {
        /*
         if (jarFile.exists()) {
         long lastBuild = jarFile.lastModified();
         if (config.manifest != null && config.manifest.exists() &&
         config.manifest.lastModified() > lastBuild) {
         log("Build needed because manifest " + config.manifest + " is out of date",
         Project.MSG_VERBOSE);
         return true;
         }
         Iterator fileIter = ejbFiles.values().iterator();
         // Loop through the files seeing if any has been touched
         // more recently than the destination jar.
         while(fileIter.hasNext()) {
         File currentFile = (File) fileIter.next();
         if (lastBuild < currentFile.lastModified()) {
         log("Build needed because " + currentFile.getPath() + " is out of date",
         Project.MSG_VERBOSE);
         return true;
         }
         }
         return false;
         }
         */
        return  true;
    }

    /**
     * Returns the Public ID of the DTD specified in the EJB descriptor.  Not
     * every vendor-specific <code>DeploymentTool</code> will need to reference
     * this value or may want to determine this value in a vendor-specific way.
     *
     * @return         Public ID of the DTD specified in the EJB descriptor.
     */
    protected String getPublicId () {
        return  handler.getPublicId();
    }

    /**
     * Check if a EJB Class Inherits from a Superclass, and if a Remote Interface
     * extends an interface other then javax.ejb.EJBObject directly.  Then add those
     * classes to the generic-jar so they dont have to added elsewhere.
     * Will try to recompile once in case of any NoClassDefFoundError
     * or NoClassFoundException.
     */
    protected void checkAndAddInherited (Hashtable checkEntries) throws BuildException {
        //Copy hashtable so were not changing the one we iterate through
        //Hashtable copiedHash = (Hashtable)checkEntries.clone();
        //create a new hashtable so that after partial packaging and recompilation
        //we still have the original checkEntries.  Not sure about the clone behavior.
        //	Hashtable copiedHash = new Hashtable(checkEntries);
        //	log("checkEntries=" + checkEntries.toString(), Project.MSG_VERBOSE);
        Hashtable copiedHash = (Hashtable)checkEntries.clone();
        if (!TSBuildListener.skipMakeupCompile() && !TSBuildListener.getAlreadyMadeup()) {
            makeupCompile(copiedHash);
        }
        // Walk base level EJBs and see if they have superclasses or extend extra interfaces which extend EJBObject
        String classname = null;
        try {
            for (Iterator entryIterator = copiedHash.keySet().iterator(); entryIterator.hasNext();) {
                String entryName = (String)entryIterator.next();
                File entryFile = (File)copiedHash.get(entryName);
                // only want class files, xml doesnt reflect very well =)
                if (entryName.endsWith(".class")) {
                    classname = entryName.substring(0, entryName.lastIndexOf(".class")).replace(File.separatorChar,
                            '.');
                    ClassLoader loader = getClassLoaderForBuild();
                    Class c = loader.loadClass(classname);
                    // No primatives!!  sanity check, probably not nessesary
                    if (!c.isPrimitive()) {
                        if (c.isInterface()) {
                            log("looking at interface " + c.getName(), Project.MSG_VERBOSE);
                            Class[] interfaces = c.getInterfaces();
                            for (int i = 0; i < interfaces.length; i++) {
                                log("     implements " + interfaces[i].getName(),
                                        Project.MSG_VERBOSE);
                                addInterface(interfaces[i], checkEntries);
                            }
                        }
                        else {
                            log("looking at class " + c.getName(), Project.MSG_VERBOSE);
                            Class s = c.getSuperclass();
                            addSuperClass(c.getSuperclass(), checkEntries);
                        }
                    }           //if primative
                }               // if(".class")
            }                   //for (iterator)
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            throw  new BuildException(cnfe.getMessage());
        } catch (NoClassDefFoundError ncdfe) {
            ncdfe.printStackTrace();
            throw  new BuildException(ncdfe.getMessage());
        }
    }

    private void makeupCompile (Hashtable copiedHash) {
        //        project.log("in makeupCompile: copiedHash:" + copiedHash.toString());
        String includesPattern = "";
        File local = TaskUtil.getCurrentSrcDir(project);
        for (Iterator entryIterator = copiedHash.keySet().iterator(); entryIterator.hasNext();) {
            String entryName = (String)entryIterator.next();
            //	    file shoud be like:
            //	    /home/ab123456/ts_ws/classes/com/sun/ts/tests/jdbc/TestConn.class
            File file = (File)copiedHash.get(entryName);
            if (entryName.endsWith(".class")) {
                String javaFileName = entryName.substring(0, entryName.lastIndexOf(".class"))
                        + ".java";
                File javaFile = new File(TSBuildListener.tsHome + "/src", javaFileName);
                if (!file.exists()) {
                    includesPattern = includesPattern + javaFileName + ",";
                }
                else if (local.compareTo(javaFile.getParentFile()) != 0) {
                    long clsTime = file.lastModified();
                    long javaTime = javaFile.lastModified();
                    if (clsTime < javaTime) {
                        includesPattern = includesPattern + javaFileName + ",";
                    }
                }
            }                   //if endsWith .class
        }       //for
        if (includesPattern.endsWith(",")) {                    //something to compile
            includesPattern = includesPattern.substring(0, includesPattern.length()
                    - 1);
            project.log("Some classes have not been compiled, so recompile:\n" +
                    includesPattern);
            doCompile(includesPattern);
        }
        else {
        //	    project.log("No need to makeupCompile. includesPattern=" + includesPattern);
        }
    }

    private void doCompile (String includesPattern) {
        String tsClasspath = project.getProperty("ts.classpath");
        if (tsClasspath == null || tsClasspath.length() == 0) {
            project.log("ts.classpath not set in ts.jte or build.properties.", Project.MSG_WARN);
            tsClasspath = project.getProperty("j2ee.home.ri") + "/lib/j2ee.jar:"
                    + TSBuildListener.tsHome + "/lib/tsharness.jar:" + TSBuildListener.tsHome
                    + "/lib/javatest.jar:" + TSBuildListener.tsHome + "/classes";
        }
        String localClassRi = project.getProperty("local.classes.ri");
        if (localClassRi != null && localClassRi.length() != 0) {
            tsClasspath = tsClasspath + ":" + localClassRi;
        }
        Path tsAndLocalClasspath = new Path(project, tsClasspath);
        Javac compiler = new Javac();
        compiler.setProject(project);
        compiler.init();
        Path srcPath = new Path(project, TSBuildListener.sSrcDir);
        compiler.setSrcdir(srcPath);
        compiler.setDestdir(TSBuildListener.fClassDir);
        compiler.setFork(false);
        compiler.setIncludes(includesPattern);
        compiler.setDeprecation(true);
        compiler.setDebug(true);
        compiler.setClasspath(tsAndLocalClasspath);
        compiler.setFailonerror(false);
        compiler.setTaskName("recompile");
        compiler.perform();
    }

    protected void addInterface (Class theInterface, Hashtable checkEntries) {
	boolean acceptedIntf = !theInterface.getName().startsWith("java") &&
	    !theInterface.getName().startsWith("com.sun.ts.lib.util") &&
	    !theInterface.getName().startsWith("com.sun.javatest.Status");
	
        if (acceptedIntf) {
	    log("** adding interface:" + theInterface.getName(), Project.MSG_VERBOSE);
            File interfaceFile = new File(config.srcDir.getAbsolutePath() + File.separatorChar
                    + theInterface.getName().replace('.', File.separatorChar) +
                    ".class");
            if (interfaceFile.exists() && interfaceFile.isFile()) {
		String tmp = theInterface.getName().replace('.', File.separatorChar)
                        + ".class";
		if(config.autocheckexcludes == null) {
                    checkEntries.put(tmp, interfaceFile);
		} else if(!config.autocheckexcludes.contains(tmp)){
		    checkEntries.put(tmp, interfaceFile);
		}
                Class[] superInterfaces = theInterface.getInterfaces();
                for (int i = 0; i < superInterfaces.length; i++) {
                    addInterface(superInterfaces[i], checkEntries);
                }
            }
        } else {
            log("****** Interface skipped '" + theInterface.getName() + "'", Project.MSG_VERBOSE);
	}
    }

    protected void addSuperClass (Class superClass, Hashtable checkEntries) {
	boolean acceptedClass = !superClass.getName().startsWith("java") &&
	    !superClass.getName().startsWith("com.sun.ts.lib.util") &&
	    !superClass.getName().startsWith("com.sun.javatest.Status");
        if (acceptedClass) {
	    log("*** adding super class:" + superClass.getName(), Project.MSG_VERBOSE);
            File superClassFile = new File(config.srcDir.getAbsolutePath() + File.separatorChar
                    + superClass.getName().replace('.', File.separatorChar) + ".class");
            if (superClassFile.exists() && superClassFile.isFile()) {
		String tmp = superClass.getName().replace('.', File.separatorChar)
		    + ".class";
		if(config.autocheckexcludes == null) {
                    checkEntries.put(tmp, superClassFile);
		} else if(!config.autocheckexcludes.contains(tmp)){
		    checkEntries.put(tmp, superClassFile);
		}
                // now need to get super classes and interfaces for this class
                Class[] superInterfaces = superClass.getInterfaces();
                for (int i = 0; i < superInterfaces.length; i++) {
                    addInterface(superInterfaces[i], checkEntries);
                }
                addSuperClass(superClass.getSuperclass(), checkEntries);
            }
        } else {
            log("****** Class skipped '" + superClass.getName() + "'", Project.MSG_VERBOSE);
	}
    }

    /**
     * Returns a Classloader object which parses the passed in generic Packager classpath.
     * The loader is used to dynamically load classes from javax.ejb.* and the classes
     * being added to the jar.
     *
     */
    protected ClassLoader getClassLoaderForBuild () {
        if (classpathLoader != null) {
            return  classpathLoader;
        }
        Path combinedClasspath = getCombinedClasspath();
        // only generate a new ClassLoader if we have a classpath
        if (combinedClasspath == null) {
            classpathLoader = getClass().getClassLoader();
        }
        else {
            classpathLoader = new AntClassLoader(getTask().getProject(), combinedClasspath);
        }
        return  classpathLoader;
    }

    /**
     * Called to validate that the tool parameters have been configured.
     *
     * @throws BuildException If the Deployment Tool's configuration isn't
     *                        valid
     */
    public void validateConfigured () throws BuildException {
        if ((destDir == null) || (!destDir.isDirectory())) {
            //            String msg = "destDir has not been specified using \"destdir\" attribute. Will use ${dist.dir} or ${basedir}";
            //            project.log(msg, Project.MSG_VERBOSE);
            destDir = TaskUtil.getFullDistDir(project);
            destDir.mkdirs();
        }        /* don't know why contentFileSets.size() == 0. got indexOutOfBoundsException.
         because contentFileSet and content element is specific to webwar task.
         if((config.contentDir == null) || (!config.contentDir.isDirectory())) {
         FileSet fs = (FileSet) config.contentFileSets.get(0);
         config.contentDir = fs.getDir(project);
         }*/

    }

    protected Packager.Config getConfig () {
        return  config;
    }

    public void configure (Packager.Config config) {
        this.config = config;
        classpathLoader = null;
    }

    protected void autoCheckExclude(Map map) {
	if(config.autocheckexcludes != null) {
	    for(int i = 0,n = config.autocheckexcludes.size(); i < n; i++) {
		Object o = config.autocheckexcludes.get(i);
		map.remove(o);
	    }
	}
    }
}



