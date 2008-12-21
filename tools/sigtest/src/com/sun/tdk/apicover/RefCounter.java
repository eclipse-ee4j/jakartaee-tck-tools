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

package com.sun.tdk.apicover;



import com.sun.tdk.signaturetest.core.Erasurator;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;



import java.util.*;

class RefCounter {
    private HashMap<String, ClassDescription> api = new HashMap<String, ClassDescription>();
    private enum MODE { REAL, WORST }
    private MODE mode = MODE.WORST;
    private Erasurator erasurator = new Erasurator();
    private Map<String, ClassDescription> ts = new HashMap<String, ClassDescription>();

    Map<String, Integer> results = new HashMap<String, Integer>();

    public RefCounter() {
        super();
    }

    public void addClass(ClassDescription cd) {
        ArrayList<MemberDescription> modified = new ArrayList<MemberDescription>();
        for (Iterator i = cd.getMembersIterator(); i.hasNext();) {
                MemberDescription md = (MemberDescription)i.next();
                MemberDescription md2 = (MemberDescription) md.clone();
                if (mode.equals(MODE.WORST) && !md.getDeclaringClassName().equals(
                        cd.getQualifiedName()) && !md.isFinal()) {
                    md2.setDeclaringClass(cd.getQualifiedName());
                }
                i.remove();
                modified.add(md2);
        }
        for (MemberDescription md : modified) {
            cd.add(md);
        }
        api.put(cd.getQualifiedName(), cd);
    }

    public void addTSClass(ClassDescription cd, boolean fromAPI) {
        if (fromAPI) {
            ts.put(cd.getQualifiedName(), erasurator.erasure(cd));
            return;
        }

        String parent = findSuper(cd);
        if (parent.equals(cd.getQualifiedName())
                && cd.getInterfaces().length == 0) {
            return;
        }

        ts.put(cd.getQualifiedName(), erasurator.erasure(cd));
    }

    public void addRef(MemberDescription call) {
        String calledClass = call.getDeclaringClassName();
        if (ts.get(call.getDeclaringClassName()) == null) {
            return;
        }
        try {
            calledClass = findDecl(ts.get(calledClass), call);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ClassDescription apiClass = api.get(calledClass);
        if (apiClass != null) {
            erasurator.parseTypeParameters(apiClass);
            for (Iterator j = apiClass.getMembersIterator(); j.hasNext();) {
                MemberDescription orig = (MemberDescription)j.next();
                MemberDescription erased = erasurator.processMember(orig);
                if (erased.equals(call)) {
                    results.put(orig.toString(), 1);
                }
            }
        }
    }

    private String findDecl(ClassDescription tsClass, MemberDescription md) {
        boolean foundSuper = true;
        while (foundSuper) {
            // contain Collection or not
            if (tsClass.getMembersIterator().hasNext()) {
                if (tsClass.containsMember(md))
                    return tsClass.findMember(md).getDeclaringClassName();
            } else {
                for (MemberDescription decl: tsClass.getDeclaredConstructors()) {
                    if (decl.equals(md)) {
                        return tsClass.getQualifiedName();
                    }
                }
                for(MemberDescription decl: tsClass.getDeclaredFields()) {
                    if (decl.equals(md)) {
                        return tsClass.getQualifiedName();
                    }
                }
                for(MemberDescription decl: tsClass.getDeclaredMethods()) {
                    if (decl.equals(md)) {
                        return tsClass.getQualifiedName();
                    }
                }
            }

            foundSuper = false;
            if (tsClass.getSuperClass() != null
                    && ts.get(tsClass.getSuperClass().getQualifiedName()) != null) {
                tsClass = ts.get(tsClass.getSuperClass().getQualifiedName());
                foundSuper = true;
            }
        }
        return tsClass.getQualifiedName();
    }

    boolean isCovered(MemberDescription md) {
        return results.get(md.toString()) != null;
    }

    int refsCount(MemberDescription md) {
        if (results.get(md.toString()) != null) {
            return results.get(md.toString());
        }
        return 0;
    }


    private String findSuper(ClassDescription tsClass) {
        while (tsClass.getSuperClass() != null) {
            if (ts.get(tsClass.getSuperClass().getQualifiedName()) != null) {
                tsClass = ts.get(tsClass.getSuperClass().getQualifiedName());
            } else {
                return tsClass.getQualifiedName();
            }
        }
        return tsClass.getQualifiedName();
    }



    public void setMode(String mode) {
        this.mode = "r".equals(mode) ? MODE.REAL : MODE.WORST;
    }

    private void  clearInheretid() {
        for (ClassDescription cd : api.values()) {
            for (Iterator i = cd.getMembersIterator(); i.hasNext();) {
                MemberDescription md = (MemberDescription)i.next();
                if (!(md.isConstructor() || md.isField() || md.isMethod())) {
                    i.remove();
                    continue;
                }
                if (mode.equals(MODE.REAL) && !md.getDeclaringClassName().equals(
                        cd.getQualifiedName())) {
                    i.remove();
                    continue;
                }
            }
        }
    }

    public Collection <ClassDescription> getClasses() {
        clearInheretid();
        return api.values();
    }
}
