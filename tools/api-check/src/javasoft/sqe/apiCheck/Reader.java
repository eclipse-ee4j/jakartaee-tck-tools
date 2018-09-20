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
 *      This module creates ReadClasses class (ReadReflect or ReadStatic) according
 *      to selected mode of operation.
 */


package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;



class API
{
    Props props = new Props();
    Units units = null;
    XProg xprog = null;


    boolean IsEmpty ()
    {
        if (units != null) return units.IsEmpty();
        if (xprog != null) return xprog.IsEmpty();
        return true;
    }


    XProg GetXProg ()
    {
        if (units != null && xprog == null)
        {
            xprog = new XProg();
            Converter.Convert(units, xprog);           
        }

        return xprog;
    }


    Units GetUnits ()
    {
        if (xprog != null && units == null)
        {
            units = new Units();
            Converter.Convert(xprog, units);
        }

        return units;
    }


    // Read API description in Unit or XNode format from signature file or from class files
    //
    // returns
    //     0: no api was read
    //     1: api was read, no error
    //    -1: api was read but errors detected
    //
    int ReadAll (String name, PackageSet pset)
    {
        //  Try new signature file first

        ReadJh r1 = new ReadJh();
        int r = r1.Read(this, name, pset);
        if (r != 0)
            return r;

        r1 = null;

        //  Try old signature file second

        ReadSig r2 = new ReadSig();
        r = r2.Read(this, name, pset);
        if (r != 0)
            return r;

        r2 = null;

        // Try zip file

        return ReadPath(name, pset) ? 1 : 0;
    }


    // Read API description in XNode format from class files (for Setup program)
    //
    boolean ReadPath (String name, PackageSet pset)
    {
        boolean values = (Main.args.getProperty("constvalues") != null);

        ReadClasses reader;
        if (Main.args.getProperty("reflect") != null )
            reader = new ReadReflect(values);
        else
            reader = new ReadStatic(values);

        return reader.ReadPath(this, name, pset);
    }
}



class Converter
{

//
//  Converter Units -> XProg
//


    static
    void Convert (Units units, XProg xprog)
    {
        long t0 = Main.GetTimer();

    //  Create all known classes
        Loop1 loop1 = new Loop1();
        loop1.xprog = xprog;
        loop1.Walk(units);
        loop1 = null;

    //  Populate classes
        Loop2 loop2 = new Loop2();
        loop2.xprog = xprog;
        loop2.Walk(units);
        loop2 = null;

    //  Propagate "strictfp" modidier
        Loop3 loop3 = new Loop3();
        loop3.Walk(xprog, null);

        Main.PrintTimer("ConvertUnitsToXProg", t0);
    }


    static
    class Loop1 extends UnitsWalk
    {
        XProg xprog;

    //  set of known class names
        HashSet classes = new HashSet();


        void Walk (Units uu)
        {
            super.Walk(uu);

        //  Sort all known classes
            String[] temp = new String[classes.size()];
            classes.toArray(temp);
            classes = null;
            Arrays.sort(temp);

        //  Create all known classes
            for (int i = 0; i< temp.length; i++)
                if (XProg.DefineTypePrimitive(temp[i]) == null)
                    xprog.DefineClass(temp[i]);
        }

        
        void Walk (UnitPack u)
        {
            xprog.DefinePack(new Path(u.topname+u.name), true);
        }


        void Walk (UnitClass u)
        {
            DefineType(u.topname+u.name);
            DefineType(u.extend);
            DefineType(u.implement);
        }


        void Walk (UnitConstructor u)
        {
            DefineType(u.args);
            DefineType(u.xthrows);
        }


        void Walk (UnitMethod u)
        {
            DefineType(u.type);
            DefineType(u.args);
            DefineType(u.xthrows);
        }


        void Walk (UnitField u)
        {
            DefineType(u.type);
        }


        void DefineType (String[] ss)
        {
            if (ss != null)
                for (int i = 0; i < ss.length; i++)
                    DefineType(ss[i]);
        }

        
        void DefineType (String t)
        {
            if (t != null)
            {
                for (int l = t.length(); t.endsWith("[]"); )
                {
                    l -= 2;
                    t = t.substring(0, l);
                }
                classes.add(t);
            }
        }
    }



    static
    class Loop2 extends UnitsWalk
    {
        XProg xprog;


