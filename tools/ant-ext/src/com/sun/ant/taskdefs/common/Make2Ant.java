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
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.taskdefs.*;

import com.sun.ant.taskdefs.web.*;
import com.sun.ant.taskdefs.ejb.*;
import com.sun.ant.taskdefs.common.*;
import com.sun.ant.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/** @todo
 *  1, separate doLeaf to an inner class so that there is no left-over
 *  values and interference between subdirs
 *  2, add a SubdirVisitor interface to be implemented by all tasks
 *  that need to traverse directory tree.
 *
 */
public class Make2Ant extends Task {
    final public static String FS = File.separator;
    final public static String NL = System.getProperty("line.separator");
    final public static String SIX = "      ";
    final public static String EIGHT = "        ";
    private boolean makeFilePresent = true;
    private String emptyContentDir = "${ts.home}/src/web/empty";
    private String tsHome;
    private File template;
    private Properties known = new Properties();

    private ArrayList alreadyIncludedClasses = new ArrayList();
    private ArrayList vehicleClasses = new ArrayList(4);
    private Set processedJarWars = new HashSet();

    private boolean sccscreate = true;
    private boolean sccsedit = true;
    private boolean sccsdelget = true;
    private Properties tokenVals = new Properties();
    final private File tmpDir = new File("/tmp/antgen");
    public static List serviceList = new ArrayList(15);
    private boolean isCompatDir;
    private boolean vehicleDirsSet;

    //explicitly set not to use *template.nopkg
    private boolean nonopkg;

    static {
	String[] ss = {
	  "jdbc", "jta", "javamail", "jaxp", "jaas",
	  "j2eetools"
	};
	serviceList = Arrays.asList(ss);
    }

    public void setNonopkg(boolean b) {
	this.nonopkg = b;
    }
    public void setSccscreate(boolean b) {
	this.sccscreate = b;
    }
    public void setSccsedit(boolean b) {
	this.sccsedit = b;
    }
    public void setSccsdelget(boolean b) {
	this.sccsdelget = b;
    }
    public boolean getSccscreate() {
	return sccscreate;
    }
    public boolean getSccsedit() {
	return sccsedit;
    }
    public boolean getSccsdelget() {
	return sccsdelget;
    }
    public void setTemplate(File template) {
	this.template = template;
    }

    public void init() {
	tsHome = project.getProperty("ts.home");
	known.setProperty("ts.home", tsHome);
	known.setProperty("TS_HOME", tsHome);
	known.setProperty("SLASH", "/");
	known.setProperty("SEP", ":");

	if(!tmpDir.exists()) {
	    tmpDir.mkdir();
	}
	String vehicleDir = project.getProperty("vehicle.dir");
	String vehiclePkg = vehicleDir.replace('/', '.');
	this.vehicleClasses.add(vehiclePkg + ".ejb.EJBVehicle.class");
	this.vehicleClasses.add(vehiclePkg + ".ejb.EJBVehicleRemote.class");
	this.vehicleClasses.add(vehiclePkg + ".ejb.EJBVehicleHome.class");
	this.vehicleClasses.add(vehiclePkg + ".servlet.ServletVehicle.class");
    }
    private void initTemplate() {
	if(template == null) {
	    File serviceTemplate = new File(tsHome + "/tools/make2ant", "service.template");
	    File nonServiceTemplate = new File(tsHome + "/tools/make2ant", "nonservice.template");
	    String sUserDir = System.getProperty("user.dir");
	    if(sUserDir.indexOf(FS + "samples" + FS) != -1) {
		if(sUserDir.indexOf(FS + "ejb" + FS) != -1) {
		    template =  nonServiceTemplate;
		    return;
		}
		template = serviceTemplate;
		return;
	    }
	    String toFind = "com" + FS + "sun" + FS + "ts" + FS + "tests" + FS;
	    int i = sUserDir.indexOf(toFind);
	    if(i != -1) {
		int endPos = sUserDir.indexOf(FS, i + toFind.length());
		String testArea = null;
		if(endPos == -1) {
		    testArea = sUserDir.substring(i + toFind.length());
		} else {
		    testArea = sUserDir.substring(i + toFind.length(), endPos);
		}
		log("Current test area is:" + testArea);
		if(serviceList.contains(testArea)) {
		    template = serviceTemplate;
		} else {
		    template = nonServiceTemplate;
		}
	    }//if(i != -1)
	}
	if(!template.exists()) {
	    throw new BuildException("template file does not exist:" + template.getPath());
	}
    }
    public void execute() throws BuildException {
	initTemplate();
	String testDir = tsHome + FS + "src" + FS
	    + "com" + FS + "sun" + FS + "ts" + FS + "tests";
//	if(template == null) {
//	    template = new File(tsHome, "tools/make2ant/default.template");
//	}
	String sUserDir = project.getProperty("user.dir");
	if(sUserDir.replace('\\', '/').indexOf(testDir.replace('\\','/')) == -1) {
	    log("user.dir=" + sUserDir + " does not contain:" + testDir);
	    throw new BuildException("The current directory is not a test dir.");
	}
	log("Creating build.xml for tests starting " + sUserDir);
	handleDir(new File(sUserDir));
    }

