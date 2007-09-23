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

import java.util.Properties;

/** This is class for formating member definition. It can removes specified
 *  modifiers and throws clause. The subclasses of this class can provides
 *  other operations. **/
class DefinitionFormat implements SignatureConstants {
    /** specify if qualified names will be used. **/
    protected boolean isQualifiedName;
    /** specify if throws clause will be tracked. **/
    protected boolean isThrowsTracked;
    /** specify removed modifiers. **/
    protected String removedModifiers[][];

    /** creates new DefinitionFormat.
     *  @param isQualifiedName specify if qualified names will be used.
     *  @param isThrowsTracked specify if throws clause will be tracked. **/
    public DefinitionFormat(boolean isQualifiedName, boolean isThrowsTracked,
                            Properties details) {
        this.isQualifiedName = isQualifiedName;
        this.isThrowsTracked = isThrowsTracked;
        String[] classModif = null;
        if (details.getProperty("NestedProtected") != null) {
            if (details.getProperty("NestedStatic") != null)
                classModif = new String[] {CLASS, FLAG_SUPER,
                                           "protected", "static"};
            else 
                classModif = new String[] {CLASS, FLAG_SUPER, "protected"};
        } else {
            if (details.getProperty("NestedStatic") != null)
                classModif = new String[] {CLASS, FLAG_SUPER, "static"};
        }    
        if (!isQualifiedName) {
            String temp[][] = {
                {FIELD, TRANSIENT, PRIMITIVE_CONSTANT},
                {CONSTRUCTOR, SYNCHRONIZED, NATIVE},
                {METHOD, SYNCHRONIZED, NATIVE},
                {CLASS, FLAG_SUPER},
                {INNER, FLAG_SUPER, SYNCHRONIZED}
            };
            if (classModif != null) {
                temp[3] = classModif;
                temp[4] = (String[])classModif.clone();
                temp[4][0] = INNER;
            }
            removedModifiers = temp;
        } else {
            removedModifiers = new String [0][0];
            if (classModif != null) {
                removedModifiers = new String[][] {
                    classModif, (String[])classModif.clone() };
                removedModifiers[1][0] = INNER;
            }
        }
    }

    /** creates new DefinitionFormat.
     *  @param isQualifiedName specify if qualified names will be used.
     *  @param isThrowsTracked specify if throws clause will be tracked.
     *  @param removedModif modifiers which are required to be deleted. **/
    public DefinitionFormat(boolean isQualifiedName, boolean isThrowsTracked,
                            String[][] removedModif) {
        this.isQualifiedName = isQualifiedName;
        this.isThrowsTracked = isThrowsTracked;
        if (removedModif == null)
            this.removedModifiers = new String [0][0];
        else {
            this.removedModifiers = removedModif;
        }
    }

    /** return formatted definition. **/
    public String getDefinition(String definition) {
        String retVal = definition;
        if (!isThrowsTracked) {
            int pos = definition.lastIndexOf(" throws ");
            if (pos >= 0)
                retVal = definition.substring(0, pos);
        }
        for (int j = 0; j < removedModifiers.length; j++) {
            if (retVal.startsWith(removedModifiers[j][0])) 
                for (int i = 1; i < removedModifiers[j].length; i++)
                    retVal = removeModifier(removedModifiers[j][i], retVal);
        }
        return retVal;
    }

    /** determines if qualified names are used. **/
    public boolean isQualifiedNamesUsed() {
        return isQualifiedName;
    }

    /** removes word from String
     *  @param word the removed word
     *  @param mes the String which is required in the deleting of word. **/
    static private String removeWord(String word, String mes) {
        if (mes.indexOf(" " + word + " ") >= 0) {
            int n = mes.indexOf(" " + word + " ");
            return mes.substring(0, n) + mes.substring(n + word.length() + 1);
        }
        if (mes.endsWith(" " + word)) {
            return mes.substring(0, mes.lastIndexOf(" " + word));
        }
        if (mes.startsWith(word + " ")) 
            return mes.substring(word.length() + 1);
        return mes;
    }

    /** removes modifier from String
     *  @param word the removed modifier
     *  @param mes the String which is required in the deleting of modifier.**/
    public static String removeModifier(String word, String mes) {
        int pos = mes.lastIndexOf(" throws ");
        pos = (pos < 0) ? mes.lastIndexOf(' ') : mes.lastIndexOf(' ', pos - 1);
        pos = (pos < 0) ? 0 : pos;
        return removeWord(word, mes.substring(0, pos)) + mes.substring(pos);
    }
}
    
    
                
