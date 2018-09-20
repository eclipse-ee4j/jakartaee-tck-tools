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

package com.sun.cts.spec.data;

import org.jdom.*;
import java.util.*;
import com.sun.cts.common.IDIntf;

public class SpecAssertion implements IDIntf {

    public static final DefinedByEnum TECHNOLOGY = new DefinedByEnum(0);
    public static final DefinedByEnum PLATFORM   = new DefinedByEnum(1);
    public static final StatusEnum    ACTIVE     = new StatusEnum(0);
    public static final StatusEnum    DEPRECATED = new StatusEnum(1);
    public static final StatusEnum    REMOVED    = new StatusEnum(2);

    public static class DefinedByEnum {
	private static String[] values = {"technology","platform"};
	private int val;
	private DefinedByEnum(int val) { this.val = val; }
	public String toString() { return values[val]; }
    }

    public static class StatusEnum {
	private static String[] values = {"active","deprecated","removed"};
	private int val;
	private StatusEnum(int val) { this.val = val; }
	public String toString() { return values[val]; }
    }

    /*
     * If this SpecAssertion instance is a sub-assertion then the parent member
     * will be set to the sub-assertion's parent.  This is useful for determining
     * if a sub-assertion is tested by recursively processing its parents looking
     * for an assertion ID that has been tested.  We assume if a parent assertion
     * ID is tested then all testable sub-assertions of that parent are also covered
     * by that test.  If parent is null this is a top-level assertion not a
     * sub-assertion.
     */
    private SpecAssertion   parent;

    private boolean         required;
    private boolean         implSpec;
    private DefinedByEnum   definedBy; // technology or platform
    private StatusEnum      status;    // active, deprecated or removed
    private boolean         testable;
    private String          id;
    private String          description;
    private List            keywords        = new ArrayList();
    private LocationPointer locationPointer;
    private String          comment;
    private DependList      depends;
    private List            subAssertions   = new ArrayList();

    /* Added for checking parent assertion's tested state */
    private boolean isTested;
    public  void    setTested() { isTested = true; }
    public  boolean isTested() {
	if (!hasSubAssertions()) {
	    //	    System.out.println("\t\t*** no sub-ass isTested = " + isTested);
	    return isTested;
	}
	if (isTested) {
	    //	    System.out.println("\t\t*** isTested already true");
	    return true;
	}

	boolean result = true;
	int numSubs = subAssertions.size();
	for (int i = 0; i < numSubs; i++) {
	    SpecAssertion sub = (SpecAssertion)(subAssertions.get(i));
	    //System.out.println("\t\t*** Checking sub-assertions id = " + sub.getID());
	    result = result && sub.isTested();
	    if (!result) {
		break; // fail fast once result is false
	    }
	}
	if (result) {
	    //	    System.out.println("\t\t*** setting ID " + getID() + " to true");
	    setTested();
	}
	return result;
    }



    public SpecAssertion(Element assertionEl) {
	parseAttributes(assertionEl);
	parseContent(assertionEl);
    }

    public boolean       getRequired()                   { return required;      }
    public boolean       getImplSpec()                   { return implSpec;      }
    public DefinedByEnum getDefinedBy()                  { return definedBy;     } 
    public StatusEnum    getStatus()                     { return status;        }
    public boolean       getTestable()                   { return testable;      }
    public String        getID()                         { return id;            }
    public String        getDescription()                { return description;   }
    public List          getKeywords()                   { return keywords;      }
    public LocationPointer getLocationPointer()          { return locationPointer; }
    public String        getComment()                    { return comment;       }
    public DependList    getDepends()                    { return depends;       }
    public List          getSubAssertions()              { return subAssertions; }

    public void          setParent(SpecAssertion parent) { this.parent = parent; }
    public SpecAssertion getParent()                     { return this.parent;   }

    public void clearSubAssertions() {
	subAssertions.clear();
    }

    public SpecAssertion[] getSubAssertionArray() {
	SpecAssertion[] subs = new SpecAssertion[subAssertions.size()];
	return (SpecAssertion[])(subAssertions.toArray(subs));
    }

    private void parseAttributes(Element assertionEl) {
	required = asBoolean
	    (assertionEl.getAttributeValue(Globals.REQUIRED_ATR));
	implSpec = asBoolean
	    (assertionEl.getAttributeValue(Globals.IMPL_SPEC_ATR));
	definedBy = getDefinedEnum
	    (assertionEl.getAttributeValue(Globals.DEFINED_BY_ATR));
	status = getStatusEnum
	    (assertionEl.getAttributeValue(Globals.STATUS_ATR));
	testable = asBoolean
	    (assertionEl.getAttributeValue(Globals.TESTABLE_ATR));
    }

    private DefinedByEnum getDefinedEnum(String value) {
	DefinedByEnum result = null;
	if (TECHNOLOGY.toString().equalsIgnoreCase(value)) {
	    result = TECHNOLOGY;
	} else if (PLATFORM.toString().equalsIgnoreCase(value)) {
	    result = PLATFORM;
	}
	return result;
    }

    private StatusEnum getStatusEnum(String value) {
	StatusEnum result = null;
	if (ACTIVE.toString().equalsIgnoreCase(value)) {
	    result = ACTIVE;
	} else if (DEPRECATED.toString().equalsIgnoreCase(value)) {
	    result = DEPRECATED;
	} else if (REMOVED.toString().equalsIgnoreCase(value)) {
	    result = REMOVED;
	}
	return result;
    }

