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
import com.sun.cts.common.IDIntf;

/**
 * The <code>JavadocAssertion</code> class represents a Javadoc assertion.
 * These assertions are auto-generated using the assert-gen script and
 * associated tools.  This class is simply an abject representation of
 * the assertion.  This class must conform to the assertion element of
 * the javadoc_assertion DTD.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell-CTS</a>
 * @version 1.0
 */
public class JavadocAssertion implements IDIntf {

    public static final String ACTIVE     = "active";
    public static final String DEPRECATED = "deprecated";
    public static final String REMOVED    = "removed";
    public static final String DEFAULT_STATUS_ATTR_VALUE = ACTIVE;

    /**
     * The <code>required</code> variable denotes whether the assertion
     * is required or optional.
     */
    private Boolean required = Boolean.TRUE;
    /**
     * The <code>implSpec</code> variable denotes .
     */
    private Boolean implSpec = Boolean.FALSE;
    /**
     * The variable <code>status</code> represents the status attribute
     * of this assertion.  The status attribute must have one of the
     * following values: active | deprecated | removed
     */
    private String status = DEFAULT_STATUS_ATTR_VALUE;
    /**
     * The variable <code>testable</code> represents the testable attribute
     * of this assertion.  A test is either testable or not testable.
     */
    private Boolean testable = Boolean.TRUE;
    /**
     * The variable <code>modified</code> denotes whether or not
     * this assertions has been altered from its original form.
     * Normally this variable will be false but it may be set
     * to true by the assertion merge tool if an assertion changes
     * from one version of the source to the next.
     */
    private boolean modified;
    /**
     * The variable <code>id</code> represents an assertion ID.
     */
    private String id;
    /**
     * The variable <code>description</code> represents the assertion description
     * string.
     */
    private String description;
    /**
     * The <code>keywords</code> variable holds a list of Strings.  Each
     * string is a keyword associated with the assertion.
     */
    private List keywords = new ArrayList();
    /**
     * The variable <code>assertionPackage</code> represents the assertion package.
     */
    private String assertionPackage;
    /**
     * The variable <code>classInterface</code> represents the class or interface
     * that the assertion method lives in.
     */
    private String classInterface;
    /**
     * The variable <code>method</code> represents the method of
     * this assertion.
     */
    private JavadocMethod method;
    /**
     * The variable <code>field</code> represents a static variable.
     */
    private JavadocField field;
    /**
     * The variable <code>comment</code>  represents a free form comment
     * about this assertion.
     */
    private String comment;
    /**
     * The variable <code>dependAssertions</code> represents a list of assertion
     * IDs that this assertion depends on.  Each list element is a string.
     */
    private List dependAssertions = new ArrayList();

    /**
     * Creates a new <code>JavadocAssertion</code> instance from the specified
     * XML element.
     *
     * @param assertionElement an <code>Element</code> value with the content
     *        to be placed in this object
     */
    public JavadocAssertion(Element assertionElement) {
	parseAttributes(assertionElement);
	parseContent(assertionElement);
    }

    private void parseAttributes(Element assertionElement) {
	Attribute attribute = null;
	if ((attribute = assertionElement.getAttribute
	     (Globals.REQUIRED_ATR)) != null) {
	    required = Boolean.valueOf(attribute.getValue());
	}
	if ((attribute = assertionElement.getAttribute
	     (Globals.IMPL_SPEC_ATR)) != null) {
	    implSpec = Boolean.valueOf(attribute.getValue());
	}
	if ((attribute = assertionElement.getAttribute
	     (Globals.STATUS_ATR)) != null) {
	    status = attribute.getValue();
	}
	if ((attribute = assertionElement.getAttribute
	     (Globals.TESTABLE_ATR)) != null) {
	    testable = Boolean.valueOf(attribute.getValue());
	}
    }

