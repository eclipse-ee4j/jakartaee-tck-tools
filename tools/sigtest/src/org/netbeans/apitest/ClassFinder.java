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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Properties;

/** This class find an load SignatureClass. The method Class.forName
 *  is used for founding of the Class object. If the Class.forName(String,
 *  boolean, ClassLoader) is available, than this method will be
 *  used. Otherwise, Class.forName(String) will be used. **/
public class ClassFinder {
    /** formats member definitions. **/
    protected DefinitionFormat filter;
    protected Properties details;
 
   /** The the Class.forName(String, boolean, ClassLoader) method.
     *  If this method is not available, then rhis field is null. **/
    private Method forName = null;
    /** arguments of the Class.forName method. **/
    private Object[] args = null;

    /** creates ClassFinder. **/
    public ClassFinder(DefinitionFormat filter, Properties details) {
        this.details = details;
        args = new Object[] {
            "",
            Boolean.FALSE,
            this.getClass().getClassLoader()
        };
        this.filter = filter;
        Class c = Class.class;
        Class[] param = {
            String.class,
            Boolean.TYPE,
            ClassLoader.class
        };
        try {
            forName = c.getDeclaredMethod("forName", param);
        } catch (NoSuchMethodException e) {
        } catch (SecurityException e1) {
        }
    } 

    /** loads class with the given name. **/
    public SignatureClass loadClass(String name)
        throws ClassNotFoundException {
        if (forName == null) {
            return new SignatureClass(Class.forName(name), filter, this,
                                      details);
        } else {
            args[0] = name;
            try {
                return new SignatureClass((Class)forName.invoke(null, args),
                                          filter, this, details);
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof LinkageError)
                    throw (LinkageError)t;
                else if (t instanceof ClassNotFoundException)
                    throw (ClassNotFoundException)t;
                else
                    // if the other than Exception of the 
                    return new SignatureClass(Class.forName(name), filter, this,
                                              details);
            } catch (Throwable t) {
                return new SignatureClass(Class.forName(name), filter, this,
                                          details);
            }
        }
    }
}
