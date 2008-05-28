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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <b>PackageGroup</b> is intended to maintain a list of packages
 * names. It provides a tool to check if given class belongs to
 * some of the packages listed by <b>PackageGroup</b>, or if that
 * class belongs to some subpackage of some of the listed packages.
 *
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 */
public class PackageGroup {
    /**
     * For every package listed in the <code>group</code> filed,
     * indicate if its subpackages are implicitly implied.
     *
     * @see #group
     */
    private boolean isSubpackagesUses = true;

    /**
     * List of strings intended to contain names of packages.
     * If the field <code>isSubpackagesUses</code> is <code>true</code>,
     * all subpackages names are implicitly implied for each package
     * name listed here.
     *
     * @see #isSubpackagesUses
     */
    private List group;

    /**
     * Create empty list of packages, and decide if subpackages
     * should be implied.
     */
    public PackageGroup(boolean isSubpackagesUses) {
        this.isSubpackagesUses = isSubpackagesUses;
        group = new ArrayList();
    }

    public boolean isEmpty() {
        return group.isEmpty();
    }

    public String toString() {
        return group.toString();
    }

    /**
     * Add some package <code>name</code> to <code>this</code> group.
     */
    public void addPackage(String packageName) {
        group.add(packageName);
    }

    public void addPackages(String[] packageNames) {
        for (int i = 0; i < packageNames.length; ++i)
            group.add(packageNames[i]);
    }


    /**
     * Check if the given class <code>name</code> belongs to some of
     * the packages listed by <code>this</code> <b>PackageGroup</b>.
     * If <code>isSubpackagesUsed</code> policy is set, also check if
     * that class belongs to some subpackage of some of the packages
     * listed here.
     */
    public boolean checkName(String className) {
        for (Iterator i = group.iterator(); i.hasNext();) {
            String pack = (String) i.next();
            if ((className.startsWith(getPackageName(pack)) &&
                    ((className.lastIndexOf('.') <= pack.length()) || isSubpackagesUses)) || className.equals(pack))
                return true;
        }
        return false;
    }

    /**
     * Terminate the given package <code>name</code> with the dot symbol,
     * if the <code>name</code> is nonempty.
     */
    private static String getPackageName(String name) {
        return name + ((name.endsWith(".") || name.equals("")) ? "" : ".");
    }

}
