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

public class DependList extends ArrayList {

    private String order;
    
    public DependList(Element el) {
	super();
	// the order attribute is optional but set it
	// just in case its there, else it stays null
	setOrder(el.getAttributeValue(Globals.ORDER_ATR));
	List dependEls = el.getChildren(Globals.DEPEND);
	int numEls = dependEls.size();
	for (int i = 0; i < numEls; i++) {
	    Element dependEl = (Element)dependEls.get(i);
	    String dependStr = dependEl.getTextTrim();
	    add(dependStr);
	}
    }

    public String getOrder() { return order; }
    public void   setOrder(String order) { this.order = order; }

    public Element toXML() {
	Element dependListEl = new Element(Globals.DEPENDS);
	if (order != null) {
	    dependListEl.setAttribute(Globals.ORDER_ATR, order);
	}
	int numDepends = size();
	for (int i = 0; i < numDepends; i++) {
	    String depend = (String)get(i);
	    dependListEl.addContent(new Element(Globals.DEPEND)
		.addContent(depend));
	}
	return dependListEl;
    }

    public boolean equals(Object thatObj) {
	if (thatObj == null || (thatObj.getClass() != this.getClass())) {
	    return false;
	}
	DependList that = (DependList)thatObj;
	boolean result = (order == null) ?
	    that.getOrder() == null :
	    order.equals(that.getOrder());
	int size = this.size();
	if (size != that.size()) {
	    return false;
	}
	for (int i = 0; i < size; i++) {
	    String depend = (String)this.get(i);
	    if (!that.contains(depend)) {
		return false;
	    }
	}
	return result;
    }

} // end class DependList
