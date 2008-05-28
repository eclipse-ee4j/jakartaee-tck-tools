/*
 * $Id: SwissKnife.java 4516 2008-03-17 18:48:27Z eg216457 $
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

package com.sun.tdk.signaturetest.util;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class SwissKnife {

    /**
     * Determines whether the object <code>x</code> is equal to
     * object <code>y</code>. If (<code>x</code> == <code>null</code>) and
     * (<code>y</code> == <code>null</code>) the result is true.
     *
     * @param x - first comparable object, may by <code>null</code>
     * @param y - second comparable object, may by <code>null</code>
     * @return true if x equal to y
     */
    public static boolean equals(Object x, Object y) {
        if (x == null)
            return y == null;

        return x.equals(y);
    }

    private static final ArrayList EMPTY_ARRAY_LIST = new ArrayList();
    
    /**
     * Converts source string to ArrayList of tokens
     * 
     * @param source - source string 
     * @param delimiters - argument are the delimiters for separating tokens. 
     * @return ArrayList of tokens found in the source
     */
    
    public static ArrayList stringToArrayList(String source, String delimiters) {
        if (source == null) {
            return EMPTY_ARRAY_LIST;
        }
        
        StringTokenizer st = new StringTokenizer(source, delimiters);
        ArrayList result = new ArrayList();
        while(st.hasMoreTokens()) {
            result.add(st.nextToken());
        }
        return result;
    }
    
}