    private void parseContent(Element assertionElement) {
	Element modifiedElement = assertionElement.getChild(Globals.MODIFIED_TAG);
	if (modifiedElement != null) {
	    modified = true;
	}
	id = assertionElement.getChild(Globals.ID_TAG).getTextNormalize();
	description = assertionElement.getChild(Globals.DESCRIPTION_TAG).getTextNormalize();
	Element keywordsElement = assertionElement.getChild(Globals.KEYWORDS_TAG);
	if (keywordsElement != null) {
	    parseKeywords(keywordsElement);
	}
	assertionPackage = assertionElement.getChild(Globals.PACKAGE_TAG).getTextNormalize();
	classInterface =assertionElement.getChild(Globals.CLASS_INTERFACE_TAG).getTextNormalize();
	Element methodElement = assertionElement.getChild(Globals.METHOD_TAG);
	if (methodElement != null) {
	    parseMethod(methodElement);
	} else {
	    Element fieldElement = assertionElement.getChild(Globals.FIELD_TAG);
	    parseField(fieldElement);
	}
	Element commentElement = assertionElement.getChild(Globals.COMMENT_TAG);
	if (commentElement != null) {
	    comment = commentElement.getTextNormalize();
	}
	Element dependsElement = assertionElement.getChild(Globals.DEPENDS_TAG);
	if (dependsElement != null) {
	    parseDepends(dependsElement);
	}
    }

    private void parseKeywords(Element keywordsElement) {
	Globals.addChildrenToList(this.keywords, keywordsElement, Globals.KEYWORD_TAG);
    }
    
    private void parseMethod(Element methodElem) {
	method = new JavadocMethod(methodElem);
	field = null;
    }
    
    private void parseField(Element fieldElem) {
	field = new JavadocField(fieldElem);
	method = null;
    }
    
    private void parseDepends(Element dependsElem) {
	Globals.addChildrenToList(this.dependAssertions,
				  dependsElem, Globals.DEPEND_TAG);	
    }
    
    /* Accessor methods */

    public boolean       getRequired()         { return required.booleanValue(); }
    public boolean       getImplSpec()         { return implSpec.booleanValue(); }
    public String        getStatus()           { return status; }
    public boolean       getTestable()         { return testable.booleanValue(); }
    public boolean       getModified()         { return modified; }
    public String        getID()               { return id; }
    public String        getDescription()      { return description; }
    public String        getAssertionPackage() { return assertionPackage; }
    public String        getClassInterface()   { return classInterface; }
    public JavadocMethod getMethod()           { return method; }
    public JavadocField  getField()            { return field; }
    public String        getComment()          { return comment; }
    public String[] getDependAssertions() {
	return (String[])(dependAssertions.toArray
			  (new String[dependAssertions.size()]));
    }
    public String[] getKeywords() {
	return (String[])(keywords.toArray(new String[keywords.size()]));
    }    

    public boolean isDeprecated() {
	return DEPRECATED.equals(getStatus());
    }

    /* Modifiers */

    public void setRequired(boolean bool) {
	required = (bool == true) ? Boolean.TRUE : Boolean.FALSE;
    }
    public void setImplSpec(boolean bool) {
	implSpec = (bool == true) ? Boolean.TRUE : Boolean.FALSE;
    }
    public void setTestable(boolean bool) {
	testable = (bool == true) ? Boolean.TRUE : Boolean.FALSE;
    }
    public void setStatus(String value) {
	if (value != null &&
	    (value.equals(ACTIVE) || value.equals(REMOVED) ||
	     value.equals(DEPRECATED))) {
	    status = value;
	}
    }
    public void setModified(boolean bool) {
	modified = bool;
    }
    public void setID(String value) {
	this.id = value;
    }    
    
    /* Instance Methods */

    /**
     * The <code>equals</code> method returns true if this object and the specified
     * object contain the same value within their instance fields.
     *
     * @param thatObj an <code>Object</code> to compare to this object
     * @return a <code>boolean</code>, true if the specified object is equal else
     *         false.
     */
    public boolean equals(Object thatObj) {
	boolean almostEqual = equalsExceptDescription(thatObj);
	if (almostEqual) {
	    JavadocAssertion that = (JavadocAssertion)thatObj;
	    return ((description == null) ? that.getDescription() == null
		    : description.equals(that.getDescription()));
	} else {
	    return false;
	}
    }
    
