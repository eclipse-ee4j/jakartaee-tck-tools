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

/*
 * $URL$ $LastChangedDate$
 */

package com.sun.ts.assertion.coverage;

import org.jdom.*;
import java.util.*;
import com.sun.cts.api.data.Globals;

public class TestedAssertion {

    private static final String COMMENT_DELIM = "-";

    private String id;
    private String description;
    private String fullComment;
    private String testClass;
    private String testName;

    public TestedAssertion(Element el) {
	id = el.getChildTextTrim(Globals.ID_TAG);
	Element temp = null;
	if ((temp = el.getChild(Globals.DESCRIPTION_TAG)) != null) {
	    description = temp.getTextTrim();
	}
	if ((temp = el.getChild(Globals.COMMENT_TAG)) != null) {
	    fullComment = temp.getTextTrim();
	    testClass   = parseClassName(fullComment);
	    testName    = parseTestName(fullComment);
	}
    }

    private String parseClassName(String str) {
	if (str == null) {
	    return null;
	}
	int index = str.indexOf(COMMENT_DELIM);
	return str.substring(0, index);
    }

    private String parseTestName(String str) {
	if (str == null) {
	    return null;
	}
	int index = str.indexOf(COMMENT_DELIM);
	return str.substring(index + 1);
    }

    public String getID()          { return id;          }
    public String getDescription() { return description; }
    public String getFullComment() { return fullComment; }
    public String getTestClass()   { return testClass;   }
    public String getTestName()    { return testName;    }

    public String toString() {
	StringBuffer buf = new StringBuffer("Test Assertion Info:" + Globals.NL);
	buf.append("\tID         : " + getID() + Globals.NL);
	buf.append("\tTest       : " + getTestClass() + COMMENT_DELIM + getTestName() + Globals.NL);
	buf.append("\tDescription: " + getDescription() + Globals.NL);
	buf.append("\tComment    : " + getFullComment() + Globals.NL);
	return buf.toString();
    }

} // end class TestedAssertion
