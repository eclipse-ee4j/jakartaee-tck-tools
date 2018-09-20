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
import org.jdom.Element;
import org.jdom.filter.ContentFilter;
import org.jdom.Namespace;

public class AddProcessor extends BaseProcessor {

    public AddProcessor() {
	mode = ADD;
    }

    public void processElement(Element el) {
       	Element[] children = taskData.getFragment().getElement();
	for (int i = 0; i < children.length; i++) {
	    Element child = children[i];
	    project.log("*** Child Element \"" + child, project.MSG_VERBOSE);        
	    el.addContent(child);
	    setNamespace(child);
	}
    }
    
    private void setNamespace0(Element el) {
	String elURI = el.getNamespaceURI();
	if (elURI.length() > 0) {
	    // this element has a namespace declaration so leave the element
	    // alone and dump the namespace to a verbose console
	    project.log("Element \"" + el.getName() + "\" already has namespace defined",
			project.MSG_VERBOSE);
	} else {
	    /* We may want to simply get the parent element's namespace
	     * and leave it at that, for now see if the user specified a default
	     * namespace first
	     */
	    // get the default namespace since the user specified it
	    Namespace ns = taskData.getDefaultNamespace();
	    if (ns == null) { // no default, so get the parent's namespace
                Element parentEl = (Element)el.getParent();
		ns = parentEl.getNamespace(); // get parent namespace
	    }
	    el.setNamespace(ns);
	    project.log("Element \"" + el.getName() + "\" added to namespace \"" +
			ns.getURI() + "\"", project.MSG_VERBOSE);
	}
    }
    
    private void setNamespace(Element el) {    
	setNamespace0(el);
	List content = el.getContent(new ContentFilter(ContentFilter.ELEMENT));
	int numEls = content.size();
	for (int i = 0; i < numEls; i++) {
	    Element e = (Element)content.get(i);
	    setNamespace(e);
	}
    }
    
}
