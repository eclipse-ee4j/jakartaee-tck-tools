/*
 * $Id$
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

package com.sun.tdk.signaturetest.model;

/**
 * @author Mikhail Ershov
 */
public class ExoticCharTools {

    private static final char BOUND = '/';
    private static final char DELIM = ';';


    public static String encodeExotic(String name) {

        if (name == null) {
            return "";
        }

        if (name.indexOf(BOUND) != -1) {
            // already encoded
            return name;
        }
        boolean hasExotic = false;
        int firstExoPos = -1;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isExoticChar(c)) {
                hasExotic = true;
                firstExoPos = i;
                break;
            }
        }
        if (!hasExotic) {
            return name;
        }
        StringBuffer sb = new StringBuffer(name.substring(0, firstExoPos));
        boolean open = false;
        for (int i = firstExoPos; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isExoticChar(c)) {
                if (open) {
                    sb.append(DELIM);
                } else {
                    sb.append(BOUND);
                }
                sb.append(Integer.toHexString(c));
                open = true;
            } else {
                if (open) {
                    open = false;
                    sb.append(BOUND);
                }
                sb.append(c);
            }
        }
        if (open) {
            sb.append(BOUND);
        }
        return sb.toString();
    }
    public static String decodeExotic(String name) {

        if (name == null) {
            return "";
        }

        int exoPos = name.indexOf(BOUND);

        if (exoPos == -1) {
            return name;
        }
        StringBuffer sb = new StringBuffer(name.substring(0, exoPos));

        int pPos;
                
        while ((pPos = name.indexOf(BOUND, exoPos+1)) != -1) {
            String s = name.substring(exoPos+1, pPos);
            decodeExoticSq(sb, name.substring(exoPos+1, pPos));
            int nextPaund = name.indexOf(BOUND, pPos +1);
            if (nextPaund == -1) {
                sb.append(name.substring(pPos+1));
                return sb.toString();
            } else {
                sb.append(name.substring(pPos + 1, nextPaund));
                exoPos = nextPaund;
            }
        }

        return sb.toString();
    }

    private static boolean isExoticChar(char c) {
        // don't use isJavaIdentifierPart because it since 1.5
        // don't use Character.isJavaLetterOrDigit because cdc does not contain this
        return !isJavaLetterOrDigit(c) && !("/.;<>[\"".indexOf(c) != -1);
    }

    private static boolean isJavaLetterOrDigit(char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_' || c == '$' || c == '-';
    }


    private static void decodeExoticSq(StringBuffer sb, String str) {
        int start=0;
        int end = 0;
        boolean was = false;
        while ((end = str.indexOf(DELIM, start+1)) != -1) {
            was = true;
            if (str.charAt(start) == DELIM) {
                start++;
            }
            sb.append((char) Integer.parseInt(str.substring(start, end), 16));
            start = end;
        }
        if (was) {
            start ++;
        }
        sb.append((char) Integer.parseInt(str.substring(start), 16));
    }

    /*
    public static void main(String [] args) {
        doit ("qw@#$$ert");
        doit ("qwert!@#$%^~");
        doit ("qw$$$@@%#%$%%#%^#^ert!@#$%^~");
        doit ("***");
        doit ("опа3243244да");
    }

    private static void doit(String s) {
        System.out.println("-------------------");
        System.out.println(s);
        System.out.println(encodeExotic(s));
        System.out.println(decodeExotic(encodeExotic(s)));
        if (!s.equals(decodeExotic(encodeExotic(s)))) {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }
*/
}
