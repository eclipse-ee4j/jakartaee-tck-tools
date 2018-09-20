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
import org.jdom.Attribute;
import org.jdom.Text;
import org.jdom.filter.ContentFilter;

public class ReadProcessor extends BaseProcessor {

    private StringBuffer buf = new StringBuffer();
    private boolean      firstInsert;
    private String       separator;

    public ReadProcessor() {
	mode = READ;
	separator = System.getProperty("PROPERTY_DELIMITER", "##");
	firstInsert = true;
    }

    private void addToBuf(String str) {
	if (str == null || str.length() == 0) {
	    return;
	}
	if (firstInsert) {
	    firstInsert = false;
	} else {
	    buf.append(separator);
	}
	buf.append(str);
    }

    public void processElement(Element el) {
	List allTextNodes = el.getContent
	    (new ContentFilter(ContentFilter.TEXT | ContentFilter.ELEMENT));
	int numNodes = allTextNodes == null ? 0 : allTextNodes.size();
	for (int i = 0; i < numNodes; i++) {
	    Object o = allTextNodes.get(i);
	    if (o instanceof Text) { 
		Text t = (Text)o;
		addToBuf(t.getTextNormalize());
	    } else if (o instanceof Element) {
		Element e = (Element)o;
		processElement(e);
	    } else {
		project.log("JDOM filter failed in ReadProcessor.processElement()",
			    project.MSG_ERR);
	    }
	}
    }

    public void processAttrprocessTextibute(Attribute attr) {
	addToBuf(attr.getValue());
    }
    
    public void processText(Text text) {
	addToBuf(text.getTextNormalize());
    }
    
    public String getMessage() {
	return "Results loaded into property \"" + taskData.getProperty() + "\"";
    }
    
    public void fini() {
	project.setNewProperty(taskData.getProperty(), buf.toString());
    }
}
