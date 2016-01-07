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

package com.sun.tdk.signaturetest.model;

import java.util.ArrayList;
import java.util.List;

public class PackageDescr {

    private List classes;
    private List subpackages;
    private String fqname;
    private String name = "";

    public String getQualifiedName() {
        return fqname;
    }

    public String getName() {
        return name;
    }

    public PackageDescr(String fqname) {
        this.fqname = fqname;
        this.name = fqname.lastIndexOf(".") > 0 ? fqname.substring(fqname.lastIndexOf(".") + 1) : fqname;
        this.classes = new ArrayList();
        this.subpackages = new ArrayList();
    }

    public void add(ClassDescription cd) {
        for (int i = 0; i < classes.size(); i++)
            if (((ClassDescription) classes.get(i)).getQualifiedName()
            		.compareTo(cd.getQualifiedName()) > 0) {
                classes.add(i, cd);
                return;
            }
        classes.add(cd);
    }

    public void add(PackageDescr pd) {
        for (int i = 0; i < subpackages.size(); i++)
            if (((PackageDescr) subpackages.get(i)).getQualifiedName().compareTo(pd.getQualifiedName()) > 0) {
                subpackages.add(i, pd);
                return;
            }

        subpackages.add(pd);
    }


    public void addAll(List list) {
        classes.addAll(list);
    }

    public void clear() {
        this.classes.clear();
    }

    public List getDeclaredClasses() {
        return classes;
    }

    public List getDeclaredPackages() {
        return subpackages;
    }

    public String toString() {
        return fqname;
    }

    public boolean equals(Object arg) {
        return arg instanceof PackageDescr && this.fqname.equals(((PackageDescr) arg).getQualifiedName());
    }

    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.fqname != null ? this.fqname.hashCode() : 0);
        return hash;
    }

    public boolean isCompatible(MemberDescription m) {
        return this.equals(m);
    }

}