        void Walk (UnitClass w)
        {
            XClass c = xprog.DefineClass(w.topname+w.name);
            c.fname  = w.fname;
            c.linenb = w.linenb;

            c.defined  = w.defined;
            c.modifier = w.modifier;

            if (w.extend != null)
                c.extend = xprog.DefineClass(w.extend);

            if (w.implement != null)
                for (int i = 0; i < w.implement.length; i++)
                    c.implement.Add(xprog.DefineClass(w.implement[i]));
        }


        void Walk (UnitConstructor w)
        {
            XClassConstructor x = new XClassConstructor();
            x.fname    = w.fname;
            x.linenb   = w.linenb;
            x.modifier = w.modifier;
            x.name     = w.name;
            x.args     = xprog.DefineTypes(w.args);
            x.xthrows  = xprog.DefineTypes(w.xthrows);
            x.Link(FindTop(xprog, w.topname));
        }


        void Walk (UnitMethod w)
        {
            XClassMethod x = new XClassMethod();
            x.fname    = w.fname;
            x.linenb   = w.linenb;
            x.modifier = w.modifier;
            x.name     = w.name;
            x.type     = xprog.DefineType(w.type);
            x.args     = xprog.DefineTypes(w.args);
            x.xthrows  = xprog.DefineTypes(w.xthrows);
            x.Link(FindTop(xprog, w.topname));
        }


        void Walk (UnitField w)
        {
            XClassField x = new XClassField();
            x.fname    = w.fname;
            x.linenb   = w.linenb;
            x.modifier = w.modifier;
            x.name     = w.name;
            x.type     = xprog.DefineType(w.type);
            x.value    = w.value;
            x.Link(FindTop(xprog, w.topname));
        }
    }



    static 
    class Loop3 extends XProgWalk
    {
        boolean xstrictfp  = false,
                xinterface = false;


        void Walk (XClass x)
        {
            boolean xstrictfpold = xstrictfp,
                    xinterfaceold = xinterface;

            xstrictfp  = (x.modifier & XModifier.xstrictfp)  != 0;

            if ((x.modifier & XModifier.xinterface) != 0)
            {
                xinterface = true;
                x.modifier &= ~XModifier.xabstract;
            }

            super.Walk(x);

            xinterface = xinterfaceold;
            xstrictfp  = xstrictfpold;
        }


        void Walk (XClassConstructor x)
        {
            if (xstrictfp)
                x.modifier |= XModifier.xstrictfp;
        }


        void Walk (XClassMethod x)
        {
            if (xstrictfp)
                x.modifier |= XModifier.xstrictfp;

            if (xinterface)
                x.modifier &= ~XModifier.xabstract;
        }
    }



    static 
    XClass FindTop (XProg prog, String s)
    {
        return prog.DefineClass(s.substring(0, s.length()-1));
    }



//
//  Converter XProg -> Units
//


    static
    void Convert (XProg xprog, Units units)
    {
        long t0 = Main.GetTimer();

        XProgConverter c = new XProgConverter();
        c.units = units;
        c.Walk(xprog, null);

        Main.PrintTimer("ConvertXProgToUnits", t0);
    }


    static
    class XProgConverter extends XProgWalk
    {
        Units units;


        void Walk (XPack x)
        {
            UnitPack u = new UnitPack();
            u.fname    = x.fname;
            u.linenb   = x.linenb;
            u.Name(x.top == null ? null : x.top.FullName()+'.', x.toString());
            units.add(u);

            super.Walk(x);
        }


        void Walk (XClass x)
        {
            UnitClass u = new UnitClass();
            u.fname     = x.fname;
            u.linenb    = x.linenb;
            u.defined   = x.defined;
            u.modifier  = x.modifier;
            u.extend    = x.extend   == null ? null : x.extend.FullName().intern();
            u.implement = ConvertStrings(x.implement);
            u.Name(x.home == null ? x.pack.FullName()+'.' : x.home.FullName()+'.', x.name);
            units.add(u);

            if (x.defined)
                super.Walk(x);
        }


        void Walk (XClassConstructor x)
        {
            UnitConstructor w = new UnitConstructor();
            w.fname    = x.fname;
            w.linenb   = x.linenb;
            w.modifier = x.modifier;
            w.args     = Convert(x.args);
            w.xthrows  = Convert(x.xthrows);    
            w.Name(x.home.FullName()+'.', x.name);
            units.add(w);               
        }


        void Walk (XClassMethod x)
        {
            UnitMethod w = new UnitMethod();
            w.fname    = x.fname;
            w.linenb   = x.linenb;
            w.modifier = x.modifier;
            w.type     = x.type.toString().intern();
            w.args     = Convert(x.args);
            w.xthrows  = Convert(x.xthrows);           
            w.Name(x.home.FullName()+'.', x.name);
            units.add(w);               
        }


