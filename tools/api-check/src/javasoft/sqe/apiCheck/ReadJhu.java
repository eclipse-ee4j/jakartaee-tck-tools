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
 *      This module reads the update file and builds modification database.
 *
 */

package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;



class ReadJhu extends JReader
{
    Units units;
    Tran  tran;
    int   errs;         // error count of current statement
    
    String defpack  = null;
    String defclass = null;;

    static final int syn_UPDATE  = 1000,
                     syn_PACKAGE = 1001,
                     syn_CLASS   = 1002,
                     syn_ADD     = 1003,
                     syn_DELETE  = 1004,
                     syn_CHANGE  = 1005,
                     syn_TO      = 1006,
                     syn_END     = 1007,
                     syn_INTERFACE = 1008;

    static
    {
        words.Put("#UPDATE",  syn_UPDATE);
        words.Put("#PACKAGE", syn_PACKAGE);
        words.Put("#CLASS",   syn_CLASS);
        words.Put("#ADD",     syn_ADD);
        words.Put("#DELETE",  syn_DELETE);
        words.Put("#CHANGE",  syn_CHANGE);
        words.Put("#TO",      syn_TO);
        words.Put("#END",     syn_END);
        words.Put("#INTERFACE",  syn_INTERFACE);
    }


    ReadJhu (Tran t)
    {
        tran = t;
    }


    boolean Read (API api, String f)
    {

        File file = new File(f);
        if (file.isDirectory())
        {
            String[] flist = file.list();
            
            file = null;

            if (flist != null)
                for (int i = 0; i < flist.length; i++)
                    if (!Read(api, f + File.separatorChar + flist[i]))
                        return false;

            return true;
        }
        file = null;

        //System.err.println("Read: " + f);

        boolean r = false;

        if (Open(f))
        {
            units = api.GetUnits();
            NormUnits norm = new NormUnits();
            norm.Do(units);
            norm = null;

            if (!(r = Read()))
                System.err.println("error in \"" + f + "\"");

            defpack   = null;
            defclass  = null;

            tran.defupd = null;
            tran.defstat = null;

            units = null;

            Close();
        }

        return r;
    }


    boolean Open (String f)
    {
        if (super.Open(f))
        {
            st.wordChars((int)'#', (int)'#');
            //trace = true;
            return true;
        }
        return false;
    }


    boolean Read ()
    {       
        try
        {
            errors = 0; // total error count
            ReadWord();

            while (syn != syn_eof)
            {
                st.eolIsSignificant(true);
                errs   = 0;     // statement error count

                switch (syn)
                {
                    case syn_eol:
                        ReadWord();
                        break;

                    case syn_UPDATE:
                        ReadWord();
                        CheckIdent();
                        tran.NewUpdate(lex, fname);
                        ReadWord();
                        ReadEol();
                        defpack  = null;
                        defclass = null;
                        break;

                    case syn_PACKAGE:
                        ReadWord();
                        if (syn != syn_eol)
                        {
                            CheckIdent();
                            defpack  = lex;
                            ReadWord();
                        }
                        else
                            defpack = null;
                        ReadEol();
                        defclass = null;
                        break;

                    case syn_CLASS:
                    case syn_INTERFACE:
                        ReadWord();
                        if (syn != syn_eol)
                        {
                            CheckIdent();
                            defclass = lex;
                            ReadWord();
                        }
                        else
                            defclass = null;
                        ReadEol();
                        break;

                    case syn_ADD:
                        NewStatement("#ADD");
                        ReadWord();
                        ReadEol();
                        DoAdd();
                        ErrCheck();
                        break;

                    case syn_DELETE:
                        NewStatement("#DELETE");
                        ReadWord();
                        ReadEol();
                        DoDelete();
                        ErrCheck();
                        break;

                    case syn_CHANGE:
                        NewStatement("#CHANGE");
                        ReadWord();
                        ReadEol();
                        DoChange();
                        ErrCheck();
                        break;

                    case syn_END:
                        ReadWord();
                        //defclass = null;  ???
                        break;

                    default:
                        Err("Invalid symbol");
                        ReadWord();
                        break;
                }
            }

            return errors == 0;
        }
        catch (Error x)
        {
            return false;
        }
    } 


    void NewStatement (String s)
    {
        if (tran.defupd == null)
            tran.NewUpdate("", fname);

        tran.NewStatement(s, st.lineno());
    }


    void DoAdd () throws Fatal
    {
        Units uu = new Units();
        while (ReadUnitNew(uu))
        {
            for (int i = 0; i < uu.size(); i++)
            {
                Unit u = (Unit)uu.get(i);
                if (u instanceof UnitPack)
                {
                    Err("package not allowed in #ADD", u);
                }
                else
                {
                    units.add(u);
                    tran.Transform(null, u);
                }
            }
            uu.removeAllElements();
        }        
    }


