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
 * Represents Type Annotations (JSR 308)
 *
 * @author Mikhail Ershov
 */
public class AnnotationItemEx extends AnnotationItem {

    public static final String ANNOTATION_EX_PREFIX = "annoEx";

    public AnnotationItemEx(int target, String name) {
        super(target, name);
    }

    // for parser
    public AnnotationItemEx() {
    }

    private int targetType;
    private int [] locations;
    private int parameterIndex;
    private int boundIndex;
    private int typeIndex;

    // tracked type annotations
    public static final int TARGET_METHOD_RECEIVER = 0x06;
    public static final int TARGET_METHOD_RETURN_TYPE_GENERIC_ARRAY = 0x0B;
    public static final int TARGET_METHOD_PARAMETER_GENERIC_ARRAY = 0x0D;
    public static final int TARGET_FIELD_GENERIC_ARRAY = 0x0F;
    public static final int TARGET_CLASS_TYPE_PARAMETER_BOUND = 0x10;
    public static final int TARGET_CLASS_TYPE_PARAMETER_BOUND_GENERIC_ARRAY = 0x11;
    public static final int TARGET_METHOD_TYPE_PARAMETER_BOUND = 0x12;
    public static final int TARGET_METHOD_TYPE_PARAMETER_BOUND_GENERIC_ARRAY = 0x13;
    public static final int TARGET_CLASS_EXTENDS_IMPLEMENTS = 0x14;
    public static final int TARGET_CLASS_EXTENDS_IMPLEMENTS_GENERIC_ARRAY = 0x15;
    public static final int TARGET_EXCEPTION_TYPE_IN_THROWS = 0x16;
    public static final int TARGET_WILDCARD_BOUND = 0x1C;
    public static final int TARGET_WILDCARD_BOUND_GENERIC_ARRAY = 0x1D;
    public static final int TARGET_METHOD_TYPE_PARAMETER = 0x20;
    public static final int TARGET_CLASS_TYPE_PARAMETER = 0x22;

    // ignored type annotations
    public static final int TARGET_TYPECAST = 0x00;
    public static final int TARGET_TYPECAST_GENERIC_ARRAY = 0x01;
    public static final int TARGET_TYPE_TEST = 0x02;
    public static final int TARGET_TYPE_TEST_GENERIC_ARRAY = 0x03;
    public static final int TARGET_OBJECT_CREATION = 0x04;
    public static final int TARGET_OBJECT_CREATION_GENERIC_ARRAY = 0x05;
    public static final int TARGET_LOCAL_VARIABLE = 0x08;
    public static final int TARGET_LOCAL_VARIABLE_GENERIC_ARRAY = 0x09;
    public static final int TARGET_TYPE_ARGUMENT_IN_CONSTRUCTOR_CALL = 0x18;
    public static final int TARGET_TYPE_ARGUMENT_IN_CONSTRUCTOR_CALL_GENERIC_ARRAY = 0x19;
    public static final int TARGET_TYPE_ARGUMENT_IN_METHOD_CALL = 0x1A;
    public static final int TARGET_TYPE_ARGUMENT_IN_METHOD_CALL_GENERIC_ARRAY = 0x1B;
    public static final int TARGET_CLASS_LITERAL = 0x1E;
    public static final int TARGET_CLASS_LITERAL_GENERIC_ARRAY = 0x1F;

    public static final String ANN_TARGET_TYPE = "type";
    public static final String ANN_TYPE_IND = "typeIndex";
    public static final String ANN_BOUND_IND = "boundIndex";
    public static final String ANN_LOCATIONS = "locations";
    public static final String ANN_PARAM_IND = "parameterIndex";

    public int getTargetType() {
        return targetType;
    }

    public AnnotationItemEx setTargetType(int target_type) {
        this.targetType = target_type;
        return this;
    }

    public int[] getLocations() {
        return locations;
    }

    public AnnotationItemEx setLocations(int[] locations) {
        this.locations = locations;
        return this;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public AnnotationItemEx setParameterIndex(int parameterIndex) {
        this.parameterIndex = parameterIndex;
        return this;
    }

    public int getBoundIndex() {
        return boundIndex;
    }

    public AnnotationItemEx setBoundIndex(int boundIndex) {
        this.boundIndex = boundIndex;
        return this;
    }

    public int getTypeIndex() {
        return typeIndex;
    }

    public AnnotationItemEx setTypeIndex(int typeIndex) {
        this.typeIndex = typeIndex;
        return this;
    }

    // -----------------
    protected String getSpecificData() {
        StringBuffer sb = new StringBuffer();
        addTargetType(sb);
        switch (targetType) {
            case TARGET_METHOD_RETURN_TYPE_GENERIC_ARRAY:
            case TARGET_FIELD_GENERIC_ARRAY:
                addLocations(sb);
                break;
            case TARGET_METHOD_PARAMETER_GENERIC_ARRAY:
                addParameterInd(sb);
                addLocations(sb);
                break;
            case TARGET_CLASS_TYPE_PARAMETER_BOUND:
            case TARGET_METHOD_TYPE_PARAMETER_BOUND:
                addParameterInd(sb);
                addBoundInd(sb);
                break;
            case TARGET_CLASS_TYPE_PARAMETER_BOUND_GENERIC_ARRAY:
            case TARGET_METHOD_TYPE_PARAMETER_BOUND_GENERIC_ARRAY:
                addParameterInd(sb);
                addBoundInd(sb);
                addLocations(sb);
                break;
            case TARGET_CLASS_EXTENDS_IMPLEMENTS:
            case TARGET_EXCEPTION_TYPE_IN_THROWS:
                addTypeInd(sb);
                break;
            case TARGET_CLASS_EXTENDS_IMPLEMENTS_GENERIC_ARRAY:
                addTypeInd(sb);
                addLocations(sb);
                break;
            case TARGET_METHOD_TYPE_PARAMETER:
            case TARGET_CLASS_TYPE_PARAMETER:
                addParameterInd(sb);
                break;
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    private void addTargetType(StringBuffer sb) {
        sb.append("[" + ANN_TARGET_TYPE + "=" + intToHex(targetType) + ";");
    }

    private void addTypeInd(StringBuffer sb) {
        sb.append(ANN_TYPE_IND + "=" + typeIndex + ";");
    }

    private void addBoundInd(StringBuffer sb) {
        sb.append(ANN_BOUND_IND + "=" + boundIndex + ";");
    }

    private void addLocations(StringBuffer sb) {
        sb.append(ANN_LOCATIONS + "=" + arrayToString(locations) + ";");
    }

    private void addParameterInd(StringBuffer sb) {
        sb.append(ANN_PARAM_IND + "=" + parameterIndex + ";");
    }

    private String intToHex(int i) {
        String s = Integer.toHexString(i).toUpperCase();
        if (s.length() == 1) {
            s = "0" + s;
        }
        return "0x" + s;
    }

    // this code should be 1.3 compatible
    // so we can't use 1.5's Arrays.toString()
    private String arrayToString(int [] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuffer b = new StringBuffer();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.append(']').toString();
            b.append(",");
        }
    }

    protected String getPrefix() {
        return ANNOTATION_EX_PREFIX;
    }


}
