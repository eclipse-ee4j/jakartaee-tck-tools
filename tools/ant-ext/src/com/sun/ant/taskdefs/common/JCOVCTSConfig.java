/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.io.*;
import java.util.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import org.jdom.xpath.*;

/**
 * This ant task configures CTS to run against an instrumented version
 * of SJSAS for the purpose of collecting coverage numbers.  This task
 * uses the configuration information contained in the
 * $TS_HOME/internal/coverage/jcov.properties file to edit the 
 * $TS_HOME/bin/ts.jte and $TS_HOME/bin/build.xml files.  The gist of
 * changes are JCOV files and JCOV ports are added to the appclient,
 * standalone and run cts (batch mode) VM invocations.  The task also
 * adds the jcov.jar to the appropriate paths in the ts.jte file.  Note,
 * this task assumes the jcov.jar lives in the $J2EE_HOME/lib directory.
 *
 * Add this to $TS_HOME/bin/xml/s1as.xml
 *   <taskdef name="configctsjcov" classname="com.sun.ant.taskdefs.common.JCOVCTSConfig"/>
 *
 * Then add a target that executes the config.cts.jcov task.
 *
 *   <target name="config.cts.jcov">
 *     <configctsjcov ts.home="${ts.home}" j2ee.home="${j2ee.home}"/>
 *   </target>
 *
 * Invoke the task using "$TS_HOME/bin/tsant config.cts.jcov".
 *
 */
public class JCOVCTSConfig extends Task {

    public static class JCOVInfo {
	private String file;
	private String port;
	public JCOVInfo(String info) throws BuildException {
	    String[] a = info.split(":");
	    if (a.length == 1) {
		port = a[0];
		file = "";    
	    } else if (a.length == 2) {
		file = a[0];
		port = a[1];	    	    
	    } else {
		throw new BuildException("Badly formed JCOV data \"" + info + "\"");
	    }
	}
	public String getPort() { return port; }
	public String getFile() { return file; }
    }
    
    private JCOVInfo standAlone;
    private JCOVInfo appClient;
    private JCOVInfo appClient2;
    private JCOVInfo deploy;
    private File tshome;
    private File j2eehome;
    private File jcovPropsFile;
    private File tsPropsFile;
    private File tsPropsFileOut;
    private File jcovJar;
    private File buildFile;
    private File buildFileOut;
    private Properties jcovProps = new Properties();
    private Properties tsJTEProps = new Properties();

    public void setTshome(File tshome) {
	this.tshome = tshome;
    }

    public void setJ2eehome(File j2eehome) {
	this.j2eehome = j2eehome;
    }

    public void execute() throws BuildException {
	checkPreConditions();
	setProps();
	getJcovConfigData();
	editTsJte();
	editBuildXml();
    }

    private void checkPreConditions() throws BuildException {
	if (tshome == null || !tshome.isDirectory()) {
	    throw new BuildException("TS_HOME is not set or is not an existing directory \""
				     + tshome + "\"");
	}
	if (j2eehome == null || !j2eehome.isDirectory()) {
	    throw new BuildException("J2EE_HOME is not set or is not an existing directory \""
				     + j2eehome + "\"");
	}
    }

    private void setProps() throws BuildException {
	FileInputStream jcovPropsStream = null;
	FileInputStream tsPropsStream = null;
	try {
	    jcovJar = new File(j2eehome, "lib" + File.separator + "jcov.jar");
	    if (!jcovJar.isFile()) {
		throw new BuildException("JCOV jar does not exist at \"" + jcovJar.getPath() + "\"");
	    }
	    jcovPropsFile = new File(tshome, "internal" + File.separator +
				     "coverage" + File.separator + "jcov.properties");
	    if (!jcovPropsFile.isFile()) {
		throw new BuildException("JCOV properties file does not exist at \""
					 + jcovPropsFile.getPath() + "\"");
	    }
	    tsPropsFileOut = new File(tshome, "bin" + File.separator + "ts.jte");
	    tsPropsFile = new File(tshome, "bin" + File.separator + "ts.jte.original");
	    if (!tsPropsFile.isFile()) {
		throw new BuildException("TS JTEproperties file does not exist at \""
					 + tsPropsFile.getPath() + "\"");
	    }
	    buildFileOut = new File(tshome, "bin" + File.separator + "build.xml");
	    buildFile = new File(tshome, "bin" + File.separator + "build.xml.original");
	    if (!buildFile.isFile()) {
		throw new BuildException("Build file does not exist at \""
					 + buildFile.getPath() + "\"");
	    }
	    jcovPropsStream = new FileInputStream(jcovPropsFile);
	    jcovProps.load(jcovPropsStream);
	    tsPropsStream = new FileInputStream(tsPropsFile);
	    tsJTEProps.load(tsPropsStream);
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { jcovPropsStream.close(); tsPropsStream.close(); } catch(Throwable t) {} // do nothing
	}
    }

    private void parseCoverageData(Properties p) throws BuildException {
	standAlone = new JCOVInfo(p.getProperty("standalone"));
	appClient  = new JCOVInfo(p.getProperty("appclient"));
	appClient2 = new JCOVInfo(p.getProperty("appclient2"));
	deploy     = new JCOVInfo(p.getProperty("deploy"));
    }

