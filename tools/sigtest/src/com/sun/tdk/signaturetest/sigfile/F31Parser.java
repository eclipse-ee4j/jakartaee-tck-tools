/*
 * $Id: F31Parser.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Parse string representation used in sigfile v3.1 and create corresponding member object
 *
 * @author Roman Makarchuk
 */ 
class F31Parser implements Parser {

    private String line;
    private int linesz;
    private int idx;
    private char chr;
    private List elems;

    private int directInterfaceCount;

    public ClassDescription parseClassDescription(String classDefinition, List members) {

        directInterfaceCount = 0;

        ClassDescription classDescription = (ClassDescription) parse(classDefinition);
        MemberCollection classMembers = new MemberCollection();
        MemberDescription member = classDescription;
        List alist = new ArrayList();

        for (Iterator it = members.iterator(); it.hasNext();) {
            String str = (String) it.next();
            if (str.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                alist.add(str);
            } else {
                appendAnnotations(member, alist);
                member = parse(str);
                classMembers.addMember(member);
            }
        }

        appendAnnotations(member, alist);

        classDescription.setMembers(classMembers);

        if (directInterfaceCount > 0) {
            classDescription.createInterfaces(directInterfaceCount);
            int count = 0;
            for (Iterator it = classMembers.iterator(); it.hasNext();) {
                MemberDescription mr = (MemberDescription) it.next();
                if (mr.isSuperInterface()) {
                    SuperInterface si = (SuperInterface) mr;
                    if (si.isDirect())
                        classDescription.setInterface(count++, si);
                }
            }
        }
        ArrayList methods = new ArrayList();
        ArrayList fields = new ArrayList();
        ArrayList constrs = new ArrayList();
        ArrayList inners = new ArrayList();
        ArrayList intfs = new ArrayList();
        for (Iterator it = classMembers.iterator(); it.hasNext();) {
        	MemberDescription md = (MemberDescription) it.next();
        	if (md instanceof SuperClass) {
				classDescription.setSuperClass((SuperClass)md);
			} else if (md instanceof SuperInterface) {
				intfs.add(md);
			}
        	if (!md.getDeclaringClassName().equals(classDescription.getQualifiedName()))
        		continue;
        	if (md instanceof MethodDescr) {				
				methods.add(md);
			}else if (md instanceof ConstructorDescr) {
				constrs.add(md);			
			} else if (md instanceof FieldDescr) {
				fields.add(md);				
			} else if (md instanceof InnerDescr) {
				inners.add(md); 
			}
        }
        classDescription.setConstructors((ConstructorDescr[])constrs.toArray(new ConstructorDescr[0]));
        classDescription.setMethods((MethodDescr[])methods.toArray(new MethodDescr[0]));
        classDescription.setFields((FieldDescr[])fields.toArray(new FieldDescr[0]));
        classDescription.setNestedClasses((InnerDescr[])inners.toArray(new InnerDescr[0]));
        classDescription.setInterfaces((SuperInterface[])intfs.toArray(new SuperInterface[0]));
        return classDescription;
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

        if (type == MemberType.CLASS)
            member = parse(new ClassDescription(), definition);
        else if (type == MemberType.CONSTRUCTOR)
            member = parse(new ConstructorDescr(), definition);
        else if (type == MemberType.METHOD)
            member = parse(new MethodDescr(), definition);
        else if (type == MemberType.FIELD)
            member = parse(new FieldDescr(), definition);
        else if (type == MemberType.SUPERCLASS)
            member = parse(new SuperClass(), definition);
        else if (type == MemberType.SUPERINTERFACE) {
            member = parse(new SuperInterface(), definition);
            if (((SuperInterface) member).isDirect())
                ++directInterfaceCount;
        } else if (type == MemberType.INNER)
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
        //System.out.println(elems);
    }

    protected MemberDescription parse(ClassDescription cls, String def) {

        init(cls, def);

        cls.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        cls.setupGenericClassName(s);

        return cls;
    }


    protected MemberDescription parse(ConstructorDescr ctor, String def) {

        init(ctor, def);

        ctor.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        if (s != null && s.charAt(0) == '<') {
            ctor.setTypeParameters(s);
            s = getElem();
        }

        ctor.setupConstuctorName(s);

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


    protected MemberDescription parse(MethodDescr method, String def) {

        init(method, def);

        method.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        if (s != null && s.charAt(0) == '<') {
            method.setTypeParameters(s);
            s = getElem();
        }

        method.setType(s);

        method.setupMemberName(getElem());

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

    protected MemberDescription parse(FieldDescr field, String def) {

        init(field, def);

        field.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        field.setType(s);

        s = getElem();

        field.setupMemberName(s);

        if (elems.size() != 0) {
            s = getElem();
            if (!s.startsWith("="))
                err();

            field.setConstantValue(s.substring(1).trim());
        }

        return field;
    }

    protected MemberDescription parse(SuperClass superCls, String def) {

        init(superCls, def);

        int n = elems.size();
        if (n == 0)
            err();
        superCls.setupGenericClassName((String) elems.get(n - 1));

        return superCls;
    }

    protected MemberDescription parse(SuperInterface superIntf, String def) {

        init(superIntf, def);

        int n = elems.size();
        if (n == 0)
            err();

        if ("@".equals(elems.get(0)))
            superIntf.setDirect(true);

        superIntf.setupGenericClassName((String) elems.get(n - 1));
        return superIntf;
    }

    protected MemberDescription parse(InnerDescr inner, String def) {

        init(inner, def);

        inner.setModifiers(Modifier.scanModifiers(elems));

        String s = getElem();
        int i = s.lastIndexOf('$');
        if (i < 0)
            err();

        inner.setupClassName(s);

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

    void err() {
        throw new Error(line);
    }
}
