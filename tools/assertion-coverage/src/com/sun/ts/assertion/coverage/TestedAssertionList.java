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

public class TestedAssertionList {

    private List assertions = new ArrayList(); // list of TestedAssertion instances
    private Set  idSet      = new HashSet();
    private Set  testSet    = new HashSet();

    public TestedAssertionList(Document doc) {
	Element root          = doc.getRootElement(); // assertions element
	List    assertionEls  = root.getChildren(Globals.ASSERTION_TAG);
	int     numAssertions = assertionEls.size();
	for (int i = 0; i < numAssertions; i++) {
	    Element assertionEl = (Element)(assertionEls.get(i));
	    TestedAssertion assertion = new TestedAssertion(assertionEl);
	    assertions.add(assertion);
	    idSet.add(assertion.getID()); // unique assertion ids
	    testSet.add(assertion.getFullComment()); // unique test classes-test names
	}
    }

    private List getTests(String key, boolean byID) {
	List result = new ArrayList();
	int iters = assertions.size();
	for (int i = 0; i < iters; i++) {
	    TestedAssertion test = (TestedAssertion)(assertions.get(i));
	    if (byID) {
		if (test.getID().equals(key)) {
		    result.add(test);  
		}
	    } else {
		if (test.getFullComment().equals(key)) {
		    result.add(test);  
		}
	    }
	}
	return result;
    }

    private Map getTestMap(Set set, boolean byID) {
	Map result = new HashMap();
	Iterator iter = set.iterator();
	while (iter.hasNext()) {
	    String key = (String)iter.next();
	    List tests = getTests(key, byID);
	    result.put(key, tests);
	}
	return result;
    }

    public Map getAssertionsByID() {
	Map result = getTestMap(idSet, true);
	return result;
    }

    public Map getAssertionsByTest() {
	Map result = getTestMap(testSet, false);
	return result;
    }

    public boolean contains(String id) {
	return idSet.contains(id);
    }

    public boolean numsMatch(String id) {
	boolean result = false;
	Iterator iter = idSet.iterator();
	while (iter.hasNext()) {
	    String aID = (String)iter.next();
	    int index = aID.lastIndexOf(":");
	    if ((index) + 1 < aID.length()) {
		aID = aID.substring(index + 1);
	    }
	    if (aID.equals(id)) {
		result = true;
		break;
	    }
	}
	return result;
    }

    public Set getIDs() {
        return idSet;
    }
    
} // end class TestedAssertionList
