/*
 * $Id: FieldDescr.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import com.sun.tdk.signaturetest.util.SwissKnife;

import java.util.Set;

/**
 * @author Roman Makarchuk
 */
public final class FieldDescr extends MemberDescription {

    // this field was introduced to break FieldDescr class dependency on SigTest.isConstantValuesTracked
    public static boolean  isConstantValuesTracked = true;

    public static final FieldDescr[] EMPTY_ARRAY = new FieldDescr[0];

    public FieldDescr() {
        super(MemberType.FIELD, MEMBER_DELIMITER);
    }

    public FieldDescr(String methodName, String className, int modifiers) {
        super(MemberType.FIELD, MEMBER_DELIMITER);
        setupMemberName(methodName, className);
        setModifiers(modifiers);
    }


    public boolean isField() {
        return true;
    }

    public boolean isConstant() {
        return isFinal() && constantValue != null/*isStatic()*/;
    }


    //  constant value representation or null
    private String constantValue;

    public void setConstantValue(String value) {
        constantValue = value;
    }

    public String getConstantValue() {
        return constantValue;
    }


    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("field");

        String modifiers = Modifier.toString(memberType, getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        if (type.length() != 0) {
            buf.append(' ');
            buf.append(type);
        }

        buf.append(' ');
        buf.append(declaringClass);
        buf.append(delimiter);
        buf.append(name);

        if (typeParameters != null) {
            buf.append(typeParameters);
        }

        String constantValue = getConstantValue();

        if (constantValue != null) {
            buf.append(" = ");
            buf.append(constantValue);
        }

        AnnotationItem[] annoList = getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }

        return buf.toString();
    }


    // NOTE: Change this method carefully if you changed the code,
    // please, update the method isCompatible() in order it works as previously
    public boolean equals(Object o) {
        // == used instead of equals() because name is always assigned via String.intern() call        
        return o instanceof FieldDescr && name==((FieldDescr) o).name;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean isCompatible(MemberDescription m) {
        return isCompatible(m, false);
    }

    public boolean isCompatible(MemberDescription m, boolean noValue) {

        if (!equals(m))
            throw new IllegalArgumentException("Only equal members can be checked for compatibility!");
        
        FieldDescr another = (FieldDescr) m;

        boolean result = memberType.isCompatible(getModifiers(), another.getModifiers());

        if (result) {
            result = type.equals(another.type) &&
                    SwissKnife.equals(typeParameters, another.typeParameters);
            if (result && isConstantValuesTracked && !noValue) {
                result = SwissKnife.equals(constantValue, another.constantValue);
            }
        }

        return result;
    }

    protected void populateDependences(Set set) {
        addDependency(set, type);
    }

}
