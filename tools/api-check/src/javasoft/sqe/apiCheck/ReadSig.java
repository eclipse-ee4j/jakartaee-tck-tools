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
 *      This module reads old signature files from 
 *      javasoft.sqe.tests.api.signaturetest.setup.Setup -apichanges
 *
 */


package javasoft.sqe.apiCheck;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;



class ReadSig
{
    static final int syn_eof   = -1,
                     syn_eol   = 0,
                     syn_ident = 1,
                     syn_sep   = 2,

                     syn_CLSS   = 10,
                     syn_supr   = 11,
                     syn_intf   = 12,
                     syn_cons   = 13,
                     syn_meth   = 14,
                     syn_fld    = 15,
                     syn_innr   = 16,
                     syn_throws = 17,
                     syn_null   = 18;

    static class Keywords extends Hashtable
    {
        Keywords ()
        {
            Put("CLSS",     syn_CLSS);
            Put("supr",     syn_supr);
            Put("intf",     syn_intf);
            Put("cons",     syn_cons);
            Put("meth",     syn_meth);
            Put("fld",      syn_fld);
            Put("innr",     syn_innr);
            Put("throws",   syn_throws);
            Put("null",     syn_null);
        }


        void Put (String word, int key)
        {
            super.put(word, new Integer(key));
        }


        int Look (String word)
        {
            Integer i = (Integer)super.get(word);
            return i == null ? syn_ident : i.intValue();
        }
    }


    static Keywords words = new Keywords();

    String fname;
    BufferedReader is;
    String line;
    StringTokenizer tokens;
    String lex;
    char   chr;
    int    syn;
    int    errors;

    XProg prog;
    PackageSet pset;
    String version;


    int Read (API api, String f, PackageSet packs)
    {
        if (OpenStream(f) != null)
            return 0;

        if (!ReadLine() || !line.equals("#API Master signature file"))
        {
            Close();
            return 0;
        }

        prog = api.xprog = new XProg();
        pset = packs;

        api.props.Add("sigfile", f);

        long t0 = Main.GetTimer();

        if (version != null)
            api.props.Add("version", version);

        boolean r = Read();

        Main.PrintTimer("ReadSig "+f, t0);

        pset = null;
        prog = null;

        Close();
        return r ? +1 : -1;
    }


    String OpenStream (String f)
    {
        Close();

        if (f == null || (fname = f.trim()).length() == 0)
            return "Missing source file name";

        try
        {
            is = new BufferedReader(new FileReader(fname));
        }
        catch (IOException x)
        {
            return "Failed to open source file \""+fname+"\"";
        }

        return null;
    }


