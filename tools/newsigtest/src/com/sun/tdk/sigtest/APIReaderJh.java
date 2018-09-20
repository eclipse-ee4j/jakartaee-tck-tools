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


class APIReaderJh extends API
{
    public static final String magic = "//Java signature file";

    static final int
    	syn_eof   = -1,
        syn_eol   = 0,
        syn_ident = 1,
        syn_sep   = 2,
        syn_numb  = 3,

        syn_package    = 10,
        syn_class      = 11,
        syn_interface  = 12,
        syn_extends    = 13,
        syn_implements = 14,
        syn_throws     = 15;


    static class Keywords extends HashMap 
    {
        Keywords () 
        {
            put("package",      syn_package);
            put("class",        syn_class);
            put("interface",    syn_interface);
            put("extends",      syn_extends);
            put("implements",   syn_implements);
            put("throws",       syn_throws);
        }


        void put(String word, int key) 
        {
            super.put(word, new Integer(key));
        }


        int look (String word)
        {
            Integer i = (Integer)super.get(word);
            return i == null ? syn_ident : i.intValue();
        }
    }

    static Keywords words = new Keywords();


    String fname;
    StreamTokenizer st;
    String lex;
    char   chr;
    int    syn;
    int    errors;
    String packname;
    XClass rclass;
    boolean trace = false;
    boolean propsread = false;
	
	
    static boolean check (BufferedReader rdr) 
    {
        String s = null;
        
        try {
            s = rdr.readLine();
        }
        catch (IOException e) {
   	    }
		
        return s != null && s.regionMatches(0, magic, 0, magic.length());
    }
    
    
    APIReaderJh (BufferedReader rdr, String f) 
    {
    	packname = null;
    	errors   = 0;
        fname = f;
		
        st = new StreamTokenizer(rdr);
        st.ordinaryChar((int)'-');
        st.ordinaryChars((int)'0', (int)'9');
        st.wordChars((int)'-', (int)'-');
        st.wordChars((int)'0', (int)'9');
        st.wordChars((int)'_', (int)'_');
        st.wordChars((int)'$', (int)'$');
        st.wordChars((int)'\\', (int)'\\');
        st.wordChars(128, 65537);
        st.slashStarComments(true);
        st.slashSlashComments(true);
        readWord();
    }


    //  Implementation of API interface
    
    public void close () 
    {
        propsread = false;
    	packname = null;
        lex  = null;
        st   = null;
        fname = null;
    }
    
    
    //  Implementation of API interface
    
    public ClassIterator getClassIterator ()
    {
        return null;
    }
    
    
    //  Implementation of API interface
    //  Random-access (by fully qulified name) method not supported
    
    public XClass getXClass (String fqn)
    {
        throw new UnsupportedOperationException();
    }


    //  Implementation of API interface
    //  Sequental-access method
    
    public XClass getXClass ()
    {
        if (!propsread) {
            propsRead();
            propsread = true;
        }
        
	    try {
	        for (rclass = null; syn != syn_eof && rclass == null;) 
	            if (!read(null))
        		    err("invalid symbol");
                
    	}
	    catch (Error e) {
	        System.err.println(e.getMessage());
    	}
        
        return rclass;
    }


    //  Implementation of API interface
    //  Sequental-access method
    
    public void rewind ()
    {
        throw new UnsupportedOperationException();
    }
    
    
    void propsRead ()
    {
        st.eolIsSignificant(true);
        
        for (;;) {
            
            if (syn == syn_eol) {
                readWord();
                continue;
            }
            
            if (syn != syn_sep || chr != '@') 
                break;
            readWord();
            
            checkIdent();
            String key   = lex;
            String value = "[default]";
            readWord();
            
            if (syn == syn_sep && chr == '=') {
                readWord();
                value = null;
            }

            if (syn != syn_eol) {                
                value = st.sval;
                readWord();
            }
                
            if (syn != syn_eol)
                err("invalid propery value");
            readWord();
             
            setProp(key, value);   
            //System.out.println("key '" + key + "' value '" + value + "'");
        }
        
        st.eolIsSignificant(false);
        
        while (syn == syn_eol)
            readWord();
    }


