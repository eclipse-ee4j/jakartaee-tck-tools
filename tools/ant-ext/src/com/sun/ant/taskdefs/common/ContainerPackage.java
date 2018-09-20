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

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.util.FileUtils;
import com.sun.ant.*;
import com.sun.ant.taskdefs.*;
import com.sun.ant.taskdefs.ejb.*;
import com.sun.ant.taskdefs.web.*;
import com.sun.ts.lib.harness.*;
import com.sun.ts.lib.util.*;

public class ContainerPackage extends Task implements TaskContainer {
    private List nestedTasks = new ArrayList();
    public final static byte COMPILE_LEVEL = 1;
    public final static byte COMPONENT_LEVEL = 2;
    public final static byte APPLICATION_LEVEL = 3;
    static byte buildLevel;  //will only be initialized in execute().
    private TSVehicles vehiclesTask;
    private static List builtCommonApps = new ArrayList();
    private static ClassLoader deliverableClassLoader;
    private File srcDir;
    private File distDir;
    private FileUtils fileUtils = FileUtils.newFileUtils();

    public void myinit() {
    srcDir = TaskUtil.getCurrentSrcDir(project);
    distDir = TaskUtil.getFullDistDir(srcDir, project);
    if(buildLevel != 0) {
        return;
    }
    String sLevel = project.getProperty("build.level");
    if(sLevel == null) {
       sLevel = System.getProperty("build.level");
    }
    if(sLevel != null) {
        try {
        sLevel = sLevel.trim();
        buildLevel = Byte.parseByte(sLevel);
        if(buildLevel != 1 && buildLevel != 2 && buildLevel != 3) {
            buildLevel = APPLICATION_LEVEL;
            log("Invalid build.level " + sLevel + ". Set to default "
            + APPLICATION_LEVEL);
        } else {
            log("build level is set to " + String.valueOf(buildLevel)
            + ". (1-compile only; 2-compile and build jar and war; 3-application archives.",
            Project.MSG_VERBOSE);
        }
        } catch(NumberFormatException nex) {
        log("WARNING: Could not parse build.level.  Set to default "
            + APPLICATION_LEVEL);
        buildLevel = APPLICATION_LEVEL;
        }
    } else {
        buildLevel = APPLICATION_LEVEL;
        log("build.level from project property and system property is null.  Set to default application level.");
    }
    }

    public void execute() {
    try {
        myinit();
        if(buildLevel == COMPILE_LEVEL || nestedTasks.size() == 0) {
        return;
        }
        //	showTasks();
        //if building from top, no need to build common apps
        if(!TSBuildListener.skipMakeupCompile()) {
        //doCommonApps();
        }
        for(int i = 0, n = nestedTasks.size(); i < n; i++) {
        Task nestedTask = (Task) nestedTasks.get(i);
        if(buildLevel == COMPONENT_LEVEL){
            if(!nestedTask.getTaskName().equals("appear")) {
            nestedTask.perform();
            }
        } else {
            nestedTask.perform();
        }
        }
        if(vehiclesTask == null) {
        copyRuntime();
        }
        removeJarWar();
        if(TaskUtil.isCompatDir(srcDir)) {
        CompatHelper.copyArchivesToSrc(distDir, srcDir, project);
        }
    } catch (Throwable th) {
        th.printStackTrace();
        TSLogger.addFailedDir(srcDir.getPath());
    }
    }

    private void removeJarWar() {
    if(buildLevel == APPLICATION_LEVEL) {
        File[] jarwars = distDir.listFiles(ArchiveInfo.jarWarFilter);
        if(jarwars == null) {
            if(this.vehiclesTask.isStandAlone) {
                return;  //for standalone, distDir is not created.
            } else {
                throw new BuildException("distDir does not exist: " + distDir.getPath());
            }
        }
        for (int i = 0; i < jarwars.length; i++) {
        String fileName = jarwars[i].getName();
        if(fileName.indexOf("_component_") == -1){
            if(!jarwars[i].delete()) {
            log("WARNING: failed to delete " + jarwars[i].getPath());
            }
        }
        }
    }
    }

