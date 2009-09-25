/*
 * $Id: F41Parser.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import com.sun.tdk.signaturetest.model.ClassDescription;
import java.util.Set;

/**
 * Parse string representation used in sigfile v4.1 and create corresponding member object
 */
class F41Parser extends F40Parser {

    protected boolean parseFutureSpecific(String str, ClassDescription cl) {
        if (str.startsWith(F41Format.X_FIELDS)) {
            Set internalFields = parseInternals(str);
            cl.setXFields(internalFields);
            return true;
        }
        if (str.startsWith(F41Format.X_CLASSES)) {
            Set internalClasses = parseInternals(str);
            cl.setXClasses(internalClasses);
            return true;
        }
        return false;
    }

}

