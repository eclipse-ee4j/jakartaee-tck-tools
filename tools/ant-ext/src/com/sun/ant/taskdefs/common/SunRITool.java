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

package com.sun.ant.taskdefs.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Replace;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

import com.sun.ant.TSBuildListener;
import com.sun.ant.TSLogger;

import com.sun.ts.lib.harness.VehicleVerifier;

/**
 * A deployment tool which creates generic EJB jars. Generic jars contains
 * only those classes and META-INF entries specified in the EJB 1.1 standard
 *
 * This class is also used as a framework for the creation of vendor specific
 * deployment tools. A number of template methods are provided through which the
 * vendor specific tool can hook into the EJB creation process.
 */
public class SunRITool extends GenericDeploymentTool {
    //they are made non-final to be set to dtd of earlier versoin by compat tests
    //names may be misleading
    public static String PUBLICID_EJB20 = "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 2.0//EN";
    public static String PUBLICID_APP_CLIENT = "-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN";
    public static String PUBLICID_WEB = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    protected static String DEFAULT_SUNRI13_EJB20_DTD_LOCATION = "ejb-jar_2_0.dtd";
    protected static String DEFAULT_SUNRI13_APP_CLIENT_DTD_LOCATION = "application-client_1_3.dtd";
    protected static String DEFAULT_SUNRI13_WEB_DTD_LOCATION = "web-app_2_3.dtd";
    protected String descriptorXmlFileName = "";
    protected String descriptorRuntimeXmlFileName = "";
    
    //Holds the last classes packaged by the EJBTool.  The Appclient tool uses this when packaging
    //the appclient ejb vehicle jar
    protected static String sLastFilesAdded = "";

    public boolean invokePackager(File jarFile, String args) {
        return invokePackager(jarFile, args, true);
    }

    /**
     * @return true if successful, otherwise false
     * @param jarFile the archive file to create
     * @param args the complete application args that should be passed to packager tool
     * @param postWrite whether to invoke postWrite method
     */
    public boolean invokePackager(File jarFile, String args, boolean postWrite) {
        boolean retval = false;
        String systemClassPath = System.getProperty("java.class.path");
        String execClassPath = project.translatePath(systemClassPath + ":" + classpath);
        Java packager = (Java) project.createTask("java");
	// Set VM debug settings for packager
	//	packager.setJvmargs("-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=12345");
        packager.setTaskName("packager");
        packager.setFork(true);
        packager.setFailonerror(false);
        packager.setClassname("com.sun.enterprise.tools.packager.Main");
        Commandline.Argument arguments = packager.createArg();
        arguments.setLine(args);
        Environment.Variable var = null;
        String endorsedDir = project.getProperty("ri.java.endorsed.dirs");
        String use23 = project.getProperty("use.web.23");
        if (use23 != null) { 
           Environment.Variable eVar = new Environment.Variable();
           eVar.setKey("use.web.23");
           eVar.setValue("true"); 
           packager.addSysproperty(eVar);
        }
        if (endorsedDir == null)
            endorsedDir = project.getProperty("java.endorsed.dirs");
        if (endorsedDir == null)
            endorsedDir = System.getProperty("java.endorsed.dirs");
        if (endorsedDir != null) {
            log("passing java.endorsed.dirs to packager: " + endorsedDir, Project.MSG_VERBOSE);
            var = new Environment.Variable();
            var.setKey("java.endorsed.dirs");
            var.setValue(endorsedDir);
            packager.addSysproperty(var);
        }
        if (TSBuildListener.j2eeHomeRi != null) {
            Environment.Variable var2 = new Environment.Variable();
            var2.setKey("com.sun.enterprise.home");
            var2.setValue(TSBuildListener.j2eeHomeRi);
            packager.addSysproperty(var2);
        } else {
            if (TSBuildListener.getDtdDir(project) != null) {
                Environment.Variable var3 = new Environment.Variable();
                var3.setKey("alt.dtd.dir");
                var3.setValue(TSBuildListener.getDtdDir(project));
                packager.addSysproperty(var3);
            }
            if (TSBuildListener.getSchemaDir(project) != null) {
                Environment.Variable var4 = new Environment.Variable();
                var4.setKey("alt.schema.dir");
                var4.setValue(TSBuildListener.getSchemaDir(project));
                packager.addSysproperty(var4);
            }
        }

        Environment.Variable var5 = new Environment.Variable();
	var5.setKey("sun.application.prefix");
	var5.setValue(normalizeFile(jarFile));
	packager.addSysproperty(var5);

        packager.setClasspath(new Path(project, execClassPath));
        //	Commandline.Argument jvmarg1 = packager.createJvmarg();
        //	jvmarg1.setValue("-verbose");
        //	Commandline.Argument jvmarg2 = packager.createJvmarg();
        //	jvmarg2.setValue("-Xms32m");
        packager.setMaxmemory("96m");
        if (packager.executeJava() != 0) {
            TSLogger.addFailedDir(jarFile.getParent());
        } else if (postWrite) {
            postWrite(jarFile);
            retval = true;
        } else {
            retval = true;
        }
        return retval;
    }