    private void getJcovConfigData() throws BuildException {
	FileInputStream in = null;
	try {
	    in = new FileInputStream(jcovPropsFile);
	    Properties props = new Properties();
	    props.load(in);
	    parseCoverageData(props);
	    log("Read JCOV configuration data from file \"" + jcovPropsFile + "\"", project.MSG_INFO);	    
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { in.close(); } catch (Throwable t) {} // do nothing
	}
    }

    private void editTsJte() throws BuildException {
	updatePathProp("javaee.classes.ri");
	updatePathProp("javaee.classes");
	updatePathProp("ts.run.classpath.ri");
	updatePathProp("ts.run.classpath");
	updatePathProp("ts.harness.classpath");
	editClientVM("command.testExecute");
	editClientVM("command.testExecuteAppClient");
	editClientVM("command.testExecuteAppClient2");
	writeTsJte();
    }

    private void updatePathProp(String pathProp) throws BuildException {
	String path = tsJTEProps.getProperty(pathProp);
	if (path == null) {
	    throw new BuildException("The ts.jte property \"" + pathProp + "\" does not exist.");
	}
	int index = path.indexOf("jcov.jar");
	if (index == -1) {
	    tsJTEProps.setProperty(pathProp, path + File.pathSeparator + jcovJar.getPath());
	    log("Added \"" +  jcovJar.getPath() + "\" to path \"" + pathProp + "\" in ts.jte", project.MSG_VERBOSE);
	}
    }

    private void editClientVM(String vm) throws BuildException {
	String vmCommand = tsJTEProps.getProperty(vm);
	if (vmCommand == null) {
	    throw new BuildException("The ts.jte property \"" + vm + "\" does not exist.");
	}
	int index = vmCommand.indexOf("jcov");
	if (index == -1) {
	    StringBuffer buf = new StringBuffer(vmCommand);
	    int index2 = buf.indexOf("-D");
	    String jcovInfo = "-Djcov.file=" + getFile(vm) + " -Djcov.server_port=" + getPort(vm);
	    buf.insert(index2, jcovInfo + " ");
	    tsJTEProps.setProperty(vm, buf.toString());
	    log("Added \"" +  jcovInfo + "\" to VM command \"" + vm + "\" in ts.jte", project.MSG_VERBOSE);
	}
    }

    private String getFile(String vm) {
	String result = "";
	if (vm.equals("command.testExecute")) {
	    result = standAlone.getFile();
	} else if (vm.equals("command.testExecuteAppClient")) {
	    result = appClient.getFile();
	} else if (vm.equals("command.testExecuteAppClient2")) {
	    result = appClient2.getFile();
	}
	return result;
    }

    private String getPort(String vm) {
	String result = "";
	if (vm.equals("command.testExecute")) {
	    result = standAlone.getPort();
	} else if (vm.equals("command.testExecuteAppClient")) {
	    result = appClient.getPort();
	} else if (vm.equals("command.testExecuteAppClient2")) {
	    result = appClient2.getPort();
	}
	return result;
    }

    private void writeTsJte() throws BuildException {
	FileOutputStream out = null;
	try {
	    tsPropsFileOut.delete(); // original backed up in ant target
	    out = new FileOutputStream(tsPropsFileOut);
	    tsJTEProps.store(out, "JCOV Configged ts.jte File");
	    log("Wrote JCOVed file \"" + tsPropsFileOut + "\"", project.MSG_INFO);	    
	} catch(Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { out.close(); } catch (Throwable t) {} // do nothing
	}
    }

    private boolean jcovved(Element el) {
	boolean result = false;
	try {
	    XPath xpath = XPath.newInstance("sysproperty[@key='jcov.file']");
	    List javaElement = xpath.selectNodes(el);
	    result = javaElement.size() > 0;
	} catch(Exception e) {
	}
	return result;
    }

    private void editBuildXml() throws BuildException {
	List javaElement = null;
	Document doc = null;
	try {
	    SAXBuilder builder = new SAXBuilder(false);
	    doc = builder.build(buildFile);
	    XPath xpath = XPath.newInstance("//target[@name='runclient']/java");
	    javaElement = xpath.selectNodes(doc);
	} catch(Exception e) {
	    throw new BuildException("Error parsing build file.");
	}
	if (javaElement.size() != 1) {
	    throw new BuildException("Could not find runclient/java element");
	}
	Element javaEl = (Element)javaElement.get(0);
	if (!jcovved(javaEl)) {	
	    javaEl.addContent(new Element("sysproperty").setAttribute("key", "jcov.file").
		setAttribute("value", deploy.getFile()));
	    javaEl.addContent(new Element("sysproperty").setAttribute("key", "jcov.server_port").
		setAttribute("value", deploy.getPort()));
	    log("Added jcov.file \"" + deploy.getFile() + "\" jcov.server_port \"" + deploy.getPort() +
		"\" to runclient target in build.xml", project.MSG_VERBOSE);
	    writeBuildXml(doc);
	}
    }

    private void writeBuildXml(Document doc) throws BuildException {
	FileWriter out = null;
	try {
	    buildFileOut.delete();
	    out = new FileWriter(buildFileOut);
	    XMLOutputter writer = new XMLOutputter(Format.getPrettyFormat());
	    writer.output(doc, out);
	    log("Wrote JCOVed file \"" + buildFileOut + "\"", project.MSG_INFO);	    
	} catch(Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { out.close(); } catch(Throwable t) {} // do nothing
	}
    }

}
