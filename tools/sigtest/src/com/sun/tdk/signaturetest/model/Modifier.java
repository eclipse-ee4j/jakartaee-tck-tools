/*
 * $Id: Modifier.java 4516 2008-03-17 18:48:27Z eg216457 $
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Makarchuk
 */
public final class Modifier implements Serializable {

    private static final Map knownModifiers = new HashMap();

    public static final Modifier ABSTRACT = new Modifier("abstract", 0x00000400, java.lang.reflect.Modifier.ABSTRACT,  true);
    public static final Modifier PUBLIC = new Modifier("public", 0x00000001, java.lang.reflect.Modifier.PUBLIC, true);
    public static final Modifier PRIVATE = new Modifier("private", 0x00000002, java.lang.reflect.Modifier.PRIVATE, true);
    public static final Modifier PROTECTED = new Modifier("protected", 0x00000004, java.lang.reflect.Modifier.PROTECTED, true);
    public static final Modifier STATIC = new Modifier("static", 0x00000008, java.lang.reflect.Modifier.STATIC, true);
    public static final Modifier FINAL = new Modifier("final", 0x00000010, java.lang.reflect.Modifier.FINAL, true);
    public static final Modifier SYNCHRONIZED = new Modifier("synchronized", 0x00000020, java.lang.reflect.Modifier.SYNCHRONIZED, false);
    public static final Modifier NATIVE = new Modifier("native", 0x00000100, java.lang.reflect.Modifier.NATIVE, false);
    public static final Modifier INTERFACE = new Modifier("interface", 0x00000200, java.lang.reflect.Modifier.INTERFACE, true);

    public static final Modifier TRANSIENT = new Modifier("transient", 0x00000080, java.lang.reflect.Modifier.TRANSIENT, false);
    public static final Modifier VOLATILE = new Modifier("volatile", 0x00000040, java.lang.reflect.Modifier.VOLATILE, true);

    // used for classes only
    public static final Modifier ENUM = new Modifier("!enum", 0x00004000, 0, true);
    // This modifier is equivalent of ENUM but it's not tracked and used for fields only
    public static final Modifier FIELD_ENUM = new Modifier("!fld_enum", 0x00004000, 0, false);


    public static final Modifier ANNOTATION = new Modifier("!annotation", 0x00002000, 0, true);
    public static final Modifier VARARGS = new Modifier("!varargs", 0x00000080, 0, true);
    public static final Modifier BRIDGE = new Modifier("!bridge", 0x00000040, 0, false);
    public static final Modifier HASDEFAULT = new Modifier("!hasdefault", 0x02000000, 0, true);

    public static final Modifier ACC_SUPER = new Modifier("acc_super", 0x00000020, 0, false);    
    public static final Modifier ACC_SYNTHETIC = new Modifier("acc_synthetic", 0x00001000, 0, false);
    public static final Modifier ACC_STRICT = new Modifier("acc_strict", 0x00000800, java.lang.reflect.Modifier.STRICT, false);

    public String toString() {
        return name;
    }

    public boolean isTracked() {
        return isTracked;
    }

    public int getValue() {
        return value;
    }

    public void setTracked(boolean isTracked) {
        this.isTracked = isTracked;
    }

    public static Modifier getModifier(String name) {
        return (Modifier) knownModifiers.get(name);
    }

    public static Modifier[] getAllModifiers() {
        return (Modifier[]) knownModifiers.values().toArray(new Modifier[] {});
    }


    public static int scanModifiers(List elems) {

        int result = 0;

        while (elems.size() > 0) {
            Modifier m = getModifier((String) elems.get(0));
            if (m != null) {
                result |= m.value;
                elems.remove(0);
            } else break;
        }
        return result;
    }

    public static boolean hasModifier(int modifiers, Modifier m) {
        return (modifiers & m.value) != 0;
    }

    static int addModifier(int modifiers, Modifier m) {
        return modifiers | m.value;
    }

    static int removeModifier(int modifiers, Modifier m) {
        return modifiers & ~m.value;
    }

    public static String toString(MemberType type, int modifiers, boolean trackedOnly) {

        StringBuffer buf = new StringBuffer();

        Modifier[] applicableModifiers = type.getApplicableModifiers();
        boolean addSpace = false;

        for (int i = 0; i < applicableModifiers.length; ++i) {
            Modifier m = applicableModifiers[i];
            if ((m.value & modifiers) != 0 && (!trackedOnly || m.isTracked())) {
                if (addSpace)
                    buf.append(' ');
                addSpace = true;
                buf.append(m.name);
            }
        }

        return buf.toString();
    }

    private Modifier(String name, int vmID, int reflID, boolean isTracked) {

        if (vmID != reflID && reflID != 0)
            throw new IllegalArgumentException();

        this.name = name;
        this.value = vmID;
        this.isTracked = isTracked;
        knownModifiers.put(name, this);
    }

    public Modifier(String name, boolean isTracked) {

        Modifier[] ms = Modifier.getAllModifiers();
        int v = 0;

        for (int i = 0; i < ms.length; i++) {
            if (ms[i].name.equals(name)) {
                throw new IllegalArgumentException("Name " + name + " is already used");
            }
            v |= ms[i].getValue();
        }
        v = highestOneBit(~v);
        if (v == 0) {
            throw new IllegalArgumentException("No room for the new modifier " + name);
        }

        this.name = name;
        this.value = v;
        this.isTracked = isTracked;
        knownModifiers.put(name, this);
    }


    /**
     * This is copy of Integer.highestOneBit from JDK 1.5
     * Because this class must be 1.4 compatible
     * we can not use the original method
     */
    private int highestOneBit(int i) {
        i |= (i >>  1);
        i |= (i >>  2);
        i |= (i >>  4);
        i |= (i >>  8);
        i |= (i >> 16);
        return i - (i >>> 1);
    }

    private final String name;
    private final int value;

    // true if the modifier has an influence on compatibility
    private boolean isTracked;

}
