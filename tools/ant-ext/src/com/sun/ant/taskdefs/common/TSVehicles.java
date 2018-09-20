/*
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */


package com.sun.ant.taskdefs.common;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.Replace;
import org.apache.tools.ant.taskdefs.Touch;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.util.FileUtils;

import com.sun.ant.TSBuildListener;
import com.sun.ant.taskdefs.app.J2eeEar;
import com.sun.ant.taskdefs.client.ClientJar;
import com.sun.ant.taskdefs.ejb.EJBJar;
import com.sun.ant.taskdefs.web.WebWar;
import com.sun.ant.types.Support;
import com.sun.ts.lib.harness.VehicleVerifier;

public class TSVehicles extends Task {
    private static String runnerClasses =
        "com/sun/ts/tests/common/vehicle/VehicleRunnerFactory.class, com/sun/ts/tests/common/vehicle/VehicleRunnable.class";
    static String pkgName = "com" + File.separator + "sun" + File.separator + "ts";
    static String j2eeHome;
    static String vehicleDir;
    //    private Path tsPath;
    //    private Path j2eetsPath;
    private File srcDir;
    private File distDir;
    //where to store ear, war, jar, etc. Currently same as srcDir.
    private String name;
    private File manifest;
    private String mainclass;
    private String testClientClass;
    private String extraJars;
    private List supports = new ArrayList();
    private String ejbName;
    private String servletName;
    private String jspName;
    private String appclientName;
    private List contentFileSets = new ArrayList();
    static String ejbXml = "ejb_vehicle_ejb";
    static String ejbClientXml = "ejb_vehicle_client";
    static String servletXml = "servlet_vehicle_web";
    static String jspXml = "jsp_vehicle_web";
    static String appclientXml = "appclient_vehicle_client";
    private static FileSet defaultJspContent;
    private static File defaultJspContentDir;

    private Set neededXml = new HashSet(19);
    private Map neededFilters = new HashMap(11);

    private String extra4Ejb = "";
    private String extra4Servlet = "";
    private String extra4Jsp = "";
    private String extra4Appclient = "";

    //is null until set inside execute(). for example,
    //com/sun/ts/tests/samples/jdbc/ee/testConn/testConnClient
    //the same data as mainclass.  Defined here to avoid repeated computation.
    private String mainclassAsDir;
    private String testClientClassAsDir;
    public String autocheckexcludes;
    private boolean separateRuntimeFiles;

    boolean isStandAlone; //used by ContainerPackager.removeJarWars.
    private FileUtils fileUtils = FileUtils.newFileUtils();

    public void setAutocheckexcludes(String s) {
        autocheckexcludes = s;
    }

