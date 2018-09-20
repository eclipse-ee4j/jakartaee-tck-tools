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

import org.apache.tools.ant.BuildException;

/**
 * This ant taks represents a namespace.  The namespace has a
 * prefix and a URI.  This task is used in conjunction with
 * the ModifyXML task which needs to map prefixes to namespace
 * URIs so XPath expressions have meaning within the XML
 * instances being processed.
 */
public class NameSpace {
    
    private String  prefix;
    private String  uri;
    private boolean docdefault;
    
    public void setPrefix(String prefix) {
	this.prefix = prefix;
    }

    public void setUri(String uri) {
	this.uri = uri;
    }

    public void setDocdefault(boolean docdefault) {
	this.docdefault = docdefault;
    }

    public String getPrefix() {
	return prefix;
    }

    public String getUri() {
	return uri;
    }

    public boolean isDocdefault() {
	return docdefault;
    }

    public void init() throws BuildException {
	if (prefix == null || prefix.length() == 0) {
	    throw new BuildException("Namespace elements must have a prefix attribute.");
	}
	if (uri == null || uri.length() == 0) {
	    throw new BuildException("Namespace elements must have a URI attribute.");
	}
    }

    public String toString() {
	return "[" + prefix + ", " + uri + ", docdefault " + docdefault + "]";
    }

} // end class NameSpace
