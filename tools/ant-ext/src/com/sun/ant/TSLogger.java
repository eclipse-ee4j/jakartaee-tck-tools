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

package com.sun.ant;

import org.apache.tools.ant.*;
import java.util.*;

public class TSLogger extends NoBannerLogger {
    private static String[] msg2Ignore = new String[] {
	"No local string for",
    "WARNING: no help for",
    "CESV: no help for"
    };

    private static List failedDirs;
    private long startTime;

    public void messageLogged(BuildEvent event) {
	String msg = event.getMessage();
	for (int i = 0; i < TSLogger.msg2Ignore.length; i++) {
	    if(msg.startsWith(TSLogger.msg2Ignore[i])) {
		return;
	    }
	}
	super.messageLogged(event );
    }

    public void buildStarted(BuildEvent event) {
	startTime = System.currentTimeMillis();
    }

    public void buildFinished(BuildEvent event) {
	Throwable error = event.getException();
	StringBuffer sb = null;
        if (error == null && failedDirs == null) {
            out.println(lSep + "BUILD SUCCESSFUL");
        } else if(error != null) {
            err.println(lSep + "BUILD FAILED" + lSep);

            if (Project.MSG_VERBOSE <= msgOutputLevel ||
                !(error instanceof BuildException)) {
                error.printStackTrace(err);
            }
            else {
                if (error instanceof BuildException) {
                    err.println(error.toString());
                }
                else {
                    err.println(error.getMessage());
                }
            }
        }
	if(failedDirs != null) {
	    sb = new StringBuffer();
	    for (int i = 0, n = failedDirs.size(); i < n; i++) {
		sb.append(failedDirs.get(i).toString()).append('\n');
	    }
	    if(error != null) {
		err.println(lSep + "In addition, the following directories failed:");
	    } else {
		err.println(lSep + "BUILD FAILED");
		err.println(lSep + "The following directories failed:");
	    }
	    err.println(sb.toString());
	}
        out.println(lSep + "Total time: " + formatTime(System.currentTimeMillis() - startTime));

	// if we have any directories that failed to compile we
	// need to notify Ant that the build failed.
	if (failedDirs != null) {
	    throw new BuildException("ERROR: Some directories failed to build." +
				     "  See list of failed directories above.");
	}
	// if error is not null we've been given an event with an exception in
	// it, we need to notify Ant the build failed.
	if (error != null) {
	    throw new BuildException(error);
	}

    }

    public static void addFailedDir(String s) {
	if(failedDirs == null) {
	    failedDirs = new ArrayList();
	}
	if(!failedDirs.contains(s)) {
	    failedDirs.add(s);
	}
    }
}