    boolean read (XClass topclass) 
    {
        if (syn == syn_package && topclass == null) {
	    //  package <ident>;
	        readWord();
            checkIdent();
            packname = lex;
            readWord();
            readSep(';');
            return true;
        }
		
    //  class/interface

        int mod = readModifier();

        if (syn == syn_class || syn == syn_interface) {
		
            if (syn == syn_interface)
                 mod |= XModifier.xinterface | XModifier.xabstract;

            readWord();
            checkIdent();

            String pack   = packname;
            String simple = lex;
            int dot = lex.lastIndexOf('.');
            if (dot != -1) {
                pack   = lex.substring(0, dot);
                simple = lex.substring(dot+1);
                if (packname != null && !packname.equals(pack))
                    err("class name conflicts with package directive");
            }
                
            if (topclass != null && dot != -1)
                err("internal class should have simple name");
                
            XClass xclass = newXClass();
	        xclass.defined   = true;
    	    xclass.modifiers = mod;
            xclass.name      = simple;
            
    	    if (topclass == null) {
		        xclass.packname = pack;
	        }
            else {
        		xclass.link(topclass);
	        }
    	    xclass.modifiers = mod;
            
            readWord();

            // extends <ident>
            if (syn == syn_extends) {
                readWord();

                String[] w;
                if ((w = readClassNames()) == null)
                    err("class name(s) expected after extends");
					
                if ((mod & XModifier.xinterface) != 0)
                    xclass.implement = w;
                else {
                    xclass.extend = w[0];
                    if (w.length > 1)
                        err("only one superclass allowed", xclass);
                }
            }
            else if ((mod & XModifier.xinterface) == 0) {
                if (!"java.lang.Object".equals(xclass.getFullName()))
                    xclass.extend = "java.lang.Object";
            }

            // implements <ident>, <ident>, ...
            if (syn == syn_implements)
            {
                readWord();

                if ((xclass.implement = readClassNames()) == null)
                    err("class name(s) expected after implements");

                if ((mod & XModifier.xinterface) != 0)
                    err("implements not allowed in interfaces", xclass);
            }

            checkModifier(xclass, (xclass.modifiers & XModifier.xinterface) == 0 
                                ? XModifier.xpublic    | 
                                  XModifier.xprivate   | 
                                  XModifier.xprotected | 
                                  XModifier.xstatic    |
                                  XModifier.xfinal     |
                                  XModifier.xabstract  |
                                  XModifier.xstrictfp
                                : XModifier.xinterface |
                                  XModifier.xpublic    | 
                                  XModifier.xprivate   | 
                                  XModifier.xprotected | 
                                  XModifier.xstatic    |
                                  XModifier.xabstract  |
                                  XModifier.xstrictfp);

            if (topclass != null)
                checkClassMember(xclass);

            // { <class body> }
            if (syn == syn_sep && chr == '{') {
                readSep('{');
                    
                while (read(xclass))
                    ;

                readSep('}');
            }
            
            if (topclass == null)
                rclass = xclass;

            xclass.setDefaults();
            
            return true;
        }

    //  constructor/method/field

        if (syn == syn_ident && topclass != null) {
            String s1 = lex;
            readWord();
            
            if (syn == syn_sep && chr == '(') {
    	    // 	constructor
                checkSimpleName(s1);
                XClassCtor unit = newXClassCtor();
                unit.name = s1;
                unit.modifiers = mod;
                unit.args = readArgs();
                unit.xthrows = readThrows();
                unit.link(topclass);
                checkClassMember(unit);
                checkModifier(unit, XModifier.flagctor | XModifier.xstrictfp); //***
                readSep(';');
                return true;
            }
            else {
            //  method/field
            //  we see now return type of a method or type of a field
                while (syn == syn_sep && chr == '[') {
                    readWord();
                    readSep(']');
                    s1 += "[]";
                }
             // now we see name of a method or field
                if (syn != syn_ident)
                    err("name of a method or field expected");
                checkSimpleName(lex);
                String s2 = lex;
                readWord();

                if (syn == syn_sep) {
                    if (chr == '(') {
                    //  it's a method
                        checkSimpleName(s2);
                        XClassMethod unit = newXClassMethod();
                        unit.name = s2;
                        unit.modifiers = mod;
                        unit.type = s1;
                        unit.args = readArgs();
                        unit.xthrows = readThrows();
                        unit.link(topclass);
                        checkClassMember(unit);
                        checkModifier(unit, XModifier.flagmethod);
                        readSep(';');
                        return true;
                    }
                    else {
        		    //	only field remains
                        for (;;) {
                            String s3 = "";
                            while (syn == syn_sep && chr == '[') {
                                readWord();
                                readSep(']');
                                s3 += "[]";
                            }

                            checkSimpleName(s2);
                            XClassField unit = newXClassField();
                            unit.name = s2;
                            unit.modifiers = mod;
                            unit.type = s1+s3;

                            if (chr == '=') {
            			    //  it's a field with assignment
                                readWord();
                                unit.value = readExpr(unit.type);
                            }

                            unit.link(topclass);
                            checkClassMember(unit);
                            checkModifier(unit, XModifier.flagfield);

                            if (syn != syn_sep || chr != ',') break;
                            readWord();
                            if (syn != syn_ident)
                                err("field name expected");
                            s2 = lex;
                            readWord();
                        }
                        readSep(';');
                        return true;
                    }
                }
            }
        }

        return false;
    }


