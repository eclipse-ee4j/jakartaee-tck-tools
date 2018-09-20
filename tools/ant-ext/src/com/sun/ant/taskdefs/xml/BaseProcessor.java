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

package com.sun.ant.taskdefs.xml;

import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.jdom.Element;
import org.jdom.Attribute;
import org.jdom.Text;

public abstract class BaseProcessor implements NodeProcessorIntf {

    public static final String[] MODES = {"modified", "added", "removed", "read"};
    public static final int MODIFY = 0;
    public static final int ADD    = 1;
    public static final int REMOVE = 2;
    public static final int READ   = 3;
 
    public static NodeProcessorIntf getProcessor(TaskDataIntf taskData) throws BuildException {
	String      value       = taskData.getValue();
	XMLFragment fragment    = taskData.getFragment();
	boolean     deletenodes = taskData.getDeletenodes();
	String      property    = taskData.getProperty();
	
	if (value != null && fragment == null && !deletenodes && property == null) {
	    return new ModifyProcessor();
	} else if (value == null && fragment != null && !deletenodes && property == null) {
	    return new AddProcessor();
	} else if (value == null && fragment == null && deletenodes && property == null) {
	    return new RemoveProcessor();
	} else if (value == null && fragment == null && !deletenodes && property != null) {
	    return new ReadProcessor();
	} else {
	    throw new BuildException("Error: a value attribute, a nested xmlfragment element" +
				     " must be specified or the deletenodes attribute must be" +
				     " set to true.");
	}
    }

    protected int          mode;
    protected List         nodes;
    protected int          numNodes;
    protected TaskDataIntf taskData;
    protected Project      project;

    public BaseProcessor() {
    }

    public abstract void processElement(Element el);

    public void processAttribute(Attribute attr) {
    }

    public void processText(Text text) {
    }

    public String getMessage() {
	return "Results written to \"" + taskData.getOutfile() + "\"";
    }
    
    public void fini() {
    }

    public String getModeStr() {
	return MODES[mode];
    }

    public int getMode() {
	return mode;
    }

    public void process(List nodes, TaskDataIntf taskData) {
	this.project  = taskData.getProject();
	this.nodes    = nodes;
	this.taskData = taskData;
	this.numNodes = nodes == null ? 0 : nodes.size();
	for (int i = 0; i < numNodes; i++) {
	    Object node = nodes.get(i);
	    if (node instanceof Element) {
		processElement((Element)node);
	    } else if (node instanceof Attribute) {
		processAttribute((Attribute)node);
	    } else if (node instanceof Text) {
		processText((Text)node);
	    } else {
		project.log("Element is neither Element, Attribute nor Text node.",
			    project.MSG_WARN);
	    }
	}
	fini();
    }
    
} // end class BaseProcessor