    private boolean asBoolean(String attribute) {
	return Boolean.valueOf(attribute).booleanValue();
    }

    private void parseContent(Element assertionEl) {
	id = assertionEl.getChildTextTrim(Globals.ID);
	description = assertionEl.getChildTextTrim(Globals.DESCRIPTION);
	Element keywordsEl = assertionEl.getChild(Globals.KEYWORDS);
	if (keywordsEl != null) {
	    parseKeywords(keywordsEl);
	}
	locationPointer = new LocationPointer
	    (assertionEl.getChild(Globals.LOCATION));
	comment = assertionEl.getChildTextTrim(Globals.COMMENT);
	Element dependsEl = assertionEl.getChild(Globals.DEPENDS);
	if (dependsEl != null) {
	    depends = new DependList(dependsEl);
	}
	Element subAssertionsEl = assertionEl.getChild(Globals.SUB_ASSERTIONS);
	if (subAssertionsEl != null) {
	    parseAssertions(subAssertionsEl);
	}
    }

    private void parseKeywords(Element keywordsEl) {
	List keywordEls = keywordsEl.getChildren(Globals.KEYWORD);
	int numKeywordEls = keywordEls.size();
	for (int i = 0; i < numKeywordEls; i++) {
	    Element keywordEl = (Element)keywordEls.get(i);
	    String keyword = keywordEl.getTextTrim();
	    keywords.add(keyword);
	}
    }

    private void parseAssertions(Element assertionsEl) {
	List assertionEls = assertionsEl.getChildren(Globals.ASSERTION);
	int numAssertionEls = assertionEls.size();
	for (int i = 0; i < numAssertionEls; i++) {
	    Element assertionEl = (Element)assertionEls.get(i);
	    SpecAssertion assertion = new SpecAssertion(assertionEl);
	    assertion.setParent(this);
	    subAssertions.add(assertion);
	}
    }

    public Element toXML() {
	Element assertionEl = new Element(Globals.ASSERTION);
	addAttributes(assertionEl);
	addContent(assertionEl);
	return assertionEl;
    }

    private void addAttributes(Element el) {
	el.setAttribute(Globals.REQUIRED_ATR, Boolean.toString(required));
	el.setAttribute(Globals.IMPL_SPEC_ATR, Boolean.toString(implSpec));
	el.setAttribute(Globals.DEFINED_BY_ATR, definedBy.toString());
	el.setAttribute(Globals.STATUS_ATR, status.toString());
	el.setAttribute(Globals.TESTABLE_ATR, Boolean.toString(testable));
    }

    private void addContent(Element el) {
	el.addContent(new Element(Globals.ID).addContent(id));
	el.addContent(new Element(Globals.DESCRIPTION).addContent(description));
	if (keywords.size() > 0) {
	    el.addContent(addKeywords());
	}
	el.addContent(locationPointer.toXML());
	if (comment != null) {
	    el.addContent(new Element(Globals.COMMENT).addContent(comment));
	}
	if (depends != null) {
	    el.addContent(depends.toXML());
	}
	if (subAssertions.size() > 0) {
	    el.addContent(addSubAssertions());
	}
    }

    private Element addKeywords() {
	Element keywordsEl = new Element(Globals.KEYWORDS);
	int numKeywords = keywords.size();
	for (int i = 0; i < numKeywords; i++) {
	    String keyword = (String)keywords.get(i);
	    keywordsEl.addContent(new Element(Globals.KEYWORD)
		.addContent(keyword));
	}
	return keywordsEl;
    }

    private Element addSubAssertions() {
	Element subassertionsEl = new Element(Globals.SUB_ASSERTIONS);
	int numSubAssertions = subAssertions.size();
	for (int i = 0; i < numSubAssertions; i++) {
	    SpecAssertion sa = (SpecAssertion)subAssertions.get(i);
	    subassertionsEl.addContent(sa.toXML());
	}
	return subassertionsEl;
    }

    public boolean hasSubAssertions() {
	return subAssertions.size() > 0;
    }

    public SpecAssertion[] getAssertions() {
	SpecAssertion[] result = new SpecAssertion[subAssertions.size()];
	return (SpecAssertion[])(subAssertions.toArray(result));
    }

    public boolean equals(Object thatObj) {
	SpecAssertion that = null;
	if (thatObj == null || (thatObj.getClass() != this.getClass())) {
	    return false;
	}
	that = (SpecAssertion)thatObj;
	boolean result = (required == that.getRequired()) &&
	    (implSpec == that.getImplSpec()) &&
	    (definedBy == that.getDefinedBy()) &&
	    (status == that.getStatus()) &&
	    (testable == that.getTestable()) &&
	    (id.equals(that.getID())) &&
	    (description.equals(that.getDescription())) &&
	    (keywordsEqual(that.getKeywords())) &&
	    (locationPointer.equals(that.getLocationPointer())) &&
	    ((comment == null) ? that.getComment() == null : comment.equals(that.getComment())) &&
	    ((depends == null) ? that.getDepends() == null : depends.equals(that.getDepends()));

	return result;
	// Currently sub-assertions are ignored in determining equality
    }

    private boolean keywordsEqual(List words) {
	int size = keywords.size();
	if (size != words.size()) {
	    return false;
	}
	for (int i = 0; i < size; i++) {
	    String keyword = (String)keywords.get(i);
	    if (!words.contains(keyword)) {
		return false;
	    }
	}
	return true;
    }

} // end class SpecAssertion