    void checkSimpleName (String s)
    {
        if (s.indexOf('.') != -1)
            err("only simple name allowed inside class definition");
    }


    void checkClassMember (XClassMember u)
    {
        if (u.name == null || u.name.length() == 0) 
            err("name empty", u);

        if (u instanceof XClassCtor) {
            if (!u.name.equals(u.home.name))
                err("invalid name of class constructor", u);
        }
    }


    void checkModifier (XClassMember u, int m)
    {
        int x = u.modifiers & ~m;
        if (x != 0)
            err("invalid modifier(s): \"" + XModifier.toString(x) + "\"", u);
    }


    String[] readArgs ()
    {
        ArrayList v = new ArrayList();
        readSep('(');

        while (syn == syn_ident) {
            if (lex.equals("final")) {
                readWord(); // ignore 'final'
                if (syn != syn_ident)
                    err("type name expected after final");
            }

            String s = readType();

            if (syn == syn_ident)
                readWord(); // ignore argument name

            String s1 = "";
            while (syn == syn_sep && chr == '[') {
                readWord();
                readSep(']');
                s1 += "[]";
            }

            v.add(s+s1);

            if (syn != syn_sep || chr != ',') 
                break;
                
            readWord();
        }

        readSep(')');
        return toArrayString(v);
    }


    String[] readThrows ()
    {
        if (syn == syn_throws) {
            ArrayList v = new ArrayList();

            readWord();
            while (syn == syn_ident) {
                v.add(readType());
                if (syn != syn_sep || chr != ',') 
                    break;
                readWord();
            }

            if (v.size() == 0)
                err("type name(s) expected after throws");

            return toArrayString(v);
        }
        else
            return null;
    }


    String readType ()
    {
        checkIdent();
        String s = lex;
        readWord();

        while (syn == syn_sep && chr == '[') {
            readWord();
            readSep(']');
            s += "[]";
        }

        return s;
    }


    String[] readClassNames ()
    {
        ArrayList v = new ArrayList();

        while (syn == syn_ident) {
            v.add(lex);
            readWord();

            if (syn != syn_sep || chr != ',') 
		break;
				
            readWord();
        }

        return toArrayString(v);
    }


    static String[] toArrayString (ArrayList v)
    {
        if (v == null || v.size() == 0)
            return new String[0];
        else {
            String[] a = new String[v.size()];

            for (int i = 0; i < v.size(); i++)
                a[i] = ((String)v.get(i)).intern();

            return a;
        }
    }


    int readModifier () 
    {
        int mask = 0, m;

        while (syn == syn_ident && (m = XModifier.convert(lex)) != 0) {
            mask |= m;
            readWord();
        }

        return mask;
    }


