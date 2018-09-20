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

import java.io.File;

public interface HTMLDataProvider {

    /* Returns the names of the 6 Assertion list file names */
    public File[] getAssertionFileNames();

    /* Returns the names of the ID to Test and Test to ID mapping file names */
    public File[] getTestIDMappingFileNames();

    /* Returns the count of the appropriate assertion type */
    public int getNumTestedNoParent();
    public int getNumUntestedNoParent();
    public int getNumTested();
    public int getNumUntested();
    public int getNumUntestable();
    public int getNumOptional();
    public int getNumDeprecated();
    public int getNumRemoved();

    public String getTechnology();
    public String getID();
    public String getName();
    public String getVersion();
    public boolean isSpec();

} // end interface HTMLDataProvider
