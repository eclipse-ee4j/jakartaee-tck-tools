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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private static String replace(String definition, String prefix, String with) {
        if (definition.startsWith(prefix)) {
            definition = with + definition.substring(prefix.length());
        }
        return definition;
    }
    
    private Map<Integer,String> genericTypes = new HashMap<Integer, String>();
    private static Pattern BOUND = Pattern.compile("[^%]*%([0-9]*) (extends ([0-9a-zA-Z\\.]*))");
    
    /** return formatted definition. **/
    @Override
    public String getDefinition(String definition) {
        definition = replace(definition, "CLASS ", SignatureConstants.CLASS);
        definition = replace(definition, "method ", SignatureConstants.METHOD);
        definition = replace(definition, "field ", SignatureConstants.FIELD);
        definition = replace(definition, "constructor ", SignatureConstants.CONSTRUCTOR);
        
        if (definition.startsWith(SignatureConstants.FIELD)) {
            int eqsign = definition.indexOf('=');
            if (eqsign >= 0) {
                definition = definition.substring(0, eqsign).trim();
            }
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
            Matcher m = BOUND.matcher(definition);
            while (m.find()) {
                int index = Integer.parseInt(m.group(1));
                if (m.groupCount() == 3) {
                    genericTypes.put(index, m.group(3));
                } else {
                    genericTypes.put(index, "java.lang.Object");
                }
            }
            definition = definition.substring(0, beg) + definition.substring(end + 1);
        }
        for (;;) {
            int beg = definition.indexOf('{');
            if (beg == -1) {
                break;
            }
            int end = definition.indexOf('}', beg);
            if (end == -1) {
                throw new IllegalStateException("Missing } in " + definition);
            }
            if (
                definition.charAt(beg + 1) == '%' &&
                definition.charAt(beg + 2) == '%'
            ) {
                // reference
                int index = Integer.parseInt(definition.substring(beg + 3, end));
                String middle = genericTypes.get(index);
                if (middle == null) {
                    throw new IllegalStateException("No type for index " + index + " in " + genericTypes);
                }
                definition = definition.substring(0, beg) + middle + definition.substring(end + 1);
            } else {
                // reference
                int percent = definition.indexOf('%', beg);
                int index = Integer.parseInt(definition.substring(percent + 1, end));
                String middle = genericTypes.get(index);
                if (middle == null) {
                    throw new IllegalStateException("No type for index " + index + " in " + genericTypes);
                }
                definition = definition.substring(0, beg) + middle + definition.substring(end + 1);
            }
        }
        int newLine = definition.indexOf('\n');
        if (newLine >= 0) {
            definition = definition.substring(0, newLine);
        }

        definition = definition.replaceAll("byte\\[\\]", "[B")
                .replaceAll("short\\[\\]", "[S")
                .replaceAll("int\\[\\]", "[I")
                .replaceAll("float\\[\\]", "[F")
                .replaceAll("double\\[\\]", "[D")
                .replaceAll("char\\[\\]", "[C");
                
        for (;;) {
            int array = definition.indexOf("[]");
            if (array == -1) {
                break;
            }
            int pos = array - 1;
            while (pos >= 0) {
                char ch = definition.charAt(pos);
                if (ch == '.' || Character.isJavaIdentifierPart(ch)) {
                    pos--;
                    continue;
                }
                break;
            }
            pos++;
            String arrName = definition.substring(pos, array);
            String beg = definition.substring(0, pos);
            String end = definition.substring(array + 2);
            definition = beg + "[L" + arrName + ";" + end;
        }
        
        return super.getDefinition(definition);
    }
}

    