    private String normalizeFile (File jarFile) {
	String result = jarFile.getPath();
	final int DIST_LENGTH = 4;
	if (!isVehicleTest(jarFile)) {
	    int index = result.lastIndexOf("dist");
	    String baseDir = result.substring(0, index); // includes last /
	    String extDir = result.substring(index + DIST_LENGTH);
	    result = baseDir + "src" + extDir;
	}
	//System.err.println(" ***** normalizeFile() returning " + result);
	return result;
    }

    private boolean isVehicleTest (File file) {
        boolean containsFrom = file.getName().indexOf("_vehicle.ear") != -1;
	String[] vehicles;
        VehicleVerifier vehicleVerifier = VehicleVerifier.getInstance(file);
        vehicles = vehicleVerifier.getVehicleSet();
	boolean result = (vehicles != null && vehicles.length > 0) && containsFrom;
        //System.err.println(" ***** isVehicleTest() " + result);
        return result;
    }

    //to do anything after writeWar, writeJar, writeEar ...
    public void postWrite(File archive) {
        Manifest manifest = null;
        if (config.manifest != null) {
            if (!config.manifest.exists()) {
                throw new BuildException("manifest file does not exist:" + config.manifest.getPath());
            }
            FileInputStream in = null;
            try {
                in = new FileInputStream(config.manifest);
                manifest = new Manifest(in);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BuildException(e);
            } finally {
                try { in.close(); } catch (Exception ee) {}
            }
         } else {
            manifest = new Manifest();
	    Attributes mainAttrs = manifest.getMainAttributes();
	    mainAttrs.putValue("Manifest-Version", "1.0");
         }   
         File newManifest = addAttributes(manifest, archive);
         addManifest(archive, newManifest);
	 boolean result = newManifest.delete();
	 if (result) {
	     //System.out.println("$$ Removed Manifest \"" + newManifest + "\"");
	 }
    }

    private File addAttributes(Manifest manifest, File archiveFile)
    {
	String archive = archiveFile.getName();
	String archivePath = archiveFile.getPath();
	//	System.err.println(" **** ARCHIVE " + archivePath);
	// We don't want to process any compat tests
	int indexCompat = archivePath.indexOf("tests" + File.separator + "compat1");
        int index = archive.indexOf("_component");
        if ( (archive.endsWith(".ear") || archive.endsWith(".rar") || (index != -1)) &&
	     (indexCompat == -1)) 
        {
          Attributes mainAttrs = manifest.getMainAttributes();
          String extListValue = mainAttrs.getValue(Attributes.Name.EXTENSION_LIST);
          String extList = "cts tsharness";
          if (extListValue != null) {
            extList = addExtList(extListValue);
          }
	  //          mainAttrs.putValue("Manifest-Version", "1.0");
          mainAttrs.putValue(Attributes.Name.EXTENSION_LIST.toString(), extList);
          mainAttrs.putValue("cts-" + Attributes.Name.EXTENSION_NAME.toString(), "cts");
          mainAttrs.putValue("cts-" + Attributes.Name.SPECIFICATION_VERSION.toString(), "1.4");
          mainAttrs.putValue("cts-" + Attributes.Name.IMPLEMENTATION_VERSION.toString(), "1.4");
          mainAttrs.putValue("cts-" + Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString(), "com.sun");
          mainAttrs.putValue("tsharness-" + Attributes.Name.EXTENSION_NAME.toString(), "tsharness");
          mainAttrs.putValue("tsharness-" + Attributes.Name.SPECIFICATION_VERSION.toString(), "1.4");
          mainAttrs.putValue("tsharness-" + Attributes.Name.IMPLEMENTATION_VERSION.toString(), "1.4");
          mainAttrs.putValue("tsharness-" + Attributes.Name.IMPLEMENTATION_VENDOR_ID.toString(), "com.sun");
          /* DEBUG CODE 
          Iterator keys = mainAttrs.keySet().iterator(); 
          while(keys.hasNext())
          {
              Attributes.Name  next = (Attributes.Name)keys.next();
              System.err.print("[" + next.toString() + ", ");
              System.err.println(mainAttrs.getValue(next) + "]");
          } 
          */
        }
        File outFile = null;
        FileOutputStream out = null;
        try {
            outFile = File.createTempFile("ts-manifest", ".mf");
            out = new FileOutputStream(outFile);
            manifest.write(out);
        } catch (IOException e) {
            e.printStackTrace();
            throw new BuildException(e);
        } finally {
             try { out.close(); } catch (Exception ee) {}
        }
        return outFile;
    }
  
