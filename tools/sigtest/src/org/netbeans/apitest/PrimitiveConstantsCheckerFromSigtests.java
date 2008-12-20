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
    private static Pattern ARRAY = Pattern.compile("[^\\p{Alnum}]((byte|short|int|long|float|double|char)\\[\\])");
    
    /** return formatted definition. **/
    @Override
    public String getDefinition(String definition) {
        String orig = definition;
        
        definition = replace(definition, "CLASS ", SignatureConstants.CLASS);
        definition = replace(definition, "method ", SignatureConstants.METHOD);
        definition = replace(definition, "field ", SignatureConstants.FIELD);
        definition = replace(definition, "constructor ", SignatureConstants.CONSTRUCTOR);
        definition = definition.replace("!enum ", "");
        definition = definition.replaceFirst("abstract static", "static abstract");
        definition = definition.replaceFirst("final static", "static final");
        
        if (definition.startsWith(SignatureConstants.FIELD)) {
            int eqsign = definition.indexOf('=');
            if (eqsign >= 0) {
                definition = definition.substring(0, eqsign).trim();
            }
        }
        int newLine = definition.indexOf('\n');
        if (newLine >= 0) {
            definition = definition.substring(0, newLine);
        }

        for(;;) {
            Matcher primitiveArray = ARRAY.matcher(definition);
            if (!primitiveArray.find()) {
                break;
            }
            String pa;
            String match = primitiveArray.group(2);
            switch (match.charAt(0)) {
                case 'b': pa = "[B"; break;
                case 's': pa = "[S"; break;
                case 'i': pa = "[I"; break;
                case 'l': pa = "[J"; break;
                case 'f': pa = "[F"; break;
                case 'd': pa = "[D"; break;
                case 'c': pa = "[C"; break;
                default: throw new IllegalStateException(match);
            }
            definition = definition.substring(0, primitiveArray.start(1)) + 
                pa + 
                definition.substring(primitiveArray.end(1));
        }
        
        for (;;) {
            int array = definition.indexOf("[]");
            if (array == -1) {
                break;
            }
            String prefix = "[L";
            int arrayEnd = array + 2;
            while (
                arrayEnd + 1 < definition.length() && 
                definition.charAt(arrayEnd) == '[' && 
                definition.charAt(arrayEnd + 1) == ']'
            ) {
                prefix = "[" + prefix;
                arrayEnd += 2;
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
            String end = definition.substring(arrayEnd);
            definition = beg + prefix + arrName + ";" + end;
        }
        
        return super.getDefinition(definition);
    }
}

    