    void DoDelete () throws Fatal
    {
        Units uu = new Units();
        while (ReadUnitOld(uu))
        {
            for (int i = 0; i < uu.size(); i++)
            {
                Unit u = (Unit)uu.get(i);
                if (!tran.Transform(u, null))
                    Err("already deleted", u);
                else
                    if (u instanceof UnitPack || u instanceof UnitClass)
                    {
                        String parent = u.topname+u.name+".";
                        for (Enumeration e = units.elements(); e.hasMoreElements();)
                        {
                            Unit x = (Unit)e.nextElement();
                            if (x.topname != null && x.topname.startsWith(parent))
                                tran.Transform(x, null);
                        }
                    }
            }
            uu.removeAllElements();
        }        
    }


    void DoChange () throws Fatal
    {
        Units oldunits = new Units();
        if (!ReadUnitOld(oldunits) || oldunits.size() == 0)
        {
            Err("error in change-from item", (String)null);
            errs++;
        }
        else if (oldunits.size() != 1)
        {
            Err("only single item can be changed", (String)null);
            errs++;
        }

        st.eolIsSignificant(true);
        if (syn != syn_TO)
        {
            Err("#TO expected");
            errs++;
        }
        ReadWord();
        ReadEol();

        Units newunits = new Units();
        if (!ReadUnitNew(newunits) || newunits.size() == 0)
        {
            Err("error in change-to item", (String)null);
            errs++;
        }
        else if (newunits.size() != 1)
        {
            Err("only single item can be changed", (String)null);
            errs++;
        }
       
        st.eolIsSignificant(true);
        if (syn != syn_END)
        {
            Err("#END expected");
            errs++;
        }
        ReadWord();
        ReadEol();

        if (errs != 0)
            return;

        //System.err.println("change from");
        //oldunits.Print();
        //System.err.println("change to");
        //newunits.Print();

    //  Parsing is over

        Unit oldunit = (Unit)oldunits.get(0);
        Unit newunit = (Unit)newunits.get(0);

        if (oldunit.getClass() != newunit.getClass())
        {
            Err("uncompatible change ", newunit);
            return;
        }

        if (oldunit instanceof UnitPack)
        {
            if (newunit.topname == oldunit.topname
             && newunit.name    == oldunit.name)
                Err("nothing to rename", oldunit);

            units.add(newunit);
            tran.Transform(oldunit, newunit);

            RenameAll(oldunit.topname+oldunit.name, newunit.topname+newunit.name);
        }
        else if (oldunit instanceof UnitClass)
        {
        /*
            if ((oldunit.modifier & XModifier.xinterface) != 
                (newunit.modifier & XModifier.xinterface))
            {
                Err("uncompatible change ", newunit);
                return;
            }
        */

            if (oldunits.size() != 1 || newunits.size() != 1)
                Err("error ???");
                
            units.add(newunit);
            tran.Transform(oldunit, newunit);

            if (newunit.topname != oldunit.topname
             || newunit.name    != oldunit.name)
                RenameAll(oldunit.topname+oldunit.name, newunit.topname+newunit.name);
        }
        else
        {
            units.add(newunit);
            tran.Transform(oldunit, newunit);
        }
    }


    void RenameAll (String oldname, String newname)
    {
        Vector /*Unit*/ temp = new Vector();

        for (Enumeration e = units.elements(); e.hasMoreElements();)
        {
            Unit u = (Unit)e.nextElement();
            if (u.UsesType(oldname))
            {
                Unit w = (Unit)u.clone();
                w.Rename(oldname, newname);
                tran.Transform(u, w);
                temp.addElement(w);
            }
        }

        units.addAll(temp);
    }


    boolean ReadUnitNew (Units uu)
    {
        Units ww = new Units();
        if (!ReadUnit(ww, Topname()))
            return false;

        for (int i = 0; i < ww.size(); i++)
        {
            Unit w = (Unit)ww.get(i);
            if (units.contains(w) || uu.contains(w))
                Err("duplicate name", w);
            else
                uu.add(w);
        }

        return true;
    }


    boolean ReadUnitOld (Units uu)
    {
        Units ww = new Units();
        if (!ReadUnit(ww, Topname()) || ww.size() == 0)
            return false;

        for (int i = 0; i < ww.size(); i++)
        {
            Unit w = (Unit)ww.get(i);
            Unit u = units.Find(w);
            if (u == null)
                Err("name not found", w);
            else
                uu.add(u);
        }

        return true;
    }


    String Topname ()
    {
        String s = defpack == null ? "" : defpack+'.';

        if (defclass != null)
            s += defclass+'.';

        return s;
    }


    boolean Err (String m, Unit u)
    {
        errs++;
        return super.Err(m, u);
    }


    void ErrCheck ()
    {
        if (errs != 0)
            System.err.println(tran.defstat.name + ", line " + tran.defstat.linenb);
    }
}
