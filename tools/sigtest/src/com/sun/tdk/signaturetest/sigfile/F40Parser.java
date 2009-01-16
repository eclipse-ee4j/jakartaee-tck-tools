/*
 * $Id: F40Parser.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import com.sun.tdk.signaturetest.model.*;

import java.util.*;

/**
 * Parse string representation used in sigfile v3.1 and create corresponding member object
 *
 * @author Roman Makarchuk
 */
class F40Parser implements Parser {

    private String line;
    private int linesz;
    private int idx;
    private char chr;
    private List elems;

    private String currentClassName;

    public ClassDescription parseClassDescription(String classDefinition, List /*String*/ members) {

        ClassDescription classDescription = (ClassDescription) parse(classDefinition);

        MemberDescription m = classDescription;
        List alist = new ArrayList();
        List items = new ArrayList();

        int method_count = 0, field_count = 0, constructor_count = 0, inner_count = 0, interfaces_count = 0;

        for (Iterator it = members.iterator(); it.hasNext();) {

            String str = (String) it.next();

            if (str.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                alist.add(str);
            } else if (str.startsWith(F40Format.HIDDEN_FIELDS)) {
                Set internalFields = parseInternals(str);
                classDescription.setInternalFields(internalFields);
            } else if (str.startsWith(F40Format.HIDDEN_CLASSES)) {
                Set internalClasses = parseInternals(str);
                classDescription.setInternalClasses(internalClasses);
            } else {
                appendAnnotations(m, alist);
                m = parse(str);

                MemberType mt = m.getMemberType();

                if (mt == MemberType.METHOD)
                    method_count++;
                else if (mt == MemberType.FIELD)
                    field_count++;
                else if (mt == MemberType.CONSTRUCTOR)
                    constructor_count++;
                else if (mt == MemberType.INNER)
                    inner_count++;
                else if (mt == MemberType.SUPERINTERFACE)
                    interfaces_count++;

                if (m != classDescription)
                    items.add(m);
            }
        }

        appendAnnotations(m, alist);

        if (constructor_count > 0)
            classDescription.createConstructors(constructor_count);
        if (method_count > 0)
            classDescription.createMethods(method_count);
        if (field_count > 0)
            classDescription.createFields(field_count);
        if (inner_count > 0)
            classDescription.createNested(inner_count);
        if (interfaces_count > 0)
            classDescription.createInterfaces(interfaces_count);


        constructor_count = 0;
        method_count = 0;
        field_count = 0;
        inner_count = 0;
        interfaces_count = 0;

        for (Iterator it = items.iterator(); it.hasNext();) {
            m = (MemberDescription) it.next();
            MemberType mt = m.getMemberType();

            if (mt == MemberType.METHOD) {
                classDescription.setMethod(method_count, (MethodDescr) m);
                method_count++;
            } else if (mt == MemberType.FIELD) {
                classDescription.setField(field_count, (FieldDescr) m);
                field_count++;
            } else if (mt == MemberType.CONSTRUCTOR) {
                classDescription.setConstructor(constructor_count, (ConstructorDescr) m);
                constructor_count++;
            } else if (mt == MemberType.INNER) {
                classDescription.setNested(inner_count, (InnerDescr) m);
                inner_count++;
            } else if (mt == MemberType.SUPERCLASS) {
                classDescription.setSuperClass((SuperClass) m);
            } else if (mt == MemberType.SUPERINTERFACE) {
                SuperInterface si = (SuperInterface) m;
                si.setDirect(true);
                classDescription.setInterface(interfaces_count, si);
                interfaces_count++;
            } else
                assert false;
        }

        return classDescription;
    }

    private Set parseInternals(String str) {

        Set result = new HashSet();
        int startPos = str.indexOf(' ') + 1;
        int nextPos;
        do {
            nextPos = str.indexOf(',', startPos);
            String name;
            if (nextPos != -1) {
                name = str.substring(startPos, nextPos);
                startPos = nextPos + 1;
            } else {
                name = str.substring(startPos);
            }

            result.add(name);

        } while (nextPos != -1);

        return result;
    }

    private void appendAnnotations(MemberDescription fid, List/*String*/ alist) {
        if (alist.size() != 0) {

            AnnotationItem[] tmp = new AnnotationItem[alist.size()];

            for (int i = 0; i < alist.size(); ++i)
                tmp[i] = AnnotationItem.parse((String) alist.get(i));

            fid.setAnnoList(tmp);
            alist.clear();
        }
    }

