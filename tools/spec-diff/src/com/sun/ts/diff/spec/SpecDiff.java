/*
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.ts.diff.spec;

import java.util.*;
import org.jdom.*;
import com.sun.cts.spec.data.*;

public class SpecDiff {

    private static final String NL = System.getProperty("line.separator", "\n");
    private static final String SEPARATOR =
	"*********************************************************************"  + NL;
    private static final String REMOVED_COMMENT =
	"This section lists all the removed assertions.  This means" + NL +
	"that an assertion ID listed in this section was part of the"  + NL +
	"previous assertion document but no longer exists in the new"  + NL +
	"assertions document.  This section should never have any assertion"  + NL +
	"IDs listed since users should use the removed element to denote"  + NL +
	"a deleted assertion."  + NL;
    private static final String DIFF_COMMENT =
	"This section lists all the assertions that have different element" + NL +
	"content between the previous assertion document and the new assertion" + NL +
	"document.  Only the elements with different content are listed all other" + NL +
	"element content should be assumed to the same in both assertion documents." + NL;
    private static final String NEW_COMMENT =
	"This section lists all the new assertions.  This means" + NL +
	"that an assertion ID listed in this section is listed in the"  + NL +
	"new assertion document but does not exist in the previous"  + NL +
	"assertion document." + NL;



    private static final String DIFFS_TAG             = "diffs";
    private static final String DIFFS_DESCRIPTION_ATR = "description";
    private static final String NEW_ASSERTIONS_TAG    = "new-assertions";
    private static final String REM_ASSERTIONS_TAG    = "removed-assertions";
    private static final String DIFF_ASSERTIONS_TAG   = "diff-assertions";
    private static final String ASSERTION_TAG         = "assertion";
    private static final String ASSERTION_ID_ATR      = "id";
    private static final String DIFF_ELEMENT_TAG      = "diff-element";
    private static final String DIFF_ELEMENT_NAME_ATR = "name";
    private static final String OLD_CONTENT_TAG       = "old-content";
    private static final String NEW_CONTENT_TAG       = "new-content";

    private SpecAssertions prevAssertions;
    private SpecAssertions newAssertions;
    private Document       diffDoc;

    private int removedCount;
    private int modifiedCount;
    private int newCount;

    public SpecDiff(SpecAssertions prevAssertions, SpecAssertions newAssertions) {
	this.prevAssertions = prevAssertions;
	this.newAssertions  = newAssertions;
    }


    /**** Utility Methods ****/

    private void createDoc() {
	Element diffsRootElem = new Element(DIFFS_TAG);
	String description = prevAssertions.getTechnology() + " version: " +
	    prevAssertions.getVersion() + " diffed against " + 
	    newAssertions.getTechnology() + " version: " +
	    newAssertions.getVersion();
	diffsRootElem.setAttribute(DIFFS_DESCRIPTION_ATR, description);
	diffDoc = new Document(diffsRootElem);
    }

    private boolean assertionInArray(SpecAssertion assertion, SpecAssertion[] assertions) {
	boolean result = false;
	String searchID = assertion.getID();
	for (int i = 0; i < assertions.length; i++) {
	    String currID = assertions[i].getID();
	    if (currID.equals(searchID)) {
		result = true;
		break;
	    }
	    if (assertions[i].hasSubAssertions()) {
		SpecAssertion[] subs = assertions[i].getSubAssertionArray();
		result = assertionInArray(assertion, subs);
		if (result == true) {
		    break;
		}
	    }
	}
	return result;
    }

    private SpecAssertion getAssertion(String searchID, SpecAssertion[] assertions) {
	SpecAssertion result = null;
	for (int i = 0; i < assertions.length; i++) {
	    String currID = assertions[i].getID();
	    if (currID.equals(searchID)) {
		result = assertions[i];
		break;
	    }
	    if (assertions[i].hasSubAssertions()) {
		SpecAssertion[] subs = assertions[i].getSubAssertionArray();
		result = getAssertion(searchID, subs);
		if (result != null) {
		    break;
		}
	    }
	}
	return result;
    }


    /**** Process Removed Assertions ****/

    private void processRemoved(SpecAssertion[] prev, SpecAssertion[] curr, Element el) {
	for (int i = 0; i < prev.length; i++) {
	    SpecAssertion a = prev[i];
	    if (!assertionInArray(a, curr)) {
		el.addContent(new Element(ASSERTION_TAG)
		    .setAttribute(ASSERTION_ID_ATR, a.getID()));
		System.out.println("\tRemoved ID is \"" + a.getID() + "\"");
		removedCount++;
	    }
	    if (a.hasSubAssertions()) {
		SpecAssertion[] subs = a.getSubAssertionArray();
		processRemoved(subs, curr, el);
	    }
	}
    }

    private void getRemovedAssertions() {
	System.out.println(NL + NL + NL + "$$$$$$$$ Finding \"Removed\" Assertions");
	addRootComment(REMOVED_COMMENT);
	Element removedElem = new Element(REM_ASSERTIONS_TAG);
	diffDoc.getRootElement().addContent(removedElem);
	SpecAssertion[] prev = prevAssertions.getAssertions();
	SpecAssertion[] curr = newAssertions.getAssertions();
	processRemoved(prev, curr, removedElem);
    }


    /**** Process Modified Assertions ****/

    private Element createDiffElement(String name, String oldContent, String newContent) {
	Element result = new Element(DIFF_ELEMENT_TAG)
	    .setAttribute(DIFF_ELEMENT_NAME_ATR, name);
	result.addContent(new Element(OLD_CONTENT_TAG).addContent(oldContent));
	result.addContent(new Element(NEW_CONTENT_TAG).addContent(newContent));
	return result;
    }

    private boolean requiredSame(SpecAssertion a, SpecAssertion b) {
	return a.getRequired() == b.getRequired();
    }
    private boolean implSpecSame(SpecAssertion a, SpecAssertion b) {
	return a.getImplSpec() == b.getImplSpec();
    }
    private boolean definedBySame(SpecAssertion a, SpecAssertion b) {
	return a.getDefinedBy() == b.getDefinedBy();
    }
    private boolean statusSame(SpecAssertion a, SpecAssertion b) {
	return a.getStatus() == b.getStatus();
    }
    private boolean testableSame(SpecAssertion a, SpecAssertion b) {
	return a.getTestable() == b.getTestable();
    }
    private boolean idSame(SpecAssertion a, SpecAssertion b) {
	return a.getID().equals(b.getID());
    }
    private boolean descriptionSame(SpecAssertion a, SpecAssertion b) {
	return a.getDescription().equals(b.getDescription());
    }
    private boolean keywordsSame(SpecAssertion a, SpecAssertion b) {
	List aWords = a.getKeywords();
	List bWords = b.getKeywords();
	int size = aWords.size();
	if (size != bWords.size()) {
	    return false;
	}
	for (int i = 0; i < size; i++) {
	    String keyword = (String)aWords.get(i);
	    if (!bWords.contains(keyword)) {
		return false;
	    }
	}
	return true;
    }
    private boolean locationSame(SpecAssertion a, SpecAssertion b) {
	return a.getLocationPointer().equals(b.getLocationPointer());
    }
    private boolean commentSame(SpecAssertion a, SpecAssertion b) {
	return (a.getComment() == null) ? b.getComment() == null :
	    a.getComment().equals(b.getComment());
    }
    private boolean dependsSame(SpecAssertion a, SpecAssertion b) {
	return (a.getDepends() == null) ? b.getDepends() == null :
	    a.getDepends().equals(b.getDepends());
    }

    private Element createDiffAssertion(SpecAssertion a, SpecAssertion b) {
	Element assertionElem = new Element(ASSERTION_TAG)
	    .setAttribute(ASSERTION_ID_ATR, a.getID());
	if (!requiredSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.REQUIRED_ATR,
						       Boolean.toString(a.getRequired()),
						       Boolean.toString(b.getRequired())));
	}
	if (!implSpecSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.IMPL_SPEC_ATR,
						       Boolean.toString(a.getImplSpec()),
						       Boolean.toString(b.getImplSpec())));
	}
	if (!definedBySame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.DEFINED_BY_ATR,
						       a.getDefinedBy().toString(),
						       b.getDefinedBy().toString()));
	}
	if (!statusSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.STATUS_ATR,
						       a.getStatus().toString(),
						       b.getStatus().toString()));
	}
	if (!testableSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.TESTABLE_ATR,
						       Boolean.toString(a.getTestable()),
						       Boolean.toString(b.getTestable())));
	}
	if (!idSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.ID_ATR,
						       a.getID(),
						       b.getID()));
	}
	if (!descriptionSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.DESCRIPTION,
						       a.getDescription(),
						       b.getDescription()));
	}
	if (!keywordsSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.KEYWORDS,
						       a.getKeywords().toString(),
						       b.getKeywords().toString()));
	}
	if (!locationSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.LOCATION,
						       a.getLocationPointer().toString(),
						       b.getLocationPointer().toString()));
	}
	if (!commentSame(a, b)) {
	    assertionElem.addContent(createDiffElement(Globals.COMMENT,
						       a.getComment(),
						       b.getComment()));
	}
	if (!dependsSame(a, b)) {
	    String aDep = "";
	    String bDep = "";
	    if (a.getDepends() != null) {
		aDep = a.getDepends().toString();
	    }
	    if (b.getDepends() != null) {
		bDep = b.getDepends().toString();
	    }
	    //	    System.out.println("\taDep = " + aDep + "  bDep = " + bDep);
	    assertionElem.addContent(createDiffElement(Globals.DEPENDS, aDep, bDep));
	}
	return assertionElem;
    }

    private void processDiff(SpecAssertion[] prev, SpecAssertion[] curr, Element el) {
	for (int i = 0; i < prev.length; i++) {
	    SpecAssertion a = prev[i];
	    SpecAssertion b = getAssertion(a.getID(), curr);
	    if (b == null || a.equals(b)) {
		continue;
	    } else {
		el.addContent(createDiffAssertion(a, b));
		System.out.println("\tModified Assertion ID \"" + a.getID() + "\"");
		modifiedCount++;
	    }
	    if (a.hasSubAssertions()) {
		SpecAssertion[] subs = a.getSubAssertionArray();
		processDiff(subs, curr, el);
	    }
	}
    }

    private void getDiffAssertions() {
	System.out.println(NL + NL + NL + "$$$$$$$$ Finding \"Modified\" Assertions");
	addRootComment(DIFF_COMMENT);
	Element diffElem = new Element(DIFF_ASSERTIONS_TAG);
	diffDoc.getRootElement().addContent(diffElem);
	SpecAssertion[] prev = prevAssertions.getAssertions();
	SpecAssertion[] curr = newAssertions.getAssertions();
	processDiff(prev, curr, diffElem);
    }


    /**** Process New Assertions ****/

    private void processNew(SpecAssertion[] prev, SpecAssertion[] curr, Element el) {
	for (int i = 0; i < curr.length; i++) {
	    SpecAssertion a = curr[i];
	    if (!assertionInArray(a, prev)) {
		el.addContent(new Element(ASSERTION_TAG)
		    .setAttribute(ASSERTION_ID_ATR, a.getID()));
		System.out.println("\tNew Assertion ID \"" + a.getID() + "\"");
		newCount++;
	    }
	    if (a.hasSubAssertions()) {
		SpecAssertion[] subs = a.getSubAssertionArray();
		processNew(prev, subs, el);
	    }
	}
    }

    private void getNewAssertions() {
	System.out.println(NL + NL + NL + "$$$$$$$$ Finding \"New\" Assertions");
	addRootComment(NEW_COMMENT);
	Element newElem = new Element(NEW_ASSERTIONS_TAG);
	diffDoc.getRootElement().addContent(newElem);
	SpecAssertion[] prev = prevAssertions.getAssertions();
	SpecAssertion[] curr = newAssertions.getAssertions();
	processNew(prev, curr, newElem);
    }



    public void createDiffDoc() {
	createDoc();
	getRemovedAssertions();
	getDiffAssertions();
	getNewAssertions();
	System.out.println();
	System.out.println("*** Assertion Diff Totals ***");
	System.out.println("\t" + removedCount  + " Removed Assertions");
	System.out.println("\t" + modifiedCount + " Modified Assertions");
	System.out.println("\t" + newCount      + " New Assertions");
	System.out.println();
    }

    public Document getDoc() {
	return diffDoc;
    }

    private void addRootComment(String comment) {
	Element root = diffDoc.getRootElement();
	root.addContent(new Comment(NL + SEPARATOR + SEPARATOR + SEPARATOR + comment));
    }

} // end class SpecDiff
