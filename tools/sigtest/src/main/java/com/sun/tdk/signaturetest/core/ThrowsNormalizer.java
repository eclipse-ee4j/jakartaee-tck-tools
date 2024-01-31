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

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Normalize the throws list completely for 'src' mode
 *
 * @author Maxim Sokolnikov*
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class ThrowsNormalizer {

    public ThrowsNormalizer() {
        
    }
    public ThrowsNormalizer(JDKExclude jdkExclude) {
        this.jdkExclude = jdkExclude;
    }
    public void normThrows(ClassDescription c, boolean removeJLE) throws ClassNotFoundException {

        ClassHierarchy h = c.getClassHierarchy();
   
        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor()) {
                normThrows(h, mr, removeJLE);
            }
        }
    }


    private boolean checkException(ClassHierarchy h, String candidate, String matchedException) throws ClassNotFoundException {
        return candidate.equals(matchedException) || h.isSubclass(candidate, matchedException);
    }

    private void normThrows(ClassHierarchy h, MemberDescription mr, boolean removeJLE) throws ClassNotFoundException {
        assert mr.isMethod() || mr.isConstructor();

        String throwables = mr.getThrowables();

        if (throwables.length() != 0) {

            xthrows.clear();

            {
                int startPos=0, pos;
                do {
                    pos =throwables.indexOf(MemberDescription.THROWS_DELIMITER, startPos);
                    if (pos!=-1) {
                        xthrows.add(throwables.substring(startPos, pos));
                        startPos=pos+1;
                    }
                    else
                        xthrows.add(throwables.substring(startPos));                    

                } while (pos!=-1);
            }

            int superfluousExceptionCount = 0;

            //  Scan over all throws ...

            for (int i = 0; i < xthrows.size(); i++) {
                String s = (String) xthrows.get(i);

                if (s == null)
                    continue;


                if (!jdkExclude.isJdkClass(s) && s.charAt(0) != '{' /* if not generic */) {

                    if (checkException(h, s, "java.lang.RuntimeException") || (removeJLE && checkException(h, s, "java.lang.Error"))) {
                        xthrows.set(i, null);
                        superfluousExceptionCount++;
                    } else {
                        for (int k = i + 1; k < xthrows.size(); ++k) {
                            String anotherThrowable = (String) xthrows.get(k);

                            if (anotherThrowable == null)
                                continue;

                            if (checkException(h, s, anotherThrowable)) {
                                xthrows.set(i, null);
                                superfluousExceptionCount++;
                                break;
                            }

                            if (checkException(h, anotherThrowable, s)) {
                                xthrows.set(k, null);
                                superfluousExceptionCount++;
                            }
                        }
                    }
                }
            }

            //  Should the throws list be updated ?

            if (superfluousExceptionCount != 0) {
                int count = 0;
                sb.setLength(0);

                for (int i = 0; i < xthrows.size(); i++) {
                    String s = (String) xthrows.get(i);
                    if (s != null) {
                        if (count++ != 0)
                            sb.append(MemberDescription.THROWS_DELIMITER);
                        sb.append(s);
                    }
                }

                if (count == 0)
                    mr.setThrowables(MemberDescription.EMPTY_THROW_LIST);
                else
                    mr.setThrowables(sb.toString());
            }
        }
    }
    
    private List/*String*/ xthrows = new ArrayList();
    private StringBuffer sb = new StringBuffer();
    private JDKExclude jdkExclude = new JDKExclude() {
        @Override
        public boolean isJdkClass(String name) {
            return false;
        }
    };
}
