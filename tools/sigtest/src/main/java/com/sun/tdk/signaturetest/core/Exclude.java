/*
 * $Id: Exclude.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;

/**
 *  The exclusion plugin allow ignore classes, methods, fields during signature check.
 *  The SignatureTest use property "exclude.plugin" to find implementation of plugin.
 *  Plugin can use specific command-line arguments to define exclusion criteria.
 *  The method 'check' is invoked by SignatureTest for each API item.
 *   
 * @author Leonid Mesnik
 */
public interface Exclude {

    /**
     * This method parses parameters specific to exclusion plugin 
     * @param args vector of all parameters
     * @return vector of parameters which can't be parsed by this extension
     */
    public String[] parseParameters(String[] args);


    /**
     * Checks if the given signature is excluded.
     * @param testedClass the full qualified name of class being testing now
     * @param signature full qualified signature of class, method or field which is tested,
     * for methods and fields signature includes the name of class where this member
     * is declared, for classes this signature is it name 
     * @throws ExcludeException if signature is to be excluded with message which is
     * used by SignatureTest for diagnostic messages
     */
    public void check(ClassDescription testedClass, MemberDescription signature) throws ExcludeException;


    /**
     * This method is invoked after all checks, to get plugin's summary report.
     * This report is logged. 
     * @return report for plugin work or null if no report is needed. 
     */
    public String report();
}
