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
import java.text.DateFormat;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;

/**
 * This ant task allows users to execute the entire CTS test suite
 * with a single ant task invocation.  The server is restarted in
 * between each test area to clear any memory leak type issues.
 * The task also execs the appropriate pre and post tasks for certain
 * test areas.  For instance, before running the JACC tests, enable.jacc
 * is invoked.  Once the JACC tests finishes, diable.jacc is invoked.
 * The RMIIIOP server is started before the RMIIIOP tests and stopped
 * after the tests are complete.
 *
 * If you want to use this task add a task definition to the
 * $TS_HOME/bin/build.xml file, like the following:
 *
 *   <taskdef name="runcts" classname="com.sun.ant.taskdefs.common.RunCTS"/>
 *
 * Then add a target that executes the runcts task.  The example below
 * runs the samples, assembly, integration and appclient test areas:
 *
 *   <target name="run.cts">
 *     <runcts reportdir="/tmp/jtreport"
 *             workdir="/tmp/jtwork"
 *             tshome="${ts.home}"
 *             j2eehome="${j2ee.home}"
 *             testareas="samples,appclient,assembly,javamail,foobar"/>
 *   </target>
 *
 * Invoke the task using "$TS_HOME/bin/tsant run.cts".
 *
 */
public class RunCTS extends Task {
    
    private static final String DEFAULT_LOG_FILE = System.getProperty("java.io.tmpdir", "/tmp") +
            File.separator + "RunCTSAntLog.txt";
    private static final String NL = System.getProperty("line.separator", "\n");
    private static final String PASSWORD_FILE_NAME = "password.txt";

    private File     reportdir;
    private File     workdir;
    private File     tshome;
    private File     j2eehome;
    private File     tshomebin;
    private File     tshomebincommon;
    private File     j2eehomebin;
    private File     javahome;
    private File     rijava;
    private File     windir;
    private File     tempdir;
    private File     systemroot;
    private File     spshome;
    private File     anthome;
    private File     logfile;
    private File     coverageconfigfile;
    private String[] env;
    private String[] testareas;
    private String   asadmin;   // J2EE_HOME/bin/asadmin
    private String   tsant;     // TS_HOME/bin/tsant
    private String   tstests;   // TS_HOME/src/com/sun/ts/tests
    private List     preprocs;  // List of RunCTSProcIntf objects
    private List     postprocs; // List of RunCTSProcIntf objects
    private File     currentTestDir;
    private Process  proc;
    private StreamWatcher errThread;
    private StreamWatcher outThread;
    private List          procs   = new ArrayList(); // processes to kill before exiting
    private List          threads = new ArrayList(); // threads to interrupt before exiting
    private Properties    processorProps = new Properties();
    private FileWriter    antLog;
    private File          jdkversion;
    private boolean       inited;
    private String        username = "admin";
    private String        password = "adminadmin";
    private File          passwordfile;
    private boolean       remoteinstance;
    private String        adminport = "4849";
    private String        instancename = "server-1";
    private String        hostname = "localhost";
    private boolean       is9;
    private String        runclientargs = "";
    private long          jointimeout = 1000l * 180l; // 180 seconds = 3 minutes
    private String        antopts;
    private String        deliverabledir;
    private boolean       bufferingenabled; // defaults to false
    private boolean       buildJwsJaxws =  true;
    private boolean       skipserverrestart;
    
    
    public void setSkipServerRestart (boolean value) {
    	this.skipserverrestart =  value;
    }
    
    public void setBuildJwsJaxws(boolean value) {
        this.buildJwsJaxws = value;
    }
    
    public void setBufferingEnabled(boolean value) {
        this.bufferingenabled = value;
    }
    
    public void setDeliverabledir(String dir) {
	this.deliverabledir = dir;
    }

    public void setAntopts(String antopts) {
	this.antopts = antopts;
    }

    public void setJointimeout(long jointimeout) {
	this.jointimeout = jointimeout * 1000l;  // convert to seconds
    }

    public void setRunclientargs(String runclientargs) {
        this.runclientargs = runclientargs;
    }
    
