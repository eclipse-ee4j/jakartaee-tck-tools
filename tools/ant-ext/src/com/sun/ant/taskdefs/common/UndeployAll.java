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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;

/**
 * This ant task allows users to undeploy all the deployed applications
 * on a Sun Appserver with a single ant task invocation. 
 *
 * I have added the the below to the $TS_HOME/bin/xml/s1as.xml file.
 *
 *   <taskdef name="undeployall" classname="com.sun.ant.taskdefs.common.UndeployAll"/>
 *
 *   <target name="undeploy.all">
 *       <undeployall j2eehome="${j2ee.home}"/>
 *   </target>
 *
 * To invoke the task use "$TS_HOME/bin/tsant -f $TS_HOME/bin/xml/s1as.xml undeploy.all".
 *
 */

public class UndeployAll extends Task {

    private static final String NL = System.getProperty("line.separator", "\n");
    private static final String PASSWORD_FILE_NAME = "password.txt";

    private File     j2eehome;
    private File     j2eehomebin;
    private File     tshome;
    private File     tshomebin;
    private String[] env;
    private String   asadmin;   // J2EE_HOME/bin/asadmin
    private Process  proc;
    private StreamWatcher errThread;
    private StreamWatcher outThread;
    private List          procs   = new ArrayList(); // processes to kill before exiting
    private List          threads = new ArrayList(); // threads to interrupt before exiting
    private Properties    processorProps = new Properties();
    private String        username = "admin";
    private String        password = "adminadmin";
    private File          passwordfile;
    private boolean       remoteinstance;
    private String        adminport = "4849";
    private String        instancename = "server-1";
    private String        hostname = "localhost";
    private boolean       is9x;

