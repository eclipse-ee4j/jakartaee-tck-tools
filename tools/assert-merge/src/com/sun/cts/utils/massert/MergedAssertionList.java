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

package com.sun.cts.utils.massert;

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
    private List   modifiedAssertions   = new ArrayList();

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
	createModifiedAssertions(assertionsElement);
	createRemovedAssertions(assertionsElement);
	createNewAssertions(assertionsElement);
	return assertionsElement;
    }

    /*
     * The following methods add the various assertion lists to the output
     * document.
     */
    private void addAssertionsToList(List list, Element parent) {
	JavadocAssertion assertion = null;
	int numAssertions = list.size();
	for (int i = 0; i < numAssertions; i++) {
	    assertion = (JavadocAssertion)(list.get(i));
	    parent.addContent(assertion.toXML());
	}
    }

    private void addModifiedAssertionsToList(List list, Element parent) {
	JavadocModifiedAssertion modAssertion = null;
	int numAssertions = list.size();
	for (int i = 0; i < numAssertions; i++) {
	    modAssertion = (JavadocModifiedAssertion)(list.get(i));
	    parent.addContent(new Comment(" ****** ORIGINAL ASSERTION ****** "));
	    parent.addContent(modAssertion.getOriginal().toXML());
	    parent.addContent(new Comment(" ****** MODIFIED ASSERTION ****** "));
	    parent.addContent(modAssertion.getModified().toXML());
	}
    }

    private void createCommonAssertions(Element parent) {
	if (commonAssertions.size() > 0) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(" UNMODIFIED ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(commonAssertions, parent);
	}
    }

    private void createDeprecatedAssertions(Element parent) {
	if (deprecatedAssertions.size() > 0) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(" DEPRECATED ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(deprecatedAssertions, parent);
	}
    }

    private void createModifiedAssertions(Element parent) {
	if (modifiedAssertions.size() > 0) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(" MODIFIED ASSERTIONS LISTED BELOW "));
	    parent.addContent(new Comment
		    (" The original assertion is followed by the modified version "));
	    parent.addContent(new Comment
		    (" of the assertion. Users must edit the file to verify "));
	    parent.addContent(new Comment
		    (" that the modified assertion is correct. User should then "));
	    parent.addContent(new Comment
		    ( " delete the original assertion or use the style sheet "));
	    parent.addContent(new Comment
		    (" that removes them automatically.  The original and modified "));
	    parent.addContent(new Comment
		    (" assertions differ by their IDs, notice the original assertion "));
	    parent.addContent(new Comment
		(" now ends with \"" + JavadocModifiedAssertion.OLD_ID_EXT + "\" "));
	    addModifiedAssertionsToList(modifiedAssertions, parent);
	}
    }

    private void createRemovedAssertions(Element parent) {
	if (removedAssertions.size() > 0) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(" REMOVED ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(removedAssertions, parent);
	}
    }

    private void createNewAssertions(Element parent) {
	if (newAssertions.size() > 0) {
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(ASSERTION_SEPARATOR));
	    parent.addContent(new Comment(" NEW ASSERTIONS LISTED BELOW "));
	    addAssertionsToList(newAssertions, parent);
	}
    }
    

    /*
     * These methods simply add the specified list of assertions to the
     * appropriate assertion list.
     */
    public void addCommonAssertions(List list) {
	commonAssertions.addAll(list);
    }

    public void addNewAssertions(List list) {
	newAssertions.addAll(list);
    }

    public void addRemovedAssertions(List list) {
	removedAssertions.addAll(list);
    }

    public void addDeprecatedAssertions(List list) {
	deprecatedAssertions.addAll(list);
    }

    public void addModifiedAssertions(List list) {
	/*
	 * This list contains JavadocModifiedAssertion objects.  This is
	 * because we want to echo the verified assertion as well as the
	 * modified assertion so the user can edit the output file and
	 * decide which assertion should remain.
	 */
	modifiedAssertions.addAll(list);
    }

    /*
     * These methdso simply return the number of elements in each assertion
     * list.  They are used to provide a merge summary report.  The report
     * quantifies the number of the different kinds of assertions found.
     */

    public int numCommon() {
	return commonAssertions.size();
    }
    public int numDeprecated() {
	return deprecatedAssertions.size();
    }
    public int numModified() {
	return modifiedAssertions.size();
    }
    public int numRemoved() {
	return removedAssertions.size();
    }
    public int numNew() {
	return newAssertions.size();
    }
}
