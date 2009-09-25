/*
 * $Id: F40Writer.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import java.io.PrintWriter;
import java.util.*;

/**
 * @author Roman Makarchuk
 */
public class F40Writer implements Writer {

    private Set features = new HashSet();
    protected Format format;
    private PrintWriter out;

    private boolean isConstantValuesSaved = true;
    private String apiVersion;

    private StringBuffer buf = new StringBuffer(512);
    private ArrayList members = new ArrayList();


    public F40Writer() {
        format = new F40Format();
    }

    public void init(PrintWriter out) {
        this.out = out;
    }

    public void addFeature(Format.Feature feature) {
        assert format.isFeatureSupported(feature);
        features.add(feature);
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void writeHeader() {

        out.println(format.getVersion());
        out.println(Format.VERSION + apiVersion);

        if (!features.contains(FeaturesHolder.ConstInfo)) {
            out.println(FeaturesHolder.ConstInfo);
            isConstantValuesSaved = false;
        }

        if (!features.contains(FeaturesHolder.TigerInfo))
            out.println(FeaturesHolder.TigerInfo);

        out.println("");
    }

    public void write(ClassDescription classDescription) {

        buf.setLength(0);
        members.clear();

        String clsName = classDescription.getQualifiedName();

        // sorts members
        for (Iterator e = classDescription.getMembersIterator(); e.hasNext();) {

            MemberDescription mr = (MemberDescription) e.next();

            if ((mr.isMethod() || mr.isField() || mr.isInner()) && !clsName.equals(mr.getDeclaringClassName()))
                continue;

            if (mr.isSuperInterface() && !((SuperInterface) mr).isDirect())
                continue;

            write(buf, mr);
            members.add(buf.toString());
            buf.setLength(0);
        }

        Collections.sort(members);

        // print class description
        write(buf, classDescription);
        out.println(buf.toString());

        int size = members.size();
        for (int i = 0; i < size; i++)
            out.println(members.get(i));

        if (format.isFeatureSupported(FeaturesHolder.ListOfHiders)) {
            writeHiders(classDescription, buf);
        }

        // write empty string
        out.println("");
    }

    protected void writeHiders(ClassDescription classDescription, StringBuffer buf) {
        writeInternalMembers(buf, F40Format.HIDDEN_FIELDS, classDescription.getInternalFields());
        writeInternalMembers(buf, F40Format.HIDDEN_CLASSES, classDescription.getInternalClasses());
    }


    private void write(StringBuffer buf, MemberDescription m) {

        MemberType type = m.getMemberType();

        if (type == MemberType.CLASS)
            write(buf, (ClassDescription) m);
        else if (type == MemberType.CONSTRUCTOR)
            write(buf, (ConstructorDescr) m);
        else if (type == MemberType.METHOD)
            write(buf, (MethodDescr) m);
        else if (type == MemberType.FIELD)
            write(buf, (FieldDescr) m);
        else if (type == MemberType.SUPERCLASS)
            write(buf, (SuperClass) m);
        else if (type == MemberType.SUPERINTERFACE)
            write(buf, (SuperInterface) m);
        else if (type == MemberType.INNER)
            write(buf, (InnerDescr) m);
        else
            assert false;  // unknown member type
    }


    private void write(StringBuffer buf, ClassDescription m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);

        String modifiers = Modifier.toString(memberType, m.getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        buf.append(' ');
        buf.append(m.getQualifiedName());

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null)
            buf.append(typeParameters);

        AnnotationItem[] annoList = m.getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }
    }


    private void write(StringBuffer buf, ConstructorDescr m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);

        String modifiers = Modifier.toString(memberType, m.getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null) {
            buf.append(' ');
            buf.append(typeParameters);
        }

        buf.append(' ');
        buf.append(m.getName());
        buf.append('(');
        buf.append(m.getArgs());
        buf.append(')');

        String throwables = m.getThrowables();
        if (throwables.length() > 0) {
            buf.append(" throws ");
            buf.append(throwables);
        }

        AnnotationItem[] annoList = m.getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }
    }

    private void write(StringBuffer buf, MethodDescr m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);

        String modifiers = Modifier.toString(memberType, m.getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null) {
            buf.append(' ');
            buf.append(typeParameters);
        }

        String type = m.getType();

        if (type.length() != 0) {
            buf.append(' ');
            buf.append(type);
        }

        buf.append(' ');
        buf.append(m.getName());
        buf.append('(');
        buf.append(m.getArgs());
        buf.append(')');

        String throwables = m.getThrowables();
        if (throwables.length() > 0) {
            buf.append(" throws ");
            buf.append(throwables);
        }

        AnnotationItem[] annoList = m.getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }
    }

    private void write(StringBuffer buf, FieldDescr m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);

        String modifiers = Modifier.toString(memberType, m.getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        String type = m.getType();

        if (type.length() != 0) {
            buf.append(' ');
            buf.append(type);
        }

        buf.append(' ');
        buf.append(m.getName());

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null) {
            buf.append(typeParameters);
        }

        String constantValue = m.getConstantValue();

        if (isConstantValuesSaved && constantValue != null) {
            buf.append(" = ");
            buf.append(constantValue);
        }

        AnnotationItem[] annoList = m.getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }
    }

    private void write(StringBuffer buf, InnerDescr m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);

        String modifiers = Modifier.toString(memberType, m.getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        buf.append(' ');
        buf.append(m.getName());
    }

    private void write(StringBuffer buf, SuperClass m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);
        buf.append(' ');
        buf.append(m.getQualifiedName());

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null) {
            buf.append(typeParameters);
        }
    }

    private void write(StringBuffer buf, SuperInterface m) {

        MemberType memberType = m.getMemberType();

        buf.append(memberType);
        buf.append(' ');
        buf.append(m.getQualifiedName());

        String typeParameters = m.getTypeParameters();

        if (typeParameters != null) {
            buf.append(typeParameters);
        }
    }

    protected void writeInternalMembers(StringBuffer buf, String prefix, Set internalMembers) {

        // sort members   
        ArrayList intMembers = new ArrayList();
        intMembers.addAll(internalMembers);
        Collections.sort(intMembers);

        buf.setLength(0);

        buf.append(prefix);
        buf.append(" ");
        int count = 0;
        for (Iterator i = intMembers.iterator(); i.hasNext();) {
            if (count != 0)
                buf.append(',');
            buf.append(i.next());
            count++;
        }

        if (count == 0) {
            buf.setLength(0);
        }

        if (buf.length() > 0) {
            out.println(buf);
        }

    }


    public void close() {
        out.close();
    }
}