    public void setTshome(File tshome) {
	this.tshome = tshome;
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

    public void setJ2eehome(File j2eehome) {
	this.j2eehome = j2eehome;
    }

    public void execute() throws BuildException {
	checkPreConditions();
	setProps();
	log("J2EE_HOME       : \"" + j2eehome + "\"", project.MSG_VERBOSE);
	log("TS_HOME         : \"" + tshome + "\"", project.MSG_VERBOSE);
        try {
	    adjustFor9x();
	    undeployComponents(listComponents());	
        }catch (Exception ex) {
	    System.err.println(ex); 
	    throw new BuildException(ex);
	}
        
    }
    
    private void adjustFor9x() throws BuildException {
	/*
	 * See if we are running SJSAS 9.x, if so set is9x to denote we are using 9.x.
	 * Create the default password file location if the user has not specified one.
	 * Check that the file exists.  If they specify a password file and we are not
	 * running against 9.x we will ignore it.  This will keep the runcts task running
	 * as it always has against versions previous to 9.x, using password instead of
	 * passwordfile.  The task will now use passwordfile when run against 9.x. This
	 * code will need to be updated to include future version when necessary.
	 */
	try {
	    StringBuffer buf = runAsadmin("version");
            if ( (buf.indexOf("8.") != -1) || (buf.indexOf("7.") != -1) ||
                 (buf.indexOf("Reference Implementation 1.4") != -1) ) {
                is9x = false;
            } else {
                is9x = true;
            }
	} catch (Exception e) {
	    throw new BuildException(e);
	}
	if (is9x && passwordfile == null) {
	    passwordfile = new File(tshomebin, PASSWORD_FILE_NAME);
	}
        if (is9x && !passwordfile.isFile()) {
	    throw new BuildException("The passwordfile, \"" +
				     passwordfile + "\", does not exist, specify a valid password " +
				     "file using the passwordfile attribute");
	}
	log("Running against SJSAS 9.x is " + is9x, project.MSG_INFO);
    }

    private void setProps() {
	j2eehomebin = new File(j2eehome, "bin");
	asadmin = j2eehomebin.getPath() + File.separator + "asadmin";	
	tshomebin = new File(tshome, "bin");
    }

    private void checkPreConditions() throws BuildException {
	// Don't check to see if the work and report directories exist
	// since they will be created by Javatest when this target is run.
	if (tshome == null || j2eehome == null) {
	    throw new BuildException("Must specify the attributes: " + 
				     "tshome and j2eehome.");
	}
	if (!tshome.isDirectory()) {
	    throw new BuildException("The specified tshome attribute, \"" +
				     tshome + "\", does not exist");
	}	
	if (!j2eehome.isDirectory()) {
	    throw new BuildException("The specified j2eehome attribute, \"" +
				     j2eehome + "\", does not exist");
	}	
    }

    private String getPasswordArgs() {
	String result = null;
	if (is9x) {
	    result = " --passwordfile " + passwordfile.getPath();
	} else{
	    result = " --password " + password;
	}
	return result;
    }


    //
    // List the applications that are deployed and build an ArrayList
    // 
    private String[] listComponents() throws Exception {
	List appnames = new ArrayList();
	String args;

	if (remoteinstance) {
	    args = "--user " + username + getPasswordArgs() + " --host " +
		 hostname + " --port " + adminport + " " + instancename;
	} else {
	    args = "--user " + username + getPasswordArgs();
	}

	runAsadmin("list-components " + args);
	StringTokenizer outtokn = new StringTokenizer(outThread.getBuffer().toString(), "\n");
	String line;

	if (errThread.getBuffer().length() > 0) {
	    System.err.println("ERROR: " + errThread.getBuffer().toString());
	}
	while (outtokn.hasMoreTokens()) {
	    line = outtokn.nextToken();
	    if (line.trim().matches("Command list-components executed successfully.") ||
		line.trim().matches("Nothing to list.") || line.trim().startsWith("WARNING:")) {
	        continue;
	    }
	    else {
	        String delimiters = " \t";
	        String app;
	        StringTokenizer apptokn = new StringTokenizer(line, delimiters); 
	        app = apptokn.nextToken();
	        appnames.add(app);
	        log("LIST COMPONENT        : \"" + app + "\"", project.MSG_VERBOSE);
	    }
	}
	return (String [])appnames.toArray(new String[appnames.size()]);
    }

    //
    // Undeploy the deployed applictions in the specified Array.
    //
    private void undeployComponents(String[] appnames) throws Exception {
	int total = (appnames == null) ? 0 : appnames.length;
	String component;
	String args;

	if (total == 0) {
	    log("No Applications To Undeploy", project.MSG_INFO);
	} else {
	    for (int i=0; i < total; i++) {
	        component = appnames[i];
	        log("UNDEPLOY COMPONENT        : \"" + component + "\"", project.MSG_VERBOSE);
	 
                if (remoteinstance) {
                    args = "--user " + username + getPasswordArgs() + " --cascade=true" +
                        " --host " + hostname + " --port " + adminport + " --target " +  instancename;
                } else {
                    args = "--user " + username + getPasswordArgs() + " --cascade=true"; 
                }
                runAsadmin("undeploy " + args +" "+ component);
	    }
	}
    }

    ////////////////////////////////////////
    ///// Methods for execing processes ////
    ////////////////////////////////////////

    private StringBuffer runAsadmin(String cmd) throws Exception {
	return runCommand(asadmin + " " + cmd, j2eehomebin, true);
    }

    private boolean isWindows() {
	return System.getProperty("os.name", "").toUpperCase().startsWith("WIN");
    }

    private StringBuffer runCommand(String cmd, File dir, boolean blocking) throws Exception {
	StringBuffer buf = new StringBuffer();
	int pstat = 0;
	final long THREAD_TIMEOUT = 1000l * 600l; // 600 seconds = 10 minutes
	try{
	    Runtime rt = Runtime.getRuntime();
	    if (isWindows()) {
		cmd = "cmd /c " + cmd; // invoke a windows shell to interpret the command
	    }
	    log("Running ==> \"" + cmd + "\"", project.MSG_INFO);
	    proc = rt.exec(cmd, this.env, dir);
	    errThread = new StreamWatcher(proc.getErrorStream(), project, "ERR");
	    errThread.start();
	    outThread = new StreamWatcher(proc.getInputStream(), project, "OUT");
	    outThread.start();
	    if (blocking) {
		pstat = proc.waitFor();
		errThread.join(THREAD_TIMEOUT);
		outThread.join(THREAD_TIMEOUT);
		buf = outThread.getBuffer();
	    } else {
		procs.add(proc);
		threads.add(errThread);
		threads.add(outThread);
	    }
	    log("Proc status ==> \"" + pstat + "\"", project.MSG_INFO);
	} catch (Exception ex) {
	    log(ex.toString(), project.MSG_ERR);
	    throw ex;
	}
	return buf;
    }

    public static class StreamWatcher extends Thread {
        private final String NL;
	private StringBuffer buf = new StringBuffer();
	private InputStream in;
	private String id;
	private Project project;
	public StreamWatcher(InputStream in, Project project, String id) {
	    this.in = in;
	    this.project = project;
	    this.id = id;
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
		    buf.append(line + NL); // buffer results if any one cares they can call getBuffer
		    line = breader.readLine();
		    Thread.sleep(2);
		}
		int index = buf.lastIndexOf(NL);
		if (index != -1) {
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

} // end class UndeployAll
