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

import java.io.StringReader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import java.util.List;

/**
 * This ant task represents an XML fragment.  This fragment can
 * be added to an XML document using the ModifyXML task.
 */
public class XMLFragment {

    private String xmlfragment;
    private String xmlfragmentattribute;
    private Element fragmentRoot;
    private boolean ignoreroot;

    public void addText(String text) {
	//	System.err.println("XMLFragment.addText() = \"" + text + "\"");
	this.xmlfragment = text;
    }

    public void setXmlfragmentattribute (String value) {
	//	System.err.println("XMLFragment.setXmlfragmentattribute() = \"" + value + "\"");
	this.xmlfragmentattribute = value;
    }

    public void setIgnoreroot(boolean value) {
	this.ignoreroot = value;
    }

    /*
     * Return an array of elements that can be added to a parent node.
     * If the ignoreroot value is set to false the value returned by this
     * method is a single element array with the root element of the
     * fragment in it.  If the ignoreroot value is set to true the
     * value returned by this method is an Element array with one array
     * element for each child parented under the specified fragment's root
     * element. This allows people to specify a well formed fragment but
     * only add the child elements if desired.  Example:
     *
     *  foo.xml:
     *    <foo>
     *    </foo>
     *
     *     Adding this fragment:
     *        <classes>
     *          <class>a.class</class>
     *          <class>b.class</class>
     *          <class>c.class</class>
     *          <class>d.class</class>
     *        </classes>
     *
     *     to foo.xml with ignoreroot set to false results in:
     *
     *      <foo>
     *        <classes>
     *          <class>a.class</class>
     *          <class>b.class</class>
     *          <class>c.class</class>
     *          <class>d.class</class>
     *        </classes>
     *      </foo>	     
     * 
     *     Adding the same fragment to foo.xml with ignoreroot set to true
     *     results in:
     *      <foo>
     *          <class>a.class</class>
     *          <class>b.class</class>
     *          <class>c.class</class>
     *          <class>d.class</class>
     *      </foo>	     
     * 
     */
    public Element[] getElement() {
	Element[] result;
	Element root = (Element)(this.fragmentRoot.clone());
	if (ignoreroot) {
	    List children = root.getChildren();
	    result = new Element[children.size()];
	    for (int i = 0; i < result.length; i++) {
		Element el = (Element)(children.get(i));
		result[i] = (Element)(el.clone());
	    }
	} else {
	    result = new Element[] { root };
	}
	return result;
    }

    public String getContainingElementName() {
	return fragmentRoot.getQualifiedName();
    }

    public void init() throws BuildException {
	if ((xmlfragmentattribute ==  null || xmlfragmentattribute.length() == 0) &&
	    (xmlfragment ==  null || xmlfragment.length() == 0))
	    {
		throw new BuildException("The xmlfragmentattribute must contain an xml fragment OR " +
					 "the xmlfragment element content must contain an xml fragment");
	    }
	try {
	    SAXBuilder builder = new SAXBuilder(false);
	    StringReader reader =  null;
	    if (xmlfragmentattribute != null) {
		reader = new StringReader(xmlfragmentattribute);
	    } else {
		reader = new StringReader(xmlfragment);
	    }
	    Document doc = builder.build(reader);
	    Element rootElement = doc.getRootElement();
	    this.fragmentRoot = (Element)(rootElement.clone());
	} catch (Exception e) {	    
	    throw new BuildException(e);
	}
    }

} // end class XMLFragment
