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

package com.sun.cts.api.data;

import java.util.List;
import java.util.Arrays;
import org.jdom.*;

/**
 * The <code>Globals</code> class contains the element and attribute names
 * of the javadoc assertion DTD.  This is a complete list of all element
 * and attribute values even though the assertion merge tool will only
 * use a subset of these tags.  No instances of this class should be
 * created.  This is also a good place to dump global utility type methods
 * as well as global values.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell - CTS</a>
 * @version 1.0
 */
public final class Globals {

    private Globals(){} // allow no instances of this class

    public static final String NEW_LINE              =
	System.getProperty("line.separator", "\n");
    public static final String NL                    =
	NEW_LINE; // alias for NEW_LINE
    public static final String SYSTEM_ID             =
	"http://dummy.domain.com/CTS/XMLassertions/dtd/javadoc_assertions.dtd";

    public static final String JAVADOC_TAG           = "javadoc";
    public static final String NEXT_AVAILABLE_ID_TAG = "next-available-id";
    public static final String PREVIOUS_ID_TAG       = "previous-id";
    public static final String TECHNOLOGY_TAG        = "technology";
    public static final String MODIFIED_TAG          = "modified";
    public static final String ID_TAG                = "id";
    public static final String NAME_TAG              = "name";
    public static final String VERSION_TAG           = "version";
    public static final String ASSERTIONS_TAG        = "assertions";
    public static final String ASSERTION_TAG         = "assertion";
    public static final String REQUIRED_ATR          = "required";
    public static final String IMPL_SPEC_ATR         = "impl-spec";
    public static final String STATUS_ATR            = "status";
    public static final String TESTABLE_ATR          = "testable";
    public static final String DESCRIPTION_TAG       = "description";
    public static final String KEYWORDS_TAG          = "keywords";
    public static final String KEYWORD_TAG           = "keyword";
    public static final String PACKAGE_TAG           = "package";
    public static final String CLASS_INTERFACE_TAG   = "class-interface";
    public static final String METHOD_TAG            = "method";
    public static final String NAME_ATR              = "name";
    public static final String RETURN_TYPE_ATR       = "return-type";
    public static final String PARAMETERS_TAG        = "parameters";
    public static final String PARAMETER_TAG         = "parameter";
    public static final String THROW_TAG             = "throw";
    public static final String FIELD_TAG             = "field";
    public static final String TYPE_ATR              = "type";
    public static final String COMMENT_TAG           = "comment";
    public static final String DEPENDS_TAG           = "depends";
    public static final String DEPEND_TAG            = "depend";


    /**
     * The <code>toStringArray</code> method is a utility method used to
     * create a string representation of a list that contains String
     * elements.
     *
     * @param values a <code>List</code> value holding the String elements
     * @param name a <code>String</code> value appended to each element value
     * @return a <code>String</code> representing all list elements within
     *         the returned string
     */
    public static String toStringArray(List values, String name) {
	int numValues = (values == null) ? 0 : values.size();
	String prefix = (name == null) ? "" : name;
	StringBuffer buf = new StringBuffer("[");
	for (int i = 0; i < numValues; i++) {
	    Object obj = values.get(i);
	    buf.append(prefix + "[" + i + "]" + "=" + obj.toString());
	    if (i < numValues - 1) {
		buf.append(",");
	    }
	}
	buf.append("]");
	return buf.toString();
    }

    /**
     * The <code>toStringArray</code> method is a utility method used to
     * create a string representation of a list that contains String
     * elements.
     *
     * @param values a <code>String[]</code> value holding the String elements
     * @param name a <code>String</code> value appended to each element value
     * @return a <code>String</code> representing all list elements within
     *         the returned string
     */
    public static String toStringArray(String[] values, String name) {
	return (toStringArray(Arrays.asList(values), name));
    }

    /**
     * The <code>addChildrenToList</code> method takes the child nodes
     * nodes of an XML element and adds their text values to the specified
     * list.
     *
     * @param list a <code>List</code> value where the text values of the
     *        child nodes is stored
     * @param elem an <code>Element</code> value that contains the child
     *        XML elements
     * @param childName a <code>String</code> value that is the name of
     *        the child XML elements
     */
    public static void addChildrenToList(List list, Element elem, String childName) {
	List childElems = elem.getChildren(childName);
	int numChildren = (childElems == null) ? 0 : childElems.size();
	for (int i = 0; i < numChildren; i++) {
	    String childText = ((Element)childElems.get(i)).getTextNormalize();
	    list.add(childText);
	}
    }

    /**
     * Describe <code>createElementListString</code> method here.
     *
     * @param list a <code>List</code> value to get the child values from
     * @param parentName a <code>String</code> value the name of the containing element
     * @param childName a <code>String</code> value the name of the child elements
     * @return an <code>Element</code> value that is the containing element
     */
    public static Element createElementListString(List list,
						  String parentName,
						  String childName) {
	Element parentElement = new Element(parentName);
	String childText = null;
	Element childElement = null;
	int numChildren = (list == null) ? 0 : list.size();
	for (int i = 0; i < numChildren; i++) {
	    childText = (String)list.get(i);
	    childElement = new Element(childName).setText(childText);
	    parentElement.addContent(childElement);
	}
	return parentElement;
    }

}
