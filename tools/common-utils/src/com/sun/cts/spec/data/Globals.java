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

public class Globals {
    
    private Globals(){} // create no instances

    public static final String NEW_LINE              =
	System.getProperty("line.separator", "\n");
    public static final String NL        =
	NEW_LINE; // alias
    public static final String SYSTEM_ID =
	"http://dummy.domain.com/CTS/XMLassertions/dtd/spec_assertions.dtd";
    

    public static final String SPEC           = "spec";
    public static final String NEXT_AVAIL_ID  = "next-available-id";
    public static final String PREVIOUS_ID    = "previous-id";
    public static final String TECH           = "technology";
    public static final String ID             = "id";
    public static final String NAME           = "name";
    public static final String VERSION        = "version";
    public static final String LOC_NAMES      = "location-names";
    public static final String CHAPTERS       = "chapters";
    public static final String CHAPTER        = "chapter";
    public static final String ID_ATR         = "id";
    public static final String NAME_ATR       = "name";
    public static final String SECTIONS       = "sections";
    public static final String SECTION        = "section";
    public static final String REQUIRED_ATR   = "required";
    public static final String IMPL_SPEC_ATR  = "impl-spec";
    public static final String DEFINED_BY_ATR = "defined-by";
    public static final String STATUS_ATR     = "status";
    public static final String TESTABLE_ATR   = "testable";
    public static final String DESCRIPTION    = "description";
    public static final String KEYWORDS       = "keywords";
    public static final String KEYWORD        = "keyword";
    public static final String LOCATION       = "location";
    public static final String CHAPTER_ATR    = "chapter";
    public static final String SECTION_ATR    = "section";
    public static final String COMMENT        = "comment";
    public static final String DEPENDS        = "depends";
    public static final String ORDER_ATR      = "order";
    public static final String DEPEND         = "depend";
    public static final String SUB_ASSERTIONS = "sub-assertions";
    public static final String ASSERTIONS     = "assertions";
    public static final String ASSERTION      = "assertion";

}
