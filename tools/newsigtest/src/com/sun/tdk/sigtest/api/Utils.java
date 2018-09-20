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

import java.io.*;
import java.util.*;


public class Utils 
{
    public static class XComparator implements Comparator 
    {
        public int compare (Object o1, Object o2)
        {
            return o1.toString().compareTo(o2.toString());
        }
    }
    
    
    public static String getSimpleClassName (String fqn)
    {
        int k = fqn.lastIndexOf('.'),
            l = fqn.lastIndexOf('$');
        if (l > k) 
            k = l;
		
        return k > 0 ?  fqn.substring(k+1) : fqn;
    }
    
    
    static public String getPackClassName (String fqn, String simple)
    {
        return fqn.substring(0, fqn.length() - simple.length()-1);
    }
    

    public static String convertVMType (StringBuffer s) 
    {
        char chr = s.charAt(0);
        s.deleteCharAt(0);

        if (chr == '[') 
            return convertVMType(s) + "[]";
            
        else if (chr == 'L') {
            int n = s.length();
            for (int i = 1; i < n; i++)
                if (s.charAt(i) == ';') {
                    String t = s.substring(0, i).replace('/', '.');
                    s.delete(0, i+1);
                    return t;
                }
                
           throw new Error("invalid VM type " + s);
        }
        
        else 
            return getPrimitive(chr);
    }
    
    
    public static String convertToVMType (String s)
    {
        StringBuffer sb = new StringBuffer();
        
        int l = s.length();
        while (l > 2 && s.charAt(l-2) == '[') {
            l -= 2;
            sb.append('[');
        }
        String t = s.substring(0, l);
        
        char c = isPrimitive(t);
        if (c != 0)
            sb.append(c);
        else {
            sb.append('L');
            sb.append(t);
            sb.append(';');
        }
        
        return sb.toString();
    }
        
	
    final static String[] primitives = 
    {
        "V", "void",
        "Z", "boolean",
        "B", "byte",
        "S", "short",
        "I", "int",
        "J", "long",
        "C", "char",
        "F", "float",
        "D", "double",
        "#", "???"
    };


    public static char isPrimitive (String s) 
    {
	    for (int i = 0; primitives[i].charAt(0) != '#'; i += 2) 
	        if (s.equals(primitives[i+1]))
        		return primitives[i].charAt(0);

    	return 0;
    }


    public static String getPrimitive (char c) 
    {
        for (int i = 0; ; i += 2) {
    	    char x = primitives[i].charAt(0);

	        if (x == c)
	            return primitives[i+1];
    	    if (x == '#')
	            throw new Error("not primitive type: " + c);
    	}
    }


    public static String list (String[] list)
    {
        StringBuffer sb = new StringBuffer();

        if (list != null && list.length != 0) {
            sb.append(list[0]);
            for (int i = 1; i < list.length; i++)
                sb.append(", ").append(list[i]);
        }

        return sb.toString();
    }


    public static String list (List list)
    {
        StringBuffer sb = new StringBuffer();

        if (list != null && list.size() != 0) {
            sb.append(list.get(0));
            for (int i = 1; i < list.size(); i++)
                sb.append(", ").append(list.get(i));
        }

        return sb.toString();
    }
    
    
    public static void printSorted (PrintWriter out, Set set)
    {
        String[] tmp = new String[set.size()];
        set.toArray(tmp);
        Arrays.sort(tmp);
        for (int i = 0; i < tmp.length; i++)
            out.println("  " + tmp[i]);
    }
}
