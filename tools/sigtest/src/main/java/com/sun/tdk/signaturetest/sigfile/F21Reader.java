/*
 * $Id: F21Reader.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.MemberDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mikhail Ershov
 */
class F21Reader extends SignatureClassLoader implements Reader {

    F21Reader(Format format) {
        super(format);
    }

    protected Parser getParser() {
        // use F31Parser!
        return new F31Parser();
    }

    protected String convertClassDescr(String descr) {
        return descr;
    }

    protected List convertClassDefinitions(List definitions) {
        ArrayList newDef = new ArrayList();
        Iterator it = definitions.iterator();
        while (it.hasNext()) {
            String memberDef = (String) it.next();

            // 1) skip "supr null"
            if ("supr null".equals(memberDef)) {
                continue;
            }
            // 2) convert constant declaration to the new form
            memberDef = processConstants(memberDef);
            // 3) arrays - from VM from to Java
            memberDef = processArrays(memberDef);
            // 4) convert constructor to the new form
            memberDef = processConstructors(memberDef);

            newDef.add(memberDef);

        }
        return newDef;
    }

    private String processConstructors(String memberDef) {

        if (memberDef.startsWith("cons ")) {
            Matcher m = constructorName.matcher(memberDef);
            if (m.find())
                memberDef = m.replaceFirst("(");
        }
        return memberDef;
    }

    private String processArrays(String memberDef) {

        Matcher m = arrayDeclaration.matcher(memberDef);
        while (m.find()) {
            int stPos = m.start();
            int eqPos = memberDef.indexOf(" = ");
            if (eqPos > -1 && stPos > eqPos) {
                // this is string constant value
                break;
            }
            int endPos = stPos;
            int tmp = memberDef.indexOf(' ', stPos);
            if (tmp >= 0) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(',', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(')', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp;
            }
            tmp = memberDef.indexOf(';', stPos);
            if (tmp >= 0 && (tmp < endPos || endPos == stPos)) {
                endPos = tmp + 1;
            }
            String p1 = memberDef.substring(0, stPos);
            String p4 = memberDef.substring(stPos, endPos);
            String p2 = MemberDescription.getTypeName(p4.replace('/', '.'));
            String p3 = memberDef.substring(endPos);

            memberDef = p1 + p2 + p3;
            m = arrayDeclaration.matcher(memberDef);
        }
        return memberDef;
    }

    private String processConstants(String memberDef) throws NumberFormatException {

        Matcher m = constantDeclaration.matcher(memberDef);
        if (m.find()) {
            String constDef = memberDef.substring(m.start(), m.end());
            memberDef = m.replaceFirst("");
            Matcher v = valueDeclaration.matcher(constDef);
            if (v.find()) {
                String value = constDef.substring(v.start() + 1, v.end() - 1);

                // try to determine constant type
                int end = memberDef.lastIndexOf(' ');
                int start = memberDef.lastIndexOf(' ', end - 1);
                String type = memberDef.substring(++start, end);
                Object oVal = null;
                if ("java.lang.String".equals(type)) {
                    // decode unicode
                    Matcher uc = unicodeSim.matcher(value);
                    while (uc.find()) {
                        String uValue = value.substring(uc.start() + 2, uc.end());
                        char ch = (char) Integer.parseInt(uValue, 16);
                        String repl = "" + ch;
                        if (ch == '\\' || ch == '$') {
                            repl = "\\" + ch;
                        }
                        value = uc.replaceFirst(repl);
                        uc = unicodeSim.matcher(value);
                    }
                    oVal = value;

                } else if ("boolean".equals(type)) {
                    if ("0".equals(value)) {
                        oVal = Boolean.FALSE;
                    } else {
                        oVal = Boolean.TRUE;
                    }
                } else if ("int".equals(type)) {
                    oVal = new Integer(value);
                } else if ("long".equals(type)) {
                    oVal = new Long(value);
                } else if ("char".equals(type)) {
                    oVal = new Character((char) Integer.parseInt(value));
                } else if ("byte".equals(type)) {
                    oVal = new Byte(value);
                } else if ("double".equals(type)) {
                    oVal = new Double(value);
                } else if ("float".equals(type)) {
                    oVal = new Float(value);
                }

                if (oVal != null) {
                    value = MemberDescription.valueToString(oVal);
                }

                memberDef += " = " + value;
            }
        }
        return memberDef;
    }

    private static Pattern constantDeclaration = Pattern.compile("<constant> <value=\".*\">");
    private static Pattern valueDeclaration = Pattern.compile("\".*\"");
    private static Pattern arrayDeclaration = Pattern.compile("\\[+[BCDFIJSZVL]");
    private static Pattern constructorName = Pattern.compile("\\.\\w+\\(");
    private static Pattern unicodeSim = Pattern.compile("\\\\u(?i)[\\da-f]{4}");

}