        void Walk (XClassField x)
        {
            UnitField w = new UnitField();
            w.fname    = x.fname;
            w.linenb   = x.linenb;
            w.modifier = x.modifier;
            w.type     = x.type.toString().intern();
            w.value    = x.value;
            w.Name(x.home.FullName()+'.', x.name);
            units.add(w);               
        }
    }


    static 
    String[] ConvertStrings (Vector v)
    {
        if (v.size() == 0)
            return null;

        String[] x = new String[v.size()];

        for (int i = 0; i < v.size(); i++)
            x[i] = ((XClass)v.elementAt(i)).FullName().intern();

        return x;
    }


    static
    String[] Convert (XTypes v)
    {
        if (v.size() == 0)
            return null;

        String[] x = new String[v.size()];

        for (int i = 0; i < v.size(); i++)
            x[i] = ((XType)v.elementAt(i)).toString().intern();

        return x;
    }


    static 
    String Form (String[] a)
    {
        if (a == null || a.length == 0)
            return "";
        else
        {
            String s = "";

            for (int i = 0; i < a.length; i++)
            {
                if (s.length() != 0)
                    s += ",";
                s += a[i];
            }
        
            return s;
        }
    }
}


//
//  Expand simple class names
//

class Expander
{
    HashMap known = new HashMap();
    HashSet unknown;


    void PrintResults (PrintStream out)
    {
        if (!unknown.isEmpty())
        {
            out.println("unknown classes:");

            for (Iterator i = unknown.iterator(); i.hasNext();)
                out.println((String)i.next());

            out.println();
        }
    }


    void Expand (Units units)
    {
        unknown = new HashSet();

        ExpandLoop1 loop1 = new ExpandLoop1();
        loop1.Walk(units);
        loop1 = null;

        ExpandLoop2 loop2 = new ExpandLoop2();
        loop2.Walk(units);
        loop2 = null;
    }


    class ExpandLoop1 extends UnitsWalk
    {
        void Walk (UnitClass u)
        {
            if (!known.containsKey(u.name))
                known.put(u.name, (u.topname + u.name).intern());
            else
            {
                //System.err.println(u.name + " DUPLICATE ");
                known.put(u.name, null);
            }
        }
    }


    class ExpandLoop2 extends UnitsWalk
    {
        void Walk (UnitClass u)
        {
            u.extend = Expand(u.extend);
            Expand(u.implement);
        }


        void Walk (UnitConstructor u)
        {
            Expand(u.args);
            Expand(u.xthrows);
        }


        void Walk (UnitMethod u)
        {
            u.type = Expand(u.type);
            Expand(u.args);
            Expand(u.xthrows);
        }


        void Walk (UnitField u)
        {
            u.type = Expand(u.type);
        }


        void Expand (String[] ss)
        {
            if (ss != null)
                for (int i = 0; i < ss.length; i++)
                    ss[i] = Expand(ss[i]);
        }


        String Expand (String s)
        {
            if (s == null || s.indexOf('.') != -1)
                return s;

        //  Simple name

            int i = s.indexOf("[]");
            String x = (i == -1) ? s : s.substring(0, i);
            if (XProg.DefineTypePrimitive(x) != null)
                return s;

            String y = (String)known.get(x);
            if (y == null)
            {
                y = /*"UNKNOWN." +*/ x;
                unknown.add(x); 
            }

            //System.err.println("EXPAND " + y);

            return i == -1 ? y : (y + s.substring(i)).intern();
        }
    }
}



class NormUnits
{
    HashSet intfs;


    void Do (Units uu)
    {
        intfs = new HashSet();

        Loop loop = new Loop();
        loop.Walk(uu);

        intfs = null;
    }


    class Loop extends UnitsWalk
    {
        void Walk (UnitClass u)
        {
            if ((u.modifier & XModifier.xinterface) != 0)
            {
                intfs.add(u.topname+u.name+".");
                u.modifier |= XModifier.xabstract;
            }
        }


        void Walk (UnitMethod u)
        {
            if (intfs.contains(u.topname))
                u.modifier |= (XModifier.xpublic | XModifier.xabstract);
        }


        void Walk(UnitField u)
        {
            if (intfs.contains(u.topname))
                u.modifier |= (XModifier.xpublic | XModifier.xstatic | XModifier.xfinal);
        }

    }
}