    public void setRemoteinstance(boolean value) {
        this.remoteinstance = value;
    }
    
    public void setAdminport(String adminport) {
        this.adminport = adminport;
    }
    
    public void setInstancename(String instancename) {
        this.instancename = instancename;
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public void setPasswordfile(File passwordfile) {
	this.passwordfile = passwordfile;
    }

    public void setReportdir(File reportdir) {
        this.reportdir = reportdir;
    }
    
    public void setWorkdir(File workdir) {
        this.workdir = workdir;
    }
    
    public void setTshome(File tshome) {
        this.tshome = tshome;
    }
    
    public void setJ2eehome(File j2eehome) {
        this.j2eehome = j2eehome;
    }
    
    public void setSpshome(File spshome) {
        this.spshome = spshome;
    }
    
    public void setLogfile(File logfile) {
        this.logfile = logfile;
    }
    
    public void setCoverageconfigfile(File coverageconfigfile) {
        this.coverageconfigfile = coverageconfigfile;
    }
    
    public void setTestareas(String testareas) {
        String delimiters = " \t\n\r\f,";
        StringTokenizer tokens = new StringTokenizer(testareas, delimiters);
        this.testareas = new String[tokens.countTokens()];
        for (int i = 0; tokens.hasMoreTokens(); i++) {
            this.testareas[i] = project.translatePath(tokens.nextToken().trim());
        }
    }
    
    public void setPreprocs(String procs) {
        preprocs = createObjects(procs);
    }
    
    public void setPostprocs(String procs) {
        postprocs = createObjects(procs);
    }
    
    private List createObjects(String classes) {
        List result = new ArrayList();
        String delimiters = " \t\n\r\f,";
        StringTokenizer tokens = new StringTokenizer(classes, delimiters);
        for (int i = 0; tokens.hasMoreTokens(); i++) {
            String className = tokens.nextToken().trim();
            try {
                Class clazz = Class.forName(className);
                result.add(clazz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
    
    public void setJdkversion(File jdkversion) {
        this.jdkversion = jdkversion;
        if (jdkversion.isFile()) {
            jdkversion.delete();
        }
    }
    
    private void writeJDKVersion() throws BuildException {
        if (!inited && jdkversion != null) {
            String version = System.getProperty("java.version");
            FileWriter out = null;
            inited = true;
            try {
                out = new FileWriter(jdkversion);
                out.write(version + NL);
            } catch (Exception e) {
                throw new BuildException(e);
            } finally {
                try { out.close(); } catch(Throwable t) {} // do nothing
            }
        }
    }
    
    public void execute() throws BuildException {
        setProps();
        log("Report Directory          : \"" + reportdir + "\"", project.MSG_VERBOSE);
        log("Work Directory            : \"" + workdir + "\"", project.MSG_VERBOSE);
        log("TS_HOME                   : \"" + tshome + "\"", project.MSG_VERBOSE);
        log("J2EE_HOME                 : \"" + j2eehome + "\"", project.MSG_VERBOSE);
        log("S1AS_HOME                 : \"" + j2eehome + "\"", project.MSG_VERBOSE);
        log("SPS_HOME                  : \"" + spshome + "\"", project.MSG_VERBOSE);
        log("ANT_OPTS                  : \"" + antopts + "\"", project.MSG_VERBOSE);
        log("jointimeout (milliseconds): \"" + jointimeout + "\"", project.MSG_VERBOSE);
        log("deliverabledir            : \"" + deliverabledir + "\"", project.MSG_VERBOSE);
        log("bin dir                   : \"" + tshomebin.getPath() + "\"", project.MSG_VERBOSE);
        log("common bin dir            : \"" + tshomebincommon.getPath() + "\"", project.MSG_VERBOSE);
        log("bufferingenabled          : \"" + bufferingenabled + "\"", project.MSG_VERBOSE);
        log("buildJwsJaxws             : \"" + buildJwsJaxws + "\"", project.MSG_VERBOSE);
        log("skipserverrestart         : \"" + skipserverrestart + "\"", project.MSG_VERBOSE);
        if (windir != null) {
        	log("windir                    : \"" + windir.getPath() + "\"", project.MSG_VERBOSE);
        	log("tempdir                    : \"" + tempdir.getPath() + "\"", project.MSG_VERBOSE);	    
	}
	if (systemroot != null) {
	    log("systemroot                : \"" + systemroot.getPath() + "\"", project.MSG_VERBOSE);
	}
        checkPreConditions();
	adjustFor90();
        initLogFile();
        writeJDKVersion();
        runTests();
        finiLogFile();
    }
    
    private void adjustFor90() throws BuildException {
	/*
	 * See if we are running SJSAS 9.x or greater, if so set is9 to denote we are using 9.x.
	 * Create the default password file location if the user has not specified one.
	 * Check that the file exists.  If they specify a password file and we are not
	 * running against 9.x we will ignore it.  This will keep the runcts task running
	 * as it always has against versions previous to 9.x, using password instead of
	 * passwordfile.  The task will now use passwordfile when run against 9.x. This
	 * code will need to be updated to include future version when necessary.
	 */
	try {
	    StringBuffer buf = runAsadmin("version");
	    //System.err.println("&&&&&&");
	    //System.err.println(buf);
	    //System.err.println("&&&&&&");
            if ( ((buf.indexOf("8.") != -1) && (buf.indexOf("GlassFish") == -1) ) ||
		 (buf.indexOf("Reference Implementation 1.4") != -1)
	       ) {
                is9 = false;
            } else {
                is9 = true;
            }
	} catch (Exception e) {
	    throw new BuildException(e);
	}
	if (is9 && passwordfile == null) {
	    passwordfile = new File(tshomebin, PASSWORD_FILE_NAME);
	}
        if (is9 && !passwordfile.isFile()) {
	    throw new BuildException("The passwordfile, \"" +
				     passwordfile + "\", does not exist, specify a valid password " +
				     "file using the passwordfile attribute");
	}
	log("Running against GlassFish is " + is9, project.MSG_INFO);
    }
    
    private void finiLogFile() {
        try {
            antLogWrite(NL + "LOG ENDED" + NL);
            log("Log File       : \"" + logfile + "\" closed", project.MSG_VERBOSE);
        } catch (Exception e) {
            throw new BuildException(e);
        } finally {
            try { antLog.close(); } catch (Throwable e) {} //do nothing
        }
    }
    
    private void initLogFile() {
        try {
            antLog = new FileWriter(logfile, true);
            antLogWrite("LOG STARTED" + NL);
            log("Log File       : \"" + logfile + "\" opened", project.MSG_VERBOSE);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    
    private String getDate() {
        return DateFormat.getDateInstance().format(new Date());
    }
    
    private void runTests() throws BuildException {
        for (int i = 0; i < testareas.length; i++) {
            String testarea = testareas[i];
            try {
            	if (skipserverrestart) {
                    log("Note: Skipping server restart", project.MSG_INFO);
            	} else {
                    restartServer(testarea);
            	}
                runTestArea(testarea);
            } catch (Exception e) {
                e.printStackTrace();
                log("ERROR running test area \"" + testarea + "\"", project.MSG_ERR);
                antLogWrite("ERROR running test area \"" + testarea + "\"" + NL);
            }
            antLogWrite("Completed test area \"" + testarea + "\"" + NL);
        }
        killProcs();
        killThreads();
    }
    
    private void antLogWrite(String data) {
        try {
            antLog.write(getDate() + ": " + data + NL);
            antLog.flush();
        } catch (Exception e) {
            // do nothing
        }
    }
    
    private void runProcessors(String testDirectory, List procs) throws Exception {
        int numProcs = (procs == null) ? 0 : procs.size();
        for (int i = 0; i < numProcs; i++) {
            RunCTSProcIntf proc = (RunCTSProcIntf)procs.get(i);
            boolean result = proc.execute(testDirectory, project, processorProps);
            if (!result) {
                break;
            }
        }
    }
    
    private void runTestArea(String testArea) throws Exception {
        String testDirectory = tstests + File.separator + testArea;
        currentTestDir = new File(testDirectory);
        String command = "runclient " + runclientargs + " -Dreport.dir=" +
                reportdir.getPath() + File.separator + mapTestArea(testArea) +
                " -Dwork.dir=" + workdir.getPath() + File.separator + mapTestArea(testArea);
        
        if (!currentTestDir.isDirectory()) {
            throw new BuildException("Directory does not exist \"" +
                    currentTestDir + "\"");
        }
        if (testArea.startsWith("webservices12")) {
            enableWebservicesClients();
        }
        if (testArea.startsWith("jacc")) {
            enableJacc();
        }
        if (testArea.startsWith("rmiiiop")) {
            startRmiiiopServer();
        }
       	if (testArea.startsWith("ejb30/lite")) {
            configureDataSourceTests();
        }
       	if (testArea.startsWith("jaspic")) {
            enableJaspic();
        }
       	if (testArea.startsWith("pluggability")) {
       		enableTsPersistenceProvider();
        }
       	if (testArea.startsWith("jaxrs")) {
            updateJaxRSWars();
        }
       	if (testArea.startsWith("jws")) {
            buildReverse("jws");
        }
       	if (testArea.startsWith("jaxws")) {
            buildReverse("jaxws");
        }        
        
        log("Testing \"" + currentTestDir + "\"", project.MSG_INFO);
        runProcessors(testDirectory, preprocs);
        runTsant(command, currentTestDir);
        runProcessors(testDirectory, postprocs);
        log("Completed \"" + currentTestDir + "\"", project.MSG_INFO);

        if (testArea.startsWith("jacc")) {
            disableJacc();
        }
        if (testArea.startsWith("jaspic")) {
            disableJaspic();
        }
        if (testArea.startsWith("pluggability")) {
        	disableTsPersistenceProvider();
        }
    }
    
    private void killProcs() {
        int numProcs = procs.size();
        for (int i = 0; i < numProcs; i++) {
            Process proc = (Process)procs.get(i);
            try {
                proc.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void killThreads() {
        int numThreads = threads.size();
        for (int i = 0; i < numThreads; i++) {
            Thread thread = (Thread)threads.get(i);
            try {
                thread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /*
     *  In order for Webservice12 to run through the following will need to be
     * called prior to running the test area.
     *
     *  Webservices clients need to get built against predeployed webservices
     * endpoints. During the build process the clients will import the wsdl from
     * the predeployed webservices endpoints in order to generate the required
     * artifacts needed to communicate with the endpoint itself.
     */
    private void enableWebservicesClients() throws Exception {
        log("Building webservices clients ...", project.MSG_INFO);
        runTsant("build.special.webservices.clients", tshomebin, true);
        log("Webservices clients built.", project.MSG_INFO);
    }
    
    private void enableJacc() throws Exception {
        log("Enabling JACC...", project.MSG_INFO);
        runTsant("-f xml/impl/glassfish/s1as.xml create.jacc.vm.options", tshomebincommon, true);
        restartServer();
        log("JACC Enabled.", project.MSG_INFO);
    }
    
    private void configureDataSourceTests() throws Exception {
        log("Configuring data sources for EJB30 Lite...", project.MSG_INFO);
        runTsant("configure.datasource.tests", tshomebin, true);
        log("Data sources configured for EJB30 Lite.", project.MSG_INFO);
    }
    
    private void enableJaspic() throws Exception {
        log("Enabling JASPIC...", project.MSG_INFO);
        runTsant("enable.jaspic", tshomebin, true);
        log("JASPIC Enabled.", project.MSG_INFO);
    }
    
    private void enableTsPersistenceProvider() throws Exception {
        log("Enabling TS Persistence Provider...", project.MSG_INFO);
        runTsant("enable.ts.persistence.provider", tshomebin, true);
        log("TS Persistence Provider Enabled.", project.MSG_INFO);
    }
    
    private void updateJaxRSWars() throws Exception {
        log("Updating JAXRS Wars...", project.MSG_INFO);
        runTsant("update.jaxrs.wars", tshomebin, true);
        log("JAXRS Wars Updated.", project.MSG_INFO);
    }
    
    private void buildReverse(String area) throws Exception {
    	// must be reverse too, if forward only or no keywords specified skip this
    	boolean runBuild = buildJwsJaxws;
    	if ((runclientargs.length() > 0) && runBuild) {
    		int index = runclientargs.indexOf("-Dkeywords=");
    		if (index != -1) {
    			int index2 = runclientargs.indexOf("forward");
    			int index3 = runclientargs.indexOf("reverse");
    			runBuild = (index2 != -1) && (index3 == -1); 
    		}
    	}
    	if (runBuild) {
    		log("Building reverse tests for " + area + "...", project.MSG_INFO);
    		String dir = tstests + File.separator + area;
    		File dirFile = new File(dir);
    		runTsant("-Dbuild.vi=true build", dirFile, true);
    		log("Reverse tests built for " + area + ".", project.MSG_INFO);
    	} else {
    		log("Skipping build of reverse tests for " + area + "...", project.MSG_INFO);    		
    	}
    }

    private void disableJaspic() throws Exception {
        log("Disabling JASPIC...", project.MSG_INFO);
        runTsant("disable.jaspic", tshomebin, true);
        log("JASPIC Disabled.", project.MSG_INFO);
    }
    
    private void disableTsPersistenceProvider() throws Exception {
        log("Disabling TS Persistence Provider...", project.MSG_INFO);
        runTsant("disable.ts.persistence.provider", tshomebin, true);
        log("TS Persistence Provider Disabled.", project.MSG_INFO);
    }
    
    private void disableJacc() throws Exception {
        log("Disabling JACC...", project.MSG_INFO);
        runTsant("-f xml/impl/glassfish/s1as.xml delete.jacc.vm.options", tshomebincommon, true);
        restartServer();
        log("JACC Disabled.", project.MSG_INFO);
    }
    
    private void startRmiiiopServer() throws Exception {
        log("Starting RMIIIOP Server...", project.MSG_INFO);
        runTsant("start.rmiiiop.server", tshomebin, false);
        try {
            Thread.sleep(5 * 1000); // let the RMI-IIOP server come up
        } catch (Exception e) {
        }
        log("RMIIIOP Server Started.", project.MSG_INFO);
    }
    
    private void checkPreConditions() throws BuildException {
        // Don't check to see if the work and report directories exist
        // since they will be created by Javatest when this target is run.
        if (tshome == null || reportdir == null || workdir == null || j2eehome == null) {
            throw new BuildException("Must specify the attributes: " +
                    "reportdir, workdir, tshome and j2eehome.");
        }
        if (testareas.length == 0) {
            throw new BuildException("Must specify at least on test area to run");
        }
        if (!tshome.isDirectory()) {
            throw new BuildException("The specified tshome attribute, \"" +
                    tshome + "\", does not exist");
        }
        if (!j2eehome.isDirectory()) {
            throw new BuildException("The specified j2eehome attribute, \"" +
                    j2eehome + "\", does not exist");
        }
        for (int i = 0; i < testareas.length; i++) {
            String testarea = testareas[i];
            currentTestDir = new File(tstests + File.separator + testarea);
            if (!currentTestDir.isDirectory()) {
                throw new BuildException("The specified test directory does not exist, \"" +
                        currentTestDir + "\"");
            }
        }
        if (coverageconfigfile != null && !coverageconfigfile.isFile()) {
            throw new BuildException("The specified coverage config file does not exist, \"" +
                    coverageconfigfile + "\"");
        }
        if (logfile == null) {
            logfile = new File(DEFAULT_LOG_FILE);
        }
        if (logfile.isFile()) {
            logfile.delete();
        }
    }
    
    private void setProps() {
        String osname = System.getProperty("os.name");
        asadmin = j2eehome.getPath() + File.separator + "bin" + File.separator + "asadmin";
        anthome = new File(System.getProperty("ant.home"));
        tsant   = anthome.getPath() + File.separator + "bin" +  File.separator + "ant";
        tstests = tshome.getPath() + File.separator + "src" + File.separator + "com" +
                File.separator + "sun" + File.separator + "ts" +
                File.separator + "tests";
        javahome = new File(System.getProperty("java.home"));

        if (System.getenv("RI_JAVA_HOME") != null) {
            rijava = new File(System.getenv("RI_JAVA_HOME"));
        } else  {
            rijava = javahome;
        }

        tshomebincommon = new File(tshome, "bin");
        tshomebin = new File(tshome, "install/" + deliverabledir + "/bin");
    	if (!tshomebin.isDirectory()) {
    	    tshomebin = tshomebincommon;
    	}
        j2eehomebin = new File(j2eehome, "bin");
        if (antopts == null || antopts.length() == 0) {
		    antopts = "-Djava.endorsed.dirs=" + j2eehome.getPath() + File.separator +
			"modules" + File.separator + "endorsed";
		}
        processorProps.setProperty("j2ee.home", j2eehome.getPath());
        processorProps.setProperty("ts.home", tshome.getPath());
        String spsHome = "";
        if (spshome != null) {
            processorProps.setProperty("sps.home", spshome.getPath());
            spsHome = spshome.getPath();
        }
    	if (deliverabledir == null || deliverabledir.length() == 0) {
    	    deliverabledir = "j2ee";
    	}
        if(osname.startsWith("Win")) {
            windir = new File(System.getenv("windir"));
            systemroot = new File(System.getenv("systemroot"));
            tempdir = new File(System.getenv("TMP"));
            env = new String[] {"TMP=" + tempdir.getPath(), "TS_HOME=" + tshome.getPath(),
                "JAVA_HOME=" + javahome.getPath(),
				"ANT_HOME=" + anthome.getPath(), "S1AS_HOME=" + j2eehome.getPath(),
				"SPS_HOME=" + spsHome, "windir=" + windir.getPath(),
				"SystemRoot=" + systemroot.getPath(), "ANT_OPTS=" + antopts,
                "RI_JAVA_HOME=" + rijava.getPath(),
				"PATH=" + javahome.getPath() + "\bin;" + System.getenv("PATH"),
				"deliverabledir=" + deliverabledir};
        } else {
            env = new String[] {"TS_HOME=" + tshome.getPath(), "JAVA_HOME=" + javahome.getPath(),
				"ANT_HOME=" + anthome.getPath(), "S1AS_HOME=" + j2eehome.getPath(),
				"SPS_HOME=" + spsHome, "ANT_OPTS=" + antopts,
                "RI_JAVA_HOME=" + rijava.getPath(),
				"PATH=" + javahome.getPath() + "/bin:" + System.getenv("PATH"),
                                "deliverabledir=" + deliverabledir};
        }
    }
    
    private String mapTestArea(String testDir) {
        return testDir.replace(File.separatorChar, '_');
    }
    
    private void removeJaccLog() {
        File jaccLog = new File(j2eehome, "domains" + File.separator +
                "domain1" + File.separator +
                "logs" + File.separator + "jacc_log.txt");
        File jaccLogLock = new File(j2eehome, "domains" + File.separator +
                "domain1" + File.separator +
                "logs" + File.separator + "jacc_log.txt.lck");
        if (jaccLog.isFile()) {
            jaccLog.delete();
            log("Deleted JACC Log.", project.MSG_INFO);
        }
        if (jaccLogLock.isFile()) {
            jaccLogLock.delete();
            log("Deleted JACC Log Lock.", project.MSG_INFO);
        }
    }
    
    private void restartServer() throws Exception {
        restartServer("");
    }

    private String getPasswordArgs() {
	String result = null;
	if (is9) {
	    result = " --passwordfile " + passwordfile.getPath();
	} else{
	    result = " --password " + password;
	}
	return result;
    }

    private void restartServer(String testArea) throws Exception {
	if (remoteinstance) {
	    String args = "--user " + username + getPasswordArgs() +
		" --host " + hostname + " --port " + adminport + " " + instancename;
	    runAsadmin("stop-instance " + args);
	    if (testArea.startsWith("jacc")) {
		removeJaccLog();
	    }
	    runAsadmin("start-instance " + args);
	} else {
	    runAsadmin("stop-domain");
	    if (testArea.startsWith("jacc")) {
		removeJaccLog();
	    }
	    runAsadmin("start-domain --user " + username + getPasswordArgs());
	}
    }
    
    ////////////////////////////////////////
    ///// Methods for execing processes ////
    ////////////////////////////////////////
    
    private StringBuffer runAsadmin(String cmd) throws Exception {
        return runCommand(asadmin + " " + cmd, j2eehomebin, true, true);
    }
    
    private StringBuffer runTsant(String cmd, File dir) throws Exception {
        return runTsant(cmd, dir, true);
    }
    
    private StringBuffer runTsant(String cmd, File dir, boolean blocking) throws Exception {
        return runCommand(tsant + " " + cmd, dir, blocking, bufferingenabled);
    }
    
    private boolean isWindows() {
        return System.getProperty("os.name", "").toUpperCase().startsWith("WIN");
    }
    
    private StringBuffer runCommand(String cmd, File dir, boolean blocking, boolean buffer) throws Exception {
        StringBuffer buf = new StringBuffer();
        int pstat = 0;
        try{
            Runtime rt = Runtime.getRuntime();
            if (isWindows()) {
                cmd = "cmd /c " + cmd; // invoke a windows shell to interpret the command
            }
            log("Running ==> \"" + cmd + "\"", project.MSG_INFO);
            proc = rt.exec(cmd, this.env, dir);
            errThread = new StreamWatcher(proc.getErrorStream(), project, "ERR", buffer);
            errThread.start();
            outThread = new StreamWatcher(proc.getInputStream(), project, "OUT", buffer);
            outThread.start();
            if (blocking) {
                pstat = proc.waitFor();
                errThread.join(jointimeout);
                outThread.join(jointimeout);
                buf = outThread.getBuffer();
//  		if (pstat != 0) {
//  		    throw new Exception("runCommand failed, command was \"" + cmd + "\"");
//  		}
            } else {
                procs.add(proc);
                threads.add(errThread);
                threads.add(outThread);
            }
            log("Proc status ==> \"" + pstat + "\"", project.MSG_INFO);
        } catch (Exception ex) {
            System.err.println(ex);
            ex.printStackTrace();
            throw ex;
        }
        return buf;
    }
    
    public static class StreamWatcher extends Thread {
	private boolean buffering;
        private final String NL;
	private StringBuffer buf = new StringBuffer();
	private InputStream in;
	private String id;
	private Project project;
	public StreamWatcher(InputStream in, Project project, String id, boolean buffering) {
	    this.in = in;
	    this.project = project;
	    this.id = id;
	    this.buffering = buffering;
	    NL = System.getProperty("line.separator", "\n");
	}
	public StringBuffer getBuffer() {
	    return buf;
	}
	public void run() {
	    project.log("Starting StreamWatcher for " + in, project.MSG_VERBOSE);
  	    BufferedReader breader = null;
	    try {
	        breader = new BufferedReader(new InputStreamReader (in));
		String line = breader.readLine();
		while (line != null){
		    if (buffering) {
		        buf.append(line + NL);
		    }
		    System.err.println(id + " => " + line);
		    line = breader.readLine();
		    Thread.sleep(2);
		}
		int index = buf.lastIndexOf(NL);
		if (index != -1 && buffering) {
		    buf = buf.delete(index, buf.length());
		}
	    } catch (InterruptedException e) {
		project.log ("Stream Watcher for \"" + id + "\" Stopping", project.MSG_INFO);
	    } catch (InterruptedIOException e) {
		project.log ("Stream Watcher for \"" + id + "\" Stopping", project.MSG_INFO);
	    } catch (Exception e) {
		project.log("StreamWatcher error is " + e, project.MSG_ERR);
	    } finally {
		try {
		    if (in != null) { in.close(); }
		} catch(Exception e) {
		    project.log("StreamWatcher error closing input stream, exception is: " +
				e, project.MSG_ERR);
		}
	    }
	}
    }
    
} // end class RunCTS
