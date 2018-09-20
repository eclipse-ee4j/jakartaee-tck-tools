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
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import com.sun.ant.*;

public class CompatHelper extends Task  {
    private String javaversion;
    private String riversion;
    private static String validationMsg;  //make static to be used by copyArchiveTo()

    //true if the current target is compile.compat, or build.comapt, both
    //of which depend on init.compat. copyArchiveToSrc is invoked from
    //ContainerPackager to copy any archives from dist back to src. This
    //method should do nothing if the owing task of ContainerPackager is
    //the regular build, which copies archives from src to dist
    private static boolean isCompatBuild;

    //if not set, default to com/sun/ts/tests/compat<num>, as used in current pkg.dir
    //if set, append user value after the above default.
    private String cleanpkgdirs;
    private String javaInUse;
    private String riInUse;
    private String checkin;
    private String checkout;
    private String comment = "update";

    public void setJavaversion(String s) {
	javaversion = s;
    }
    public void setRiversion(String s) {
	riversion = s;
    }
    public void setCheckin(String s) {
	checkin = s;
    }
    public void setCheckout(String s) {
	checkout = s;
    }

    public static boolean isCompatBuild() {
	return isCompatBuild;
    }
    public static void copyArchivesToSrc(File distDir, File srcDir, Project project) {
	if(!isCompatBuild) {
	    return;
	}
	if(distDir == null || srcDir == null
	    || srcDir.compareTo(distDir) == 0) {
	    return;
	}
	File[] archiveFiles = distDir.listFiles(ArchiveInfo.rarJarWarEarFilter);
	File classDir = new File(project.getProperty("class.dir"),
	    project.getProperty("pkg.dir"));
	File[] classFiles = classDir.listFiles(ArchiveInfo.classFilter);
	boolean noArchive = (archiveFiles == null) || (archiveFiles.length == 0);
	boolean noClass = (classFiles == null) || (classFiles.length == 0);
	if(noArchive && noClass) {
	    return;
	}
	String includePattern = "";
	if(!noArchive) {
	    for (int i = 0; i < archiveFiles.length; i++) {
		includePattern = includePattern + archiveFiles[i].getName() + ",";
	    }
	    if(includePattern.endsWith(",")) {
		includePattern = includePattern.substring(0, includePattern.length() - 1);
	    }
	    FileSet fs = new FileSet();
	    fs.setDir(distDir);
	    fs.setIncludes(includePattern);
	    Copy copy = new Copy();
	    copy.setProject(project);
	    copy.init();
	    copy.setTaskName("Copy");
	    copy.setTodir(srcDir);
	    copy.setOverwrite(true);
	    copy.addFileset(fs);
	    copy.perform();
	    includePattern = "";
	}
	if(!noClass) {
	    for (int i = 0; i < classFiles.length; i++) {
		includePattern = includePattern + classFiles[i].getName() + ",";
	    }
	    if(includePattern.endsWith(",")) {
		includePattern = includePattern.substring(0, includePattern.length() - 1);
	    }
	    FileSet fs = new FileSet();
	    fs.setDir(classDir);
	    fs.setIncludes(includePattern);
	    Copy copy = new Copy();
	    copy.setProject(project);
	    copy.init();
	    copy.setTaskName("Copy");
	    copy.setTodir(srcDir);
	    copy.setOverwrite(true);
	    copy.addFileset(fs);
	    copy.perform();
	    includePattern = "";
	}
	project.log("####################### IMPORTANT #############################");
	if(noArchive) {
	    project.log("No archive has been generated.");
	} else {
	    project.log(archiveFiles.length + " archives have been generated in both src and dist dirs:");
	    for (int i = 0; i < archiveFiles.length; i++) {
	        project.log(archiveFiles[i].getName());
	    }
	}
	archiveFiles = null;
	project.log("---------------------------------------------------------------");
	if(noClass) {
	    project.log("No class has been compiled.");
	} else {
	    project.log(classFiles.length + " classes have been compiled into src and class dirs:");
	    for (int i = 0; i < classFiles.length; i++) {
	        project.log(classFiles[i].getName());
	    }
	}
	project.log("--------------------------- TODO ------------------------------");
	project.log("1 reset JAVA_HOME and j2ee.home.ri");
	project.log("2 tsant build runclient");
	project.log("3 sccs delget archive and class files");
	project.log("---------------------------------------------------------------");
	project.log("To avoid repeatedly editing ts.jte or build.properties:");
	project.log("  set j2ee.home.ri=${env.J2EE_HOME_RI} in ts.jte or build.properties, and");
	project.log("  switch J2EE_HOME_RI in shell between different versions");

	String finding = searchInSrcDir(project);
	boolean noFinding = (finding == null || finding.length() == 0);
	boolean noValidationMsg = (validationMsg == null || validationMsg.length() == 0);
	if(!noFinding || !noValidationMsg) {
	    project.log("---------------------------------------------------------------");
	    if(!noFinding) {
		    project.log(finding);
	    }
	    if(!noValidationMsg) {
		project.log(validationMsg);
	    }
	}
	project.log("###############################################################");
    }
    private void doCheckin() {
	File baseDir = project.getBaseDir();
	StringTokenizer st = new StringTokenizer(checkin, " ,");
	while(st.hasMoreTokens()) {
	    String token = st.nextToken();
	    File f = new File(baseDir, token);
	    if(new File(baseDir, "SCCS/p." + token).exists()) {
		TaskUtil.sccsDelget(project, f, comment);
	    } else if(!(new File(baseDir, "SCCS/s." + token).exists())) {
		log("File " + token + " does not exist in sccs, will create.");
		TaskUtil.sccsCreate(project,f);
	    }
	}
    }
    private void doCheckout() {
	File baseDir = project.getBaseDir();
	StringTokenizer st = new StringTokenizer(checkin, " ,");
	while(st.hasMoreTokens()) {
	    String token = st.nextToken();
	    File f = new File(baseDir, token);
	    TaskUtil.sccsEdit(project,f);
	}
    }
    private String retrieveRiversion() {
	String j2eeHomeRi = TSBuildListener.j2eeHomeRi;
	if(j2eeHomeRi == null || j2eeHomeRi.length() == 0) {
	    throw new BuildException("Please set j2ee.home.ri in bin/ts.jte or build.properties to a "
		+ riversion + " installation");
	}
	while(j2eeHomeRi.endsWith("/") || j2eeHomeRi.endsWith("\\")) {
	    j2eeHomeRi = j2eeHomeRi.substring(0, j2eeHomeRi.length() - 1);
	}

	ExecTask cmd = new ExecTask();
	cmd.setProject(project);
	cmd.init();
//	cmd.setDir(new File(j2eeHomeRi, "bin"));
	cmd.setTaskName("riversion");
	cmd.setFailonerror(true);
	String exeline = null;
	if(j2eeHomeRi.endsWith("/")) {
	    exeline = j2eeHomeRi + "bin/j2ee";
	} else if(j2eeHomeRi.endsWith("\\")) {
	    exeline = j2eeHomeRi + "bin\\j2ee";
	} else if(j2eeHomeRi.indexOf("\\") != -1) {
	    exeline = j2eeHomeRi + "\\bin\\j2ee";
	} else {
	    exeline = j2eeHomeRi + "/bin/j2ee";
	}

	Commandline.Argument arg = cmd.createArg();
	arg.setLine("-version");
	cmd.setExecutable(exeline);
	cmd.setVMLauncher(true);
	cmd.setOutputproperty("j2ee.ri.version");
	cmd.setTimeout(new Integer(5000*60));
	try {
	    cmd.perform();
	} catch (BuildException ex) {
	    ex.printStackTrace();
	    log("Mostly likely your J2EE_HOME_RI is not pointing to the right version.");
	    throw new BuildException(ex.getMessage());
	}
	String vstr = project.getProperty("j2ee.ri.version");
	String result = null;
	if(vstr != null) {
	    vstr = vstr.toLowerCase();
	    int pos = vstr.indexOf("version");
	    if(pos != -1) {
		result = vstr.substring(pos + 7).trim();
	    } else {
		log("Output does not contain 'version' (case-insensitive)");
	    }
	} else {
	    log("Got null from running j2ee -version.");
	}
	return result;
    }
    private void resetDtds() {
	if(riversion.startsWith("1.2")) {
	    SunRITool.PUBLICID_APP_CLIENT =
		"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN";
	    SunRITool.PUBLICID_EJB20 =
		"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN";
	    SunRITool.PUBLICID_WEB =
		"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";

	    SunRITool.DEFAULT_SUNRI13_APP_CLIENT_DTD_LOCATION =
		"application-client_1_2.dtd";
	    SunRITool.DEFAULT_SUNRI13_EJB20_DTD_LOCATION =
		"ejb-jar_1_1.dtd";
	    SunRITool.DEFAULT_SUNRI13_WEB_DTD_LOCATION =
		"web-app_2_2.dtd";
	} else if(riversion.startsWith("1.3")) {
	    SunRITool.PUBLICID_EJB20 =
		"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
	    SunRITool.PUBLICID_APP_CLIENT =
		"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";
	    SunRITool.PUBLICID_WEB =
		"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";

	    SunRITool.DEFAULT_SUNRI13_EJB20_DTD_LOCATION =
	        "ejb-jar_2_0.dtd";
	    SunRITool.DEFAULT_SUNRI13_APP_CLIENT_DTD_LOCATION =
		"application-client_1_3.dtd";
	    SunRITool.DEFAULT_SUNRI13_WEB_DTD_LOCATION =
	        "web-app_2_3.dtd";
	}
    }