    Object readExpr (String type) 
    {
        Object v = null;

        if (type.equals("java.lang.String")) {
            if (syn != syn_sep || chr != '"' || st.sval == null)
                err("string expression expected");
            try {
                v = new String(st.sval);
            }
            catch (Exception e) {
                err("invalid string value");
            }
            readWord();
        }
        else if (type.equals("char")) {
            try {
                if (syn == syn_sep && chr == '\'')
                    v = new Character(st.sval.charAt(0));
                else {
                    int i = Integer.decode(lex).intValue();
                    if (i < 0 || i > 0xFFFF)
                        throw new Exception();
                    v = new Character((char)i);
                }
            }
            catch (Exception e) {
                err("invalid char value");
            }
            readWord();
        }
        else if (type.equals("boolean")) {
            if (syn != syn_ident)
                err("expression expected");

            if (lex.equals("true"))
                v = new Boolean(true);
            else if (lex.equals("false"))
                v = new Boolean(false);
            else
                err("invalid boolean value");

            readWord();
        } 
	else if (type.equals("byte")) {
            if (syn != syn_ident)
                err("expression expected");
            try {
                v = Byte.decode(lex);
            }
            catch (Exception e) {
                err("invalid byte value");
            }
            readWord();
        }
        else if (type.equals("short")) {
            if (syn != syn_ident)
                err("expression expected");
            try {
                v = Short.decode(lex);
            }
            catch (Exception e) {
                err("invalid short value");
            }
            readWord();
        }
        else if (type.equals("int")) {
            if (syn != syn_ident)
                err("expression expected");
            try {
                v = Integer.decode(lex);
            }
            catch (Exception e) {
                err("invalid integer value");
            }
            readWord();
        }
        else if (type.equals("long")) {
            if (syn != syn_ident)
                err("expression expected");

            try {
                if (lex.endsWith("L") || lex.endsWith("l"))
                    v = Long.decode(lex.substring(0, lex.length()-1));
                else
                    v = Long.decode(lex);
            }
            catch (Exception e) {
                err("invalid long value");
            }

            readWord();
        }
        else if (type.equals("float")) {
            if (syn != syn_ident)
                err("expression expected");
            try {
                if (lex.equals("Infinity"))
                    v = new Float(Float.POSITIVE_INFINITY);
                else if (lex.equals("-Infinity"))
                    v = new Float(Float.NEGATIVE_INFINITY);
                else if (lex.equals("NaN"))
                    v = new Float(Float.NaN);
                else if (lex.endsWith("F") || lex.endsWith("f"))
                    v = new Float(lex.substring(0, lex.length()-1));
                else
                    v = new Float(lex);
            }
            catch (Exception e) {
                err("invalid float value");
            }
            readWord();
        }
        else if (type.equals("double")) {
            if (syn != syn_ident)
                err("expression expected");
            try {
                if (lex.equals("Infinity"))
                    v = new Double(Double.POSITIVE_INFINITY);
                else if (lex.equals("-Infinity"))
                    v = new Double(Double.NEGATIVE_INFINITY);
                else if (lex.equals("NaN"))
                    v = new Double(Double.NaN);
                else if (lex.endsWith("D") || lex.endsWith("d"))
                    v = new Double(lex.substring(0, lex.length()-1));
                else
                    v = new Double(lex);
            }
            catch (Exception e) {
                err("invalid double value");
            }
            readWord();
        }
        else {
            err("values of type \""+type+"\" not supported");
        }

    /*
        if (v != null) {
            System.out.print("v=");
            if (type.equals("char"))
                System.out.println("\'"+v+"\'");
            else if (type.equals("java.lang.String"))
                System.out.println("\""+v+"\"");
            else 
                System.out.println(v);
        }
    */

        return v;
    }


    void checkIdent () 
    {
        if (syn != syn_ident)
            err("ident expected");
    }


    void readSep (char c) 
    {
        if (syn != syn_sep || chr != c)
            err("separator expected: <" + c + ">");
        readWord();
    }


    void err (String m, XNode x) 
    {
    	err(m, x.toString());
    }



    void err (String m) 
    {
    	err(m, st.toString());
    }



    void err (String m, String n) 
    {
        errors++;
        System.err.println(m);
        if (n != null)
            System.err.println(n);
        throw new Error("Syntax error");
    }



    boolean readWord ()  
    {
        try {
            st.nextToken();
        }
        catch (IOException x) {
            throw new Error("Read error from file " + fname + "\n" + x);
        }

        //System.out.println("ttype="+st.ttype+" "+st.sval);

        switch (st.ttype) {
            case StreamTokenizer.TT_EOF:
                syn = syn_eof;
                break;

            case StreamTokenizer.TT_EOL:
                syn = syn_eol;
                break;

            case StreamTokenizer.TT_NUMBER:
                syn = syn_numb;
                break;

            case StreamTokenizer.TT_WORD:
                lex = st.sval;
                syn = words.look(lex);
                break;

            default:
                chr = (char)st.ttype;
                syn = syn_sep;
        }

        if (trace) 
            System.out.println("syn="+String.valueOf(syn)+"  "+st.toString());

        return syn != syn_eof;
    }
    
}

