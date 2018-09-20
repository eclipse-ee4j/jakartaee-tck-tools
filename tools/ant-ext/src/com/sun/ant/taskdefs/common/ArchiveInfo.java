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
import java.util.zip.*;
import java.util.jar.*;

public class ArchiveInfo {
    public final static String SUN_EJB_JAR_XML            = ".sun-ejb-jar.xml";
    public final static String SUN_WEB_XML                = ".sun-web.xml";
    public final static String SUN_APPLICATION_XML        = ".sun-application.xml";
    public final static String SUN_APPLICATION_CLIENT_XML = ".sun-application-client.xml";
    public final static String SUN_CONNECTOR_XML          = ".sun-connector.xml";

        public static FileFilter convertedXmlFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(!fname.endsWith(".xml")) {
        return false;
        }
        if(fname.startsWith(",")) {
        return false;
        }
        if(fname.equals("build.xml")) {
        return false;
        }
        if(fname.indexOf("build.xml") != -1) {
        return false;
        }
        if(fname.indexOf(".props.xml") != -1) {
        return false;
        }
        long t = pathname.lastModified();
        long justNow = System.currentTimeMillis() - 3 * 60 * 1000;
        if(t < justNow) {
        return false;
        }
        return true;
    }
    };
    public static FileFilter xmlDescriptorFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(!fname.endsWith(".xml")) {
        return false;
        }
        if(fname.startsWith(",")) {
        return false;
        }
        if(fname.equals("build.xml")) {
        return false;
        }
        if(fname.indexOf("build.xml") != -1) {
        return false;
        }
        return true;
    }
    };
    public static FileFilter earFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return fname.endsWith(".ear");
    }
    };
    public static FileFilter classFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return fname.endsWith(".class");
    }
    };
    public static FileFilter otherMakeFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(fname.equals("Makefile")) {
        return false;
        }
        return fname.startsWith("Makefile.");
    }
    };
    public static FileFilter dirFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isDirectory()) {
            return false;
        }
        String fname = pathname.getName();
        return !(fname.equals("SCCS"));
    }
    };
    public static FileFilter runtimeFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(fname.startsWith(",")) {
        return false;
        }
        return fname.endsWith(".runtime.xml");
    }
    };

    public static FileFilter s1asRuntimeFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(fname.endsWith(".xml") && 
            (fname.indexOf(".sun-") != -1 || fname.startsWith("sun-"))) 
            return true;
        if(fname.endsWith("dbschema"))
            return true; 
        return false;
    }
    };

    public static FileFilter jarWarFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return (fname.endsWith(".jar") || fname.endsWith(".war"));
    }
    };

    public static FileFilter warFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return fname.endsWith(".war");
    }
    };

    public static FileFilter rarJarWarEarFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return (fname.endsWith(".ear")
        || fname.endsWith(".jar")
        || fname.endsWith(".war")
        || fname.endsWith(".rar"));
    }
    };
    public static FileFilter uselessFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return (fname.endsWith(".props.xml"));
    }
    };
    public static FileFilter vehicleXmlFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        int i = fname.indexOf("_vehicle_");
        return (fname.endsWith(".xml") && (i != -1));
    }
    };
    public static FileFilter clientJavaFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        if(fname.startsWith(",")) {
        return false;
        }
        return (fname.endsWith(".java")) && (
         (fname.indexOf("Client") != -1) ||
         (fname.indexOf("client") != -1) );
    }
    };
    public static FileFilter propsFileFilter = new FileFilter() {
    public boolean accept(File pathname) {
        if(!pathname.isFile()) {
            return false;
        }
        String fname = pathname.getName();
        return ((fname.endsWith(".props"))
            || (fname.endsWith(".cprops"))
        || (fname.endsWith(".webprops"))
        );
    }
    };

    public static void main(String[] args) throws Exception {
    ArchiveInfo arch = new ArchiveInfo();
    arch.jarWarInEar(new File(args[0]));
    }

    //Do not modify tokenVal. For read only
    public static Properties getAttrs(String archiveName,
                      Properties tokenVal,
                      File leafDir) {
    final String clientJar = "clientjar";
    final String ejbJar = "ejb-jar";
    final String webWar = "webwar";
    final String resRar = "resrar";
    Properties attrs = new Properties();
    attrs.setProperty("mainClassElement","");
    int i = archiveName.indexOf("_client.jar");
    if(i != -1){
        String appName = archiveName.substring(0, i);
        attrs.setProperty("taskName", clientJar);
        attrs.setProperty("descriptor", appName + "_client.xml");
        attrs.setProperty("name", appName);
        String mainClass = tokenVal.getProperty("mainClass");
        if(mainClass == null) {
        File[] clientJava = leafDir.listFiles(clientJavaFilter);
        if(clientJava != null) {
            //need to append pkg and replace .java with .class
            //add mainClassElement in Make2Ant.
            String temp = clientJava[0].getName();
            temp = temp.substring(0, temp.indexOf(".java"));
            String pkgDir = tokenVal.getProperty("pkgDir");
            if(pkgDir != null) {
            mainClass = pkgDir.replace('/', '.') + "." + temp;
            tokenVal.setProperty("mainClass", mainClass);
            } else {
            mainClass = "@mainClass@";
            }
        } else {
            mainClass = "@mainClass@";
        }
        }
        String mainClassElement = Make2Ant.NL + Make2Ant.EIGHT + "mainclass=\"" + mainClass + "\"";
        attrs.setProperty("mainClassElement",mainClassElement);
        return attrs;
    }
    i = archiveName.indexOf("_ejb.jar");
    if(i != -1){
        String appName = archiveName.substring(0, i);
        attrs.setProperty("taskName", ejbJar);
        attrs.setProperty("descriptor", appName + "_ejb.xml");
        attrs.setProperty("name", appName);
        return attrs;
    }
    i = archiveName.indexOf("_web.war");
    if(i != -1){
        String appName = archiveName.substring(0, i);
        attrs.setProperty("taskName", webWar);
        attrs.setProperty("descriptor", appName + "_web.xml");
        attrs.setProperty("name", appName);
        return attrs;
    } else {  //for foo_bar.war
        i = archiveName.indexOf(".war");
        if(i != -1){
        String appName = archiveName.substring(0, i);
        attrs.setProperty("taskName", webWar);
        attrs.setProperty("descriptor", appName + ".xml");
        attrs.setProperty("name", appName);
        return attrs;
        }
    }
    return attrs;
    }

    //Do not modify tokenVal. For read only
    public static Properties getAttrs(File archive,
                      Properties tokenVal,
                      File leafDir) {
    if(archive == null) {
        return null;
    }
    return getAttrs(archive.getName(), tokenVal, leafDir);
    }
    //remove META-INF/classes
    public static String cleanName(String name) {
    String result = null;
    int i = name.indexOf("/com/");
    if(i != -1) {
         result = name.substring(i + 1);
    } else {
        int j = name.indexOf("/classes/");
        if(j != -1) {
        result = name.substring(j + "/classes/".length());
        } else {
        result = name;
        }
    }
    return result;
    }
    //see if the className is already included in the inlist
    //return true if className is an inner class whose baseclass
    //is in inlist.
    static boolean isIncluded(List inlist, String className) {
    String s = className.replace('/', '.');
    if(inlist.contains(s)) {
        return true;
    }
    int pos = s.indexOf("$");
    if(pos == -1) {
        return false;
    }
    String baseClass = s.substring(0, pos) + ".class";
    return inlist.contains(baseClass);
    }


    //for example: return "ejb_sam_hello_ejb.jar, ejb_sam_hello_client.jar"
    public static String jarWarInEar(File file){
    String result = "";
    try {
        JarFile ear = new JarFile(file);
        for(Enumeration enu = ear.entries(); enu.hasMoreElements();) {
        ZipEntry infile = (ZipEntry) enu.nextElement();
        if(!infile.isDirectory()) {
            String name = infile.getName();
            if(name.endsWith(".jar")) {
            result = result + name + ", ";
            } else if(name.endsWith(".war")) {
            result = result + name + ", ";
            } else if(name.endsWith(".rar")) {
            result = result + name + ", ";
            }
        }
        }//for
    } catch(Exception ex) {
        ex.printStackTrace();
    }
    if(result.endsWith(", ")) {
        result = result.substring(0, result.length() - 2);
        return result;
    } else {
        System.out.println("WARNING: No jar, war, or rar in ear file:" + file.getPath());
        return null;
    }
    }
}