    public void setCleanpkgdirs(String s) {
        cleanpkgdirs = s;
    }
    private void cleanOne(String pkgDir) {
        String classDir = (String) project.getProperty("class.dir");
        File dir1 = new File(classDir);
        FileSet fs1 = new FileSet();
        fs1.setDir(dir1);
        File dir2 = new File((String) project.getProperty("dist.dir"), pkgDir);
        FileSet fs2 = new FileSet();
        fs2.setDir(dir2);

        Delete del = new Delete();
        del.setProject(project);
        del.init();
        del.setTaskName(getTaskName());
        del.setFailOnError(false);
        del.setIncludeEmptyDirs(false);
        del.addFileset(fs1);
        del.perform();

        del = new Delete();
        del.setProject(project);
        del.init();
        del.setTaskName(getTaskName());
        del.setFailOnError(false);
        del.setIncludeEmptyDirs(false);
        del.addFileset(fs2);
        del.perform();
        dir1.mkdirs();
    }
    private void cleanup() {
       String pkgDir = (String) project.getProperty("pkg.dir");
       pkgDir = pkgDir.replace('\\', '/');
       int pos1 = pkgDir.indexOf("/tests/");
       if(pos1 == -1) {
           throw new BuildException("pkg.dir does not contain '/tests/':" + pkgDir);
       }
       int pos2 = pkgDir.indexOf("/", pos1 + 7);
       if(pos2 == -1) {
          cleanOne(pkgDir);
       } else {
          cleanOne(pkgDir.substring(0, pos2));
       }
       if(cleanpkgdirs != null) {//user defined cleanpkgdirs
          StringTokenizer st = new StringTokenizer(cleanpkgdirs, " ,\n\r\f\t");
          while(st.hasMoreTokens()) {
             String token = st.nextToken();
             cleanOne(token);
          }
       }
    }
    public void execute() throws BuildException {
	if(javaversion == null || javaversion.length() == 0
	    || riversion == null || riversion.length() == 0) {
	    throw new BuildException("Please specify javaversion and riversion that should be used to build.");
	}
	javaversion = javaversion.trim();
	if(!javaversion.equals("1.2") && !javaversion.equals("1.3")) {
	    throw new BuildException("Invalid javaversion:" + javaversion + ". Choose 1.2 or 1.3");
	}
	if(!riversion.equals("1.2") && !riversion.equals("1.3")) {
	    throw new BuildException("Invalid riversion:" + riversion + ". Choose 1.2 or 1.3");
	}
	javaInUse = project.getProperty("java.version");
	if(javaInUse.startsWith(javaversion)) {
        project.log("####################### IMPORTANT #############################");
	    project.log("java version = " + javaInUse);
	} else {
	    throw new BuildException("Current java.version is " + javaInUse
		+ ". Please setenv JAVA_HOME to a JDK" + javaversion + " installation.");
	}
	riInUse = retrieveRiversion();
	if(riInUse.startsWith(riversion)) {
	    project.log("ri   version = " + riInUse);
	} else {
	    throw new BuildException("Current RI version is " + riInUse
		+ ". Please set j2ee.home.ri in bin/ts.jte or build.properties to a j2ee " + riversion + " installation.");
	}
	CompatValidator va = new CompatValidator();
	va.validate(project.getProperty("ant.file"));
	validationMsg = va.getMsg();
	if(validationMsg.length() > 0) {
	    project.log("---------------------------------------------------------------");
	    project.log(validationMsg);
	}

    //log("###############################################################");
    project.log("---------------------------------------------------------------");
    project.log("Make sure you've sccs-edited archives & classes to be built.");
    project.log("###############################################################");
    resetDtds();
    //clean up specified pkg.dirs to prevent byte code of later version from leaking into prior archives.
    cleanup();
    isCompatBuild = true;
//	System.setProperty("riversion", riversion); //to be used by packag task

//	if(checkout != null) {
//	    doCheckout();
//	    if(checkin != null) {
//		doCheckin();
//	    } else {
//		log("Files: " + checkout + " have been checked out and updated. Please delget them.");
//	    }
//	} else {
//	    if(sccsdelget != null) {
//
//	    }
//	}
    }

