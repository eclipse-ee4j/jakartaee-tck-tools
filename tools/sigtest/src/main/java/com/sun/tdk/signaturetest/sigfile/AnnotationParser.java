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


package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.model.AnnotationItemEx;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.StringTokenizer;

/**
 * @author Sergey Ivashin
 * @author Mikhail Ershov
 */
public class AnnotationParser {

    private static final String CLASS_PREFIX = "java.lang.Class";
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(AnnotationParser.class);


    // Opposite action that toString() method does.
    // TODO should be moved to the parser as well as "toString" moved to the writer
    public AnnotationItem parse(String str) {

//        str = "anno 0 javax.xml.ws.BindingType(java.lang.String value=\"http://schemas.xmlsoap.org/wsdl/soap/http\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2005/08/addressing/module\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2004/08/soap/features/http-optimization\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\"MTOM_THRESHOLD\", java.lang.String value=\"1000\")])]):     anno 0 javax.xml.ws.BindingType(java.lang.String value=\\\"http://schemas.xmlsoap.org/wsdl/soap/http\\\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2005/08/addressing/module\\\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2004/08/soap/features/http-optimization\\\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\\\"MTOM_THRESHOLD\\\", java.lang.String value=\\\"1000\\\")])])";

        AnnotationItem item;

        if (!str.startsWith(AnnotationItem.ANNOTATION_PREFIX))
            throw new IllegalArgumentException(i18n.getString("AnnotationParser.error.bad_annotation_descr") + str);

        if (str.startsWith(AnnotationItemEx.ANNOTATION_EX_PREFIX)) {
            item = new AnnotationItemEx();
        } else {
            item = new AnnotationItem();
        }

        int pos;
        pos = str.indexOf(' ');
        // skip the prefix
        str = str.substring(pos).trim();

        pos = str.indexOf(' ');
        String specificData = str.substring(0, pos);
        if (item instanceof AnnotationItemEx) {
            parseAnnExData((AnnotationItemEx) item, specificData);
        } else {
            parseAnnData(item, specificData);
        }

        // remove target
        str = str.substring(pos + 1);

        pos = str.indexOf('(');
        item.setName(str.substring(0, pos).trim());
        int endPos = findCorresponding(str, '(', ')');
        str = str.substring(pos + 1, endPos);

        if (str.length() != 0) {

            while (str.length() > 0 && str.charAt(0) != ')') {
                pos = parseMember(item, str);
                str = str.substring(pos);
                if (str.length() > 0 && str.charAt(0) == ',')
                    str = str.substring(1).trim();
            }
        }
        return item;

    }

    private int findCorresponding(String str, char open, char close) {
        char[] chars = str.toCharArray();
        int count = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == open) {
                count++;
            } else if (chars[i] == close) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void parseAnnData(AnnotationItem item, String specificData) {
        item.setTarget(Integer.valueOf(specificData).intValue());
    }

    private void parseAnnExData(AnnotationItemEx item, String specificData) {
        assert specificData.startsWith("[");
        assert specificData.endsWith("]");
        specificData = specificData.substring(1, specificData.length() - 1);
        StringTokenizer st = new StringTokenizer(specificData, ";");
        while (st.hasMoreTokens()) {
            String set = st.nextToken();
            int delPos = set.indexOf("=");
            assert delPos > 0;
            String name = set.substring(0, delPos);
            String val = set.substring(delPos + 1, set.length());
            if (AnnotationItemEx.ANN_TARGET_TYPE.equals(name)) {
                item.setTargetType(Integer.parseInt(val.substring(2), 16));
            } else if (AnnotationItemEx.ANN_TYPE_IND.equals(name)) {
                item.setTypeIndex(Integer.parseInt(val));
            } else if (AnnotationItemEx.ANN_BOUND_IND.equals(name)) {
                item.setBoundIndex(Integer.parseInt(val));
            } else if (AnnotationItemEx.ANN_LOCATIONS.equals(name)) {
                assert val.startsWith("[");
                assert val.endsWith("]");
                val = val.substring(1, val.length() - 1);
                StringTokenizer st2 = new StringTokenizer(val, ",");
                int [] locs = new int [st2.countTokens()];
                for (int i = 0; i < locs.length; i++) {
                    locs[i] = Integer.parseInt(st2.nextToken());
                }
                item.setLocations(locs);
            } else if (AnnotationItemEx.ANN_PARAM_IND.equals(name)) {
                item.setParameterIndex(Integer.parseInt(val));
            } else {
                assert false;
            }

        }
    }

    protected int parseMember(AnnotationItem item, String str) {

        int pos, result = 0;

        AnnotationItem.Member m = new AnnotationItem.Member();

        pos = str.indexOf(' ');

        // java.lang.Class<? extends java.util.ArrayList<? super javax.swing.JLabel>> value=class com.sun.tdk.signaturetest.model.Regtest_6564000$CL_4
        if (str.startsWith(CLASS_PREFIX + "<")) {
            // skip possible spaces inside
            char [] strChar = str.toCharArray();
            int level = 0;
            for (int i = CLASS_PREFIX.length(); i < strChar.length; i++) {
                if (strChar[i] == '<') level++;
                else if (strChar[i] == '>') level--;
                if (level == 0 && strChar[i + 1] == ' ') {
                    pos = i + 1;
                    break;
                }
            }
        }

        m.type = str.substring(0, pos);
        str = str.substring(pos + 1).trim();
        result += pos + 1;

        pos = str.indexOf('=');
        m.name = str.substring(0, pos);

        str = str.substring(pos + 1).trim();
        result += pos + 1;

        char ch = str.charAt(0);

        switch (ch) {
            case'[': {
                pos = findClosingBracket(str, 1, '[', ']') + 1;
                break;
            }

            case'"':
            case'\'': {
                pos = str.indexOf(ch, 1) + 1;
                break;
            }

            case'a': {
                if (str.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                    AnnotationItem a = parse(str);
                    pos = a.toString().length();
                    break;
                }
            }

            default: {
                pos = str.indexOf(',');

                if (pos == -1)
                    pos = str.indexOf(')');

                if (pos == -1)
                    pos = str.length();
            }
        }

        m.value = str.substring(0, pos);
        //System.err.println("Added member name=" + m.name + " type=" + m.type + " value=" + m.value);
        item.addMember(m);

        result += pos;
        return result;
    }

    private int findClosingBracket(String str, int startPos, char openingChar, char closingChar) {

        int level = 0;
        int len = str.length();
        for (int i = startPos; i < len; ++i) {

            char ch = str.charAt(i);

            if (ch == openingChar) {
                ++level;
                continue;
            }

            if (ch == closingChar) {
                if (level == 0)
                    return i;
                --level;
            }
        }

        return -1;
    }

}
