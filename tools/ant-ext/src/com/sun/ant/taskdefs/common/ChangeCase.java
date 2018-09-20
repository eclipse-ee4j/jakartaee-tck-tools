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

public class ChangeCase extends Task  {
    private String name=null;
    private String value=null;
	// upper or lower
    private String toCase="lower";

    public void execute() throws BuildException {
	if (toCase.equals("lower")){
		getProject().setProperty(name, value.toLowerCase());
	} else if (toCase.equals("upper")){
		getProject().setProperty(name, value.toUpperCase());
	} else {
		throw new BuildException("Error: invalid value specified for case: "+toCase);
	}
		
    }

    public void setName(String name) {
	this.name = name;
    }
    public void setValue(String value) {
	this.value = value;
    }
    public void setToCase(String toCase) {
	this.toCase = toCase.toLowerCase();
    }
}
