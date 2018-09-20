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

public class Location {

    private Map chapters = new HashMap();

    public Location(Element el) {
	Element chaptersEl = el.getChild(Globals.CHAPTERS);
	List chapterEls = chaptersEl.getChildren(Globals.CHAPTER);
	int numChapters = chapterEls.size(); // must be at least one per DTD
	for (int i = 0; i < numChapters; i++) {
	    Element chapterEl = (Element)chapterEls.get(i);
	    Chapter chapter = new Chapter(chapterEl);
	    this.chapters.put(chapter.getID(), chapter);
	}
    }

    // the getChapter and getSection may be better declared private
    // need some use cases to decide
    public Chapter getChapter(String chapterID) {
	if (chapters.containsKey(chapterID)) {
	    return (Chapter)chapters.get(chapterID);
	}
	return null;
    }

    public Section getSection(String chapterID, String sectionID) {
	Chapter chapter = getChapter(chapterID);
	if (chapter != null) {
	    return (Section)(chapter.getSection(sectionID));
	}
	return null;
    }

    public String getChapterName(String chapterID) {
	Chapter chap = getChapter(chapterID);
	if (chap != null) {
	    return chap.getName();
	}
	return null;
    }

    public String getSectionName(String chapterID, String sectionID) {
	Section sect = getSection(chapterID, sectionID);
	if (sect != null) {
	    return sect.getName();
	}
	return null;
    }

    public Element toXML() {
	Element locationEl = new Element(Globals.LOCATION);
	locationEl.addContent(new Element(Globals.CHAPTERS));
	Chapter[] chapterValues = (Chapter[])
	    (chapters.values().toArray(new Chapter[chapters.size()]));
	for (int i = 0; i < chapterValues.length; i++) {
	    locationEl.addContent(chapterValues[i].toXML());
	}
	return locationEl;
    }

}
