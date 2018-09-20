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


package com.sun.tdk.sigtest;

import com.sun.tdk.sigtest.api.*;
import java.io.*;
import java.util.*;


class APIWriterJh 
{
    public static final String magic = "//Java signature file";

    boolean debug = false;
    PrintWriter os;
    ClassMemberFilter mf;
    int outercount, innercount;
    
    String  indent;
    
    
    void write (PrintWriter pw, API api, ClassFilter cfilter, ClassMemberFilter mfilter) 
    {
        os = pw;
        mf = mfilter;
        outercount = innercount = 0;
        
        os.println(magic);
        
        out(api.getProps());
        
        indent = "";
        
        String packname = null;
        
        api.rewind();
        for (XClass xclass; (xclass = api.getXClass()) != null;) 
            if (xclass.home == null)
                if (cfilter == null || cfilter.inPath(xclass.getFullName())) 
                    if (mfilter == null || mfilter.ok(xclass)) {
                        if (packname == null || !packname.equals(xclass.packname))     
                            os.println("\n//package " + (packname = xclass.packname) + ";");
                        out(xclass);
                    }

        os.println("\n//end of Java signature file");
        os.flush();
        
        mf = null;
        os = null;
    }
    
    
    void out (Map props)
    {
        if (props != null)
            for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry)it.next();
                String key = (String)e.getKey();
                String val = e.getValue() == null ? null : e.getValue().toString();
            
                if (val == null || val.equals("[default]"))
                    os.println("@" + key);
                else
                    os.println("@" + key + " = \"" + val + "\"");
            }
    }


    void out (XClass xclass) 
    {
        if (xclass.home == null)
            ++outercount;
        else
            ++innercount;
    
        os.println();
        os.print(indent);

	// Class header

        debugOut(xclass);
        print(XModifier.toString(xclass.modifiers & ~XModifier.xinterface));

        if (xclass.isInterface()) {
            os.print("interface ");
            os.print((xclass.home == null) ? xclass.getFullName() : xclass.name);

            if (xclass.implement != null && xclass.implement.length != 0) {
                os.print(" extends ");
                outStrings(xclass.implement);
            }
        }
        else {
            os.print("class ");
            os.print((xclass.home == null) ? xclass.getFullName() : xclass.name);

            if (xclass.extend != null)
                if (!xclass.extend.equals("java.lang.Object")) {
                        os.print(" extends ");
                        os.print(xclass.extend);
                    }

            if (xclass.implement != null && xclass.implement.length != 0) {
                os.print(" implements ");
                outStrings(xclass.implement);
            }
        }

    // Class members

        os.print(" {");

        int first = 0;

        // Construcrors

        if (xclass.ctors != null)
            for (Iterator e = xclass.ctors.iterator(); e.hasNext();) {
                XClassCtor xcons = (XClassCtor)e.next();
                if (mf != null && !mf.ok(xcons))
                    continue;

                if (first++ == 0) os.println();
                os.print(indent);
                os.print("\t");

                debugOut(xcons);
                print(XModifier.toString(xcons.modifiers));
                os.print(xcons.name);
                os.print("(");
                outStrings(xcons.args);
                os.print(")");
                if (xcons.xthrows != null && xcons.xthrows.length != 0) {
                    os.print(" throws ");
                    outStrings(xcons.xthrows);
                }
                os.println(";");
            }

        // Methods

        if (xclass.methods != null)
            for (Iterator e = xclass.methods.iterator(); e.hasNext();) {
                XClassMethod xmethod = (XClassMethod)e.next();
                if (mf != null && !mf.ok(xmethod))
                    continue;

                if (first++ == 0) os.println();
                os.print(indent);
                os.print("\t");

                debugOut(xmethod);
                print(XModifier.toString(xmethod.modifiers));
                os.print(xmethod.type);
                os.print(" ");
                os.print(xmethod.name);
                os.print("(");
                outStrings(xmethod.args);
                os.print(")");
                if (xmethod.xthrows != null && xmethod.xthrows.length != 0) {
                    os.print(" throws ");
                    outStrings(xmethod.xthrows);
                }
                os.println(";");
            }

        // Fields

        if (xclass.fields != null)
            for (Iterator e = xclass.fields.iterator(); e.hasNext();) {
                XClassField xfield = (XClassField)e.next();
                if (mf != null && !mf.ok(xfield))
                    continue;
                    
                if (first++ == 0) os.println();
                os.print(indent);
                os.print("\t");

                debugOut(xfield);
                print(XModifier.toString(xfield.modifiers));
                os.print(xfield.type);
                os.print(" ");
                os.print(xfield.name);
                outValue(xfield);
                os.println(";");
            }

        // Inner classes

        if (xclass.inners != null)
            for (Iterator e = xclass.inners.iterator(); e.hasNext();) {
                XClass xinner = (XClass)e.next();
                if (mf != null && !mf.ok(xinner))
                    continue;

                if (first++ == 0) os.println();

                indent += "\t";
                out(xinner);
                indent = indent.substring(0, indent.length()-1);
            }

        if (first != 0) os.print(indent);
        os.println("}");
    }


    void debugOut (XClassMember x)
    {
        if (debug)
            if (x.inherited != null) {
                os.print("<<");
                os.print(x.inherited);
                os.print(">> ");
            }
    }


    void outStrings (String[] list) 
    {
    	if (list != null)
	        for (int i = 0; i < list.length; i++) {
	            if (i != 0) os.print(",");
    	   	    os.print(list[i]);
	        }
    }


    void outValue (XClassField xfield) 
    {
        if (xfield.value != null)
            os.print("="+getValue(xfield.value));
    }


    void print (String s) 
    {
        if (s.length() != 0)
            os.print(s + " ");
    }


    void print (String s1, String s2) 
    {
        if (s2.length() != 0)
            os.print(s1 + s2);
    }


    static String getValue (Object value) 
    {
        String s = null;

        if (value instanceof Character)
            s = "\'" + stuffOut(value.toString()) + "\'";

        else if (value instanceof String)
            s = "\"" + stuffOut(value.toString()) + "\"";

        else if (value instanceof Long)
            s = value.toString() + "L";

        else if (value instanceof Float) {
            Float f = (Float)value;
            s = f.toString();
            if (!f.isNaN() && !f.isInfinite())
                s += "f";
        }

        else if (value instanceof Double) {
            Double d = (Double)value;
            s = d.toString();
            if (!d.isNaN() && !d.isInfinite())
                s += "d";
        }

        else // boolean, byte, short, int
            s = value.toString();

        return s;
    }


    static final String spec1 = "\b\t\n\f\r\"\'\\",
                        spec2 = "btnfr\"\'\\";

    static String stuffOut (String s) 
    {
        StringBuffer x = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            int  k = spec1.indexOf(c);
            if (k >= 0)
                x.append('\\').append(spec2.charAt(k));

            else if (c < 0x20)
                x.append(esc(c));

            else
                x.append(c);
        }

        return x.toString();
    }
       
    
    static String esc (char c) 
    {
        String s = Integer.toHexString(c);
        int n = s.length();

        if (n == 1)
            return "\\u000" + s;
        else if (n == 2)
            return "\\u00" + s;
        else if (n == 3)
            return "\\u0" + s;
        else
            return "\\u" + s;
    }
    
}

