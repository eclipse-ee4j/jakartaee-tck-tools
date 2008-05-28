/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
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
        
package org.netbeans.apitest;

/** This class scans class files for founding fields which are primitive constants.
 *  In definition of these fields PRIMITIVE_CONSTANT modifier will be added during
 *  formating. **/
class PrimitiveConstantsCheckerFromSigtests extends PrimitiveConstantsChecker {
    /** creates new DefinitionFormat.
     *  @param isQualifiedName specify if qualified names will be used.
     *  @param isThrowsTracked specify if throws clause will be tracked.**/
    public PrimitiveConstantsCheckerFromSigtests(boolean isQualifiedName, boolean isThrowsTracked) {
        super(isQualifiedName, isThrowsTracked);
    }

    /** creates new DefinitionFormat.
     *  @param isQualifiedName specify if qualified names will be used.
     *  @param isThrowsTracked specify if throws clause will be tracked.
     *  @param removedModif modifiers which are required to be deleted. **/
    public PrimitiveConstantsCheckerFromSigtests(boolean isQualifiedName, boolean isThrowsTracked,
                                     String[][] removedModif) {
        super(isQualifiedName, isThrowsTracked, removedModif);
    }
        
    /** return formatted definition. **/
    @Override
    public String getDefinition(String definition) {
        if (definition.startsWith("CLASS ")) {
            definition = "CLSS " + definition.substring(6);
        }
        if (definition.startsWith("method ")) {
            definition = "meth " + definition.substring(7);
        }
        for (;;) {
            int beg = definition.indexOf('<');
            if (beg == -1) {
                break;
            }
            int end = definition.indexOf('>', beg);
            if (end == -1) {
                throw new IllegalStateException("Missing > in " + definition);
            }
            definition = definition.substring(0, beg) + definition.substring(end + 1);
        }
        return super.getDefinition(definition);
    }
}

    