    private void initStatic() {
        if (vehicleDir == null || vehicleDir.length() == 0) {
            //    vehicleDir = project.getProperty("vehicle.dir");
            vehicleDir =
                TSBuildListener.tsHome
                    + File.separator
                    + "src"
                    + File.separator
                    + pkgName
                    + File.separator
                    + "tests"
                    + File.separator
                    + "common"
                    + File.separator
                    + "vehicle";
        }
        if (defaultJspContentDir == null) {
            defaultJspContentDir = new File(vehicleDir + File.separator + "jsp" + File.separator + "contentRoot");
        }
        if (defaultJspContent == null) {
            defaultJspContent = new FileSet();
            defaultJspContent.setDir(defaultJspContentDir);
            defaultJspContent.setIncludes("*.html, *.jsp");
        }
    }
    public void myinit() throws BuildException {
        initStatic();
        srcDir = TaskUtil.getCurrentSrcDir(project);
        distDir = TaskUtil.getFullDistDir(srcDir, project);
        //        tsPath = new Path(project, TSBuildListener.sClassDir);
        //        j2eetsPath = new Path(project, j2eeJar + ":" + TSBuildListener.sClassDir
        //	    + ":" + TSBuildListener.tsHome + "/lib/javatest.jar");
        if (ContainerPackage.buildLevel == 0) {
            ContainerPackage.buildLevel = ContainerPackage.APPLICATION_LEVEL;
            String sLevel = project.getProperty("build.level");
            if (sLevel != null) {
                try {
                    ContainerPackage.buildLevel = Byte.parseByte(sLevel);
                } catch (NumberFormatException nex) {
                }
            }
        }
    }
    public void execute() throws BuildException {
        myinit();
        if (!TSBuildListener.vehiclesBuilt) {
            compileVehicles();
        }

        if (ContainerPackage.buildLevel == ContainerPackage.COMPILE_LEVEL) {
            return;
        }
        initAppName();
        mainclassAsDir = mainclass.replace('.', '/');
        testClientClassAsDir = testClientClass.replace('.', '/');
        String[] vehicles = getVehicles(srcDir);
        if (vehicles.length == 0) {
            throw new BuildException("No vehicle specified for this directory in src/vehicle.properties.");
        }
        log("vehicles=" + Arrays.asList(vehicles).toString());
        //would need only one iteration if we had function pointer
        //We want to copy, rename, and replace for all xml files at
        //the beginning.
        for (int i = 0; i < vehicles.length; i++) {
            if (vehicles[i].equals("ejb")) {
                neededXml.add(ejbXml + ".xml");
                neededXml.add(ejbXml + ".runtime.xml");
                neededXml.add(ejbClientXml + ".xml");
                neededXml.add(ejbClientXml + ".runtime.xml");
                neededFilters.put("jndi-name>com_sun_ts_tests_common_vehicle_ejb_EJBVehicle", "jndi-name>" + ejbName);
                neededFilters.put("display-name>ejb_vehicle_client", "display-name>" + ejbName + "_client");
            } else if (vehicles[i].equals("servlet")) {
                neededXml.add(servletXml + ".xml");
                neededXml.add(servletXml + ".runtime.xml");
                neededFilters.put("context-root>servlet_vehicle", "context-root>" + servletName);
                neededFilters.put("display-name>servlet_vehicle", "display-name>" + servletName);
                neededFilters.put(
                    "<web-uri>servlet_vehicle_web.war</web-uri>",
                    "<web-uri>" + servletName + "_web.war</web-uri>");
            } else if (vehicles[i].equals("jsp")) {
                neededXml.add(jspXml + ".xml");
                neededXml.add(jspXml + ".runtime.xml");
                neededFilters.put("context-root>jsp_vehicle", "context-root>" + jspName);
                neededFilters.put("display-name>jsp_vehicle", "display-name>" + jspName);
                neededFilters.put(
                    "<web-uri>jsp_vehicle_web.war</web-uri>",
                    "<web-uri>" + jspName + "_web.war</web-uri>");
            } else if (vehicles[i].equals("appclient")) {
                neededXml.add(appclientXml + ".xml");
                neededXml.add(appclientXml + ".runtime.xml");
                neededFilters.put("display-name>appclient_vehicle_client", "display-name>" + appclientName + "_client");
            } else {
                isStandAlone = true;
                //do nothing for standalone
            }
        }

        //some dirs may have non-vehicle components and their runtime.xml
        //jta/txpropagation, xa/compXres, xa/multicomp.
        File[] allRuntimeFiles = srcDir.listFiles(ArchiveInfo.runtimeFileFilter);
        if (allRuntimeFiles != null) {
            for (int i = 0; i < allRuntimeFiles.length; i++) {
                String fileName = allRuntimeFiles[i].getName();
                neededXml.add(fileName);
                if (!separateRuntimeFiles
                    && (fileName.endsWith("ejb_vehicle_ejb.runtime.xml")
                        || fileName.endsWith("ejb_vehicle_client.runtime.xml")
                        || fileName.endsWith("servlet_vehicle_web.runtime.xml")
                        || fileName.endsWith("jsp_vehicle_web.runtime.xml")
                        || fileName.endsWith("appclient_vehicle_appclient.runtime.xml"))) {
                    separateRuntimeFiles = true;
                }
            }
        }

        copyAndReplace(vehicles);
        for (int i = 0; i < vehicles.length; i++) {
            if (vehicles[i].equals("ejb")) {
                doEjb();
            } else if (vehicles[i].equals("servlet")) {
                doServlet();
            } else if (vehicles[i].equals("jsp")) {
                doJsp();
            } else if (vehicles[i].equals("appclient")) {
                doAppclient();
            } else {
                //do nothing for standalone
            }
            //after one iteration, we should have recompiled all support
            //and vehicle classes.  No way to know which one is first,
            //so we set it in every iteration.  Set it to false after
            //the current leaf dir is done.

        }
        TSBuildListener.setAlreadyMadeup(false);
    }
    //    private File getDescriptorFile(File file1, String file2){
    //	if(file1.exists()) {
    //	    return file1;
    //	}
    //	log("getDescriptorFile(...): descriptor file:" + file1.getPath(), Project.MSG_VERBOSE);
    //        File f2 = new File(file2);
    //	if(f2.exists()) {
    //	    return f2;
    //	}
    //	log("getDescriptorFile(...): descriptor file:" + file2, Project.MSG_VERBOSE);
    //	throw new BuildException("Looking for one of the two following 2 files. Neither exists:"
    //	    + file1.getPath() + ";" + file2);
    //    }
    private void doEjb() throws BuildException {
        File dfile = new File(distDir, name + "_" + ejbXml + ".xml");
        //can use project.createTask()
        EJBJar ejbJarTask = new EJBJar();
        ejbJarTask.setProject(project);
        ejbJarTask.init();
        ejbJarTask.setTaskName("ejb-jar");
        ejbJarTask.setName(ejbName);
        ejbJarTask.setSrcdir(TSBuildListener.fClassDir);
        ejbJarTask.setDescriptor(dfile);
        if (manifest != null) {
            ejbJarTask.setManifest(manifest);
        } 
        if (autocheckexcludes != null)
            ejbJarTask.setAutocheckexcludes(autocheckexcludes);
        //        ejbJarTask.setClasspath(j2eetsPath);
        //ejb_vehicle_ejb.jar needs the mainclass
        Support mainClass = new Support();
        mainClass.setDir(TSBuildListener.fClassDir);
        mainClass.setIncludes(testClientClassAsDir + ".class, " + runnerClasses);
        ejbJarTask.addSupport(mainClass);
        //        ejbJarTask.setReplace(true);
        handleSupport(ejbJarTask);
        ejbJarTask.perform();
        addFilesToArchive(ejbName + "_ejb.jar");
        TSBuildListener.setAlreadyMadeup(true);

        dfile = new File(distDir, name + "_" + ejbClientXml + ".xml");
        ClientJar clientJarTask = new ClientJar();
        clientJarTask.setProject(project);
        clientJarTask.init();
        clientJarTask.setTaskName("clientjar");
        clientJarTask.setName(ejbName);
        clientJarTask.setSrcdir(TSBuildListener.fClassDir);
        clientJarTask.setDescriptor(dfile);
        
        //Tell the task that we want to include the classes packaged in the ejb vehicle jar 
        //in the ejb vehicle client jar as well
        clientJarTask.includeLastJarredFiles(true);
        if (manifest != null) {
		clientJarTask.setManifest(manifest);
        }
        if (autocheckexcludes != null)
            clientJarTask.setAutocheckexcludes(autocheckexcludes);
        //        clientJarTask.setClasspath(j2eetsPath);
        clientJarTask.setMainClass(this.mainclass);
        //addFileset method does not include additional files.
        Support vehicleClasses = new Support();
        vehicleClasses.setDir(TSBuildListener.fClassDir);
        //        vehicleClasses.setIncludes("com/sun/ts/tests/common/vehicle/ejb/EJBVehicleHome.class,com/sun/ts/tests/common/vehicle/ejb/EJBVehicleRemote.class");
        vehicleClasses.setIncludes(
            "com/sun/ts/tests/common/vehicle/ejb/EJBVehicleHome.class, com/sun/ts/tests/common/vehicle/ejb/EJBVehicleRemote.class, com/sun/ts/tests/common/vehicle/ejb/EJBVehicleRunner.class, "
                + runnerClasses);
        clientJarTask.addSupport(vehicleClasses);
        //	clientJarTask.setReplace(true);
        handleSupport(clientJarTask);
        clientJarTask.perform();
        addFilesToArchive(ejbName + "_client.jar");

        String toInclude = null;
        if (extra4Ejb.length() > 0) {
            if (extra4Ejb.endsWith(",")) {
                toInclude = extra4Ejb + "*_ejb_vehicle*.jar";
            } else {
                toInclude = "*_ejb_vehicle*.jar" + "," + extra4Ejb;
            }
        } else {
            toInclude = "*_ejb_vehicle*.jar";
        }
        makeEar("_ejb_vehicle", toInclude);
    }
    private void doServlet() throws BuildException {
        File dfile = new File(distDir, name + "_" + servletXml + ".xml");
        WebWar webWarTask = new WebWar();
        webWarTask.setProject(project);
        webWarTask.init();
        webWarTask.setTaskName("webwar");
        webWarTask.setName(servletName);
        webWarTask.setSrcdir(TSBuildListener.fClassDir);
        webWarTask.setDescriptor(dfile);
        if (manifest != null) {
            webWarTask.setManifest(manifest);
        }
        if (autocheckexcludes != null)
            webWarTask.setAutocheckexcludes(autocheckexcludes);
        //        webWarTask.setClasspath(j2eetsPath);
        //	ServiceEETest, EETEst, and related will be added automatically
        Support support = new Support();
        support.setDir(TSBuildListener.fClassDir);
        support.setIncludes(
            testClientClassAsDir
                + ".class, com/sun/ts/tests/common/vehicle/servlet/ServletVehicleRunner.class,"
                + runnerClasses);
        webWarTask.addSupport(support);
        handleSupport(webWarTask);

        webWarTask.addAllContentFileSets(contentFileSets);
        webWarTask.perform();
        addFilesToArchive(servletName + "_web.war");

        TSBuildListener.setAlreadyMadeup(true);

        String toInclude = null;
        if (extra4Servlet.length() > 0) {
            if (extra4Servlet.endsWith(",")) {
                toInclude = extra4Servlet + "*_servlet_vehicle*.war";
            } else {
                toInclude = "*_servlet_vehicle*.war" + "," + extra4Servlet;
            }
        } else {
            toInclude = "*_servlet_vehicle*.war";
        }
        makeEar("_servlet_vehicle", toInclude);
    }
    private void doJsp() throws BuildException {
        File dfile = new File(distDir, name + "_" + jspXml + ".xml");
        WebWar webWarTask = new WebWar();
        webWarTask.setProject(project);
        webWarTask.init();
        webWarTask.setTaskName("webwar");
        webWarTask.setName(jspName);
        webWarTask.setSrcdir(TSBuildListener.fClassDir);
        webWarTask.setDescriptor(dfile);
        if (manifest != null) {
            webWarTask.setManifest(manifest);
        }
        if (autocheckexcludes != null)
            webWarTask.setAutocheckexcludes(autocheckexcludes);
        //        webWarTask.setClasspath(j2eetsPath);
        Support support = new Support();
        support.setDir(TSBuildListener.fClassDir);
        support.setIncludes(
            testClientClassAsDir + ".class, com/sun/ts/tests/common/vehicle/jsp/JSPVehicleRunner.class," + runnerClasses);
        webWarTask.addSupport(support);

        webWarTask.addcontentFileset(defaultJspContent);
        webWarTask.addAllContentFileSets(contentFileSets);
        handleSupport(webWarTask);
        webWarTask.perform();
        addFilesToArchive(jspName + "_web.war");
        TSBuildListener.setAlreadyMadeup(true);

        String toInclude = null;
        if (extra4Jsp.length() > 0) {
            if (extra4Jsp.endsWith(",")) {
                toInclude = extra4Jsp + "*_jsp_vehicle*.war";
            } else {
                toInclude = "*_jsp_vehicle*.war" + "," + extra4Jsp;
            }
        } else {
            toInclude = "*_jsp_vehicle*.war";
        }
        makeEar("_jsp_vehicle", toInclude);
    }
    private void doAppclient() throws BuildException {
        File dfile = new File(distDir, name + "_" + appclientXml + ".xml");
        ClientJar clientJarTask = new ClientJar();
        clientJarTask.setProject(project);
        clientJarTask.init();
        clientJarTask.setTaskName("clientjar");
        clientJarTask.setName(appclientName);
        clientJarTask.setSrcdir(TSBuildListener.fClassDir);
        clientJarTask.setDescriptor(dfile);
        if (manifest != null) {
            clientJarTask.setManifest(manifest);
        }
        if (autocheckexcludes != null)
            clientJarTask.setAutocheckexcludes(autocheckexcludes);
        //        clientJarTask.setClasspath(j2eetsPath);
        clientJarTask.setMainClass(this.mainclass);
        //	clientJarTask.setReplace(true);

        //add VehicleRunnable and VehicleRunnerFactory
        Support vehicleClasses = new Support();
        vehicleClasses.setDir(TSBuildListener.fClassDir);
        vehicleClasses.setIncludes(testClientClassAsDir + ".class," + runnerClasses + ",com/sun/ts/tests/common/vehicle/EmptyVehicleRunner.class");
        clientJarTask.addSupport(vehicleClasses);

        handleSupport(clientJarTask);
        clientJarTask.perform();
        String appclientJarName = appclientName + "_client.jar";
        addFilesToArchive(appclientJarName);
        TSBuildListener.setAlreadyMadeup(true);

        String toInclude = null;
        if (extra4Appclient.length() > 0) {
            if (extra4Appclient.endsWith(",")) {
                toInclude = extra4Appclient + "*_appclient_vehicle*.jar";
            } else {
                toInclude = "*_appclient_vehicle*.jar" + "," + extra4Appclient;
            }
        } else {
            toInclude = "*_appclient_vehicle*.jar";
        }
        makeEar("_appclient_vehicle", toInclude);
    }
    private void compileVehicles() {
        Javac compiler = new Javac();
        compiler.setProject(project);
        compiler.init();
        Path srcPath = new Path(project, TSBuildListener.sSrcDir);
        compiler.setSrcdir(srcPath);
        compiler.setTaskName("vehicles");
        compiler.setDestdir(TSBuildListener.fClassDir);
        compiler.setFork(false);
        compiler.setIncludes("com/sun/ts/tests/common/vehicle/**/*.java");
        compiler.setExcludes("com/sun/ts/tests/common/vehicle/jbi/**/*.java");
        compiler.setDeprecation(true);
        compiler.setDebug(true);
        compiler.setClasspath(TSBuildListener.getTsClasspath(project));
        compiler.setFailonerror(true);
        compiler.perform();
        TSBuildListener.vehiclesBuilt = true;
    }