    private static String searchInSrcDir(Project project) {
	File srcDir = project.getBaseDir();
	File sccsDir = new File(srcDir, "SCCS");
	if(!sccsDir.exists()) {
	    return "WARN: SCCS dir does not exist!";
	}
	String result = "";
//	File[] classFiles = srcDir.listFiles(ArchiveInfo.classFilter);
//	int classFilesNum = (classFiles == null) ? 0 : classFiles.length;
//	File[] archiveFiles = srcDir.listFiles(ArchiveInfo.rarJarWarEarFilter);
//	int archiveFilesNum = (archiveFiles == null) ? 0 : archiveFiles.length;

	File[] classFiles2 = sccsDir.listFiles(ArchiveInfo.classFilter);
	int classFilesNum2 = (classFiles2 == null) ? 0 : classFiles2.length;
	File[] archiveFiles2 = sccsDir.listFiles(ArchiveInfo.rarJarWarEarFilter);
	int archiveFilesNum2 = (archiveFiles2 == null) ? 0 : archiveFiles2.length;

	if(archiveFilesNum2 == 0)
	    result += "WARN: no archive file was found under SCCS\n";
	if(classFilesNum2 == 0)
	    result += "WARN: no class file was found under SCCS";
	    //add \n to the above line if need to append more to result after this
	return result;
    }
}