    //need to do something if makefile does not exist.
    private boolean isLeafDir(File userDir, Properties mkp) {
	try {
	    makeProps(new File(userDir, "Makefile"), mkp);
	} catch(IOException ex) {
	    makeFilePresent = false;
	    throw new BuildException("Makefile does not exist.");
	}
	return !(mkp.containsKey("SUBDIRS"));
    }
    private void handleDir(File userDir) throws BuildException {
	Properties mkp = new Properties();
	if(isLeafDir(userDir, mkp)) {
	    log("In leaf dir=" + userDir.getPath());
	    doLeaf(userDir, mkp);
	} else {
	    String subDirs = mkp.getProperty("SUBDIRS");
	    log("In non-leaf=" + userDir.getPath());
	    StringTokenizer st = new StringTokenizer(subDirs);
	    while(st.hasMoreTokens()) {
		String val = st.nextToken();
		handleDir(new File(userDir, val));
	    }
	}
    }

    private void handleEjbClasses(File leafDir, Properties props, String pkg) {
	String intfs = props.getProperty("INTERFACE");
	if(intfs != null) {
	    StringTokenizer st = new StringTokenizer(intfs);
	    while(st.hasMoreTokens()) {
		String token = st.nextToken();
		alreadyIncludedClasses.add(pkg + "." + token);
	    }
	}
	intfs = null;
	String beans = props.getProperty("BEANS");
	if(beans != null) {
	    StringTokenizer st = new StringTokenizer(beans);
	    while(st.hasMoreTokens()) {
		String token = st.nextToken();
		alreadyIncludedClasses.add(pkg + "." + token);
	    }
	}
	FileFilter propsFileFilter = new FileFilter() {
	    public boolean accept(File pathname) {
		if(!pathname.isFile()) {
		    return false;
		}
		String fname = pathname.getName();
		return (fname.endsWith(".props") || fname.endsWith(".webprops")
		);
	    }
        };
	File[] propsFile = leafDir.listFiles(propsFileFilter);
	for (int i = 0; i < propsFile.length; i++) {
	    Properties ps = new Properties();
	    FileInputStream ins = null;
	    try {
		ins = new FileInputStream(propsFile[i]);
		ps.load(ins);
		for(Enumeration enu = ps.propertyNames(); enu.hasMoreElements();) {
		    String key = (String) enu.nextElement();
		    if(isClassKey(key)) {
			String val = ps.getProperty(key);
			if(val != null
			    && !val.startsWith("java.")
			    && !val.startsWith("javax.")) {
			    log("Add to alreadyIncludedClass:" + val + ".class",
				Project.MSG_VERBOSE);
			    alreadyIncludedClasses.add(val + ".class");
			}
		    }
		}
	    } catch (Exception ex) {
		log("Cannot load props file:" + propsFile[i]);
	    } finally {
		if(ins != null) {
		    try {
			ins.close();
		    } catch (Exception ex) {}
		}
	    }
	}
    }
    private boolean isClassKey(String s) {
	if(s == null || s.length() == 0) {
	    return false;
	}
	List classKeys = new ArrayList(610);
	classKeys.add("remoteInterface");
	classKeys.add("homeInterface");
	classKeys.add("localInterface");
	classKeys.add("localHomeInterface");
	for (int i = 0; i < 200; i++) {
	    classKeys.add("ejb" + i + ".name");
	    classKeys.add("ejb" + i + ".entity.primaryKey");
	    classKeys.add("webComponent" + i + ".classname");
	}

	int pos = -1;
	for (int i = 0, n = classKeys.size(); i < n; i++) {
	    pos = s.indexOf((String) classKeys.get(i));
	    if(pos != -1) {
		return true;
	    }
	}
	return false;
    }
    private void doLeaf(File leafDir, Properties props) throws BuildException {
	isCompatDir = false;
	vehicleDirsSet = true;
	tokenVals.clear();
	alreadyIncludedClasses.clear();
	processedJarWars.clear();
	tokenVals.setProperty("pkgDirWarn", "");
	alreadyIncludedClasses.addAll(vehicleClasses);
	loadOtherMakefiles(leafDir, props);
	props.setProperty("SLASH", "/");
	known.putAll(props);
	resolveAllProperties(props, known);

	if(!props.containsKey("VEHICLE_DIRS")) {
	    vehicleDirsSet = false;
	}
	String pkg = props.getProperty("PACKAGE");
	String projectName = null;
	boolean pkgWarn = false;
	if(pkg == null || pkg.trim().length() == 0) {
	    pkg = TaskUtil.path2PkgDir(leafDir).replace('/','.');
	}
	String toFind = "com.sun.ts.tests.";
	int pos = pkg.indexOf(toFind);
	if(pos == -1) {
	    pkg = TaskUtil.path2PkgDir(leafDir).replace('/','.');
	    pkgWarn = true;
	}
	pos = pkg.indexOf(toFind);
	if(pos != -1) {
	    projectName = pkg.substring(pos + toFind.length()).replace('.', '_');
	} else {
	    pkgWarn = true;
	    projectName = pkg.replace('.','_');
	}
	if(pkgWarn) {
	    tokenVals.setProperty("pkgDirWarn", "<!-- WARNING: package does not mirror source tree. -->");
	}
	addFilterProps("projectName", projectName);
	String pkgDir = pkg.replace('.', '/');
	addFilterProps("pkgDir", pkgDir);


	if(pkg == null || pkg.trim().length() == 0) {
	    throw new BuildException("Cannot get pkg from Makefile or path.");
	}

	handleEjbClasses(leafDir, props, pkg);
	String appName = props.getProperty("APP_NAME");
	if(appName == null || appName.length() == 0) {
	    appName = props.getProperty("TESTNAME");
	}
	addFilterProps("appName", appName);
	if(appName == null || appName.length() == 0) {
	    tokenVals.setProperty("appNameElement", "");
	} else {
	    tokenVals.setProperty("appNameElement",
		"  <property> name=\"app.name\" value=\"" + appName + "\"/>");
	}

	String clientName = props.getProperty("CLIENT_NAME");
	String mainClass = null;
	if(clientName != null && clientName.length() != 0
	    && Character.isUpperCase(clientName.charAt(0))) {
	    mainClass = pkg + "." + clientName;
	} else {
	    mainClass = props.getProperty("FULLCLIENT");
	    if(mainClass == null || mainClass.length() == 0) {
		String client = props.getProperty("CLIENT");
		if(client != null && client.length() != 0) {
		    int i = client.lastIndexOf(".class");
		    if(i != -1) {
			client = client.substring(0, i);
		    }
		    mainClass = pkg + "." + client;
		}
	    }
	}
	if(mainClass != null) {
	    addFilterProps("mainClass", mainClass);
	    alreadyIncludedClasses.add(mainClass + ".class");
	}

	String testClasses = props.getProperty("TEST_CLASSES");
	if(template.getName().equals("service.template")) {
	    if(testClasses != null && testClasses.length() != 0) {
		if(testClasses.indexOf("${SLASH}") != -1) {
		    testClasses = TaskUtil.replace(testClasses, "${SLASH}", "/");
		}
		StringTokenizer st = new StringTokenizer(testClasses,":");
		String supportIncludes = "";
		while(st.hasMoreTokens()) {
		    String oneClass = st.nextToken();
		    if(oneClass.endsWith(".class") || oneClass.endsWith("*")) {
			if(!(ArchiveInfo.isIncluded(alreadyIncludedClasses, oneClass) ) ) {
			    supportIncludes = supportIncludes + oneClass + ", ";
			}
		    }
		}
		supportIncludes = supportIncludes.trim();
		if(supportIncludes.endsWith(",")) {
		    supportIncludes = supportIncludes.substring(0, supportIncludes.length() - 1);
		}
		if(supportIncludes.length() == 0) {  //bypass addFilterProps
		    tokenVals.setProperty("supportIncludes", "");
		    tokenVals.setProperty("supportElement", "");
		} else {
		    tokenVals.setProperty("supportIncludes", supportIncludes);
		    tokenVals.setProperty("supportElement",
			NL + EIGHT + "<support includes=\"" + supportIncludes + "\"/>");
		}
	    }
	} else { //if not service tests
	    inspectArchives(leafDir);
	}
	copyFile(leafDir);
    }
    private void addFilterProps(String key, String val) {
	if(val != null && val.length() != 0) {
	    tokenVals.setProperty(key, val.trim());
	}
    }
    //only do it for non-service tests
    private void inspectArchives(File leafDir) {
	File[] ears = leafDir.listFiles(ArchiveInfo.earFilter);
	File[] jarWars = leafDir.listFiles(ArchiveInfo.jarWarFilter);
	if(ears.length == 0 && jarWars.length == 0) {
	    File fullDist = new File(TSBuildListener.tsHome + "/dist",
		TaskUtil.path2PkgDir(leafDir));
	    if(fullDist.exists()) {
		//touch ear/jar/war in src dir so that they will not be
		//overwritten by files from dist.dir
		for (int i = 0; i < ears.length; i++) {
		    ears[i].setLastModified(System.currentTimeMillis());
		}
		for (int i = 0; i < jarWars.length; i++) {
		    jarWars[i].setLastModified(System.currentTimeMillis());
		}

		FileSet fs = new FileSet();
		fs.setDir(fullDist);
		fs.setIncludes("*.ear,*.jar,*.war,*.rar");
		Copy copyTask = new Copy();
		copyTask.setProject(project);
		copyTask.init();
		copyTask.setTodir(leafDir);
		copyTask.addFileset(fs);
		copyTask.perform();
	    }
	}
	ears = leafDir.listFiles(ArchiveInfo.earFilter);
	jarWars = leafDir.listFiles(ArchiveInfo.jarWarFilter);
	File sccsDir = new File(leafDir, "SCCS");
	if(sccsDir.exists()){
	    File[] sccsEars = sccsDir.listFiles(ArchiveInfo.earFilter);
	    if(sccsEars != null && ears.length == sccsEars.length) {
		if(ears.length > 0) {
		    isCompatDir = true;
		    return;     //ear's are checked in, not built.
		}
	    }
	}
	String earElement = "";
//	there may be standalone jar/wars
	genJarWarElement(jarWars, leafDir);
	if(ears != null) {
	    if(ears.length == 0) {
		log("WARNING:no ear in leafDir:" + leafDir.getPath(),
		    Project.MSG_WARN);
		tokenVals.put("earElement", "");
		return;
	    } else if(ears.length == 1) {
		String fileName = ears[0].getName();
		String appName = fileName.substring(0, fileName.indexOf(".ear"));
		if(jarWars.length == 0) {
		    earElement = SIX + "<appear name=\"" + appName + "\"/>";
		} else {
		    String includeJarWar = ArchiveInfo.jarWarInEar(ears[0]);
		    if(includeJarWar == null) {
			includeJarWar = "@includeJarWar@";
		    }
		    earElement = earElement + SIX + "<appear name=\"" + appName + "\">"
			+ NL + EIGHT + "<support includes=\"" + includeJarWar
			+ "\"/>"
			+ NL + SIX + "</appear>" + NL;
		}
		tokenVals.put("earElement", earElement);
	    } else {
		for(int i = 0; i < ears.length; i++) {
		    String fileName = ears[i].getName();
		    String appName = fileName.substring(0, fileName.indexOf(".ear"));
		    String includeJarWar = ArchiveInfo.jarWarInEar(ears[i]);
		    if(includeJarWar != null) {
		        earElement = earElement + SIX + "<appear name=\"" + appName + "\">"
			    + NL + EIGHT + "<support includes=\"" + includeJarWar
			    + "\"/>"
			    + NL + SIX + "</appear>" + NL;
		    } else {
			earElement = SIX + "<appear name=\"" + appName + "\"/>";
		    }
		}
		tokenVals.put("earElement", earElement);
	    }
	    expandEars(ears);
	    genJarWarElement(tmpDir.listFiles(ArchiveInfo.jarWarFilter), leafDir);
	} else {//if no ears in this dir, check war's jar's
	    tokenVals.put("earElement", "");
	    if(jarWars == null || jarWars.length == 0) {
		log("WARNING:no ear/jar/war in leafDir:" + leafDir.getPath(),
		    Project.MSG_WARN);
	    }
	}
    }

