/*
 * $Id: PrimitiveTypes.java 4504 2008-03-13 16:12:22Z sg215604 $
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

/**
 * @author Mikhail Ershov
 */
public class PrimitiveTypes {

    public static boolean isPrimitive(String jlsType) {

        for(int i=0; i<types.length; ++i)
            if (types[i].JLSNotation.equals(jlsType))
                return true;

        return false;
    }

    //  Convert VM notation to JLS

    public static String getPrimitiveType(char vmType) {

        for(int i=0; i<types.length; ++i)
            if (vmType ==types[i].VMNotation)
                return types[i].JLSNotation;

        return null;
    }
    
    public static String getVMPrimitiveType(String jlsType) {
    	for(int i=0; i<types.length; ++i)
    		if (types[i].JLSNotation.equals(jlsType))
    			return String.valueOf(types[i].VMNotation);   	 

    	return null;
    }

    	

    private static class Pair {

        Pair(char vm, String jls ) {
            VMNotation = vm;
            JLSNotation = jls;
        }

        char VMNotation;
        String JLSNotation;
    }

    private static Pair[] types = {
            new Pair('Z', "boolean"),
            new Pair('V', "void"),
            new Pair('I', "int"),
            new Pair('J', "long"),
            new Pair('C', "char"),
            new Pair('B', "byte"),
            new Pair('D', "double"),
            new Pair('S', "short"),
            new Pair('F', "float")
    };
}
