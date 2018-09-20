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

package com.sun.cts.utils.assertion.edit;

import java.util.List;
import java.util.ArrayList;
import org.jdom.*;
import com.sun.cts.api.data.*;

/**
 * The <code>MergedAssertionList</code> class represents a final merged
 * set of assertions produced by this tool.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell - CTS</a>
 * @version 1.0
 */
public class MergedAssertionList {

    private static class AssociatedAssertions {
	private JavadocAssertion assertion;
	private List             assertions;
	private AssociatedAssertions(JavadocAssertion assertion, List assertions) {
	    this.assertion  = assertion;
	    this.assertions = assertions;	    
	}
	private JavadocAssertion   getAssertion()  { return assertion; }
	private List getAssertions() {
	    return assertions;
	}
// 	private JavadocAssertion[] getAssertions() {
// 	    JavadocAssertion[] result = new JavadocAssertion[assertions.size()];
// 	    return (JavadocAssertion[])(assertions.toArray(result));
// 	}
    }
    

    private static final String ASSERTION_SEPARATOR =
	" ********************************************************************************** ";

    private int    nextAvailableID;
    private int    previousID;
    private String technology;
    private String id;
    private String name;
    private String version;
    private List   commonAssertions     = new ArrayList();
    private List   newAssertions        = new ArrayList();
    private List   removedAssertions    = new ArrayList();
    private List   deprecatedAssertions = new ArrayList();

    /* Accessors */

    public int getStartID() {
	return nextAvailableID;
    }

    public String getPrefix() {
	return technology + ":" + "JAVADOC" + ":";
    }

    /* Modifiers */

    public void setNextAvailableID(int id) {
	previousID = nextAvailableID;
	nextAvailableID = id;
    }

    /**
     * Creates a new <code>MergedAssertionList</code> instance.  The instance
     * variables of this class are set to the instance variables of the
     * specified assertion list.  The specified assertion list should be the
     * verified assertion list.
     *
     * @param list a <code>JavadocAssertionList</code> value that represents
     *        the verified assertion list
     */
    public MergedAssertionList(JavadocAssertionList list) {
	nextAvailableID = list.getNextAvailableID();
	previousID      = list.getPreviousID();
	technology      = list.getTechnology();
	id              = list.getID();
	name            = list.getName();
	version         = list.getVersion();
    }

    /**
     * The <code>createDocument</code> method creates the output XML document.
     *
     * @return a <code>Document</code> value containing the content of this
     *         merged assertion list.
     */
    private Document createDocument() {
	DocType docType = new DocType(Globals.JAVADOC_TAG, Globals.SYSTEM_ID);
	Element javadocElement = new Element(Globals.JAVADOC_TAG);
	Document doc = new Document(javadocElement, docType);
	return doc;
    }

    /**
     * The <code>toXML</code> method returns this merged assertion list as
     * an XML document.
     *
     * @return an XML <code>Document</code> representing this merged
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
	createCommonAssertions(assertionsElement);
	createDeprecatedAssertions(assertionsElement);
	createRemovedAssertions(assertionsElement);
	createNewAssertions(assertionsElement);
	return assertionsElement;
    }

    /*
     * The following methods add the various assertion lists to the output
     * document.
     */
    private void addKeyAssertion(JavadocAssertion assertion, Element parent) {
	parent.addContent
	    (new Comment("This assertion was found in the new assertion list."));
	parent.addContent
	    (new Comment("The list of assertions following this assertion match"));
	parent.addContent
	    (new Comment("this assertion's method signature.  Users should verify"));
	parent.addContent
	    (new Comment("that the assertions in the following list still belong."));
	parent.addContent(assertion.toXML());
    }

    private void addAssociatedAssertions(List assertions, Element parent) {
	parent.addContent
	    (new Comment("This is the associated assertion list."));
	int numAssertions = assertions.size();
	for (int i = 0; i < numAssertions; i++) {
	    JavadocAssertion assertion = (JavadocAssertion)assertions.get(i);
	    parent.addContent
		(new Comment("Assertion " + (i + 1) + " of " + numAssertions));
	    parent.addContent(assertion.toXML());
	}
	addCommentSeparator(parent, 1);
    }

    private void addAssertionsToList(List list, Element parent) {
	Object aAssert = null;
	int numAssertions = list.size();
	for (int i = 0; i < numAssertions; i++) {
	    aAssert = list.get(i);
	    if (aAssert instanceof AssociatedAssertions) {
		AssociatedAssertions assertion = (AssociatedAssertions)(aAssert);
		JavadocAssertion keyAssertion = assertion.getAssertion();
		addKeyAssertion(keyAssertion, parent);
		List mappedAssertions = assertion.getAssertions();
		addAssociatedAssertions(mappedAssertions, parent);
	    } else { // must be a JavadocAssertion
		JavadocAssertion assertion = (JavadocAssertion)(aAssert);
		parent.addContent(assertion.toXML());
	    }
	}
    }

    private void addCommentSeparator(Element parent, int count) {
	for (int i = 0; i < count; i++) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	}
    }

    private void createCommonAssertions(Element parent) {
	if (commonAssertions.size() > 0) {
	    addCommentSeparator(parent, 3);
	    parent.addContent(new Comment(" COMMON ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(commonAssertions, parent);
	}
    }

    private void createDeprecatedAssertions(Element parent) {
	if (deprecatedAssertions.size() > 0) {
	    addCommentSeparator(parent, 3);
	    parent.addContent(new Comment(" DEPRECATED ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(deprecatedAssertions, parent);
	}
    }

    private void createRemovedAssertions(Element parent) {
	if (removedAssertions.size() > 0) {
	    addCommentSeparator(parent, 3);
	    parent.addContent(new Comment(" REMOVED ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(removedAssertions, parent);
	}
    }

    private void createNewAssertions(Element parent) {
	if (newAssertions.size() > 0) {
	    addCommentSeparator(parent, 3);
	    parent.addContent(new Comment(" NEW ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(newAssertions, parent);
	}
    }
    

    /*
     * These methods simply add the specified list of assertions to the
     * appropriate assertion list.
     */
    public void addDeprecatedAssertion
	(JavadocAssertion newAssertion, List matchingAssertions) 
    {
	deprecatedAssertions.add
	    (new AssociatedAssertions(newAssertion, matchingAssertions));
    }

    public void addCommonAssertion
	(JavadocAssertion newAssertion, List matchingAssertions) 
    {
	commonAssertions.add
	    (new AssociatedAssertions(newAssertion, matchingAssertions));
    }

    public void addNewAssertions(List list) {
	newAssertions.addAll(list);
    }

    public void addRemovedAssertions(List list) {
	removedAssertions.addAll(list);
    }

    /*
     * These methods simply return the number of elements in each assertion
     * list.  They are used to provide a merge summary report.  The report
     * quantifies the number of the different kinds of assertions found.
     */

    public int numCommon() {
	return commonAssertions.size();
    }
    public int numDeprecated() {
	return deprecatedAssertions.size();
    }
    public int numRemoved() {
	return removedAssertions.size();
    }
    public int numNew() {
	return newAssertions.size();
    }
}