    private File getExistingMF(String jarName) {
        File jarFile = new File(distDir, jarName);
        File tmpDir = new File(distDir, jarName + "__tmp");
        if (tmpDir.exists()) {
            boolean quiet = true;
            TaskUtil.deleteDir(tmpDir, project, quiet);
        }
        Expand dfr = new Expand();
        dfr.setProject(project);
        dfr.init();
        dfr.setSrc(jarFile);
        dfr.setDest(tmpDir);
        dfr.perform();

        File mf = new File(tmpDir, "META-INF/MANIFEST.MF");
        return mf;
    }

    /**
     * @param zipName the name of the archive against which to check the toarchive
     * attribute in support
     */
    private void addFilesToArchive(String zipName) {
        Support support = null;
        File toArchive = null;
        for (int i = 0, n = supports.size(); i < n; i++) {
            support = (Support) supports.get(i);
            toArchive = support.getToarchive();
            if (toArchive != null) {
                if (toArchive.getName().equalsIgnoreCase(zipName)) {
                    Jar jar = new Jar();
                    jar.setProject(project);
                    jar.init();
                    jar.setUpdate(true);
                    toArchive = new File(distDir, toArchive.getName());
                    jar.setJarfile(toArchive);

                    ZipFileSet zipFileSet = new ZipFileSet();
                    zipFileSet.setProject(project);
                    File dir = support.getDir(project);
                    if (dir == null) {
                        dir = project.getBaseDir();
                    }
                    zipFileSet.setDir(dir);

                    String includes = support.getOriginalIncludes();
                    if (includes != null && includes.length() > 0) {
                        zipFileSet.setIncludes(includes);
                    }

                    String excludes = support.getOriginalExcludes();
                    if (excludes != null && excludes.length() > 0) {
                        zipFileSet.setExcludes(excludes);
                    }

                    String prefix = support.getPrefixinarchive();
                    boolean expanded = false;
                    if (zipName.indexOf("_client.jar") != -1) {
                        expanded = true;
                        jar.setManifest(getExistingMF(zipName));
                    }

                    /*
                    if(prefix != null && prefix.equalsIgnoreCase("meta-inf")) {
                    jar.addMetainf(zipFileSet);
                    } else {
                    */

                    if (prefix != null && prefix.length() > 0) {
                        zipFileSet.setPrefix(prefix);
                    }
                    touch(toArchive);
                    jar.addZipfileset(zipFileSet);
                    //}

                    jar.setTaskName("toarchive");
                    jar.perform();

                    if (expanded) {
                        boolean quiet = true;
                        TaskUtil.deleteDir(new File(distDir, zipName + "__tmp"), project, quiet);
                    }
                }
            }
        }
    }

