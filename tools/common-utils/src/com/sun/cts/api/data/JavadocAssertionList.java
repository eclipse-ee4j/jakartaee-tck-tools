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

import java.util.*;
import org.jdom.*;
import com.sun.cts.common.AssertionIDComparator;

/**
 * The <code>JavadocAssertionList</code> class represents a Javadoc (API)
 * assertion file in object form.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell - CTS</a>
 * @version 1.0
 */
public class JavadocAssertionList implements Cloneable {

    private int      nextAvailableID;
    private int      previousID;
    private String   technology;
    private String   id;
    private String   name;
    private String   version;
    private List     originalAssertions = new ArrayList();
    private List     assertions         = new ArrayList();
    private Document doc;

    public Object clone() throws CloneNotSupportedException {
	JavadocAssertionList result = (JavadocAssertionList)super.clone();
	result.originalAssertions = new ArrayList();
	result.assertions         = new ArrayList();
	result.doc                = null;
	return result;
    }

    /**
     * Creates a new <code>JavadocAssertionList</code> instance from the specified
     * XML document.
     *
     * @param doc a <code>Document</code> object representing the input XML file
     */
    public JavadocAssertionList(Document doc) {
	this.doc = doc;
	parseAssertionList(); // parse the assertion list attributes
    }

    private void parseAssertionList() {
	Element root = doc.getRootElement();
	this.nextAvailableID = Integer.parseInt
	    (root.getChild(Globals.NEXT_AVAILABLE_ID_TAG).getTextNormalize());
	this.previousID = Integer.parseInt
	    (root.getChild(Globals.PREVIOUS_ID_TAG).getTextNormalize());
	this.technology = root.getChild(Globals.TECHNOLOGY_TAG).getTextNormalize();
	this.id = root.getChild(Globals.ID_TAG).getTextNormalize();
	this.name = root.getChild(Globals.NAME_TAG).getTextNormalize();
	this.version = root.getChild(Globals.VERSION_TAG).getTextNormalize();
	Element assertionsElement = root.getChild(Globals.ASSERTIONS_TAG);
	if (assertionsElement != null) {
	    populateList(assertionsElement);
	}
    }

    private void populateList(Element assertionsElement) {
	List assertionElements = assertionsElement.getChildren(Globals.ASSERTION_TAG);
	int numAssertions = assertionElements.size();
	for (int i = 0; i < numAssertions; i++) {
	    Element assertionElement = (Element)assertionElements.get(i);
	    createAssertion(assertionElement);
	}
    }

    private void createAssertion(Element assertionElement) {
	JavadocAssertion assertionObj = new JavadocAssertion(assertionElement);
	this.originalAssertions.add(assertionObj);
	this.assertions.add(assertionObj);
    }




    /**
     * The <code>createDocument</code> method creates the output XML document.
     *
     * @return a <code>Document</code> value containing the content of this
     *         assertion list.
     */
    private Document createDocument() {
	DocType docType = new DocType(Globals.JAVADOC_TAG, Globals.SYSTEM_ID);
	Element javadocElement = new Element(Globals.JAVADOC_TAG);
	Document doc = new Document(javadocElement, docType);
	return doc;
    }

    /**
     * The <code>toXML</code> method returns this assertion list as
     * an XML document.
     *
     * @return an XML <code>Document</code> representing this
     *         assertion list
     */
    public Document toXML() {
	Document doc = createDocument();
	Element root = doc.getRootElement();
	root.addContent(new Element(Globals.NEXT_AVAILABLE_ID_TAG)
	    .setText(String.valueOf(nextAvailableID)));
	root.addContent(new Element(Globals.PREVIOUS_ID_TAG).
	    setText(String.valueOf(previousID)));
	root.addContent(new Element(Globals.TECHNOLOGY_TAG).setText(technology));
	root.addContent(new Element(Globals.ID_TAG).setText(id));
	root.addContent(new Element(Globals.NAME_TAG).setText(name));
	root.addContent(new Element(Globals.VERSION_TAG).setText(version));
	root.addContent(createAssertions());
	return doc;
    }

    /**
     * The <code>createAssertions</code> method creates the assertion list
     * that is part of the XML document.
     *
     * @return an <code>Element</code> that is the containing element for
     *         the assertion list
     */
    private Element createAssertions() {
	Element assertionsElement = new Element(Globals.ASSERTIONS_TAG);
	int numAssertions = assertions.size();
	for (int i = 0; i < numAssertions; i++) {
	    JavadocAssertion assertion = (JavadocAssertion)(assertions.get(i));
	    assertionsElement.addContent(assertion.toXML());
	}
	return assertionsElement;
    }


