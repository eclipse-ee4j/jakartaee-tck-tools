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

public abstract class ChapSect {

    protected String id;
    protected String name;

    protected ChapSect(Element el) {
	id   = el.getAttributeValue(Globals.ID);
	name = el.getAttributeValue(Globals.NAME);
    }

    protected ChapSect(String id, String name) {
	this.id = id;
	this.name = name;
    }

    public String getID() {
	return id;
    }

    public String getName() {
	return name;
    }

    protected abstract String getElementName();

    protected Element toXML() {
	Element el = new Element(getElementName());
	el.setAttribute(Globals.ID_ATR, id);
	el.setAttribute(Globals.NAME_ATR, name);
	return el;
    }

}
