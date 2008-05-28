/*
 * $Id$
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

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.ClassDescription;

import java.util.List;
import java.util.Set;

/**
 * @author Roman Makarchuk
 */
public interface ClassHierarchy extends  ClassDescriptionLoader {

    // track mode
    public static final int ALL_PUBLIC = 2;
    
    String getSuperClass(String fqClassName) throws ClassNotFoundException;

    List /* String */ getSuperClasses(String fqClassName) throws ClassNotFoundException;

    String[] getSuperInterfaces(String fqClassName) throws ClassNotFoundException;

    Set /* String */ getAllImplementedInterfaces(String fqClassName) throws ClassNotFoundException;

    String[] getDirectSubclasses(String fqClassName);

    String[] getAllSubclasses(String fqClassName);

    String[] getNestedClasses(String fqClassName);

    boolean isInterface(String fqClassName) throws ClassNotFoundException;

    int getClassModifiers(String fqClassName) throws ClassNotFoundException;

    boolean isSubclass(String subClassName, String superClassName) throws ClassNotFoundException;

    ClassDescription load(String name) throws ClassNotFoundException;

    boolean isAccessible(ClassDescription c);

    boolean isDocumentedAnnotation(String fqname) throws ClassNotFoundException;

    boolean isAccessible(String fqname) throws ClassNotFoundException;

    boolean isClassVisibleOutside(String fqClassName) throws ClassNotFoundException;
    boolean isClassVisibleOutside(ClassDescription cls) throws ClassNotFoundException;    

    int getTrackMode();
}
