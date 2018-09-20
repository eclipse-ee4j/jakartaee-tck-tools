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

public class LocationPointer {

    private String chapID;
    private String sectionID;

    public LocationPointer(Element el) {
	chapID    = el.getAttributeValue(Globals.CHAPTER_ATR);
	sectionID = el.getAttributeValue(Globals.SECTION_ATR);
    }

    public Element toXML() {
	Element locationPtrEl = new Element(Globals.LOCATION);
	locationPtrEl.setAttribute(Globals.CHAPTER_ATR, chapID);
	locationPtrEl.setAttribute(Globals.SECTION_ATR, sectionID);
	return locationPtrEl;
    }
 
    public String getChapID() {
	return chapID;
    }

    public String getSectionID() {
	return sectionID;
    }

    public boolean equals(Object thatObj) {
	if (thatObj == null || (thatObj.getClass() != this.getClass())) {
	    return false;
	}
	LocationPointer that = (LocationPointer)thatObj;
	return (chapID.equals(that.getChapID())) &&
	    (sectionID.equals(that.getSectionID()));
    }

    public String toString() {
	return "Chapter " + chapID + "  Section " + sectionID;
    }

} // end class LocationPointer
