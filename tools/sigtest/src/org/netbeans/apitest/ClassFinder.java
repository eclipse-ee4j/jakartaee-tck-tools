/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.apitest;

import java.util.Properties;

/** This class find an load SignatureClass. The method Class.forName
 *  is used for founding of the Class object. If the Class.forName(String,
 *  boolean, ClassLoader) is available, than this method will be
 *  used. Otherwise, Class.forName(String) will be used. **/
final class ClassFinder {
    /** formats member definitions. **/
    protected final DefinitionFormat filter;
    protected final Properties details;
    private final ClassLoader loader;
 
    /** creates ClassFinder. **/
    public ClassFinder(DefinitionFormat filter, Properties details, ClassLoader loader) {
        this.details = details;
        this.filter = filter;
        this.loader = loader;
    } 

    /** loads class with the given name. **/
    public SignatureClass loadClass(String name)
    throws ClassNotFoundException {
        Class<?> clazz = Class.forName(name, false, loader);
        return new SignatureClass(clazz, filter, this, details);
    }
}
