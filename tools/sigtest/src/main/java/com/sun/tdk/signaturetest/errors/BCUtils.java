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


package com.sun.tdk.signaturetest.errors;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.FieldDescr;
import com.sun.tdk.signaturetest.model.MethodDescr;

import java.util.List;

/**
 * @author Mikhail Ershov
 */
abstract class FieldPairedHandler extends PairedHandler {
    protected FieldDescr f1;
    protected FieldDescr f2;

    boolean acceptMessageList(List l) {
        if (!super.acceptMessageList(l))
            return false;

        init(l);

        if (m1 instanceof FieldDescr && m2 instanceof FieldDescr) {
            f1 = (FieldDescr) m1;
            f2 = (FieldDescr) m2;
            return true;
        }
        return false;
    }
}

abstract class MethodPairedHandler extends PairedHandler {
    protected MethodDescr meth1;
    protected MethodDescr meth2;

    boolean acceptMessageList(List l) {
        if (!super.acceptMessageList(l))
            return false;
        init(l);
        if (m1 instanceof MethodDescr && m2 instanceof MethodDescr) {
            meth1 = (MethodDescr) m1;
            meth2 = (MethodDescr) m2;
            return true;
        }
        return false;
    }
}


abstract class ClassPairedHandler extends PairedHandler {
    protected ClassDescription c1;
    protected ClassDescription c2;

    boolean acceptMessageList(List l) {
        if (!super.acceptMessageList(l))
            return false;

        init(l);

        if (m1 instanceof ClassDescription && m2 instanceof ClassDescription) {
            c1 = (ClassDescription) m1;
            c2 = (ClassDescription) m2;
            return true;
        }
        return false;
    }
}