    /* Accessors */

    public int    getNextAvailableID() { return nextAvailableID; }
    public int    getPreviousID()      { return previousID; }
    public String getTechnology()      { return technology; }
    public String getID()              { return id; }
    public String getName()            { return name; }
    public String getVersion()         { return version; }
    public JavadocAssertion[] getOriginalAssertions() {
	JavadocAssertion[] result = new JavadocAssertion[originalAssertions.size()];
	return (JavadocAssertion[])(originalAssertions.toArray(result));
    }
    public JavadocAssertion[] getAssertions() {
	JavadocAssertion[] result = new JavadocAssertion[assertions.size()];
	return (JavadocAssertion[])(assertions.toArray(result));
    }
    
    /* Instance Methods */

    /**
     * The <code>checkPreconditions</code> method ensures that there
     * are no issues with the assertion list before merging begins.
     * Currently, this simply means that none of the assertions are in a
     * modified state (denoted by a modified value set to true).  This
     * should only occur on verified assertion lists since the new
     * assertion lists are autogenerated and should never be marked
     * as modified.
     *
     * @throw Exception when an assertion is considered to be in the
     *        modified state.  Note: to minimize the number of times
     *        users will run this tool to find assertions that are in
     *        a modified state, the method runs across the entire list
     *        of assertions appending their IDs to the exception string
     *        if they are in a modified state.
     */
    public void checkPreconditions() throws Exception {
	String nl = Globals.NEW_LINE;
	boolean tossException = false;
	StringBuffer buf = new StringBuffer();
	JavadocAssertion[] asserts = getAssertions();
	for (int i = 0; i < asserts.length; i++) {
        //System.out.println("assert["+i+"]="+asserts[i].toString());
	    if (asserts[i].getModified() == true) {
		buf.append("\t" + asserts[i].getID() + nl);
		tossException = true;
	    }
	}
	if (tossException) {
	    throw new Exception("Modified Assertions Exist [search for <modified/> in file]"
				+ nl + buf.toString());
	}
    }

    /**
     * The <code>getCommonAssertions</code> method takes a list of assertions
     * (they should be new assertions NOT verified assertions) and places the
     * common assertions into the specified Lists.  If the assertion is common
     * to both this assertion lists and the specified asserton list but the
     * is not deprecated (from new list compared to verified) the assertion
     * is added to the commonAsserts parameter.  If the assertions is common
     * to both lists but has been deprecated from the verified list to the
     * ew list the assertion is added to the depAsserts list.  Both list
     * parameters are out parameters.
     *
     * @param other a <code>JavadocAssertionList</code> value that holds
     *        the new assertion list
     * @param commonAsserts a <code>List</code> value that is updated by
     *        this method.  The non-deprecated commmon assertions are added
     *        to this list.  This is an output parameter.
     * @param depAsserts a <code>List</code> value that is modified by this
     *        method.  All the common deprecated assertions are added to
     *        this list.  This is an output parameter.
     */
    public void getCommonAndDeprecatedAssertions(JavadocAssertionList other,
				    List commonAsserts, List depAsserts) {
	commonAsserts.clear();
	depAsserts.clear();
	if (other == null || other.size() == 0) return;
	int size = this.size();
	for (int i = 0; i < size; i++) {
	    JavadocAssertion assert1 = get(i); // get verified assertion
	    if (other.contains(assert1)) { // if in both verified and new list
		int index = other.indexOf(assert1); // index of new assertion
		JavadocAssertion newAssert = (JavadocAssertion)(other.get(index));
		/*
		 * The assertion is common to both lists so now we check to
		 * see of the verified assertion has been deprecated in the
		 * new assertion list, if so update the assertion accordingly
		 * and it to the appropriate list.
		 */
		if (assert1.getStatus().equals(JavadocAssertion.ACTIVE) &&
		    (newAssert.getStatus().equals(JavadocAssertion.DEPRECATED)))
		    {
			assert1.setStatus(JavadocAssertion.DEPRECATED);
			assert1.setModified(true);
			depAsserts.add(assert1);
		    } else {
			commonAsserts.add(assert1);
		    }
	    }
	}
    }

