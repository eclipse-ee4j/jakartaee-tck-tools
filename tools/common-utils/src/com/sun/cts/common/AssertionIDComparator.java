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

package com.sun.cts.common;

import java.util.*;

public class AssertionIDComparator implements Comparator {

    public AssertionIDComparator() {
    }

    private int[] parseInts(String ints) {
	int index = ints.lastIndexOf(":");
	String nums = ints.substring(index + 1);
	String numbers = nums.replace('.', ':');
	// added since websockets uses . and - in the integer ID section
	numbers = numbers.replace('-', ':');
	//System.err.println("raw " + nums + "   normed " + numbers);
	String[] strSect = numbers.split(":");
	int[] sects = new int[strSect.length];
	for (int i = 0; i < strSect.length; i++) {
	    try {
		sects[i] = Integer.parseInt(strSect[i]);
	    } catch (NumberFormatException nfe) {
		sects[i] = 0;
	    }
	}
	return sects;
    }

    private int compareIDs(String id1, String id2) {
	int[] ints1  = parseInts(id1);
	int[] ints2  = parseInts(id2);
	int   result = -1;
	for (int i = 0; i < ints1.length; i++) {
	    if (i < ints2.length) {
		result = ints1[i] - ints2[i];
		if (result != 0) {
		    break;
		}
	    } else {
		result = 1;
	    }
	}
	return result;
    }

    public int compare(Object o1, Object o2) {
	if (o1 == null) { return (o2 == null) ? 0 :  1; }
	if (o2 == null) { return (o1 == null) ? 0 : -1; }
	IDIntf s1 = (IDIntf)o1;
	IDIntf s2 = (IDIntf)o2;
	String firstID   = s1.getID();
	String secondID  = s2.getID();
	return compareIDs(firstID, secondID);
    }

    public boolean equals(Object o1) {
	return o1 == this;
    }
} // end class AssertionIDComparator