    private void genJarWarElement(File[] jarWars, File leafDir) {
	String jarWarElement = tokenVals.getProperty("jarWarElement","");
	if(jarWars == null || jarWars.length == 0) {
	    if(jarWarElement.equals("")) {
	        tokenVals.put("jarWarElement", "");
	    }
	    return;
	}

	for(int i = 0; i < jarWars.length; i++) {
	    if(processedJarWars.contains(jarWars[i].getName())) {
		continue;
	    }
	    Properties attrs = ArchiveInfo.getAttrs(jarWars[i], tokenVals, leafDir);
	    String smain = tokenVals.getProperty("mainClass");
	    if(smain != null) {
		alreadyIncludedClasses.add(smain + ".class");
	    }
	    String taskName = attrs.getProperty("taskName");
	    jarWarElement = jarWarElement + SIX + "<" + taskName
		+ " descriptor=\"" + attrs.getProperty("descriptor") + "\" name=\""
		+ attrs.getProperty("name") + "\""
		+ attrs.getProperty("mainClassElement") + ">" + NL;
	    //mainClassElement should be "" for non-webwar tasks
	    String supportIncludes = handleJarWar(jarWars[i]);
	    if(supportIncludes != null) {
		jarWarElement = jarWarElement + EIGHT
		    + "<support includes=\"" + supportIncludes
		    + "\"/>" + NL;
	    }
	    if(taskName.equals("webwar")) {
		jarWarElement = checkContentDir(leafDir, jarWars[i], jarWarElement);
	    }
	    jarWarElement = jarWarElement + SIX + "</" + taskName + ">" + NL;
	    processedJarWars.add(jarWars[i].getName());
	}
	//we do not need NL at the end of jarWarElement, as in template there is
	//already a NL
//	if(jarWarElement.endsWith(NL)) {
//	    jarWarElement = jarWarElement.substring(0, jarWarElement.lastIndexOf(NL));
//	}
	tokenVals.put("jarWarElement", jarWarElement);
    }

//    private void parseEJBServletClass(File file) {
//	log("parseEJBServletClass: parsing " + file.getPath());
//	File workDir = new File(tmpDir, "jarwar");
//	if(!workDir.exists()) {
//	    if(!workDir.mkdirs()) {
//		log("Cannot mkdirs:" + workDir.getPath());
//		return;
//	    }
//	}
//	Expand dfr = new Expand();
//	dfr.setProject(project);
//	dfr.init();
//	dfr.setSrc(file);
//	dfr.setDest(workDir);
//	dfr.perform();
//
//	SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
//        saxParserFactory.setValidating(false);
//        SAXParser saxParser = saxParserFactory.newSAXParser();
//	Hashtable ejbServletClasses = null;
//	if(file.getName().endsWith(".war")) {
//	    WebDescriptorHandler handler = new WebDescriptorHandler(this,
//		TSBuildListener.fClassDir);
//	    FileInputStream descriptorStream = null;
//	    try {
//		descriptorStream = new FileInputStream(new File(workDir, "WEB-INF/web.xml"));
//		saxParser.parse(new InputSource(descriptorStream), handler);
//		ejbServletClasses = ((WebDescriptorHandler) handler).getClasses();
//	    } finally {
//		if (descriptorStream != null) {
//		    try {
//			descriptorStream.close();
//		    }
//		    catch (IOException closeException) {}
//		}
//	    }
//	} else if(file.getName().endsWith(".jar")) {
//	    File ejbJarXml = new File(workDir, "META-INF/ejb-jar.xml");
//	    if(!ejbJarXml.exists()) {
//		return;
//	    }
//	    FileInputStream descriptorStream = null;
//	    EJBDescriptorHandler handler = new EJBDescriptorHandler(this,
//		TSBuildListener.fClassDir);
//	    try {
//		descriptorStream = new FileInputStream(ejbJarXml);
//		saxParser.parse(new InputSource(descriptorStream), handler);
//		ejbServletClasses = handler.getFiles();
//	    } finally {
//		if (descriptorStream != null) {
//		    try {
//			descriptorStream.close();
//		    }
//		    catch (IOException closeException) {}
//		}
//	    }
//	}
//	if(ejbServletClasses == null || ejbServletClasses.size() == 0) {
//	    log("parseEJBServletClass:no ejb servlet class collected.");
//	} else {
//	    for (Iterator i = ejbServletClasses.keySet().iterator(); i.hasNext(); ) {
//		String fileName = (String) i.next();
//		log("parseEJBServletClass: got class" + fileName);
//
//	    }
//	}
//    }

