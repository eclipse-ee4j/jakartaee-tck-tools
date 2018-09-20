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

/*
 *  $Id$
 */
package com.sun.ant.taskdefs.common;

import java.io.*;
import java.util.*;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.StringUtils;

public class PathTranslator extends Task {
	private String propname;
	private String type;
        private boolean escape;

        public void setEscape(boolean b) {
            this.escape = b;
        }

	public void setPropname(String propName) {
		this.propname = propName;
	}

	public void setType(String s) {
		type = s;
	}

	public void execute() throws BuildException {
		if (propname == null || propname.length() == 0) {
			return;
		}
        String oldVal = project.getProperty(propname);
		String oldUserVal = project.getUserProperty(propname);
		if (oldVal == null && oldUserVal == null) {
			throw new BuildException("Property " + propname + " has not been set. Do not know how to translate it.");
		}

		String newVal = this.translatePath(oldVal);
		if (oldUserVal == null) {  //not in user properties
			project.setProperty(propname, newVal);
        } else {
            project.setUserProperty(propname, newVal);
        }
    }

private String normalizeInitialFile(String oldVal) {
		if (oldVal == null) {
			return oldVal;
		}
		String result = "";
		oldVal = oldVal.replace('\\', '/');
		while (oldVal.endsWith("/")) {
			oldVal = oldVal.substring(0, oldVal.length() - 1);
		}
		if (oldVal.endsWith("/src")) {
			return result;
		}
		int pos = oldVal.indexOf("/src/");
		if (pos == -1) {
			log(propname + "=" + oldVal + ", does not contain '/src/'");
			return oldVal;
		} else {
			result = oldVal.substring(pos + 5);
		}
		return result;
	}

	private String translatePath(String oldVal) {
		String result = null;
		if (type == null) {//regular translatepath
			result = Project.translatePath(oldVal);
                        if(escape && Os.isFamily("windows")) {
                            result = StringUtils.replace(result, "\\", "\\\\");
                        }
		} else if (type.equalsIgnoreCase("initialfile")) {//type is set
			result = normalizeInitialFile(oldVal);
		}
		return result;
	}
}


