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

import org.jdom.*;

/**
 * The <code>JavadocField</code> class represents a static field
 * within an assertion.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell - CTS</a>
 * @version 1.0
 */
public class JavadocField {

    private String name;
    private String type;

    /**
     * Creates a new <code>JavadocField</code> instance with the same content as the
     * specified XML element.
     *
     * @param fieldElem an <code>Element</code> value containing the data to init
     *        this object with
     */
    public JavadocField(Element fieldElem) {
	name = fieldElem.getAttributeValue(Globals.NAME_ATR);
	type = fieldElem.getAttributeValue(Globals.TYPE_ATR);
    }
    
    /* Accessors */

    public String getName() { return name; }
    public String getType() { return type; }

    /**
     * The <code>toXML</code> method returns an XML element representing this
     * object.
     *
     * @return an <code>Element</code> value containing the instance data of
     *         this object
     */
    public Element toXML() {
	Element fieldElement = new Element(Globals.FIELD_TAG);
	fieldElement.setAttribute(Globals.NAME_ATR, name);
	fieldElement.setAttribute(Globals.TYPE_ATR, type);
	return fieldElement;
    }

    /**
     * The <code>equals</code> method returns true of the specified object
     * contains the same instance data as this object.
     *
     * @param thatObj an <code>Object</code> to compare to this object
     * @return a <code>boolean</code> value, true if the objects are equal
     *         else false
     */
    public boolean equals(Object thatObj) {
	boolean result = false;
	if (thatObj == null || thatObj.getClass() != this.getClass()) {
	    return result;
	}
	JavadocField that = (JavadocField)thatObj;
	result = ((name == null) ? that.getName() == null : name.equals(that.getName()))
	    && ((type == null) ? that.getType() == null : type.equals(that.getType()));
	return result;
    }

    /**
     * The <code>hashCode</code> method returns th hash value for this
     * object.
     *
     * @return an <code>int</code> representing this object hash value
     */
    public int hashCode() {
	int result = 17;
	result = 37 * name.hashCode();
	result = 37 * type.hashCode();
	return result;
    }

    /**
     * The <code>toString</code> method returns a string representation of
     * this object
     *
     * @return a <code>String</code> representing this object
     */
    public String toString() {
	return "[name=" + name + ", " + "type=" + type + "]" + Globals.NEW_LINE;
    }

}