    //for example: return a string
    //"com/sun/ts/tests/samples/ejb/ee/simpleHello/DBSupport.class, ..."
    private String handleJarWar(File file) {
	//need to read web.xml for servlet classes, which will not be
	//handled by support.
//	if((file.getName()).endsWith(".war") ) {
//	    parseEJBServletClass(file);
	    //will put servlet class into alreadyIncludeClasses
//	}
	String result = "";
	try {
	    JarFile jar = new JarFile(file);
	    for(Enumeration enu = jar.entries(); enu.hasMoreElements();) {
		ZipEntry infile = (ZipEntry) enu.nextElement();
		if(!infile.isDirectory()) {
		    String name = infile.getName();
		    if(name.endsWith(".class") && name.indexOf("$") == -1 ) {
			String cleanName = ArchiveInfo.cleanName(name);
			if(!(ArchiveInfo.isIncluded(alreadyIncludedClasses, cleanName)  )) {
			    result = result + cleanName + ", ";
			}
		    }
		}
	    }//for
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	if(result.endsWith(", ")) {
	    result = result.substring(0, result.length() - 2);
	    return result;
	} else {
	    System.out.println("No support class files in jar/war file:" + file.getPath());
	    return null;
	}
    }
    private void expandEars(File[] ears) {
	FileSet fs = new FileSet();
	fs.setDir(tmpDir);
	fs.setIncludes("**/*");
	Delete del = new Delete();
	del.setProject(project);
	del.init();
	del.setIncludeEmptyDirs(true);
	del.addFileset(fs);
	del.setFailOnError(false);
	del.perform();

	for(int i = 0; i < ears.length; i++) {
	    Expand dfr = new Expand();
	    dfr.setProject(project);
	    dfr.init();
	    dfr.setSrc(ears[i]);
	    dfr.setDest(tmpDir);
	    dfr.perform();
	}
    }

    private void copyFile(File leafDir) {
	File toFile = new File(leafDir, "build.xml");
	boolean newFile = true;
	if(toFile.exists()) {
	    if(!(new File(leafDir, "SCCS/s.build.xml").exists() )) {
		toFile.renameTo(new File(leafDir, "build.xml." + System.currentTimeMillis()));
	    } else {
		if(sccsedit) {
		    log("build file " + toFile.getPath() + " already exists, sccs edit it.");
		    TaskUtil.sccsEdit(project, toFile);
		    newFile = false;
		} else {
		    log("build file " + toFile.getPath() + " already exists, skip.");
		    return;
		}
	    }
	}

	File templateLocal = changeTemplate(leafDir);
	Copy copyTask = new Copy();
	copyTask.setProject(project);
	copyTask.init();
	copyTask.setFile(templateLocal);
	copyTask.setTofile(toFile);
	copyTask.setOverwrite(true);

	FilterSet filters = copyTask.createFilterSet();
	for(Enumeration enu = tokenVals.propertyNames(); enu.hasMoreElements();) {
	    Filter filter = new Filter();
	    String key = (String) enu.nextElement();
	    filter.setToken(key);
	    String val = (String) tokenVals.getProperty(key);
	    filter.setValue(val);
	    filters.addFilter(key, val);
	}
	copyTask.setFiltering(true);
	copyTask.perform();
	if(newFile && sccscreate) {
	    TaskUtil.sccsCreate(project, toFile);
	} else if((!newFile) && sccsdelget && sccsedit) {
	    TaskUtil.sccsDelget(project, toFile,
		"generated build.xml from template " + template.getPath());
	}
    }
    //look for other Makefiles in the leafDir.  If no or any exception,
    //do nothig to props.  If any other makefiles, add them to props,
    //if the property hasn't been set to a valid value.
    private void loadOtherMakefiles(File leafDir, Properties props) {
	File[] otherMake = leafDir.listFiles(ArchiveInfo.otherMakeFileFilter);
	for (int i = 0; i < otherMake.length; i++) {
	    Properties p2 = new Properties();
	    try {
	        makeProps(otherMake[i], p2);
	    } catch(Exception ex) {
		ex.printStackTrace();
		return;
	    }
	    log("Reading other Makefile:" + otherMake[i].getName());
	    Enumeration enu = p2.propertyNames();
	    while (enu.hasMoreElements()) {
		String key = (String) enu.nextElement();
		String existingVal = props.getProperty(key);
		if(existingVal == null || existingVal.length() == 0) {
		    String toSet = p2.getProperty(key);
		    if(toSet != null) {
		        props.setProperty(key, toSet.trim());
		    }
		}
	    }
	}

    }
    private void makeProps(File mk, Properties props) throws IOException {
	FileInputStream ins = null;
	try {
	    ins = new FileInputStream(mk);
	    props.load(ins);
	} finally {
	    if(ins != null) {
		try {
		    ins.close();
		} catch(IOException ioex) {}
	    }
	}
	//$(foo) ==> ${foo}
	for(Enumeration enu = props.propertyNames(); enu.hasMoreElements();) {
	    String key = (String) enu.nextElement();
	    String val = (String) props.getProperty(key);
	    if(val.indexOf("$(") != -1) {
		val = val.replace('(', '{').replace(')', '}').trim();
		props.setProperty(key, val);
//		log("After replacing ( ==> {:" + key + "=" + props.getProperty(key));
	    }
	}
    }

    private String checkContentDir(File leafDir, File warFile, String jarWarElement) {
	String webFiles = "";
	try {
	    JarFile jar = new JarFile(warFile);
	    for(Enumeration enu = jar.entries(); enu.hasMoreElements();) {
		ZipEntry infile = (ZipEntry) enu.nextElement();
		if(!infile.isDirectory()) {
		    String name = infile.getName();
		    String upperName = name.toUpperCase().replace('\\','/');
		    if(!( upperName.equals("WEB-INF/WEB.XML")
			|| upperName.equals("META-INF/MANIFEST.MF")
			|| upperName.startsWith("WEB-INF/CLASSES/") ) ) {
			webFiles = webFiles + name + ", ";
		    }
		}
	    }//for
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
	int commaPos = webFiles.lastIndexOf(", ");
	String result = null;
	boolean useCurrentDir = false;
	String sContentDir = null;
	if(commaPos != -1) {
	    sContentDir = null;
	    String aFile = webFiles.substring(0, webFiles.indexOf(", "));
	    if((new File(leafDir, "contentRoot/" + aFile ) ).exists() ) {
		sContentDir = "contentRoot";
	    } else if((new File(leafDir, "webFiles/" + aFile ) ).exists() ) {
		sContentDir = "webFiles";
	    } else if((new File(leafDir, aFile ) ).exists() ) {
		sContentDir = ".";
		useCurrentDir = true;
	    } else {  //previous 3 should solve most cases
		File[] dirs = leafDir.listFiles(ArchiveInfo.dirFilter);
		for (int i = 0; i < dirs.length; i++) {
		    if((new File(dirs[i], aFile) ).exists() ) {
			sContentDir = dirs[i].getName();
			break;
		    }
		}
	    }
	    webFiles = webFiles.substring(0, commaPos);
	    if(sContentDir == null) {
		sContentDir = "@contentDir@";
		log("Cannot find contentDir for web content:" + webFiles);
	    } else {
		log("Found " + aFile + " in " + sContentDir);
	    }
	} else {  //no web files in war.
	    return jarWarElement;
//	    we made that optional on 4/12/2002
//	    webFiles = "**/*.html, **/*.jsp";
//	    sContentDir = emptyContentDir;
//	    return jarWarElement + EIGHT + "<content dir=\""
//	    + sContentDir + "\" excludes=\"**/*\"/>" + NL;
	}
//	result = jarWarElement + EIGHT + "<content dir=\""
//	    + sContentDir + "\" includes=\"" + webFiles
//	    + "\"/>" + NL;
	if(useCurrentDir) {
	    result = jarWarElement + EIGHT + "<content dir=\""
	        + sContentDir + "\" includes=\"*.jsp, *.html\"/>" + NL;
	} else {
	    result = jarWarElement + EIGHT + "<content dir=\""
	        + sContentDir + "\"/>" + NL;
	}
	return result;
    }

    private File changeTemplate(File leafDir) {
	if(nonopkg || leafDir == null) {
	    return template;
	}
	File[] propsFile = leafDir.listFiles(ArchiveInfo.propsFileFilter);
	File[] xmlDescriptors = leafDir.listFiles(ArchiveInfo.xmlDescriptorFilter);

	if((propsFile.length == 0 && xmlDescriptors.length == 0) || isCompatDir) {
	    //some service tests do not have any props file, e.g., jaxr, signatue.
	    if(!vehicleDirsSet) {
		if(!(template.getName().endsWith(".nopkg") )) {
		    return new File(template.getPath() + ".nopkg");
		}
	    }
	}
	return template;
    }
     private void resolveAllProperties(Properties props, Properties known) {
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String name = (String)e.nextElement();
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
                        String fragment = (String)i.nextElement();
                        if (fragment == null) {
                            String propertyName = (String)j.nextElement();
                            if (propertyName.equals(name)) {
                                throw new BuildException("Property " + name + " was circularly defined.");
                            }
                            fragment = known.getProperty(propertyName);
                            if (fragment == null) {
                                if (props.containsKey(propertyName)) {
                                    fragment = props.getProperty(propertyName);
                                    resolved = false;
                                }
                                else {
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
}
