/*
 * $Id: SuperInterface.java 4504 2008-03-13 16:12:22Z sg215604 $
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

/**
 * @author Roman Makarchuk
 */
public final class SuperInterface extends MemberDescription {

    public static final SuperInterface[] EMPTY_ARRAY = new SuperInterface[0];

    public SuperInterface() {
        super(MemberType.SUPERINTERFACE, CLASS_DELIMITER);
    }

    // NOTE: Change this method carefully if you changed the code,
    // please, update the method isCompatible() in order it works as previously
    public boolean equals(Object o) {
        if (!(o instanceof SuperInterface))
            return false;

        return name == ((SuperInterface)o).name;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String getQualifiedName() {
        return name;
    }

    public String getName() {
        return getClassShortName(name);
    }

    public boolean isCompatible(MemberDescription m) {
        if (!equals(m))
            throw new IllegalArgumentException("Only equal members can be checked for compatibility!");

        return SwissKnife.equals(typeParameters, m.typeParameters);
    }

    public boolean isSuperInterface() {
        return true;
    }


    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    private boolean direct;

    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("interface");

        if (isDirect())
            buf.append(" @");

        buf.append(' ');
        buf.append(name);

        if (typeParameters != null)
            buf.append(typeParameters);

        return buf.toString();
    }
}
