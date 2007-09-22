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

import java.io.PrintStream;
import java.util.Vector;

/** This is class for founding all nested classes and sorting these
 *  classes. Classes are sorted by the following rules: the nested
 *  classes precedes regular classes alphabetically. The hierarchy
 *  tree and enclosing graph are scanned for founding of the nested classes **/
public class ClassSorter {
    /** includes all scanned regular classes. **/ 
    private Vector regularClasses = new Vector();
    /** includes all scanned nested classes. **/ 
    private Vector nestedClasses = new Vector();
    /** classes which are specified for founding of the nested
     *  classes and sorting. **/
    private Vector classes;
    /** determines if the reflection is used for founding of the nested classes.**/
    private boolean isReflectUsed;
    private ClassFinder loader;
    /** errors which are occurred during founding of the nested classes. **/
    public ClassCollection errors = new ClassCollection();

    /** creates new ClassSorter with the given Vector and isReflectUsed.
     *  @param classes includes classes which are required in sorting
     *  @param isReflectUsed determines if the reflection is used for
     *  founding of the nested classes.
     *  @param loader loads SignatureClasses for searching of the nested
     *  classes.**/
    public ClassSorter(Vector classes, boolean isReflectUsed,
                       ClassFinder loader) {
        this.classes = classes;
        this.isReflectUsed = isReflectUsed;
        this.loader = loader;
    }

    /** sorts Vector alphabetically
     *  @param h the vector which required in ordering **/
    public static void sortVector(Vector h) {
        for (int j = 0; j < h.size(); j++) {
            int pos = j;
            String min = (String)h.elementAt(j);
            for (int i = j; i < h.size(); i++) {
                String temp = (String)h.elementAt(i);
                if (temp.compareTo(min) < 0) {
                    pos = i;
                    min = (String)h.elementAt(i);
                }
            }
            h.setElementAt(h.elementAt(j), pos);
            h.setElementAt(min, j);
        }
    }

    /** found all nested classes for given class included declared in the
     *  superclasses and superinterfaces and add these classes to
     *  the nestedClasses. @param c given class. **/
    void foundNested(SignatureClass c) {
        if (c == null)
            return;
        String name = c.getName();
        if (name.indexOf('$') < 0) {
            if (regularClasses.contains(name))
                return;
            else
                regularClasses.addElement(name);
        } else {
            if (nestedClasses.contains(name))
                return;
            else
                nestedClasses.addElement(name);
        }            

        foundNested(c.getSuperclass());

        SignatureClass intf[] = c.getInterfaces();
        for (int i = 0; i < intf.length; i++)
            foundNested(intf[i]);

        if (isReflectUsed) {
            SignatureClass nestCl [] = c.getDeclaredClasses();
            for (int i = 0; i < nestCl.length; i++) {
                foundNested(nestCl[i]);
            }
        } else {
            Vector h = TableOfClass.getDeclaredNestedClasses(name);
            for (int i = 0; i < h.size(); i++) {
                try {
                    String clName = (String)h.elementAt(i);
                    SignatureClass nestCl = loader.loadClass(clName);
                    foundNested(nestCl);
                } catch (ClassNotFoundException e) {
                    errors.addUniqueElement(name, "Class not found: " +
                                            h.elementAt(i));
                } catch (LinkageError er) {
                    errors.addUniqueElement(name, "Class not linked: " +
                                            h.elementAt(i));
                }
            }
        }
    }

    /** returns vector of the sorted classes.
     *  @param errors ClassCollection which stores occurred errors. **/
    public Vector getSortedClasses(ClassCollection errors) {
        this.errors = errors;
        Vector retVal;
        String name;
        for (int i = 0; i < classes.size(); i++) {
            name = (String)classes.elementAt(i);
            try {
                SignatureClass c = loader.loadClass(name);
                foundNested(c);
                } catch (ClassNotFoundException e) {
                    errors.addUniqueElement(name, "Class not found: " + name);
                } catch (LinkageError er) {
                    errors.addUniqueElement(name, "Class not linked: " + name);
                }
        }
        sortVector(nestedClasses);
        sortVector(regularClasses);
        retVal = nestedClasses;
        for (int i = 0; i < regularClasses.size(); i++)
            if (classes.contains(regularClasses.elementAt(i)))
                retVal.addElement(regularClasses.elementAt(i));
        return retVal;
    }

    /** determines if the given class can be accessed from classes. **/
    public boolean isAccessible(String name) {
        if (name == null)
            return false;
        if (name.indexOf('$') >= 0)
            return nestedClasses.contains(name);
        else
            return regularClasses.contains(name);
    }
}
