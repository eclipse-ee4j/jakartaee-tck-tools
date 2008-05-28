/*
 * $Id: Classpath.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.classpath;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Roman Makarchuk
 */
public interface Classpath {

    /**
     * Initialize the module with given parameter: <code>classPath</code>.
     *
     * @param classPath parameter provided to initialize module (usually a directory
     *                  or file classPath).
     */
    public void init(String classPath) throws IOException;

    /**
     * Free resources used (if any) or do nothing.
     */
    public void close();

    /**
     * @return true if more classes available
     */
    public boolean hasNext();

    /**
     * Return name of the next available class.
     *
     * @return Class qualified name
     */
    public String nextClassName();

    /**
     * Reset enumeration of classes which are found by this module.
     */
    public void setListToBegin();

    /**
     * Returns <b>InputStream</b> instance providing bytecode for the required class.
     * If classpath has several classes with the same qualified name, an implementation must always
     * return first class in the path
     */

    public InputStream findClass(String qualifiedClassName) throws IOException, ClassNotFoundException;
} 
