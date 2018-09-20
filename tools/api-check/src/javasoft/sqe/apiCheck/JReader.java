/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
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

/* 
 *      This is a basic class to read pseudo-Java format into Unit structures.
 *      Two classes derived from JReader: ReadJh (for reading signature file) and
 *      ReadJhu (for reading update file).
 *
 */


package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;



abstract
class JReader
{
    static final int syn_eof   = -1,
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


    static class Keywords extends Hashtable
    {
        Keywords ()
        {
            Put("package",      syn_package);
            Put("class",        syn_class);
            Put("interface",    syn_interface);
            Put("extends",      syn_extends);
            Put("implements",   syn_implements);
            Put("throws",       syn_throws);
        }


        void Put(String word, int key)
        {
            super.put(word, new Integer(key));
        }


        int Look (String word)
        {
            Integer i = (Integer)super.get(word);
            return i == null ? syn_ident : i.intValue();
        }
    }


    String fname;
    BufferedReader is;
    //LineNumberReader is;
    StreamTokenizer st;
    String lex;
    char   chr;
    int    syn;
    int    errors;
    boolean trace = false;
    String  defpack = null;
    HashMap classes = new HashMap();
    HashSet unknown = new HashSet();


    Vector /*Unit*/ units;
    int depth;

    static Keywords words = new Keywords();


    JReader ()
    {
        defpack = Main.args.getProperty("defpack");
        if (defpack != null)
        {
            if (defpack.length() == 0)
                defpack = null;
            else
                if (!defpack.endsWith("."))
                    defpack += '.';
        }
    }


    boolean Open (String f)
    {
        String err = OpenStream(f);
        if (err == null)
        {
            OpenScan();
            return true;
        }
        else
        {
            System.err.println(err);
            return false;
        }
    }


    String OpenStream (String f)
    {
        Close();

        if (f == null || (fname = f.trim().intern()).length() == 0)
            return "Missing source file name";

        try
        {
            is = new BufferedReader(new UFileReader(fname));
            //is = new LineNumberReader(new FileReader(fname));
        }
        catch (IOException x)
        {
            return "Failed to open source file \""+fname+"\"";
        }

        return null;
    }


    void OpenScan ()
    {
        st = new StreamTokenizer(is);

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
    }


    void Close ()
    {
        lex  = null;
        st   = null;

        if (is != null)
        {
            try
            {
                is.close();
            }
            catch (IOException x)
            {
            }

            is = null;
        }

        fname = null;
    }


    void PrintUnknown (PrintStream out)
    {
        if (!unknown.isEmpty())
        {
            out.println("unknown classes:");

            for (Iterator i = unknown.iterator(); i.hasNext();)
                out.println((String)i.next());

            out.println();
        }
    }


    boolean ReadUnit (Vector /*Unit*/ uu, String topname)
    {
        st.eolIsSignificant(false);
        if (syn == syn_eol)
            ReadWord();

        units = uu;
        depth = 0;

        boolean r;

        while (syn == syn_ident && lex.equals("use"))
        {// use <ident>, ... ;
            ReadWord();

            while (syn == syn_ident)
            {
                int i = lex.lastIndexOf('.');
                if (i == -1)
                {
                        Err("invalid use", lex);
                }
                else
                {
                    if (classes.put(lex.substring(i+1), lex.substring(0, i+1)) != null)
                        Err("duplicate use", lex);
                }
                ReadWord();

                if (syn != syn_sep || chr != ',') break;
                ReadWord();
            }
            ReadSep(';');
        }
        
        if (syn == syn_package)
        {// package <ident>;

            ReadWord();
            CheckIdent();

            UnitPack unit = new UnitPack();
            unit.fname  = fname;
            unit.linenb = st.lineno();
            
            int i = lex.lastIndexOf('.');
            if (i == -1)
                unit.Name(null, lex);
            else
                unit.Name(lex.substring(0, i+1), lex.substring(i+1));

            ReadWord();
            ReadSep(';');
            units.add(unit);
            r = true;
        }
        else
            r = ReadInternal(topname);

        units = null;
        return r;
    }