    /**
     * Describe <code>equalsExceptDescription</code> method returns true if the
     * specified object is identical to this object except for the comments
     * instance field.
     *
     * @param thatObj an <code>Object</code> to compare to this object
     * @return a <code>boolean</code> value, true if the objects are equal except
     *         for the comment instance field else false
     */
    public boolean equalsExceptDescription(Object thatObj) {
	boolean result = false;
	if (thatObj == null && thatObj.getClass() != this.getClass()) {
	    return result;
	}
	JavadocAssertion that = (JavadocAssertion)thatObj;
	result = ((assertionPackage == null) ? that.getAssertionPackage() == null
		  : assertionPackage.equals(that.getAssertionPackage()))
	    && ((classInterface == null) ? that.getClassInterface() == null
		: classInterface.equals(that.getClassInterface()))
	    && ((method == null) ? that.getMethod() == null
		: method.equals(that.getMethod()))
	    && ((field == null) ? that.getField() == null
		: field.equals(that.getField()));
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
	result = 37 * result + assertionPackage.hashCode();
	result = 37 * result + classInterface.hashCode();
	if (method != null)
	    result = 37 * result + method.hashCode();
	if (field != null)
	    result = 37 * result + field.hashCode();
	return result;
    }

    /**
     * The <code>toXML</code> method returns an XML element containsing the
     * content of this JavadocAssertion object.
     *
     * @return an XML <code>Element</code> represeting this JavadocAssertion
     *         object
     */
    public Element toXML() {
	Element assertionElement = new Element(Globals.ASSERTION_TAG);
	createAttributes(assertionElement);
	createContent(assertionElement);
	return assertionElement;
    }

    private void createAttributes(Element assertionElement) {
	assertionElement.setAttribute(Globals.REQUIRED_ATR, required.toString());
	assertionElement.setAttribute(Globals.IMPL_SPEC_ATR, implSpec.toString());
	assertionElement.setAttribute(Globals.STATUS_ATR, status);
	assertionElement.setAttribute(Globals.TESTABLE_ATR, testable.toString());
    }

    private void createContent(Element assertionElement) {
	if (modified == true) {
	    assertionElement.addContent(new Element(Globals.MODIFIED_TAG));
	}
	assertionElement.addContent
	    (new Element(Globals.ID_TAG).setText(id));
	assertionElement.addContent
	    (new Element(Globals.DESCRIPTION_TAG).setText(description));
	if (keywords.size() > 0) {
	    assertionElement.addContent(createKeywords());
	}
	assertionElement.addContent
	    (new Element(Globals.PACKAGE_TAG).setText(assertionPackage));
	assertionElement.addContent
	    (new Element(Globals.CLASS_INTERFACE_TAG).setText(classInterface));
	if (method != null) {
	    assertionElement.addContent(method.toXML());
	} else if (field != null) {
	    assertionElement.addContent(field.toXML());
	} else {
	    System.err.println("ERROR, no method or field element defined,"
			       + " must not have validated against the DTD");
	}
	if (comment != null) {
	    assertionElement.addContent
		(new Element(Globals.COMMENT_TAG).setText(comment));
	}
	if (dependAssertions.size() > 0) {
	    assertionElement.addContent(createDependsList());
	}
    }

    private Element createKeywords() {
	return Globals.createElementListString(keywords,
					       Globals.KEYWORDS_TAG,
					       Globals.KEYWORD_TAG);
    }

    private Element createDependsList() {
	return Globals.createElementListString(dependAssertions,
					       Globals.DEPENDS_TAG,
					       Globals.DEPEND_TAG);
    }
    
    /**
     * The <code>toString</code> method returns a string representation
     * of this JavadocAssertion object.
     *
     * @return a <code>String</code> representing this object
     */
    public String toString() {
	String nl = Globals.NEW_LINE;
	StringBuffer buf = new StringBuffer(nl + "\trequired=" + required + nl
					    + "\timplSpec=" + implSpec + nl
					    + "\tstatus=" + status + nl
					    + "\ttestable=" + testable + nl
					    + "\tid=" + id + nl
					    + "\tdescription=" + description + nl
					    + "\tpackage=" + assertionPackage + nl
					    + "\tclass=" + classInterface + nl);
	if (method != null) {
	    buf.append("\tMethod" + nl);
	    buf.append("\t------" + nl + method);
	} else {
	    buf.append("\tField" + nl);
	    buf.append("\t-----" + nl + "\t\t" + field);
	}
	buf.append(nl + "\tcomment=" + comment + nl);
	if (dependAssertions.size() > 0) {
	    buf.append("\t" + Globals.toStringArray(dependAssertions, "depend") + nl);
	}
	if (keywords.size() > 0) {
	    buf.append("\t" + Globals.toStringArray(keywords, "keyword") + nl);
	}
	return buf.toString();
    }
}
