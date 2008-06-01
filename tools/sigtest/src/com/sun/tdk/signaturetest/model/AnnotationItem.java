/*
 * $Id: AnnotationItem.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

//import java.lang.annotation.*;

import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Serguei Ivashin (isl@nbsp.nsk.su)
 */
public class AnnotationItem implements Comparable {

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(AnnotationItem.class);

    public static final String ANNOTATION_PREFIX = "anno";

    public static final String ANNOTATION_INHERITED = "java.lang.annotation.Inherited";
    public static final String ANNOTATION_DOCUMENTED = "java.lang.annotation.Documented";
    private static final String CLASS_PREFIX = "java.lang.Class";

    public AnnotationItem(int target, String name) {
        setTarget(target);
        setName(name);
    }

    private AnnotationItem() {
    }

    public int compareTo(Object o) {
        AnnotationItem that = (AnnotationItem) o;
        int diff = target - that.target;
        if (diff == 0) {
            diff = name.compareTo(that.name);

            if (diff == 0) {
                if (members == that.members)
                    return 0;

                if (members == null)
                    return -1;

                if (that.members == null)
                    return 1;

                diff = members.size() - that.members.size();
                if (diff == 0) {

                    Iterator it = members.iterator();
                    Iterator that_it = that.members.iterator();

                    while (it.hasNext() && diff == 0) {
                        Member m = (Member) it.next();
                        diff = m.compareTo(that_it.next());
                    }
                }
            }
        }

        return diff;
    }

    public final static AnnotationItem[] EMPTY_ANNOTATIONITEM_ARRAY = new AnnotationItem[0];

    // If this annotation imposed on a method/constructor parameter, then target
    // is number of the parameter + 1, otherwise 0.
    private int target;


    // True if this annotation is marked with the 'Inherited' meta-annotation
    private boolean inheritable = false;

    //  Type name of the annotation.
    private String name;

    //  List of the member/value pairs.
    private SortedSet/*Member*/ members = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.intern();
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int t) {
        target = t;
    }

    public void addMember(Member m) {
        if (members == null)
            members = new TreeSet();
        members.add(m);
    }

    public boolean isInheritable() {
        return inheritable;
    }

    public void removeMember(Member m) {
        members.remove(m);
    }

    public static class Member implements Comparable {
        String type;
        String name;
        String value;

        public Member(String type, String name, Object value) {
            this.type = type;
            this.name = name;
            setValue(value);
        }

        public Member(String name, Object value) {
            this.name = name;
            setValue(value);
        }

        private Member() {
        }

        public int compareTo(Object x) {

            Member that = (Member) x;

            int result = 0;

            if (type != null && that.type != null)
                result = type.compareTo(that.type);
            else if (type == that.type) {
                // nothing to do
            } else if (type == null)
                result = -1;
            else
                result = 1;

            if (result == 0) {
                result = name.compareTo(that.name);
                if (result == 0)
                    result = value.compareTo(that.value);
            }
            return result;
        }


        public void setType(String type) {
            this.type = type;
        }

        public void setValue(Object value) {
            this.value = value == null ? null : MemberDescription.valueToString(value);
        }
    }


    public static class ValueWrap {
        String value;

        public ValueWrap(String s) {
            value = s;
        }

        public String toString() {
            return value;
        }
    }

    public Member findByName(String name) {
        if (members != null)
            for (Iterator it = members.iterator(); it.hasNext();) {
                Member m = (Member) it.next();
                if (m.name.equals(name))
                    return m;
            }

        return null;
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(ANNOTATION_PREFIX).append(" ").append(target).append(" ");

        sb.append(name).append('(');
        int i = 0;
        if (members != null) {
            for (Iterator it = members.iterator(); it.hasNext();) {
                Member m = (Member) it.next();
                if (i++ != 0)
                    sb.append(", ");
                sb.append(m.type).append(' ').append(m.name).append("=");
                sb.append(m.value);
            }
        }
        sb.append(')');

        return sb.toString();
    }

    // Opposite action that toString() method does.
    // TODO should be moved to the parser as well as "toString" moved to the writer  
    public static AnnotationItem parse(String str) {

//        str = "anno 0 javax.xml.ws.BindingType(java.lang.String value=\"http://schemas.xmlsoap.org/wsdl/soap/http\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2005/08/addressing/module\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\"http://www.w3.org/2004/08/soap/features/http-optimization\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\"MTOM_THRESHOLD\", java.lang.String value=\"1000\")])]):     anno 0 javax.xml.ws.BindingType(java.lang.String value=\\\"http://schemas.xmlsoap.org/wsdl/soap/http\\\", javax.xml.ws.Feature[] features=[anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2005/08/addressing/module\\\", javax.xml.ws.FeatureParameter[] parameters=[]), anno 0 javax.xml.ws.Feature(boolean enabled=true, java.lang.String value=\\\"http://www.w3.org/2004/08/soap/features/http-optimization\\\", javax.xml.ws.FeatureParameter[] parameters=[anno 0 javax.xml.ws.FeatureParameter(java.lang.String name=\\\"MTOM_THRESHOLD\\\", java.lang.String value=\\\"1000\\\")])])";

        AnnotationItem item = new AnnotationItem();

        if (!str.startsWith(ANNOTATION_PREFIX))
            throw new IllegalArgumentException(i18n.getString("AnnotationItem.error.bad_annotation_descr") + str);

        int pos;
        pos = str.indexOf(' ');
        // skip the prefix
        str = str.substring(pos).trim();

        pos = str.indexOf(' ');
        item.setTarget(Integer.valueOf(str.substring(0, pos)).intValue());
        // remove target
        str = str.substring(pos + 1);

        pos = str.indexOf('(');
        item.setName(str.substring(0, pos).trim());
        str = str.substring(pos + 1, str.lastIndexOf(')'));

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

    private static int parseMember(AnnotationItem item, String str) {

        int pos, result = 0;

        Member m = new Member();

        pos = str.indexOf(' ');

        // java.lang.Class<? extends java.util.ArrayList<? super javax.swing.JLabel>> value=class com.sun.tdk.signaturetest.model.Regtest_6564000$CL_4
        if (str.startsWith(CLASS_PREFIX + "<")) {
            // skip possible spaces inside
            char [] strChar = str.toCharArray();
            int level = 0;
            for (int i = CLASS_PREFIX.length(); i < strChar.length; i++) {
                if(strChar[i] == '<') level++;
                else if(strChar[i] == '>') level--;
                if (level == 0 && strChar[i+1] == ' ') {
                    pos = i+1;
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
                if (str.startsWith(ANNOTATION_PREFIX)) {
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
        item.addMember(m);

        result += pos;
        return result;
    }

    private static int findClosingBracket(String str, int startPos, char openingChar, char closingChar) {

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

    public void setInheritable(boolean inh) {
        inheritable = inh;
    }

    public static AnnotationItem[] toArray(List/*AnnotationItem*/ alist) {
        if (alist == null || alist.size() == 0)
            return EMPTY_ANNOTATIONITEM_ARRAY;

        final int asize = alist.size();
        AnnotationItem[] tmp = new AnnotationItem[asize];
        for (int i = 0; i < asize; ++i)
            tmp[i] = (AnnotationItem) alist.get(i);
        return tmp;
    }
}