    boolean ReadInternal (String topname)
    {
        int m = ReadModifier();

    //  class/interface

        if (syn == syn_class || syn == syn_interface)
        {
            if (syn == syn_interface)
            {
                 m |= XModifier.xinterface;
                 m |= XModifier.xabstract;
            }

            UnitClass unit = new UnitClass();
            unit.fname  = fname;
            unit.linenb = st.lineno();

            unit.defined  = true;
            unit.modifier = m;

            ReadWord();
            CheckIdent();
            CheckName(lex);
            unit.Name(topname, lex);
            ReadWord();

            // extends <ident>
            if (syn == syn_extends)
            {
                ReadWord();

                String[] w;
                if ((w = ReadClassNames()) == null)
                    Err("class name(s) expected after extends");
                else
                    if ((m & XModifier.xinterface) != 0)
                    {
                        unit.implement = w;
                    }
                    else
                    {
                        unit.extend = w[0];
                        if (w.length > 1)
                            Err("only one superclass allowed", unit.toString());
                    }
            }
            else if ((m & XModifier.xinterface) == 0)
            {
                if (!"java.lang.Object".equals(unit.topname+unit.name))
                    unit.extend = "java.lang.Object".intern();
            }

            // implements <ident>, <ident>, ...
            if (syn == syn_implements)
            {
                ReadWord();

                if ((unit.implement = ReadClassNames()) == null)
                    Err("class name(s) expected after implements");

                if ((m & XModifier.xinterface) != 0)
                    Err("implements not allowed in interfaces", unit.toString());
            }

            units.add(unit);

            CheckModifier(unit, (unit.modifier & XModifier.xinterface) == 0 
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

            if (depth != 0)
                CheckClassMember(unit);

            // { <class body> }
            if (syn == syn_sep && chr == '{')
            {
                ReadSep('{');
                depth++;
                    
                while (ReadInternal(unit.toString()+"."))
                    ;


                depth--;
                ReadSep('}');
            }

            return true;
        }

    //  constructor/method/field

        if (syn == syn_ident)
        {
            String s1 = lex;
            ReadWord();
            
            if (syn == syn_sep && chr == '(')
            {// constructor
                UnitConstructor unit = new UnitConstructor();
                unit.fname  = fname;
                unit.linenb = st.lineno();
                unit.modifier = m;
                CheckName(s1);
                unit.Name(topname, s1);
                unit.args = ReadArgs();
                unit.xthrows = ReadThrows();
                units.add(unit);
                CheckClassMember(unit);
                CheckModifier(unit, XModifier.xpublic       |
                                    XModifier.xprivate      |
                                    XModifier.xprotected    |
                                    XModifier.xstatic       |
                                    XModifier.xfinal        |
                                    XModifier.xsynchronized |
                                    XModifier.xnative       |
                                    XModifier.xabstract     |
                                    XModifier.xstrictfp);
                ReadSep(';');
                return true;
            }
            else
            {// method/field
             // we see now return type of a method or type of a field
                while (syn == syn_sep && chr == '[')
                {
                    ReadWord();
                    ReadSep(']');
                    s1 += "[]";
                }
             // now we see name of a method or field
                if (syn != syn_ident)
                    Err("name of a method or field expected");
                CheckName(lex);
                String s2 = lex;
                ReadWord();

                if (syn == syn_sep)
                {
                    if (chr == '(')
                    {// it's a method
                        UnitMethod unit = new UnitMethod();
                        unit.fname  = fname;
                        unit.linenb = st.lineno();
                        unit.modifier = m;
                        CheckName(s2);
                        unit.Name(topname, s2);
                        unit.type = ExpandClassName(s1).intern();
                        unit.args = ReadArgs();
                        unit.xthrows = ReadThrows();
                        units.add(unit);
                        CheckClassMember(unit);
                        CheckModifier(unit, XModifier.xpublic       |
                                            XModifier.xprivate      |
                                            XModifier.xprotected    |
                                            XModifier.xstatic       |
                                            XModifier.xfinal        |
                                            XModifier.xsynchronized |
                                            XModifier.xnative       |
                                            XModifier.xabstract     |
                                            XModifier.xstrictfp);
                        ReadSep(';');
                        return true;
                    }
                    else 
                    {
                        for (;;)
                        {
                            String s3 = "";
                            while (syn == syn_sep && chr == '[')
                            {
                                ReadWord();
                                ReadSep(']');
                                s3 += "[]";
                            }

                            UnitField unit = new UnitField();
                            unit.fname  = fname;
                            unit.linenb = st.lineno();
                            unit.modifier = m;
                            CheckName(s2);
                            unit.Name(topname, s2);
                            unit.type = ExpandClassName(s1+s3).intern();

                            if (chr == '=')
                            {// it's a field with assignment
                                ReadWord();
                                unit.value = ReadExpr(unit.type);
                            }

                            units.add(unit);
                            CheckClassMember(unit);
                            CheckModifier(unit, XModifier.xpublic       |
                                                XModifier.xprivate      |
                                                XModifier.xprotected    |
                                                XModifier.xstatic       |
                                                XModifier.xfinal        |
                                                XModifier.xvolatile     |
                                                XModifier.xtransient);

                            if (syn != syn_sep || chr != ',') break;
                            ReadWord();
                            if (syn != syn_ident)
                                Err("field name expected");
                            s2 = lex;
                            ReadWord();
                        }
                        ReadSep(';');
                        return true;
                    }
                }
            }
        }

        return false;
    }


/*
    void Name (Unit unit, String top, String own)
    {
        int i = own.lastIndexOf('.');
        if (i != -1 && depth > 0)
        {
            if (!top.endsWith(own.substring(0, i+1) + '.'))
                Err("invalid name");
            unit.Name(top, own.substring(i+1));
        }
        else
        {
            unit.Name(top, own);
        }
    }
*/


    void CheckName (String s)
    {
        if (depth != 0 && s.indexOf('.') != -1)
            Err("only simple name allowed inside class definition");
    }


    void CheckClassMember (Unit u)
    {
        String s = "";
        int l = u.topname.length();
        int i = u.topname.lastIndexOf('.', l-2);
        if (i != -1) s = u.topname.substring(i+1, l-1);

        if (u.name == null || u.name.length() == 0) 
        {
            Err("name empty", u);
            return;
        }

        if (s == null || s.length() == 0) 
            return;

        if (u instanceof UnitConstructor)
        {
            if (!s.equals(u.name))
                Err("invalid name of class constructor", u);
        }
    }


    void CheckModifier (Unit u, int m)
    {
        int x = u.modifier & ~m;
        if (x != 0)
            Err("invalid modifier(s): \""+XModifier.toString(x)+"\"", u);
    }


    String[] ReadArgs ()
    {
        Vector v = new Vector();
        ReadSep('(');

        while (syn == syn_ident)
        {
            if (lex.equals("final"))
            {
                ReadWord(); // ignore 'final'
                if (syn != syn_ident)
                    Err("type name expected after final");
            }

            String s = ReadType();

            if (syn == syn_ident)
                ReadWord(); // ignore argument name

            String s1 = "";
            while (syn == syn_sep && chr == '[')
            {
                ReadWord();
                ReadSep(']');
                s1 += "[]";
            }

            v.add(s+s1);

            if (syn != syn_sep || chr != ',') break;
            ReadWord();
        }

        ReadSep(')');
        return ToArrayString(v);
    }


    String[] ReadThrows ()
    {
        Vector v = new Vector();

        if (syn == syn_throws)
        {
            ReadWord();

            while (syn == syn_ident)
            {
                v.add(ReadType());
                if (syn != syn_sep || chr != ',') break;
                ReadWord();
            }

            if (v.size() == 0)
                Err("type name(s) expected after throws");

            return ToArrayString(v);
        }
        else
            return null;
    }


    String ReadType ()
    {
        CheckIdent();
        String s = ExpandClassName(lex);
        ReadWord();

        while (syn == syn_sep && chr == '[')
        {
            ReadWord();
            ReadSep(']');
            s += "[]";
        }

        return s;
    }


    String[] ReadClassNames ()
    {
        Vector v = new Vector();

        while (syn == syn_ident)
        {
            v.add(ExpandClassName(lex));
            ReadWord();

            if (syn != syn_sep || chr != ',') break;
            ReadWord();
        }

        if (v.size() == 0)
            return null;
        else
            return ToArrayString(v);
    }


    String ExpandClassName (String s)
    {
        if (s.indexOf('.') == -1)
        {
            int i = s.indexOf("[]");
            String x = (i == -1) ? s : s.substring(0, i);
                
            if (XProg.DefineTypePrimitive(x) == null)
                return i == -1 ? ExpandClass(x) : ExpandClass(x) + s.substring(i);
        }

        return s;
    }


    String ExpandClass (String x)
    {
        String s = (String)classes.get(x);
        
        if (s == null)
            unknown.add(x);
        else
            return s + x;

        if (defpack != null)
            return defpack + x;

        return x;
    }


    static
    String[] ToArrayString (Vector v)
    {
        if (v == null | v.size() == 0)
            return null;
        else
        {
            String[] a = new String[v.size()];

            for (int i = 0; i < v.size(); i++)
                a[i] = ((String)v.get(i)).intern();

            return a;
        }
    }


    int ReadModifier ()  
    {
        int mask = 0, m;

        while (syn == syn_ident && (m = XModifier.Convert(lex)) != 0)
        {
            mask |= m;
            ReadWord();
        }

        return mask;
    }


    Object ReadExpr (String type)  
    {
        Object v = null;

        if (type.equals("java.lang.String"))
        {
            if (syn != syn_sep || chr != '"' || st.sval == null)
                ErrStop("string expression expected");
            try
            {
                v = new String(st.sval);
            }
            catch (Exception e)
            {
                ErrStop("invalid string value");
            }
            ReadWord();
        }
        else if (type.equals("char"))
        {
            try
            {
                if (syn == syn_sep && chr == '\'')
                    v = new Character(st.sval.charAt(0));
                else
                {
                    int i = Integer.decode(lex).intValue();
                    if (i < 0 || i > 0xFFFF)
                        throw new Exception();
                    v = new Character((char)i);
                }
            }
            catch (Exception e)
            {
                ErrStop("invalid char value");
            }
            ReadWord();
        }
        else if (type.equals("boolean"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");

            if (lex.equals("true"))
                v = new Boolean(true);
            else if (lex.equals("false"))
                v = new Boolean(false);
            else
                ErrStop("invalid boolean value");

            ReadWord();
        }
        else if (type.equals("byte"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");
            try
            {
                v = Byte.decode(lex);
            }
            catch (Exception e)
            {
                ErrStop("invalid byte value");
            }
            ReadWord();
        }
        else if (type.equals("short"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");
            try
            {
                v = Short.decode(lex);
            }
            catch (Exception e)
            {
                ErrStop("invalid short value");
            }
            ReadWord();
        }
        else if (type.equals("int"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");
            try
            {
                v = Integer.decode(lex);
            }
            catch (Exception e)
            {
                ErrStop("invalid integer value");
            }
            ReadWord();
        }
        else if (type.equals("long"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");

            try
            {
                if (lex.endsWith("L") || lex.endsWith("l"))
                    v = Long.decode(lex.substring(0, lex.length()-1));
                else
                    v = Long.decode(lex);
            }
            catch (Exception e)
            {
                ErrStop("invalid long value");
            }

            ReadWord();
        }
        else if (type.equals("float"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");
            try
            {
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
            catch (Exception e)
            {
                ErrStop("invalid float value");
            }
            ReadWord();
        }
        else if (type.equals("double"))
        {
            if (syn != syn_ident)
                ErrStop("expression expected");
            try
            {
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
            catch (Exception e)
            {
                ErrStop("invalid double value");
            }
            ReadWord();
        }
        else 
        {
            ErrStop("values of type \""+type+"\" not supported");
        }

    /*
        if (v != null)
        {
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


    void CheckIdent ()  
    {
        if (syn != syn_ident)
            ErrStop("ident expected");
    }


    void ReadSep (char c)  
    {
        if (syn != syn_sep || chr != c)
            ErrStop("separator expected: <"+c+">");
        ReadWord();
    }


    void ReadEol ()  
    {
        if (syn != syn_eof)
        {
            if (syn != syn_eol)
                ErrStop("extra symbol(s) on line");

            while (syn != syn_eof && syn != syn_eol)
                ReadWord();

            if (syn == syn_eol)
                ReadWord();
        }
    }


//  Print error message and throw exception
    void ErrStop (String m)
    {
        errors++;
        System.err.println(m);
        System.err.println(st.toString());
        throw new Error();
    }


//  Print error message and return to caller 'false'
    boolean Err (String m, Unit u)
    {
        errors++;
        System.err.println(m);
        System.err.println("\"" + u.toString() + "\", line " + u.linenb);
        return false;
    }


//  Print error message and return to caller 'false'
    boolean Err (String m, String n)
    {
        errors++;
        System.err.println(m);
        if (n != null)
            System.err.println(n);
        return false;
    }


//  Print error message and return to caller 'false'
    boolean Err (String m)
    {
        errors++;
        System.err.println(m);
        System.err.println(st.toString());
        return false;
    }


    boolean ReadWord ()  
    {
        try
        {
            st.nextToken();
        }
        catch (IOException x)
        {
            System.err.println("Read error from file "+fname+"\n"+x);
            throw new Error();
        }

        //System.out.println("ttype="+st.ttype+" "+st.sval);

        switch (st.ttype)
        {
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
                syn = words.Look(lex);
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

