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
import com.sun.cts.common.AssertionIDComparator;

public class SpecAssertions implements Cloneable {

    private int      nextAvailableID;
    private int      previousId;
    private String   technology;
    private String   id;
    private String   name;
    private String   version;
    private Location locationNames;
    private List     assertions = new ArrayList();

    public SpecAssertions(Document doc) {
	this(doc.getRootElement());
    }

    public SpecAssertions(Element el) {
	parseContent(el);
    }

    public String getTechnology() {
	return technology;
    }

    public String getID() {
	return id;
    }

    public String getName() {
	return name;
    }

    public String getVersion() {
	return version;
    }

    public int size() {
	return assertions.size();
    }

    public Object clone() throws CloneNotSupportedException {
	SpecAssertions result = (SpecAssertions)super.clone();
	result.assertions = new ArrayList();
	return result;
    }

    private void parseContent(Element el) {
	try {
	    nextAvailableID = Integer.parseInt
		(el.getChildTextTrim(Globals.NEXT_AVAIL_ID));
	} catch (Exception e) {
	    System.err.println("!!! Error converting element \"" +
			      Globals.NEXT_AVAIL_ID +
			      "\" to an integer, setting to \"0\"");
	    nextAvailableID = 0;
	}
	try {
	    previousId = Integer.parseInt
		(el.getChildTextTrim(Globals.PREVIOUS_ID));
	} catch (Exception e) {
	    System.err.println("!!! Error converting element \"" +
			      Globals.PREVIOUS_ID +
			      "\" to an integer, setting to \"0\"");
	    previousId = 0;
	}
	technology      = el.getChildTextTrim(Globals.TECH);
	id              = el.getChildTextTrim(Globals.ID);
	name            = el.getChildTextTrim(Globals.NAME);
	version         = el.getChildTextTrim(Globals.VERSION);
	locationNames   = new Location(el.getChild(Globals.LOC_NAMES));
	Element assertionsEl = el.getChild(Globals.ASSERTIONS);
	if (assertionsEl != null) {
	    parseAssertions(assertionsEl);
	}
    }
    
    private void parseAssertions(Element assertionsEl) {
	List assertionEls   = assertionsEl.getChildren(Globals.ASSERTION);
	int  numAsssertions = assertionEls.size();
	for (int i = 0; i < numAsssertions; i++) {
	    Element       assertionEl = (Element)assertionEls.get(i);
	    SpecAssertion assertion   = new SpecAssertion(assertionEl);
	    assertions.add(assertion);
	}
    }

    public Element toXML() {
	Element specEl = new Element(Globals.SPEC);
	specEl.addContent(new Element(Globals.NEXT_AVAIL_ID).
	    addContent(Integer.toString(nextAvailableID)));
	specEl.addContent(new Element(Globals.PREVIOUS_ID).
	    addContent(Integer.toString(previousId)));
	specEl.addContent(new Element(Globals.TECH).addContent(technology));
	specEl.addContent(new Element(Globals.ID).addContent(id));
	specEl.addContent(new Element(Globals.NAME).addContent(name));
	specEl.addContent(new Element(Globals.VERSION).addContent(version));
	specEl.addContent(locationNames.toXML());
	if (assertions.size() > 0) {
	    specEl.addContent(addAssertions());
	}
	return specEl;
    }

    public Document toXMLDocument() {
	DocType  docType = new DocType(Globals.SPEC, Globals.SYSTEM_ID);
	Element  specEl  = toXML();
	Document doc     = new Document(specEl, docType);
	return doc;
    }

    private Element addAssertions() {
	Element assertionsEl  = new Element(Globals.ASSERTIONS);
	int     numAssertions = assertions.size();
	for (int i = 0; i < numAssertions; i++) {
	    SpecAssertion assertion = (SpecAssertion)assertions.get(i);
	    assertionsEl.addContent(assertion.toXML());
	}
	return assertionsEl;
    }

    public SpecAssertion[] getAssertions() {
	SpecAssertion[] result = new SpecAssertion[assertions.size()];
	return (SpecAssertion[])(assertions.toArray(result));
    }
    
    public List getAssertionList() {
    	return assertions;
    }
    
    public void add(SpecAssertion assertion) {
	assertions.add(assertion);
    }

    public void remove(SpecAssertion assertion) {
	int index = assertions.indexOf(assertion);
	if (index != -1) {
	    assertions.remove(index);
	}
    }


    /* Code to sort the assertion list based on assertion ID */
    public void sort() {
	Collections.sort(assertions, new AssertionIDComparator());
    }

} // end class SpecAssertions
