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

public class Chapter extends ChapSect {

    private Map sections = new HashMap();

    public Chapter(Element el) {
	super(el);
	Element sectionsEl = el.getChild(Globals.SECTIONS);
	if (sectionsEl != null) {
	    List sectionEls = sectionsEl.getChildren(Globals.SECTION);
	    int numSections = sectionEls.size();
	    for (int i = 0; i < numSections; i++) {
		Element sectionEl = (Element)sectionEls.get(i);
		Section section   = new Section(sectionEl);
		addSection(section);
	    }
	}
    }

    public Chapter(String id, String name) {
	super(id, name);
    }

    public Section getSection(String id) {
	if (sections.containsKey(id)) {
	    return (Section)sections.get(id);
	}
	return null;
    }

    public void addSection(Section section) {
	sections.put(section.getID(), section);
    }

    protected String getElementName() {
	return Globals.CHAPTER;
    }

    public Element toXML() {
	Element chapter = super.toXML();
	if (sections.size() > 0) {
	    Element sectionsEl = new Element(Globals.SECTIONS);
	    chapter.addContent(sectionsEl);
	    Section[] sectionValues = (Section[])
		(sections.values().toArray(new Section[sections.size()]));
	    for (int i = 0; i < sectionValues.length; i++) {
		sectionsEl.addContent(sectionValues[i].toXML());
	    }
	}
	return chapter;
    }

}
