/*
 * Copyright (c) 2003, 2018 Oracle and/or its affiliates. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.tdk.sigtest.api;


/**
 *	Modifiers for classes, constructors, methods and fields
 *
 *	Only static members are in this class.
 *
 */

public class XModifier 
{

//  The following two tables MUST BE IN SYNC

    static final String[] words =
    {
        "public",       // 1
        "protected",    // 2
        "private",      // 3
        "static",       // 4
        "abstract",     // 5
        "final",        // 6
        "native",       // 7
        "synchronized", // 8
        "transient",    // 9
        "volatile",     // 10
        "strictfp",     // 11
        "interface"     // 12
    };


    public static final int
        xpublic      = 0x001,
        xprotected   = 0x002,
        xprivate     = 0x004,
        xstatic      = 0x008,
        xabstract    = 0x010,
        xfinal       = 0x020,
        xnative      = 0x040,
        xsynchronized= 0x080,
        xtransient   = 0x100,
        xvolatile    = 0x200,
        xstrictfp    = 0x400,
        xinterface   = 0x800,
		xdefault    = 0x1000;

	public static final int
	    xvisibility = xpublic | xprotected | xdefault | xprivate;


    // bits of access_flags item : allowed in class files
    public static final int
        flagclass  = xpublic    |
                                  xfinal    | xinterface | xabstract,

        flagctor   = xpublic    | xprivate  | xprotected,


        flagfield  = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    |
                     xvolatile  | xtransient,

        flagmethod = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    |              xabstract | xstrictfp  |
                     xnative    | xsynchronized,

        flaginner  = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    | xinterface | xabstract;


    public static int convert (String s) 
    {
        for (int i = 0, m = 1; i < words.length; i++, m <<= 1)
            if (words[i].equals(s))
                return m;
                
        if (s.equals("strict"))
            return xstrictfp;

        return 0;
    }


    public static int access (int m) 
    {
        return (m & xpublic) != 0 
                        ? 3
                        :(m & xprotected) != 0 
                                ? 2
                                :(m & xprivate) != 0 
                                        ? 0
                                        : 1;
    }
    
    
    public static boolean isPublic (int m)
    {
        return (m & (xpublic | xprotected)) != 0;
    }


    public static String toString (int mask) 
    {
        StringBuffer s = new StringBuffer();

        for (int i = 0, m = 1; i < words.length; i++, m <<= 1)
            if ((mask & m) != 0) {
                if (s.length() != 0) 
                    s.append(" ");
                s.append(words[i]);
            }

        return s.toString();
    }
}