    private String addExtList (String list) {
        StringBuffer buf = new StringBuffer();
        StringTokenizer tokens = new StringTokenizer(list);
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (!(token.equalsIgnoreCase("cts") || token.equalsIgnoreCase("tsharness")))
            {
                buf.append(token + " ");
            }
        }
        buf.append("cts tsharness");
        return buf.toString();
    }	

    private void addManifest(File archive, File manifest)
      {
        //task.log("Will use the manifest file:" + manifest.getPath());
        ExecTask jar = new ExecTask();
        jar.setProject(project);
        jar.init();
        jar.setTaskName("jar");
        jar.setDir(manifest.getParentFile());
        jar.setExecutable("jar");
        jar.setFailonerror(true);
        Commandline.Argument args = jar.createArg();
        args.setLine("umf " + manifest.getName() + " " + archive.getPath());
        jar.perform();
    }   

    /** flag to tell whether need to replace display-name and jndi-name for xml files
     in service tests **/
    private boolean replace;

    public void setReplace(boolean b) {
        this.replace = b;
    }

    public boolean getReplace() {
        return this.replace;
    }

    //ddd is descriptor dest dir
    public void replace4ServiceTest(String ddd) {
        String name = config.name;
        File dirToSet = new File(ddd);
        //avoid using heavy weight collection like map.
        if (name.indexOf("ejb_vehicle") != -1) {
            String[][] oldNew =
                { { "jndi-name>com_sun_ts_tests_common_vehicle_ejb_EJBVehicle", "jndi-name>" + name }, {
                    "display-name>ejb_vehicle_client", "display-name>" + name + "_client" }
            };
            doReplace(oldNew, name, dirToSet);
        } else if (name.indexOf("jsp_vehicle") != -1) {
            String[][] oldNew = { { "context-root>jsp_vehicle", "context-root>" + name }, {
                    "display-name>jsp_vehicle", "display-name>" + name }
            };
            doReplace(oldNew, name, dirToSet);
        } else if (name.indexOf("servlet_vehicle") != -1) {
            String[][] oldNew = { { "context-root>servlet_vehicle", "context-root>" + name }, {
                    "display-name>servlet_vehicle", "display-name>" + name }
            };
            doReplace(oldNew, name, dirToSet);
        } else if (name.indexOf("appclient_vehicle") != -1) {
            String[][] oldNew = { { "display-name>appclient_vehicle_client", "display-name>" + name + "_client" }
            };
            doReplace(oldNew, name, dirToSet);
        }
    }

    private void doReplace(String[][] oldNew, String name, File dirToSet) {
        //	Project projectToSet = task.getProject();
        //cannot reuse the same replace instance.  setToken will append
        //to existing token string.
        for (int i = 0; i < oldNew.length; i++) {
            Replace replacer = new Replace();
            replacer.setProject(project);
            replacer.setDir(dirToSet);
            replacer.setDefaultexcludes(true);
            replacer.setIncludes(name + "*.xml");
            replacer.setExcludes("*.java, build.xml, *.jsp, *props*, Makefile");
            replacer.setSummary(true);
            replacer.setToken(oldNew[i][0]);
            replacer.setValue(oldNew[i][1]);
            replacer.perform();
        }
    }

    /*protected void convertProps2Xml(String propsFile) throws BuildException {
        SunRIDDCreator ddCreator = new SunRIDDCreator();
        String ddd = config.descriptorDir.getPath();
        ddCreator.setDescriptors(ddd);
        boolean resetDescriptorDir = false;
        //ddCreator.setDest(config.descriptorDir.getPath());
        //The above seDest does not work for service tests that have descriptor in vehicle dirs
        //In this case, use base.dir instead.
        if (ddd.indexOf(File.separator + "vehicle" + File.separator) == -1) {
            ddCreator.setDest(ddd);
            task.log("SunRITool:xml will be written to:" + ddd, Project.MSG_VERBOSE);
        } else {
            ddd = TaskUtil.getCurrentSrcDir(project).getPath();
            ddCreator.setDest(ddd);
            resetDescriptorDir = true;
            task.log("SunRITool:change dd output dir:xml will be written to:" + ddd, Project.MSG_VERBOSE);
            task.log(
                "SunRITool:after the xml files are created, descriptorDir will be reset to " + ddd,
                Project.MSG_VERBOSE);
        }
        ddCreator.setName(config.name);
        ddCreator.setClasspath(config.classpath.toString());
        ddCreator.setProject(project);
        ddCreator.setPropsFile(propsFile);
        ddCreator.execute();
        //reset the descriptor dir to the dest dir (generatedFileDirectory) after xml files
        //are created.
        if (resetDescriptorDir) {
            config.descriptorDir = new File(ddd);
        }
        //get xml filename so that we can pass it to the sunrideploytool
        if (replace) {
            replace4ServiceTest(ddd);
        }
    }*/

    protected void registerKnownDTDs(DescriptorHandler handler) {
        handler.registerDTD(
            PUBLICID_EJB20,
            TSBuildListener.getDtdDir(getTask().getProject()) 
            + File.separator+ DEFAULT_SUNRI13_EJB20_DTD_LOCATION);
        handler.registerDTD(
            PUBLICID_APP_CLIENT,
            TSBuildListener.getDtdDir(getTask().getProject()) 
            + File.separator + DEFAULT_SUNRI13_APP_CLIENT_DTD_LOCATION);
        handler.registerDTD(
            PUBLICID_WEB,
            TSBuildListener.getDtdDir(getTask().getProject())
            + File.separator + DEFAULT_SUNRI13_WEB_DTD_LOCATION);
    }
}
