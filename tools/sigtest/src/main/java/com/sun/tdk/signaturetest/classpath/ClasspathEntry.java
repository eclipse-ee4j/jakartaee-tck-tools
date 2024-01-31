/*
 * $Id: ClasspathEntry.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.classpath;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 */
public abstract class ClasspathEntry implements Classpath {

    protected static final String JAVA_CLASSFILE_EXTENSION = ".class";
    protected static final int JAVA_CLASSFILE_EXTENSION_LEN = JAVA_CLASSFILE_EXTENSION.length();
    final protected ClasspathEntry previousEntry;


    protected ClasspathEntry(ClasspathEntry previousEntry) {
        this.previousEntry = previousEntry;
    }

    /**
     * Qualified names for all those classes found in <code>this</code> directory.
     */
    protected LinkedHashSet classes;

    /**
     * This <code>currentPosition</code> iterator is used to browse <code>classes</code>
     */
    protected Iterator currentPosition;

    public boolean hasNext() {
        return currentPosition.hasNext();
    }

    public String nextClassName() {
        return (String) currentPosition.next();
    }

    /**
     * Reset enumeration of classes found in <code>this</code>
     * <b>ClasspathEntry</b>.
     *
     * @see #nextClassName()
     * @see #findClass(String)
     */

    public void setListToBegin() {
        currentPosition = classes.iterator();
    }

    protected boolean contains(String className) {
        return classes.contains(className) || (previousEntry != null && previousEntry.contains(className));
    }

    public boolean isEmpty() {
        return classes.isEmpty();
    }

}