    private void touch(File file) {
        Touch touch = new Touch();
        touch.setProject(project);
        touch.init();
        touch.setFile(file);
        touch.setMillis((long) 30 * 365 * 24 * 60 * 60 * 1000);
        touch.perform();
    }

    private String[] getVehicles(File file) {
        String[] vehicles;
        VehicleVerifier vehicleVerifier = VehicleVerifier.getInstance(file);
        vehicles = vehicleVerifier.getVehicleSet();
        if (vehicles != null) {
            return vehicles;
        }
        return TaskUtil.EMPTY_STRING_ARRAY;
    }
    private void handleSupport(Packager pkgr) {
        for (int i = 0, n = this.supports.size(); i < n; i++) {
            Support spt = (Support) supports.get(i);
            String tovehicle = spt.getTovehicle();
            File toArchive = spt.getToarchive();
            if (toArchive != null) {
                continue;
            }
            if (tovehicle == null) {
                pkgr.addSupport(spt);
                continue;
            }
            //	    log("TSVehicle:add extra jar to:" + tovehicle + " "
            //		+ spt.getDir(project).getPath());
            if (tovehicle.equals("ejb")) {
                extra4Ejb += spt.getOriginalIncludes() + ",";
            } else if (tovehicle.equals("servlet")) {
                extra4Servlet += spt.getOriginalIncludes() + ",";
            } else if (tovehicle.equals("jsp")) {
                extra4Jsp += spt.getOriginalIncludes() + ",";
            } else if (tovehicle.equals("appclient")) {
                extra4Appclient += spt.getOriginalIncludes() + ",";
            }
        }
    }