    void Close ()
    {
        lex    = null;
        tokens = null;
        line   = null;

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


    boolean Read ()
    {
        try
        {
            errors = 0;

            if (!ReadLine()) 
                Fatal.Stop("Empty file");

            while (line.startsWith("#"))
            {
                if (line.startsWith("#Version"))
                    version = line.substring(8).trim();
                else
                    ;

                ReadLine();
            }

            ReadWord();

            while (syn == syn_CLSS)
            {
                ReadWord();
                ReadClass();
            }
            
            if (syn != syn_eol) ErrStop("Invalid symbol");  
            if (ReadLine())     ErrStop("Invalid symbol");

            //prog.IfsClear();

            return true;
        }
        catch (Error x)
        {
            return false;
        }
    }


    void ReadClass () throws Fatal
    {
        int m = ReadModifiers();

        CheckIdent();
        XClass xclass = prog.DefineClass(lex);
        ReadWord();
        xclass.defined  = true;
        xclass.modifier = m;

        if (syn != syn_eol) ErrStop();
        ReadLine();
        ReadWord();

        while (true)
        {
            switch (syn)
            {
                case syn_supr:
                    ReadWord();
                    if (syn != syn_null)
                    {
                        CheckIdent();
                        if (xclass.extend != null) ErrStop();
                        xclass.extend = prog.DefineClass(lex);
                    }
                    ReadWord();
                    break;

                case syn_intf:
                    ReadWord();
                    CheckIdent();
                    xclass.implement.Add(prog.DefineClass(lex));
                    ReadWord();
                    break;

                case syn_cons:
                {
                    ReadWord();
                    XClassConstructor xconstructor = new XClassConstructor();
                    xconstructor.modifier = ReadModifiers();
                    xconstructor.name = ReadName(xclass);
                    if (xconstructor.name == null) ErrStop();
                    xconstructor.args = ReadArgs();
                    xconstructor.xthrows = ReadThrows();
                    xconstructor.Link(xclass);
                    break;
                }

                case syn_meth:
                {
                    ReadWord();
                    XClassMethod xmethod = new XClassMethod();
                    xmethod.modifier = ReadModifiers();
                    xmethod.type = ReadType();
                    xmethod.name = ReadName(xclass);
                    xmethod.args = ReadArgs();
                    xmethod.xthrows = ReadThrows();
                    if (xmethod.name != null)
                        xmethod.Link(xclass);
                    break;
                }

                case syn_fld:
                {
                    ReadWord();
                    XClassField xfield = new XClassField();
                    xfield.modifier = ReadModifiers();
                    xfield.type = ReadType();
                    xfield.name = ReadName(xclass);
                    if (xfield.name != null)
                        xfield.Link(xclass);
                    break;
                }

                case syn_innr:
                {
                    ReadWord();
                    int mm = ReadModifiers();

                    CheckIdent();
                    XClass xinner = prog.DefineClass(lex);
                    ReadWord();

                    xinner.modifier = mm;
                    break;
                }

                default:
                    return;
            }

            if (syn != syn_eol) 
                ErrStop();

            ReadLine();
            ReadWord();
        }
    }


    int ReadModifiers ()
    {
        int mask = 0, m;

        while (syn == syn_ident && (m = XModifier.Convert(lex)) != 0)
            ReadWord();

        return mask;
    }


    String ReadName (XClass xclass) throws Fatal
    {
        CheckIdent();
        int i = lex.lastIndexOf(".");
        if (i < 0) ErrStop();

        String s0 = lex.substring(0, i),
               s1 = lex.substring(i+1);

        ReadWord();

        return s0.equals(xclass.FullName()) ? s1 : null;
    }


    XTypes ReadArgs () throws Fatal
    {
        XTypes tt = new XTypes();
        
        if (syn != syn_sep || chr != '(') ErrStop();
        ReadWord();
        
        while (syn == syn_ident)
        {
            tt.Add(ReadType());

            if (syn != syn_sep || chr != ',') break;
            ReadWord();
        }
        
        
        if (syn != syn_sep || chr != ')') ErrStop();
        ReadWord();

        return tt;
    }


    XTypes ReadThrows () throws Fatal
    {
        XTypes tt = new XTypes();
        
        if (syn == syn_throws)
        {
            ReadWord();
        
            while (syn == syn_ident)
            {
                tt.Add(ReadType());

                if (syn != syn_sep || chr != ',') break;
                ReadWord();
        }   }

        return tt;
    }


    XType ReadType () throws Fatal
    {
        XType t;

        CheckIdent();
        t = prog.DefineType(lex);
        ReadWord();

        return t;
    }


    void CheckIdent () throws Fatal
    {
        if (syn != syn_ident)
            ErrStop("Ident expected");
    }


    boolean ReadWord ()
    {
        if (tokens.hasMoreElements())
        {
            lex = (String)tokens.nextElement();
            chr = lex.charAt(0);
            if (chr == ' ')
                return ReadWord();
            else
            {
                syn = Character.isLetter(chr) || chr == '[' ? words.Look(lex) : syn_sep;
                return true;
        }   }    
        else
        {
            syn =  syn_eol;
            return false;
        }
    }


    boolean ReadLine () throws Fatal
    {
        try
        {
            if ((line = is.readLine()) != null)
            {
                //System.err.println(line);
                tokens = new StringTokenizer(line, " ,()", true); 
                return true;
            }
            else
                return false;
        }
        catch (IOException x)
        {
            ErrStop("Read error from source file "+fname+"\n"+x);
            return false;
        }
    }


    void ErrStop ()
    {
        ErrStop("error in .sig file");
    }


    void ErrStop (String m)
    {
        errors++;
        System.err.println(m);
        if (line != null)
            System.err.println(line);
        
        throw new Error();
    }

}