    /**
     * @exception BuildException if it fails to copy any runtime file.
     */
    private void copyRuntime0(File[] runtimes) {
        File rtf = null;
        File destFile = null;
        String rtfName = null;
        boolean overwrite = true;
        try {
            for (int i = 0; i < runtimes.length; i++) {
                rtf = runtimes[i];
                rtfName = rtf.getName();
                destFile = new File(this.distDir, rtfName);
                fileUtils.copyFile(rtf, destFile, null, overwrite);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new BuildException("Failed to copy a runtime file.", ex);
        }
    }

    private void copyRuntime() {
    if(srcDir.compareTo(distDir) == 0) {
        return;
    }
    File[] s1asRuntimeFiles = srcDir.listFiles(ArchiveInfo.s1asRuntimeFileFilter);
    if(s1asRuntimeFiles.length == 0) {
        log("WARNING: No s1as runtime.xml files in:" + srcDir.getPath(), Project.MSG_VERBOSE);
    } else {
        copyRuntime0(s1asRuntimeFiles);
    }

    File[] runtimeFiles = srcDir.listFiles(ArchiveInfo.runtimeFileFilter);
    if(runtimeFiles.length == 0) {
//        log("WARNING: No RI runtime.xml files in:" + srcDir.getPath());
    } else {
        copyRuntime0(runtimeFiles);
    }
//    String includePattern = "";
//    for (int i = 0; i < runtimeFiles.length; i++) {
//        includePattern = includePattern + runtimeFiles[i].getName() + ",";
//    }
//    if(includePattern.endsWith(",")) {
//        includePattern = includePattern.substring(0, includePattern.length() - 1);
//    }
//    FileSet fs = new FileSet();
//    fs.setDir(srcDir);
//    fs.setIncludes(includePattern);
//	fs.setExcludes(",*.xml");  this would excludes all
//    Copy copy = new Copy();
//    copy.setProject(project);
//    copy.init();
//    copy.setTaskName(getTaskName());
//    copy.setTodir(distDir);
//    copy.setOverwrite(true);
//    copy.addFileset(fs);
//    copy.perform();

    if(ContainerPackage.buildLevel == ContainerPackage.COMPONENT_LEVEL) {
        checkRuntimeFileName();  //added for servlet/jsp tck with build level 2
    }
    }

    private void checkRuntimeFileName0(String warName, String extension) {
        String expectedRuntimeFileName = warName.substring(0, warName.indexOf(".war"))
                                       + extension;  // foo_web.runtime.xml
        File expectedRuntimeFile = new File(distDir, expectedRuntimeFileName);
        if(!expectedRuntimeFile.exists()) {
            int x = warName.lastIndexOf("_web");
            String otherName = null;
            if(x != -1) {
                otherName = warName.substring(0, x) + extension;
            } else {
                log("war name: " + warName + " does not contain _web, skip renaming runtime file.");
                return;
            }
            File otherFile = new File(distDir, otherName);
            if(!otherFile.renameTo(expectedRuntimeFile)) {
                log("Failed to rename " + otherName + " to " + expectedRuntimeFileName,
                    Project.MSG_VERBOSE);
            }
        }
    }

    private void checkRuntimeFileName() {
        File[] wars = distDir.listFiles(ArchiveInfo.warFilter);
        for (int i = 0; i < wars.length; i++) {
            String warName = wars[i].getName();  // foo_web.war
            checkRuntimeFileName0(warName, ".runtime.xml");
            checkRuntimeFileName0(warName, ".sun-web.xml");
        }
    }

    private void ejbJars4Vehicles() {
    String extraJars = "";
    for(int i = 0, n = nestedTasks.size(); i < n; i++) {
        Task task = (Task) nestedTasks.get(i);
        String taskName = task.getTaskName();
        if(taskName.equals("ejb-jar")) {
        String jarName = ((EJBJar) task).getName();
        extraJars = extraJars + jarName + ".jar, ";
        } else if(taskName.equals("webwar")) {
        String jarName = ((WebWar) task).getName();
        extraJars = extraJars + jarName + ".war, ";
        }
    }
    if(extraJars.endsWith(", ")) {
        extraJars = extraJars.substring(0, extraJars.length() - 2);
        vehiclesTask.setExtraJars(extraJars);
    }
    }

    public static ClassLoader getDeliverableClassLoader(Project project) {
    if(deliverableClassLoader == null) {
        deliverableClassLoader = new AntClassLoader(project,
        TSBuildListener.getTsClasspath(project));
    }
    return deliverableClassLoader;
    }
    /*private void doCommonApps() {
    //if the target is package.compat, do not build common app
    //if(CompatHelper.isCompatBuild()) {
       // return;
    //}
    //String sPath = srcDir.getPath();
    
    //commenting out this line due to changes in CTS spider
    //This code and really this class is part of the old packager and should be removed from ant-ext

    //String[] commonApps = CommonAppVerifier.getCommonApps(
        //TestUtil.srcToDist(sPath), getDeliverableClassLoader(project));
    //if(commonApps == null || commonApps.length == 0) {
        //return;
    //}
//	log("Common Apps for " + sPath + ":" + Arrays.asList(commonApps).toString());
    for(int i = 0; i < commonApps.length; i++) {
        if(builtCommonApps.contains(commonApps[i])) {
        continue;
        }
        String appSrc = TaskUtil.replace(commonApps[i].replace('\\','/'),
        "/dist/", "/src/");
        File appDir = new File(appSrc);
        if(appDir.compareTo(srcDir) == 0) {
        continue;
        }
        log("Building common app " + appSrc);
        Ant antTask = new Ant();
        antTask.setProject(project);
        antTask.init();
        antTask.setDir(appDir);
        antTask.setTarget("build");
        antTask.setInheritAll(false);
        antTask.perform();
        builtCommonApps.add(commonApps[i]);
    }
    }*/
    private void showTasks() {
    log("Nested tasks in container:", Project.MSG_VERBOSE);
        for(int i = 0, n = nestedTasks.size(); i < n; i++) {
            Task nestedTask = (Task) nestedTasks.get(i);
        log(nestedTask.getTaskName(), Project.MSG_VERBOSE);
        }
    }

    public void addTask(Task task) {
	if(buildLevel == COMPILE_LEVEL) {
	    return;
	}
	
	/*
	 * Added this check when we moved to Ant 1.6.2.  The TaskContainer.addTask is
	 * now being passed an instace of org.apache.tools.ant.UnknownElement.  To
	 * work around this we need to acquire the real task and the real task only
	 * seems to get configged with the appropriate values after calling maybeConfigure.
	 * Failure to call maybeConfigye causes getTask to return null.  See the Ant FAQ
	 * for more details.
	 */
	Task t = task;
	if (task instanceof UnknownElement) {
	    task.maybeConfigure();
	    t = ((UnknownElement)task).getTask();
	}
	
	String taskName = t.getTaskName();
	if(taskName.equals("vehicles")) {
	    //assuming only 1 vehicles task inside package task.
	    if(t instanceof TSVehicles) {
		this.vehiclesTask = (TSVehicles) t;
	    } else {
		throw new BuildException("task vehicles is not instanceof TSVehicles");
	    }
	} else if(taskName.equals("ejbjar")) {
	    throw new BuildException("Use ejb-jar task instead of ejbjar.");
	}
	nestedTasks.add(t);
    }
}