    private void makeEar(String vname, String pattern) {
        if (ContainerPackage.buildLevel < ContainerPackage.APPLICATION_LEVEL) {
            return;
        }
        if (this.extraJars != null) {
            pattern = pattern + ", " + this.extraJars;
        }
        J2eeEar j2eeEarTask = new J2eeEar();
        j2eeEarTask.setProject(project);
        j2eeEarTask.init();
        j2eeEarTask.setTaskName("appear");
        j2eeEarTask.setName(this.name + vname);
        j2eeEarTask.setSrcdir(TSBuildListener.fClassDir);
        //        j2eeEarTask.setClasspath(j2eetsPath);
        Support archives = new Support();
        archives.setDir(this.distDir);
        archives.setIncludes(pattern);
        j2eeEarTask.addSupport(archives);
        j2eeEarTask.perform();
    }
    private void initAppName() {
        ejbName = name + "_ejb_vehicle";
        servletName = name + "_servlet_vehicle";
        jspName = name + "_jsp_vehicle";
        appclientName = name + "_appclient_vehicle";
    }

    private FileSet reorgRuntimeFiles(String[] vehicles, Set customFiles) {
        FileSet result = null;
        StringBuffer sb = new StringBuffer(400);
        for (int i = 0; i < vehicles.length; i++) {
            String v = vehicles[i];
            String mergedRuntimeFileName = null;
            if (v.equalsIgnoreCase("ejb")) {
                mergedRuntimeFileName = this.name + "_ejb_vehicle.runtime.xml";
                if ((new File(this.srcDir, mergedRuntimeFileName)).exists()) {
                    sb.append(mergedRuntimeFileName).append(',');
                    customFiles.add(mergedRuntimeFileName);
                    neededXml.remove(mergedRuntimeFileName);
                    neededXml.remove("ejb_vehicle_ejb.runtime.xml");
                    neededXml.remove("ejb_vehicle_client.runtime.xml");
                    neededXml.remove(this.name + "_ejb_vehicle_ejb.runtime.xml");
                    neededXml.remove(this.name + "_ejb_vehicle_client.runtime.xml");
                }
            } else if (v.equalsIgnoreCase("servlet")) {
                mergedRuntimeFileName = this.name + "_servlet_vehicle.runtime.xml";
                if ((new File(this.srcDir, mergedRuntimeFileName)).exists()) {
                    sb.append(mergedRuntimeFileName).append(',');
                    customFiles.add(mergedRuntimeFileName);
                    neededXml.remove(mergedRuntimeFileName);
                    neededXml.remove("servlet_vehicle_web.runtime.xml");
                    neededXml.remove("servlet_vehicle_servlet.runtime.xml");
                    neededXml.remove(this.name + "_servlet_vehicle_web.runtime.xml");
                    neededXml.remove(this.name + "_servlet_vehicle_servlet.runtime.xml");
                }
            } else if (v.equalsIgnoreCase("jsp")) {
                mergedRuntimeFileName = this.name + "_jsp_vehicle.runtime.xml";
                if ((new File(this.srcDir, mergedRuntimeFileName)).exists()) {
                    sb.append(mergedRuntimeFileName).append(',');
                    customFiles.add(mergedRuntimeFileName);
                    neededXml.remove("jsp_vehicle_web.runtime.xml");
                    neededXml.remove(mergedRuntimeFileName);
                    neededXml.remove("jsp_vehicle_jsp.runtime.xml");
                    neededXml.remove(this.name + "_jsp_vehicle_web.runtime.xml");
                    neededXml.remove(this.name + "_jsp_vehicle_jsp.runtime.xml");
                }
            } else if (v.equalsIgnoreCase("appclient")) {
                mergedRuntimeFileName = this.name + "_appclient_vehicle.runtime.xml";
                if ((new File(this.srcDir, mergedRuntimeFileName)).exists()) {
                    sb.append(mergedRuntimeFileName).append(',');
                    customFiles.add(mergedRuntimeFileName);
                    neededXml.remove(mergedRuntimeFileName);
                    neededXml.remove("appclient_vehicle_client.runtime.xml");
                    neededXml.remove(this.name + "_appclient_vehicle_client.runtime.xml");
                }
            } else {
                //do nothing for other vehicles
            }
        }
        String s = sb.toString();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.length() > 5) {
            result = new FileSet();
            result.setDir(this.srcDir);
            result.setIncludes(s);
        }
        return result;
    }

    public void copyAndReplace(String[] vehicles) {
        if (neededXml.size() == 0) {
            return;
        }

        //TODO: need to rework it once all runtime files are swept into one per ear.
        List fileSets = new ArrayList();
        //not for merged runtime.xml files per ear
        Set customFiles = new HashSet();

        //used for merged runtime.xml files per ear.  Use one fileset for all since they are in
        //the same dir (src dir)
        FileSet mergedRuntimeFileSet = reorgRuntimeFiles(vehicles, customFiles);

        if (mergedRuntimeFileSet != null) {
            fileSets.add(mergedRuntimeFileSet);
        }
        Set mergedGenericAdded = new HashSet(7);
        for (Iterator it = neededXml.iterator(); it.hasNext();) {
            String fileName = (String) it.next();
            //log("### neededXml: " + fileName);
            File f1 = new File(srcDir, fileName);
            FileSet fs = new FileSet();

            if (f1.exists()) {
                fs.setDir(srcDir);
                fs.setIncludes(fileName);
                fileSets.add(fs);
            } else {
                //some may name descriptor like connection_ejb_vehicle_ejb.xml,
                //where connection is the app.name. We then take this one instead
                //of the ejb_vehicle_ejb.xml from vehicle/ejb dir.
                String s = this.name + "_" + fileName;
                File customNamedFile = new File(srcDir, s);
                if (customNamedFile.exists()) {
                    fs.setDir(srcDir);
                    fs.setIncludes(s);
                    fileSets.add(fs);
                    customFiles.add(s); //record all custom-named descriptors
                } else {
                    String whichVehicle = fileName.substring(0, fileName.indexOf("_"));
                    File vehicleTypeDir = new File(vehicleDir, whichVehicle);
                    String mergedGeneric = whichVehicle + "_vehicle.runtime.xml";
                    if (!separateRuntimeFiles
                        && fileName.endsWith(".runtime.xml")
                        && (new File(vehicleTypeDir, mergedGeneric)).exists()) {
                        if (!mergedGenericAdded.contains(mergedGeneric)) {
                            fs.setDir(vehicleTypeDir);
                            fs.setIncludes(mergedGeneric);
                            fileSets.add(fs);
                            mergedGenericAdded.add(mergedGeneric);
                        }
                    } else if ((new File(vehicleTypeDir, fileName)).exists()) {
                        fs.setDir(vehicleTypeDir);
                        fs.setIncludes(fileName);
                        fileSets.add(fs);
                    } else {
                        log("Cannot find in vehicleDir or srcDir" + fileName);
                    }
                }
            }
        }
        Copy copy = new Copy();
        copy.setProject(project);
        copy.init();
        copy.setTaskName(getTaskName());
        copy.setTodir(distDir);
        for (int i = 0, n = fileSets.size(); i < n; i++) {
            FileSet temp = (FileSet) fileSets.get(i);
            copy.addFileset(temp);
        }
        copy.setOverwrite(true);
        copy.perform();
        neededXml.addAll(mergedGenericAdded);
        for (Iterator it = neededXml.iterator(); it.hasNext();) {
            String origName = (String) it.next();
            if (origName.startsWith(name)) {
                continue;
            }
            File orig = new File(distDir, origName);
            if (!orig.exists()) {
                continue;
            }
            String sToName = name + "_" + origName;
            File toName = new File(distDir, sToName);
            if (toName.exists()) {
                if (customFiles.contains(sToName)) {
                    continue;
                    //the custom-named descriptor was copied from src.dir.
                } else if (!toName.delete()) {
                    throw new BuildException("Cannot delete old file in dist dir:" + sToName);
                }
            }
            boolean renamed = orig.renameTo(toName);
            if (!renamed) {
                throw new BuildException("Failed to rename the file in distDir:" + origName);
            }
        }
        try {
            copyS1asRuntimes(vehicles);
        } catch (IOException exp) {
            throw new BuildException("Failed to copy a s1as runtime file from common vehicle dir", exp);
        }
        doReplace();
    }

    private void copyS1asRuntimes(String[] vehicles) throws IOException {
        boolean overwrite = true;
        File[] runtimes = srcDir.listFiles(ArchiveInfo.s1asRuntimeFileFilter);
        if(runtimes.length == 0) {
            log("No s1as runtime files in " + srcDir.getPath(), Project.MSG_VERBOSE);
        }
        for (int i = 0; i < runtimes.length; i++) {
            File rtf = runtimes[i];
            String rtfName = rtf.getName();
            File destFile = new File(this.distDir, rtfName);
            fileUtils.copyFile(rtf, destFile, null, overwrite);
        }
        
        overwrite = false;
        for (int i = 0; i < vehicles.length; i++) {
            String veh = vehicles[i];
            File vehicleSubdir = new File(vehicleDir, veh);
            File[] s1asFiles = vehicleSubdir.listFiles(ArchiveInfo.s1asRuntimeFileFilter);
            if (s1asFiles == null) {
                throw new BuildException("vehicle subdir does not exist:" + vehicleSubdir.getPath());
            } else if (s1asFiles.length == 0) {
//                log("No s1as runtime files in " + vehicleSubdir.getPath());
            } else {
                for (int j = 0; j < s1asFiles.length; j++) {
                    String longName = name + "_" + s1asFiles[j].getName();
                    File destFile = new File(distDir, longName);
                    if (!destFile.exists()) {
                        fileUtils.copyFile(s1asFiles[j], destFile, null, overwrite);
                    }
                }
            }
        }
    }

    private void doReplace() {
        //cannot reuse the same replace instance.  setToken will append
        //to existing token string.
        for (Iterator it = neededFilters.keySet().iterator(); it.hasNext();) {
            String oldVal = (String) it.next();
            String newVal = (String) neededFilters.get(oldVal);
            Replace replacer = new Replace();
            replacer.setProject(project);
            replacer.init();
            replacer.setTaskName("replace");
            replacer.setDir(distDir);
            replacer.setDefaultexcludes(true);
            replacer.setIncludes(name + "*_vehicle*xml");
            replacer.setToken(oldVal);
            replacer.setValue(newVal);
            replacer.perform();
        }
    }

    //  spt has not been initialized by ant.  So cannot inspect here.
    public void addSupport(Support spt) {
        this.supports.add(spt);
    }

    public void setName(String testName) {
        this.name = testName;
    }
    public String getName() {
        return this.name;
    }

    public void setManifest(File manifest) {
        if(!manifest.isFile()) {
            throw new BuildException("Cannot find specified manifest file:  " + manifest);
        }

        this.manifest = manifest;
    }
    public File getManifest() {
        return this.manifest;
    }
 
    public void setMainclass(String mainclass) {
        this.testClientClass = mainclass;
        this.mainclass = "com.sun.ts.tests.common.vehicle.VehicleClient"; 
    }
    public String getMainclass() {
        return this.mainclass;
    }
    public String getExtraJars() {
        return extraJars;
    }
    public void setExtraJars(String extraJars) {
        this.extraJars = extraJars;
    }
    public void addcontentFileset(FileSet set) {
        contentFileSets.add(set);
    }
    public FileSet createContent() {
        FileSet contentFileSet = new FileSet();
        contentFileSets.add(contentFileSet);
        return contentFileSet;
    }
}
