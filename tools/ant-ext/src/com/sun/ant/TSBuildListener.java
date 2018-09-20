/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 */

/*
 * $Id$
 */

package com.sun.ant;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.taskdefs.Taskdef;
import org.apache.tools.ant.types.Path;

import com.sun.ant.taskdefs.common.TaskUtil;

public class TSBuildListener implements BuildListener {
   
    private static String dtdDir;
    private static String schemaDir;
    public static boolean vehiclesBuilt;
    private static boolean isBin;
    public static String tsHome;
    public static String jdkHome;
    public static String j2eeHomeRi;
    public static String sSrcDir;
    public static File fSrcDir;
    public static String sClassDir;
    public static File fClassDir;
    public static File fBin;

    private static Path tsClasspath;
    private static boolean skipMakeup;
    private static byte askTimes;
    private static boolean alreadyMadeup; //for vehicles tests

    static {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.indexOf("xp") != -1 && osName.indexOf("windows") != -1) {
            //System.out.println("In Windows XP, reset os.name to Windows 2000 to circumvent Ant bug 7006.");
            System.setProperty("os.name", "Windows 2000");
        }
    }
    public static Path getTsClasspath(Project project) {
        if (tsClasspath != null) {
            return tsClasspath;
        }
        String sTsClasspath = project.getUserProperty("ts.classpath");
        if (sTsClasspath == null || sTsClasspath.length() == 0) {
            //	    project.log("WARNING: could not get ts.classpath from project.",
            //		Project.MSG_WARN);
            sTsClasspath = System.getProperty("ts.classpath");
            if (sTsClasspath == null) {
                project.log("WARNING: could not get ts.classpath from System.", Project.MSG_WARN);
                if (TSBuildListener.j2eeHomeRi == null) {
                    project.log("j2ee.home.ri not set, use ts.home as j2ee.home.ri");
                    //            TSBuildListener.j2eeHomeRi = TSBuildListener.tsHome;
                }
                File[] libJars = new File(TSBuildListener.tsHome, "lib").listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        if (!pathname.isFile()) {
                            return false;
                        }
                        String fname = pathname.getName();
                        return (fname.endsWith(".jar") && !fname.equals("riinterceptors.jar"));
                    }
                });
                if (libJars != null) {
                    sTsClasspath = "";
                    for (int i = 0; i < libJars.length; i++) {
                        sTsClasspath += libJars[i].getPath() + ":";
                    }
                    if (TSBuildListener.j2eeHomeRi != null) {
                        sTsClasspath += TSBuildListener.j2eeHomeRi
                            + File.separator
                            + "lib"
                            + File.separator
                            + "j2ee.jar";
                    }
                }
            }
        } //sTsClasspath is not null after this point
        tsClasspath = new Path(project, sTsClasspath + ":" + TSBuildListener.sClassDir);
        return tsClasspath;
    }

    public static boolean getAlreadyMadeup() {
        return alreadyMadeup;
    }
    public static void setAlreadyMadeup(boolean b) {
        alreadyMadeup = b;
    }
    public static boolean skipMakeupCompile() {
        if (askTimes > 0) {
            return skipMakeup;
        }
        if (isBin) {
            skipMakeup = true;
        } else {
            String sUserDir = System.getProperty("user.dir");
            File userDir = new File(sUserDir);
            if (userDir.compareTo(new File(tsHome, "src")) == 0) {
                skipMakeup = true;
            } else {
                skipMakeup = false;
            }
        }
        askTimes = 1;
        return skipMakeup;
    }

    private void checkAntVersion() {
        String antVersion = org.apache.tools.ant.Main.getAntVersion();
        if (!antVersion.startsWith("Ant version 1.4.1")) {
            throw new BuildException("Expect Ant version 1.4.1, but found " + antVersion);
        }
    }

    public void buildStarted(BuildEvent event) {
        //checkAntVersion();
        Project proj = event.getProject();
        //proj.init();  //may not need it.
        //	String osName = System.getProperty("os.name","").toLowerCase();
        //	if(osName.indexOf("xp") != -1 && osName.indexOf("windows") != -1) {
        //	    System.setProperty("os.name", "Windows 2000");
        //	}
        this.defineUserEnv(proj);
        tsHome = proj.getProperty("env.TS_HOME");


        // XXXX: DEBUG ONLY
        Hashtable table = event.getProject().getProperties();
        proj.log("Dumping ANTS properties from:  TSBuildListener.buildStarted()", Project.MSG_VERBOSE);
        Enumeration keys = table.keys();
        while (keys.hasMoreElements()) {
                String sourceKey = (String)keys.nextElement();
                String sourceVal = (String) table.get(sourceKey);
                proj.log("KEY = " + sourceKey, Project.MSG_VERBOSE);
                proj.log("VAL = " + sourceVal, Project.MSG_VERBOSE);
        }

        if (tsHome == null) {
            /*System.out.println("TS_HOME Environment variable NOT set, lets see if we can get it from ANT_HOME");
            String antHome = proj.getProperty("env.ANT_HOME");
            if ((antHome != null) && ((new File(antHome)).exists())) {
                tsHome = setTsHome(antHome);
            }*/

            // now, check again to see if we were able obtain a value for tshome 
            //if (tsHome == null) {
            throw new BuildException("TS_HOME is not set in the environment or as system property.");
            //}
        }
        System.setProperty("ts.home", tsHome);
        proj.setUserProperty("ts.home", tsHome);
       
	System.out.println("TS_HOME Environment variable set to \"" +
			   tsHome + "\"");
	
        jdkHome = proj.getProperty("env.JAVA_HOME"); 

        if (jdkHome == null) {
            throw new BuildException("JAVA_HOME is not set in the environment or as system property.");
        } 
        
        System.setProperty("jdk.home", jdkHome);
        proj.setUserProperty("jdk.home", jdkHome);

        fBin = new File(tsHome, "bin");

        String sUserDir = System.getProperty("user.dir");
        if ((new File(sUserDir)).compareTo(fBin) == 0
            || (new File(sUserDir, "ts.jte")).exists()
            || (new File(sUserDir, "ts.jtx")).exists()
            || (new File(sUserDir, "version")).exists()) {
            isBin = true;
        }
        //add TS implicit properties
            String vehicleDir = //tsHome + File.separator + "src" + File.separator +
    "com"
        + File.separator
        + "sun"
        + File.separator
        + "ts"
        + File.separator
        + "tests"
        + File.separator
        + "common"
        + File.separator
        + "vehicle";
        proj.setUserProperty("vehicle.dir", vehicleDir);

        sSrcDir = tsHome + File.separator + "src";
        fSrcDir = new File(sSrcDir);
 
        Properties buildProps = new Properties();
        boolean noBuildProps = false; //default to having bin/ts.jte
        FileInputStream ins = null;
        try {
            File pfile = new File(tsHome + File.separator + "bin" + File.separator + "build.properties");
            if(!pfile.exists())
                pfile = new File(tsHome + File.separator + "bin" + File.separator + "ts.jte");
            ins = new FileInputStream(pfile);
            buildProps.load(ins);
            buildProps.setProperty("ts.home", tsHome);
            this.resolveAllProperties(buildProps, proj);
            checkJ2eeHomeRi(buildProps, proj);
            
            System.err.println("build.vi = " + System.getProperty("build.vi", "false"));
            if(System.getProperty("build.vi", "false").equals("true"))
            {
                sClassDir = tsHome + File.separator + "classes_vi_built";
            }
            else
            {
                sClassDir = tsHome + File.separator + "classes";
            }

            fClassDir = new File(sClassDir); 
            proj.setUserProperty("class.dir", sClassDir);
            System.setProperty("class.dir", sClassDir);

            System.err.println("class.dir set to:  " + System.getProperty("class.dir"));
            fClassDir.mkdir();
        
        } catch (IOException ex) {
            noBuildProps = true;
            //    in cygwin or mks a path has mixed pathSeparator
            if (sUserDir != null
                && sUserDir.replace('\\', '/').indexOf((tsHome + "/install").replace('\\', '/')) == -1) {
                throw new BuildException("Could not find top level ts.jte or build.properties");
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException iox) {
                }
            }
        }
        if (!noBuildProps) {
            for (Enumeration enum1 = buildProps.propertyNames(); enum1.hasMoreElements();) {
                String key = (String) enum1.nextElement();
                String val = buildProps.getProperty(key);
                proj.setUserProperty(key, val);
            }
            String sDeliverableClass = proj.getUserProperty("deliverable.class");
            if (sDeliverableClass == null || sDeliverableClass.length() == 0) {
                throw new BuildException("deliverable.class property not set in ts.jte or build.properties.");
            }
            String sTsClasspath = proj.getUserProperty("ts.classpath");
            if (sTsClasspath != null && sTsClasspath.length() != 0) {
                System.setProperty("ts.classpath", sTsClasspath);
            }
            System.setProperty("deliverable.class", sDeliverableClass);
            String endorsedDir = proj.getUserProperty("java.endorsed.dirs");
            if (endorsedDir == null)
                endorsedDir = proj.getProperty("java.endorsed.dirs");
            if (endorsedDir != null)
                System.setProperty("java.endorsed.dirs", endorsedDir);
        } //if(!noBuildProps)

        try {
            proj.addDataTypeDefinition("support", Class.forName("com.sun.ant.types.Support"));
            //	    proj.addTaskDefinition("checkprops", Class.forName("com.sun.ant.taskdefs.common.CheckProps"));
            proj.addTaskDefinition("translatepath", Class.forName("com.sun.ant.taskdefs.common.PathTranslator"));
            proj.addTaskDefinition("elementappend", Class.forName("com.sun.ant.taskdefs.common.ElementAppend"));
            proj.addTaskDefinition("getclient", Class.forName("com.sun.ant.taskdefs.common.GetClient"));
            proj.addTaskDefinition("dosubdirs", Class.forName("com.sun.ant.taskdefs.common.DoSubdirs"));
            proj.addTaskDefinition("dotarget", Class.forName("com.sun.ant.taskdefs.common.DoTarget"));
            proj.addTaskDefinition("webwar", Class.forName("com.sun.ant.taskdefs.web.WebWar"));
            proj.addTaskDefinition("clientjar", Class.forName("com.sun.ant.taskdefs.client.ClientJar"));
            proj.addTaskDefinition("appear", Class.forName("com.sun.ant.taskdefs.app.J2eeEar"));
            proj.addTaskDefinition("resrar", Class.forName("com.sun.ant.taskdefs.connector.ConnectorRar"));
            proj.addTaskDefinition("ejb-jar", Class.forName("com.sun.ant.taskdefs.ejb.EJBJar"));
            proj.addTaskDefinition("vehicles", Class.forName("com.sun.ant.taskdefs.common.TSVehicles"));
            proj.addTaskDefinition("package", Class.forName("com.sun.ant.taskdefs.common.ContainerPackage"));
            proj.addTaskDefinition("ant", Class.forName("org.apache.tools.ant.taskdefs.Ant"));
            proj.addTaskDefinition("property", Class.forName("org.apache.tools.ant.taskdefs.Property"));
            proj.addTaskDefinition("appendafter", Class.forName("com.sun.ant.taskdefs.common.AppendAfter"));
            proj.addTaskDefinition("insertbefore", Class.forName("com.sun.ant.taskdefs.common.InsertBefore"));
            proj.addTaskDefinition("encode", Class.forName("com.sun.ant.taskdefs.common.Encoder"));
            proj.addTaskDefinition("propertyindex", Class.forName("com.sun.ant.taskdefs.common.PropertyIndex"));


            if (TaskUtil.isCompatDir(System.getProperty("user.dir"))) {
                proj.addTaskDefinition("compathelper", Class.forName("com.sun.ant.taskdefs.common.CompatHelper"));
            }
            //	    addTaskDefinition(proj, tsHome, new String[][]{
            //		{"vehicles", "com.sun.ant.taskdefs.common.TSVehicles"},
            //		{"package", "com.sun.ant.taskdefs.common.ContainerPackage"}
            //		});
        } catch (ClassNotFoundException nodef) {
            nodef.printStackTrace();
        } catch (NoClassDefFoundError ncdfe) {
            ncdfe.printStackTrace();
        }
	addImplicitTargets(proj, tsHome);
        //set dist.dir to ts.home/dist to override any dist.dir value
        proj.setUserProperty("dist.dir", tsHome + File.separator + "dist");
        System.setProperty("dist.dir", tsHome + File.separator + "dist");
    }

    /*
     * Given a valid ANT_HOME path (which resides in the TS_HOME sub dir structrue)
     * this method, will return the TS_HOME value relative to the ANT_HOME dir.
     * This is to be used when TS_HOME is NOT supplied but ANT_HOME is.
     */
    /*private String setTsHome(String antHome) {


        // for tshome, start with ANT_HOME and work our way up
        String tshome = antHome;
        if ((tshome.charAt(tshome.length()-1) == File.separatorChar)) {
            // remove any trailing File separator (ie /tmp/cts/ ==> /tmp/cts )
            tshome = tshome.substring(0, tshome.length()-1);
        }

        // now climb up two directories - of course, this assumes 
        // that ANT_HOME lives in TS_HOME/tools/ant
        for (int ii=0 ; ii < 2; ii++) {
            if (tshome.lastIndexOf(File.separatorChar) > 0) {
                tshome = tshome.substring(0, tshome.lastIndexOf(File.separatorChar));
            } else {
                System.out.println("Found no path separators in anthome so cant set tshome");
                tshome = null;
            }
        }

        return tshome;
    }*/

    private void defineUserEnv(Project proj) {
        Property propertyTask = new Property();
        propertyTask.setProject(proj);
        propertyTask.init();
        propertyTask.setTaskName("listener");
        propertyTask.setEnvironment("env");
        propertyTask.execute();
    }

    private void checkJ2eeHomeRi(Properties props, Project proj) {
        String jri = props.getProperty("j2ee.home.ri");
        if (jri != null && jri.length() != 0) {
            j2eeHomeRi = jri;
            return;
        }
        jri = proj.getProperty("env.J2EE_HOME_RI");
        if (jri != null) {
            props.setProperty("j2ee.home.ri", jri);
            j2eeHomeRi = jri;
        } else {
            String tmp = props.getProperty("build.level");
            if (tmp == null || !tmp.equals("1")) {
                proj.log(
                    "j2ee.home.ri not set in ts.jte  or build.properties file; J2EE_HOME_RI not set in environment.",
                    Project.MSG_VERBOSE);
            }
        }
    }

    //since we add j2eects.jar to classpath in tsant script,
    //this is the same as proj.addTaskDefinition.
    private void addTaskDefinition(Project proj, String tsHome, String[][] toAdd) {
        //cannot call project.createTask(), since this task is not in yet
        if (toAdd == null) {
            return;
        }
        Path path = getTsClasspath(proj);
        for (int i = 0; i < toAdd.length; i++) {
            String taskType = toAdd[i][0];
            String className = toAdd[i][1];
            Taskdef taskdef = new Taskdef();
            taskdef.setProject(proj);
            taskdef.init();
            taskdef.setName(taskType);
            taskdef.setClassname(className);
            taskdef.setClasspath(path);
            //not to call perform() since we do not want to fire any build events
            taskdef.execute();
            proj.log("Added" + taskType + " " + className + " " + path.toString());
        }
    }

    private void printAllProps(Project proj) {
        Hashtable ht = proj.getProperties();
        proj.log("The following are all " + ht.size() + " properties:", Project.MSG_VERBOSE);
        for (Iterator it = ht.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String val = (String) ht.get(key);
            proj.log(key + "=" + val, Project.MSG_VERBOSE);
        }

        ht = proj.getUserProperties();
        proj.log("The following are all " + ht.size() + " user properties:", Project.MSG_VERBOSE);
        for (Iterator it = ht.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            String val = (String) ht.get(key);
            proj.log(key + "=" + val, Project.MSG_VERBOSE);
        }
    }

    private void addImplicitTargets(Project proj, String tsHome) {
        String guiName = "gui";
        File baseDir = proj.getBaseDir();
        if (!isBin) {
            //user.dir should be the same as baseDir.  To be safe, use user.dir
            String sUserDir = System.getProperty("user.dir");
            //add pkg.dir property to project.  It is used by common targets
            //such as clean, compile, runclient.  It has to be set in order
            //to run these targets from non-leaf dir, where there is no build
            //file.
            String pkgDir = TaskUtil.path2PkgDir(sUserDir);
            if (pkgDir != null) {
                proj.setProperty("pkg.dir", pkgDir);
            }
//             //if build.xml not present, will use bin/build.xml, which already
//             //defined gui target.
//             if ((new File(sUserDir, "build.xml")).exists()) {
//                 Ant jt = new Ant();
//                 Target rt = new Target();
//                 rt.setName(guiName);
//                 proj.addTarget(rt);
//                 rt.addTask(jt);
//                 rt.setProject(proj);
//                 jt.setProject(proj);
//                 jt.init();
//                 jt.setDir(fBin);
//                 jt.setAntfile("build.xml");
//                 jt.setTarget(guiName);

//                 String verifyName = "verify";
//                 Ant antV = new Ant();
//                 Target verify = new Target();
//                 verify.setName(verifyName);
//                 proj.addTarget(verify);
//                 verify.addTask(antV);
//                 verify.setProject(proj);
//                 antV.setProject(proj);
//                 antV.init();
//                 antV.setDir(fBin);
//                 antV.setAntfile("build.xml");
//                 antV.setTarget(verifyName);
// 	    }
        }
	
    }
    
    public void buildFinished(BuildEvent event) {
    }
    public void targetStarted(BuildEvent event) {
    }
    public void targetFinished(BuildEvent event) {
    }
    public void taskStarted(BuildEvent event) {
    }
    public void taskFinished(BuildEvent event) {
    }
    public void messageLogged(BuildEvent event) {
    }

    //The following method is copied from ant source code.
    //org.apache.tools.ant.taskdefs.Property, with minor modification
    private void resolveAllProperties(Properties props, Project proj) throws BuildException {
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = props.getProperty(name);

            boolean resolved = false;
            while (!resolved) {
                Vector fragments = new Vector();
                Vector propertyRefs = new Vector();
                ProjectHelper.parsePropertyString(value, fragments, propertyRefs);

                resolved = true;
                if (propertyRefs.size() != 0) {
                    StringBuffer sb = new StringBuffer();
                    Enumeration i = fragments.elements();
                    Enumeration j = propertyRefs.elements();
                    while (i.hasMoreElements()) {
                        String fragment = (String) i.nextElement();
                        if (fragment == null) {
                            String propertyName = (String) j.nextElement();
                            if (propertyName.equals(name)) {
                                throw new BuildException("Property " + name + " was circularly defined.");
                            }
                            fragment = proj.getProperty(propertyName);
                            if (fragment == null) {
                                if (props.containsKey(propertyName)) {
                                    fragment = props.getProperty(propertyName);
                                    resolved = false;
                                } else {
                                    fragment = "${" + propertyName + "}";
                                }
                            }
                        }
                        sb.append(fragment);
                    }
                    value = sb.toString();
                    props.put(name, value);
                }
            }
        }
    }

    /**
     * Gets dtd directory.  If j2ee.home.ri is set, returns j2ee.home.ri/lib/dtds.
     * Else if alt.dtd.dir is set, return it.  If neither is set, default to TS_HOME/lib/dtds.
     * @return String
     */
    public static String getDtdDir(Project project) {
        if (TSBuildListener.dtdDir == null) {
            if (TSBuildListener.j2eeHomeRi != null) {
                TSBuildListener.dtdDir = TSBuildListener.j2eeHomeRi + File.separator
                + "lib" + File.separator + "dtds";
            } else {
                String altDtdDir = project.getProperty("alt.dtd.dir");
                if ((altDtdDir == null) || (altDtdDir.length() == 0)) {
                   altDtdDir = tsHome + File.separator + "lib" + File.separator + "dtds";
                }
                TSBuildListener.dtdDir = altDtdDir;
            }
            project.log("Searching dtds in: " + TSBuildListener.dtdDir);
        }
        return TSBuildListener.dtdDir;
    }

    /**
     * Gets schema directory.  If j2ee.home.ri is set, returns j2ee.home.ri/lib/schemas.
     * Else if alt.schema.dir is set, return it.  If neither is set, default to TS_HOME/lib/schemas.
     * @return String
     */
    public static String getSchemaDir(Project project) {
        if (TSBuildListener.schemaDir == null) {
            if (TSBuildListener.j2eeHomeRi != null) {
                TSBuildListener.schemaDir = TSBuildListener.j2eeHomeRi + File.separator
                + "lib" + File.separator + "schemas";
            } else {
                String altschemaDir = project.getProperty("alt.schema.dir");
                if ((altschemaDir == null) || (altschemaDir.length() == 0)) {
                    altschemaDir = tsHome + File.separator + "lib" + File.separator + "schemas";
                }
                TSBuildListener.schemaDir = altschemaDir;
            }
            project.log("Searching schemas in: " + TSBuildListener.schemaDir);
        }
        return TSBuildListener.schemaDir;
    }
} //end of class
