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

import java.util.*;
import java.io.IOException;
import com.sun.cts.common.IDIntf;
import com.sun.cts.api.data.*;
import com.sun.cts.spec.data.*;

public abstract class AssertionDoc {

	private void dumpTestname(String id, Map idToTestName) {
		List assertions = (List) idToTestName.get(id);
		System.err.println("         Test Names:");
		int numTests = (assertions == null) ? 0 : assertions.size();
		for (int i = 0; i < numTests; i++) {
			TestedAssertion assertion = (TestedAssertion) assertions.get(i);
			System.err.println("              " + assertion.getTestClass()
					+ ":" + assertion.getTestName() + "()");
		}
	}

	abstract public void createAssertionReports() throws IOException;

	protected void verifyTestedAssertionIDs(SpecAssertions assertions,
			TestedAssertionList testedAssertions) {
		boolean verifyIDs = Boolean.getBoolean("verify.source.code.ids");
		if (!verifyIDs) {
			return;
		}
		System.err.println("BEGIN Assertion ID in code check...");
		List docAssertions = assertions.getAssertionList();
		Map idToTestName = testedAssertions.getAssertionsByID();
		Iterator testedIDs = testedAssertions.getIDs().iterator();
		while (testedIDs.hasNext()) {
			String id = (String) testedIDs.next();
			if (!testedIDFoundInSpecDoc(docAssertions, id)) {
				System.err
						.println("  ERROR: Assertion found in test code but not in assertion document: "
								+ id);
				dumpTestname(id, idToTestName);
			}
		}
		System.err.println("Assertion ID in code check DONE.");
	}

	protected void verifyTestedAssertionIDs(JavadocAssertionList assertions,
			TestedAssertionList testedAssertions) {
		boolean verifyIDs = Boolean.getBoolean("verify.source.code.ids");
		if (!verifyIDs) {
			return;
		}
		System.err.println("BEGIN Assertion ID in code check...");
		List docAssertions = assertions.getAssertionList();
		Map idToTestName = testedAssertions.getAssertionsByID();
		Iterator testedIDs = testedAssertions.getIDs().iterator();
		while (testedIDs.hasNext()) {
			String id = (String) testedIDs.next();
			if (!testedIDFoundInDoc(docAssertions, id)) {
				System.err
						.println("  ERROR: Assertion found in test code but not in assertion document: "
								+ id);
				dumpTestname(id, idToTestName);
			}
		}
		System.err.println("Assertion ID in code check DONE.");
	}

	protected boolean testedIDFoundInDoc(List docAssertions, String testedID) {
		boolean result = false;
		for (int i = 0; i < docAssertions.size(); i++) {
			IDIntf assertion = (IDIntf) docAssertions.get(i);
			if (assertion.getID().equalsIgnoreCase(testedID)
					|| testedID.endsWith(assertion.getID())) {
				result = true;
				break;
			}
		}
		return result;
	}

	protected boolean testedIDFoundInSpecDoc(List docAssertions, String testedID) {
		boolean result = false;
		SpecAssertion assertion = null;
		for (int i = 0; i < docAssertions.size(); i++) {
			assertion = (SpecAssertion) docAssertions.get(i);
			List subAssertions = assertion.getSubAssertions();
			if (subAssertions != null && subAssertions.size() > 0) {
				result = testedIDFoundInSpecDoc(subAssertions, testedID);
			}
			if (!result) {
				if (assertion.getID().equalsIgnoreCase(testedID)
						|| testedID.endsWith(assertion.getID())) {
					result = true;
					break;
				}
			} else {
				break;
			}
		}
		return result;
	}
	
} // end AssertionDoc
