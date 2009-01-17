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

package com.sun.tdk.signaturetest.merge;

import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.core.ClassHierarchyImpl;
import com.sun.tdk.signaturetest.core.Log;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MergedSigFile {
    
    private ClassHierarchy ch;
    private MultipleFileReader loader;
    private HashMap set;
    
    public MergedSigFile(MultipleFileReader loader , Log log) {
        this.loader = loader;
        ch = new ClassHierarchyImpl(loader, ClassHierarchy.ALL_PUBLIC);
    }
    
    public HashMap getClassSet() {
        if (set == null) {
            set = new HashMap();
            loader.rewind();
            ClassDescription cd = null;
            do {
                try         {
                    cd = loader.nextClass();
                    if (cd != null) {
                        set.put(cd.getQualifiedName(), cd);
                    }
                } catch (IOException ex) {
                    Logger.getLogger("global").log(Level.SEVERE, null, ex);
                    break;
                }
            } while(cd != null);
        }
        return set;
    }
    
    public ClassHierarchy getClassHierarchy() {
        return ch;
    }
    
    MultipleFileReader getLoader() {
        return loader;
    }
    
}
