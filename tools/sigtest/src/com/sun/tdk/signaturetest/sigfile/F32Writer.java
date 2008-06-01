/*
 * $Id: F32Writer.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import java.io.PrintWriter;
import java.util.*;

/**
 * @author Roman Makarchuk
 */
class F32Writer implements Writer {

    private Set features = new HashSet();
    private Format format;
    private PrintWriter out;

    private boolean isConstantValuesSaved = true;
    private String apiVersion;

    private StringBuffer buf = new StringBuffer(512);
    private ArrayList members = new ArrayList();


    public F32Writer(Format format) {
        this.format = format;
    }

    public void init(PrintWriter out) {
        this.out = out;
    }

    public void addFeature(Format.Feature feature) {
        features.add(feature);
    }
    
    public void setAllFeatures(FeaturesHolder features) {
		this.features.clear();
		this.features.addAll(features.getAllSupportedFeatures());
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
    }

    public void write(ClassDescription classDescription) {

        buf.setLength(0);
        members.clear();

        // sorts members
        for (Iterator e = classDescription.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
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

        // write empty string
        out.println("");
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
        buf.append(m.getQualifiedName());
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
        buf.append(m.getQualifiedName());
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
        buf.append(m.getQualifiedName());

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
        buf.append(m.getQualifiedName());
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

    public void close() {
        out.close();
    }
}
