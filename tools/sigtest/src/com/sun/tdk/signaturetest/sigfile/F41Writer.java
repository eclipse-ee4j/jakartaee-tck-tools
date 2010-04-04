/*
 * $Id: F41Writer.java 4504 2008-03-13 16:12:22Z sg215604 $
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
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.model.Modifier;

public class F41Writer extends F40Writer {

    public F41Writer() {
        format = new F41Format();
    }

    protected void writeHiders(ClassDescription classDescription, StringBuffer buf) {
        super.writeHiders(classDescription, buf);
        writeInternalMembers(buf, F41Format.X_FIELDS, classDescription.getXFields());
        writeInternalMembers(buf, F41Format.X_CLASSES, classDescription.getXClasses());
    }


   protected void write(StringBuffer buf, ClassDescription m) {

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

       if (m.getOuterClass() != null &&
               !m.getOuterClass().equals(MemberDescription.NO_DECLARING_CLASS)) {
           buf.append("\n " + ClassDescription.OUTER_PREFIX + " ");
           buf.append(m.getOuterClass());
       }

       AnnotationItem[] annoList = m.getAnnoList();
       for (int i = 0; i < annoList.length; ++i) {
           buf.append("\n ");
           buf.append(annoList[i]);
       }
   }


}
