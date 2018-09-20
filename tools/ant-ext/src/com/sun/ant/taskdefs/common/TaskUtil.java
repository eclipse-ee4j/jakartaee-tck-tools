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
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.types.*;
import com.sun.ant.TSBuildListener;

public class TaskUtil {
    final public static String[] EMPTY_STRING_ARRAY = new String[0];
    final public static String SCCS_CMD = "/usr/ccs/bin/sccs";
    final public static String DEFAULT_DIST = "dist";

    public static void deleteDir(File dir, Project project, boolean quiet) {
	Delete del = new Delete();
	del.setProject(project);
	del.init();
	del.setQuiet(quiet);
	del.setIncludeEmptyDirs(true);
	del.setFailOnError(false);
	del.setDir(dir);
	del.perform();
    }

    public static boolean isCompatDir(File dir) {
	if(dir == null) {
	    return false;
	}
	return isCompatDir(dir.getPath());
    }
    public static boolean isCompatDir(String path) {
	if(path == null) {
	    return false;
	}
	if(path.indexOf("/compat") != -1 || path.indexOf("\\compat") != -1) {
	    return true;
	}
	return false;
    }
    public static File getFullDistDir(File srcDir, Project proj) {
	File destDir = null;
	String sDistDir = proj.getProperty("dist.dir");
        if(sDistDir == null) {
            sDistDir = System.getProperty("dist.dir");
        }
	if(sDistDir != null && sDistDir.length() != 0) {
	    destDir = new File(sDistDir, TaskUtil.path2PkgDir(srcDir));
	} else {
            destDir = new File(new File(TSBuildListener.tsHome, DEFAULT_DIST),
		TaskUtil.path2PkgDir(srcDir));
	}
	return destDir;
    }
    public static File getFullDistDir(Project proj) {
        //dist.dir should be like {ts.home}/dist
	File destDir = null;
	String sDistDir = proj.getProperty("dist.dir");
	if(sDistDir != null && sDistDir.length() != 0) {
	    destDir = new File(sDistDir, TaskUtil.project2PkgDir(proj));
	} else {
            destDir = new File(new File(TSBuildListener.tsHome, DEFAULT_DIST),
		TaskUtil.path2PkgDir(getCurrentSrcDir(proj)));
	}
	return destDir;
    }

    public static String path2PkgDir(String path) {
	if(path == null || path.length() == 0) {
	    return null;
	}
	String pkgDir = null;
	String toFind = "/src/";
	path = path.replace('\\', '/');
	if(!path.endsWith("/")) {
	    path += "/";
	}
	boolean found = false;
	int i = path.indexOf(toFind);
	while(i != -1) {
	    String before = path.substring(0, i);
	    if((new File(before, "bin") ).exists() ||
	        (new File(before, "lib")).exists() ) {
		pkgDir = path.substring(i + 5);
		break;
	    } else {
		i = path.indexOf(toFind, i + 5);
	    }
	}
	if(pkgDir != null && pkgDir.endsWith("/")) {
	    pkgDir = pkgDir.substring(0, pkgDir.length() - 1);
	}
	return pkgDir;
    }

    public static String project2PkgDir(Project proj) {
	return path2PkgDir(getCurrentSrcDir(proj));
    }
    public static String path2PkgDir(File path) {
	return path2PkgDir(path.getPath());
    }
    public static File getCurrentSrcDir(Project project) {
        File srcDir = null;
        String pkgDir = project.getProperty("pkg.dir");
        if(pkgDir != null && pkgDir.length() != 0) {
            srcDir = new File(TSBuildListener.tsHome, "src/" + pkgDir);
            return srcDir;
        }
	srcDir = project.getBaseDir();
	if(srcDir.compareTo(TSBuildListener.fBin) == 0) {
	    srcDir = new File(System.getProperty("user.dir"));
	}
	return srcDir;
    }
    public static String replace(String whole, String old, String sub) {
        if(whole == null || old == null || sub == null) {
            return whole;
        }
        int start = 0;
        int pos = -1;
        int oldLength = old.length();
        StringBuffer sb = new StringBuffer();
        try {
            pos = whole.indexOf(old, start);
            while(pos >= 0) {
                sb.append(whole.substring(start, pos));
                sb.append(sub);
                start = pos + oldLength;
                pos = whole.indexOf(old, start);
            }
            sb.append(whole.substring(start));
        } catch (NullPointerException ex) {
        } catch (StringIndexOutOfBoundsException se) {
        }
        return sb.toString();
    }

    public static void sccsEdit(Project project, File file) {
	if(!((new File(file.getParentFile(), "SCCS/s." + file.getName())).exists())) {
	    System.out.println("Not in SCCS, skip sccs edit:" + file.getPath());
	    return;
	}
	if((new File(file.getParentFile(), "SCCS/p." + file.getName())).exists()) {
	    System.out.println("Already checked out, skip sccs edit:" + file.getPath());
	    return;
	}
	doSccs(project, file, "edit");
    }
    public static void sccsCreate(Project project, File file) {
	doSccs(project, file, "create");
    }
    public static void sccsCreate(Project project, File dir, String files) {
	doSccs(project, dir, files, "create");
    }

    public static void sccsDelget(Project project, File file) {
	sccsDelget(project, file, "This delta is generated by ant task");
    }
    public static void sccsDelget(Project project, File file, String comment) {
    if(!(new File(file.getParentFile(), "SCCS/s." + file.getName()).exists() )) {
        return;
    }
	doSccs(project, file, "delget " + "-y'" + comment + "'");
    }

    private static void doSccs(Project project, File dir, String files, String sccsSubCmd) {
	if(dir == null || files == null || files.length() == 0) {
	    return;
	}
	ExecTask exec = new ExecTask();
	exec.setProject(project);
	exec.init();
	exec.setExecutable(TaskUtil.SCCS_CMD);
	exec.setDir(dir);
	exec.setFailonerror(false);
	exec.setTaskName("SCCS");
	Commandline.Argument arg = exec.createArg();
	arg.setLine(sccsSubCmd + " " + files);
	exec.perform();
    }
    private static void doSccs(Project project, File file, String sccsSubCmd) {
	if(file == null) {
	    return;
	}
	ExecTask exec = new ExecTask();
	exec.setProject(project);
	exec.init();
	exec.setExecutable(TaskUtil.SCCS_CMD);
	exec.setDir(file.getParentFile());
	exec.setFailonerror(false);
	exec.setTaskName("SCCS");
	Commandline.Argument arg = exec.createArg();
	arg.setLine(sccsSubCmd + " " + file.getName());
	exec.perform();
    }
}
