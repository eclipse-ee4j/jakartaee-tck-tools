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
import java.util.List;
import java.util.ArrayList;

/**
 * The <code>JavadocMethod</code> class represents a method contained
 * within an assertion.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell - CTS</a>
 * @version 1.0
 */
public class JavadocMethod {

    private String name;
    private String returnType;
    private List   parameters      = new ArrayList(); // String elements
    private String thrownException;

    /**
     * Creates a new <code>JavadocMethod</code> instance from the content of the
     * specified XML element.
     *
     * @param methodElem an <code>Element</code> value containing the data to
     *        init this object with.
     */
    public JavadocMethod(Element methodElem) {
	parseAttributes(methodElem);
	parseContent(methodElem);
    }

    private void parseAttributes(Element methodElem) {
	name = methodElem.getAttributeValue(Globals.NAME_ATR);
	returnType = methodElem.getAttributeValue(Globals.RETURN_TYPE_ATR);
    }

    private void parseContent(Element methodElem) {
	Element parametersElem = methodElem.getChild(Globals.PARAMETERS_TAG);
	if (parametersElem != null) {
	    Globals.addChildrenToList(this.parameters, parametersElem,
				      Globals.PARAMETER_TAG);
	}
	Element throwElem = methodElem.getChild(Globals.THROW_TAG);
	if (throwElem != null) {
	    thrownException = throwElem.getTextNormalize();
	}
    }

    /* Accessors */

    public String getName()            { return name; }
    public String getReturnType()      { return returnType; }
    public String getThrownException() { return thrownException; }
    public String[] getParameters() {
	return (String[])(parameters.toArray(new String[parameters.size()]));
    }

    /**
     * The <code>toXML</code> method returns an XML element representing this
     * object.
     *
     * @return an <code>Element</code> value containing the instance data of
     *         this object
     */
    public Element toXML() {
	Element methodElement = new Element(Globals.METHOD_TAG);
	methodElement.setAttribute(Globals.NAME_ATR, name);
	methodElement.setAttribute(Globals.RETURN_TYPE_ATR, returnType);
	if (parameters.size() > 0) {
	    methodElement.addContent(createParameters());
	}
	if (thrownException != null) {
	    methodElement.addContent(new Element(Globals.THROW_TAG)
		.setText(thrownException));
	}
	return methodElement;
    }

    private Element createParameters() {
	return Globals.createElementListString(parameters,
					       Globals.PARAMETERS_TAG,
					       Globals.PARAMETER_TAG);
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
	String returnTypeNorm;
	String returnTypeThatNorm;
	String thrownExceptionNorm;
	String thrownExceptionThatNorm;

	if (thatObj == null || thatObj.getClass() != this.getClass()) {
	    return result;
	}
	JavadocMethod that = (JavadocMethod)thatObj;

	boolean ignoreReturnPkg = Boolean.getBoolean("ignore.return.pkg");
	boolean ignoreExceptionPkg = Boolean.getBoolean("ignore.exception.pkg");

	if (ignoreReturnPkg) {
	    returnTypeNorm = peelOffPackageName(returnType);
	    returnTypeThatNorm = peelOffPackageName(that.getReturnType());
	} else {
	    returnTypeNorm = returnType;
	    returnTypeThatNorm = that.getReturnType();
	}

	if (ignoreExceptionPkg) {
	    thrownExceptionNorm = peelOffPackageName(thrownException);
	    thrownExceptionThatNorm = peelOffPackageName(that.getThrownException());
	} else {
	    thrownExceptionNorm = thrownException;
	    thrownExceptionThatNorm = that.getThrownException();
	}

	result = ((name == null) ? that.getName() == null : name.equals(that.getName()))
	    && ((returnTypeNorm == null) ? returnTypeThatNorm == null
		: returnTypeNorm.equals(returnTypeThatNorm))
	    && ((thrownExceptionNorm == null) ? thrownExceptionThatNorm == null
		: thrownExceptionNorm.equals(thrownExceptionThatNorm))
	    && (parametersEqual(that));
	return result;
    }

    public int hashCode() {
	int result = 17;
	result = 37 * result + name.hashCode();
	result = 37 * result + returnType.hashCode();
	if (thrownException != null)
	    result = 37 * result + thrownException.hashCode();
	if (parameters != null)
	    result = 37 * result + hash(getParameters());
	return result;
    }

    private int hash(String[] array) {
	int result = 0;
	for (int i = 0; i < array.length; i++) {
	    result += array[i].hashCode();
	}
	return result;
    }

    private boolean parametersEqual(JavadocMethod that) {
	String[] thisParams = this.getParameters();
	String[] thatParams = that.getParameters();
	boolean result = true;
	boolean ignoreParamPkg = Boolean.getBoolean("ignore.param.pkg");

	if (thisParams.length != thatParams.length) {
	    result = false;
	} else {
            if (ignoreParamPkg) {
		thisParams = peelOffPackageNames(thisParams);
		thatParams = peelOffPackageNames(thatParams);
	    }
	    for (int i = 0; i < thisParams.length; i++) {
		if (!thisParams[i].equals(thatParams[i])) {
		    result = false;
		    break;
		}
	    }
	}
	return result;
    }

    private String peelOffPackageName(String name) {
	if (name == null) return null;
	String result;
	int    index = name.lastIndexOf(".");
	if (index != -1) {
	    result = name.substring(index + 1);
	} else {
	    result = name;
	}
	return result;
    }

    private String[] peelOffPackageNames(String[] names) {
	int length = (names != null) ? names.length : 0;
	String[] result = new String[length];
	for (int i = 0; i < length; i++) {
	    result[i] = peelOffPackageName(names[i]);
	}
	return result;
    }

    /**
     * The <code>toString</code> method returns a string representation of
     * this object
     *
     * @return a <code>String</code> representing this object
     */
    public String toString() {
	String nl = Globals.NEW_LINE;
	StringBuffer buf = new StringBuffer("\t\tname=" + name + nl
					    + "\t\treturnType=" + returnType + nl
					    + "\t\texception=" + thrownException + nl);
	buf.append("\t\t" + Globals.toStringArray(parameters, "param"));
	return buf.toString();
    }
    
}