    private MemberDescription parse(String definition) {
        MemberDescription member = null;

        MemberType type = MemberType.getItemType(definition);

        if (type == MemberType.CLASS) {
            member = parse(new ClassDescription(), definition);
            currentClassName = member.getQualifiedName();
        } else if (type == MemberType.CONSTRUCTOR)
            member = parse(new ConstructorDescr(), definition);
        else if (type == MemberType.METHOD)
            member = parse(new MethodDescr(), definition);
        else if (type == MemberType.FIELD)
            member = parse(new FieldDescr(), definition);
        else if (type == MemberType.SUPERCLASS)
            member = parse(new SuperClass(), definition);
        else if (type == MemberType.SUPERINTERFACE)
            member = parse(new SuperInterface(), definition);
        else if (type == MemberType.INNER)
            member = parse(new InnerDescr(), definition);
        else
            assert false;  // unknown member type

        return member;
    }


    private void init(MemberDescription m, String def) {
        //System.out.println(def);
        line = def.trim();
        linesz = line.length();

        // skip member type
        idx = def.indexOf(' ');

        scanElems();
    }

    private MemberDescription parse(ClassDescription cls, String def) {

        init(cls, def);

        cls.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        cls.setupGenericClassName(s);

        return cls;
    }


    private MemberDescription parse(ConstructorDescr ctor, String def) {

        init(ctor, def);

        ctor.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        if (s != null && s.charAt(0) == '<') {
            ctor.setTypeParameters(s);
            s = getElem();
        }

        ctor.setupConstuctorName(s, currentClassName);

        s = getElem();
        if (s.charAt(0) != '(')
            err();

        if (!"()".equals(s))
            ctor.setArgs(s.substring(1, s.length()-1 ));

        if (elems.size() != 0) {
            s = getElem();
            if (!s.equals("throws"))
                err();
            s = getElem();
            ctor.setThrowables(s);
        }

        return ctor;
    }


    private MemberDescription parse(MethodDescr method, String def) {

        init(method, def);

        method.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        if (s != null && s.charAt(0) == '<') {
            method.setTypeParameters(s);
            s = getElem();
        }

        method.setType(s);

        method.setupMemberName(getElem(), currentClassName);

        s = getElem();
        if (s.charAt(0) != '(')
            err();

        if (!"()".equals(s))
            method.setArgs(s.substring(1, s.length()-1 ));

        if (elems.size() != 0) {
            s = getElem();
            if (!s.equals("throws"))
                err();
            s = getElem();
            method.setThrowables(s);
        }

        return method;
    }

    private MemberDescription parse(FieldDescr field, String def) {

        init(field, def);

        field.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        field.setType(s);

        s = getElem();

        field.setupMemberName(s, currentClassName);

        if (elems.size() != 0) {
            s = getElem();
            if (!s.startsWith("="))
                err();

            field.setConstantValue(s.substring(1).trim());
        }

        return field;
    }

    private MemberDescription parse(SuperClass superCls, String def) {

        init(superCls, def);

        int n = elems.size();
        if (n == 0)
            err();
        superCls.setupGenericClassName((String) elems.get(n - 1));

        return superCls;
    }

    private MemberDescription parse(SuperInterface superIntf, String def) {

        init(superIntf, def);

        int n = elems.size();
        if (n == 0)
            err();

        superIntf.setupGenericClassName((String) elems.get(n - 1));
        return superIntf;
    }

    private MemberDescription parse(InnerDescr inner, String def) {

        init(inner, def);

        inner.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        inner.setupInnerClassName(s, currentClassName);

        return inner;
    }

    private String getElem() {
        String s = null;

        if (elems.size() != 0) {
            s = (String) elems.get(0);
            elems.remove(0);
        }

        if (s == null)
            err();

        return s;
    }


    private void scanElems() {
        elems = new LinkedList();

        for (; ;) {

            //  skip leading blanks at the start of lexeme
            while (idx < linesz && (chr = line.charAt(idx)) == ' ') idx++;

            //  test for end of line
            if (idx >= linesz)
                break;

            //  store the start position of lexeme
            int pos = idx;

            if (chr == '=') {
                idx = linesz;
                elems.add(line.substring(pos));
                break;
            }

            if (chr == '(') {
                idx++;
                skip(')');
                idx++;
                elems.add(line.substring(pos, idx));
                continue;
            }

            if (chr == '<') {
                idx++;
                skip('>');
                idx++;
                elems.add(line.substring(pos, idx));
                continue;
            }

            idx++;
            while (idx < linesz) {
                chr = line.charAt(idx);

                if (chr == '<') {
                    idx++;
                    skip('>');
                    idx++;
                    continue;
                }

                if (chr == ' ' || chr == '(')
                    break;

                idx++;
            }
            elems.add(line.substring(pos, idx));
        }
    }


    private void skip(char term) {
        for (; ;) {
            if (idx >= linesz)
                err();

            if ((chr = line.charAt(idx)) == term)
                return;

            if (chr == '(') {
                idx++;
                skip(')');
                idx++;
                continue;
            }

            if (chr == '<') {
                idx++;
                skip('>');
                idx++;
                continue;
            }

            idx++;
        }
    }

    private void err() {
        throw new Error(line);
    }
}