    /**
     * Describe <code>getModifiedAssertions</code> method here.
     *
     * @param other a <code>JavadocAssertionList</code> value
     * @return a <code>List</code> value
     */
    public List getModifiedAssertions(JavadocAssertionList other) {
	List result = new ArrayList(); // elements are JavadocModifiedAssertions
	if (other == null || other.size() == 0) return result;
	int thisSize  = this.size();
	int otherSize = other.size();
	JavadocAssertion thisAssert  = null;
	JavadocAssertion otherAssert = null;
	for (int i = 0; i < thisSize; i++) {
	    thisAssert = (JavadocAssertion)get(i); // get verified assertion
	    for (int j = 0; j < otherSize; j++) {
		otherAssert = (JavadocAssertion)other.get(j); // get new assertion
		if (thisAssert.equalsExceptDescription(otherAssert)) {
		    JavadocModifiedAssertion modifiedAssertion =
			new JavadocModifiedAssertion(thisAssert, otherAssert);
		    result.add(modifiedAssertion);
		    break;
		}
	    }
	}
	return result;
    }
    
    public boolean contains(JavadocAssertion assertion) {
	return assertions.contains(assertion);
    }

    public int indexOf(JavadocAssertion assertion) {
	return assertions.indexOf(assertion);
    }

    public List getAssertionList() {
	return assertions;
    }

    public JavadocAssertion get(int index) {
	JavadocAssertion assertion = (JavadocAssertion)(assertions.get(index));
	return assertion;
    }

    public void add(JavadocAssertion assertion) {
	assertions.add(assertion);
    }

    public void removeAssertion(int index) {
	assertions.remove(index);
    }
    
    public void removeAssertion(JavadocAssertion assertion) {
	int index = assertions.indexOf(assertion);
	if (index != -1) {
	    assertions.remove(index);
	}
    }

    public void removeAssertions(List assertions) {
	if (assertions == null) {
	    return;
	} else if (assertions == this.assertions) {
	    /*
	     * If the user passes the same list that we are removing
	     * assertions from, simply clear the list.
	     */
	    this.assertions.clear();
	} else {
	    JavadocAssertion assertion = null;
	    int numAssertions = (assertions == null) ? 0 : assertions.size();
	    for (int i = 0; i < numAssertions; i++) {
		assertion = (JavadocAssertion)assertions.get(i);
		removeAssertion(assertion);
	    }
	} // end else 
    }

    public int size() {
	return ((assertions == null) ? 0 : assertions.size());
    }

    /**
     * The <code>toString</code> method returns a string representation of
     * this object.
     *
     * @return a <code>String</code> value representing the content of
     *         this object
     */
    public String toString() {
	String nl = Globals.NEW_LINE;
	StringBuffer buf = new StringBuffer("ASSERTION LIST" + nl
					    + "--------------" + nl
					    + "nextID=" + nextAvailableID + nl
					    + "previousID=" + previousID + nl
					    + "technology=" + technology + nl
					    + "id=" + id + nl
					    + "name=" + name + nl
					    + "version=" + version + nl);
	buf.append("Original Assertions" + nl);
	buf.append("-------------------" + nl);
  	buf.append(Globals.toStringArray(originalAssertions, "originalAssertion") + nl);
	buf.append("Assertions" + nl);
	buf.append("----------" + nl);
 	buf.append(Globals.toStringArray(assertions, "Assertion") + nl);
	return buf.toString();
    }

    /**
     * The <code>getMatchingAssertions</code> method returns a list of JavadocAssertion
     * instances that have the same method signature as the specified JavadocAssertion.
     *
     * @param assertion a <code>JavadocAssertion</code> value to be matched in this
     *                  assertion list
     * @return a <code>List</code> value containing the matching JavadocAssertion
     *         instances
     */
    public List getMatchingAssertions(JavadocAssertion assertion) {
	List result = new ArrayList();
	if (assertion == null) {
	    return result;
	}
	int numAssertions = assertions.size();
	JavadocAssertion currentAssertion = null;
	for (int i = 0; i < numAssertions; i++) {
	    currentAssertion = (JavadocAssertion)assertions.get(i);
	    if (assertion.equalsExceptDescription(currentAssertion)) {
		result.add(currentAssertion);
	    }
	}
	return result;
    }

    public void sort() {
	Collections.sort(assertions, new AssertionIDComparator());
    }

}
