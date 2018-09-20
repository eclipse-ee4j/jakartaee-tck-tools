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

package com.sun.cts.api.data;

/**
 * The <code>JavadocModifiedAssertion</code> class represents a
 * modified Javadoc assertion.  The class contains the original
 * and modified assertion.  The c'tor also modifies the IDs and
 * modified status of the specified assertions.
 *
 * @author <a href="mailto:ryano@caseylou.east">Ryan O'Connell-CTS</a>
 * @version 1.0
 */
public class JavadocModifiedAssertion {

    public static final String OLD_ID_EXT = "__OLD";

    private JavadocAssertion originalAssertion;
    private JavadocAssertion modifiedAssertion;

    public JavadocModifiedAssertion(JavadocAssertion original,
				    JavadocAssertion modified) {
	originalAssertion = original;
	modifiedAssertion = modified;
	String originalID = originalAssertion.getID();
	originalAssertion.setModified(true);
	originalAssertion.setID(originalID + OLD_ID_EXT);
	modifiedAssertion.setID(originalID);
    }

    public JavadocAssertion getOriginal() { return originalAssertion; }
    public JavadocAssertion getModified() { return modifiedAssertion; }
}
