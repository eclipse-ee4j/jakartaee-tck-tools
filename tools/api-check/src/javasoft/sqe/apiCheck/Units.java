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
 *      This module contains structure definitions for internal class representations.
 */


package javasoft.sqe.apiCheck;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;



abstract 
class Unit extends NamedObject implements Cloneable
{
    String topname;     // full name of the container (including '.')
    int    modifier;    // not used for UnitPackage

    String fname;       // source file name
    int    linenb;      // source line number


    void Name (String top, String own)
    {
        topname = top == null ? "".intern() : top.intern();
        if (own == null)
            name = "".intern();
        else
        {
            int i = own.lastIndexOf('.');
            if (i == -1)
                name = own.intern();
            else
            {
                topname += own.substring(0, i+1);
                topname = topname.intern();
                name    = own.substring(i+1).intern();
            }
        }

    }


    boolean NameEq (Object x)
    {
        return x instanceof Unit && ((Unit)x).topname == topname && ((Unit)x).name == name;
    }


    abstract
    void Rename (String olds, String news);


    abstract
    boolean UsesType (String s);


    abstract
    String Descr ();    


    public 
    String toString ()
    {
        return topname+name;
    }


//  clone() method designed to serve the only specific function : support 
//  rename operation in ReadJhu.
//  So one can change only two field of cloned unit (name and/or topname).
//  An attempt to change any other field will change the corresponding field
//  of the original unit also.
//
    public
    Object clone ()
    {
        try
        {
            return (Unit)super.clone();
        }
        catch (CloneNotSupportedException x)
        {
            throw new InternalError(x.toString());
        }
    }


    static
    boolean EqX (String[] a, String[] b)
    {
        if (a == null && b == null)
            return true;

        if (a == null || b == null)
            return false;

        if (a.length != b.length)
            return false;

        boolean bfree[] = new boolean[b.length];

        int i, k;

        for (k = 0; k < b.length; k++)
            bfree[k] = true;

        for (i = 0; i < a.length; i++)
        {
            for (k = 0; k < b.length; k++)
                if (bfree[k] && a[i] == b[k])
                {
                    bfree[k] = false;
                    break;
                }

            if (k == b.length)
                return false;
        }

        return true;
    }


    static
    boolean Eq (String[] a, String[] b)
    {
        if (a == null && b == null)
            return true;

        if (a == null || b == null)
            return false;

        if (a.length != b.length)
            return false;

        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;

        return true;
    }


/***
    static
    boolean Eq (boolean[] a, boolean[] b)
    {
        if (a == null && b == null)
            return true;

        if (a == null || b == null)
            return false;

        if (a.length != b.length)
            return false;

        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i])
                return false;

        return true;
    }
***/


    static 
    String Rename (String s, String olds, String news)
    {
        if (UsesType(s, olds))
            return (news + s.substring(olds.length())).intern();
        else
            return s;
    }


    static 
    String[] Rename (String[] s, String olds, String news)
    {
        if (s == null)
            return null;

        String[] r = null;

        for (int i = 0; i < s.length; i++)
            if (UsesType(s[i], olds))
            {
                if (r == null)
                    r = (String[])s.clone();

                r[i] = (news + s[i].substring(olds.length())).intern();
            }

        return r == null ? s : r;
    }


    static 
    boolean UsesType (String[] s, String olds)
    {
        if (s == null)
            return false;

        for (int i = 0; i < s.length; i++)
            if (UsesType(s[i], olds))
                return true;

        return false;
    }


    static 
    boolean UsesType (String news, String olds)
    {
        if (news == null)
            return false;

        if (news.startsWith(olds))
        {
            int i = olds.length();
            if (news.length() > i)
            {
                char c = news.charAt(i);
                return c == '.' || c == '[';
            }
            else
                return true;
        }
        else
            return false;
    }


    String ShowModifier ()
    {
        String s = XModifier.toString(modifier);
        return s.length() == 0 ? "" : s+" ";
    }


    static 
    String Show (String t, String[] a)
    {
        return a == null || a.length == 0 ? "" : t+List(a);
    }


    static 
    String List (String[] a)
    {
        String s = "";

        if (a != null)
            for (int i = 0; i < a.length; i++)
            {
                if (s.length() != 0)
                    s += ",";
                s += a[i];
            }
        
        return s;
    }
}



class UnitPack extends Unit
{
    boolean NameEq (Object x)
    {
        return super.NameEq(x) && x instanceof UnitPack;
    }


    void Rename (String olds, String news)
    {
        topname = Rename(topname, olds, news);
    }


    boolean UsesType (String s)
    {
        return UsesType(topname, s);
    }


    String Descr ()
    {
        return "PACK  "+toString();
    }


    public
    boolean equals (Object x)
    {
        return NameEq(x);
    }
}



class UnitClass extends Unit
{
    boolean  defined = false;
    String   extend;
    String[] implement;


    boolean NameEq (Object x)
    {
        return super.NameEq(x) && x instanceof UnitClass;
    }


    void Rename (String olds, String news)
    {
        topname   = Rename(topname,   olds, news);
        extend    = Rename(extend,    olds, news);
        implement = Rename(implement, olds, news);
    }


    boolean UsesType (String s)
    {
        return UsesType(topname, s) 
            || (extend == null ? false : UsesType(extend, s))
            || UsesType(implement, s);
    }


    String Descr ()
    {
        return "CLASS "+ShowModifier()+toString()+
               (extend == null ? "" : " extends "+extend.toString())+
               Show(" implements ", implement);
    }


/***
    public
    Object clone ()
    {
        UnitClass w = (UnitClass)super.clone();
        if (implement != null)
            w.implement = (String[])implement.clone();
        return w;
    }
***/


    public
    boolean equals (Object x)
    {
        if (NameEq(x))
        {
            UnitClass w = (UnitClass)x;

            return w.modifier == modifier 
                && w.extend   == extend
                && EqX(w.implement, implement);
        }
        return false;
    }
}



abstract
class UnitFunction extends Unit
{
    String[]  args;
    String[]  xthrows;


    boolean NameEq (Object x)
    {
        return super.NameEq(x) && x instanceof UnitFunction 
            && Eq(((UnitFunction)x).args, args);
    }


    void Rename (String olds, String news)
    {
        topname = Rename(topname, olds, news);
        args    = Rename(args,    olds, news);
        xthrows = Rename(xthrows, olds, news);
    }


    boolean UsesType (String s)
    {
        return UsesType(topname, s) 
            || UsesType(args, s)
            || UsesType(xthrows, s);
    }


    public 
    String toString ()
    {
        return topname+name+"("+List(args)+")";
    }


    public
    boolean equals (Object x)
    {
        if (NameEq(x))
        {
            UnitFunction w = (UnitFunction)x;
            return w.modifier == modifier
                && EqX(w.xthrows, xthrows);
        }
        return false;
    }
}



class UnitConstructor extends UnitFunction
{
    void Rename (String olds, String news)
    {
        super.Rename(olds, news);
        int l = topname.length();
        int i = topname.lastIndexOf('.', l-2);
        name  = topname.substring(i+1, l-1).intern();
    }


    String Descr ()
    {
        return "CTOR  "+ShowModifier()+toString()+
               Show(" throws ", xthrows);
    }


/***
    public
    Object clone ()
    {
        UnitConstructor w = (UnitConstructor)super.clone();
        if (args != null)
            w.args    = (String[])args.clone();
        if (xthrows != null)
            w.xthrows = (String[])xthrows.clone();
        return w;
    }
***/


    public
    boolean equals (Object x)
    {
        return super.equals(x) && x instanceof UnitConstructor;
    }
}



class UnitMethod extends UnitFunction
{
    String    type;


    void Rename (String olds, String news)
    {
        super.Rename(olds, news);
        type = Rename(type, olds, news);
    }


    boolean UsesType (String s)
    {
        return super.UsesType(s) 
            || UsesType(type, s);
    }


    String Descr ()
    {
        return "METHD "+ShowModifier()+type+" "+toString()+
               Show(" throws ", xthrows);
    }


/***
    public
    Object clone ()
    {
        UnitMethod w = (UnitMethod)super.clone();
        if (args != null)
            w.args    = (String[])args.clone();
        if (xthrows != null)
            w.xthrows = (String[])xthrows.clone();
        return w;
    }
***/
 

    public
    boolean equals (Object x)
    {
        if (super.equals(x) && x instanceof UnitMethod)
        {
            return ((UnitMethod)x).type == type;
        }
        return false;
    }
}



class UnitField extends Unit
{
    String   type;
    Object   value;


    boolean NameEq (Object x)
    {
        return super.NameEq(x) && x instanceof UnitField;
    }


    void Rename (String olds, String news)
    {
        topname = Rename(topname, olds, news);
        type    = Rename(type,    olds, news);
    }


    boolean UsesType (String s)
    {
        return UsesType(topname, s) 
            || UsesType(type, s);
    }


    String Descr ()
    {
        return "FIELD "+ShowModifier()+type+" "+toString();
    }


    public
    boolean equals (Object x)
    {
        if (NameEq(x))
        {
            UnitField w = (UnitField)x;
            return w.modifier == modifier
                && w.type == type;
        }
        return false;
    }


/***
    public
    Object clone ()
    {
        UnitField w = (UnitField)super.clone();
        if (value != null)
            w.value = value.clone();
        return w;
    }
***/
}



class Units extends Vector
{
    void FindAll (Unit u, Units uu)
    {
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            Unit w = (Unit)e.nextElement();
            if (w.equals(u))
                uu.add(w);
        }
    }


    Unit Find (Unit u)
    {
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            Unit w = (Unit)e.nextElement();
            if (w.equals(u))
                return w;
        }

        return null;
    }


    boolean IsEmpty ()
    {
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            Unit u = (Unit)e.nextElement();
            if (u instanceof UnitClass)
                return false;
        }

        return true;
    }


    void AddPath ()
    { 
        HashSet tops  = new HashSet(),
                fulls = new HashSet();;

        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            Unit u = (Unit)e.nextElement();
            fulls.add(u.topname+u.name);

            Path path = new Path(u.topname);
            String s = "";
            for (int i = 0; i < path.size(); i++)
            {
                if (i != 0) 
                    s+= '.';
                s += (String)path.elementAt(i);
                tops.add(s);
            }
        }

        for (Iterator i = tops.iterator(); i.hasNext();)
        {
            String s = (String)i.next();
            if (!fulls.contains(s))
            {
                //System.err.println("new pack: "+s);
                UnitPack u = new UnitPack();

                int k = s.lastIndexOf('.');
                if (k == 0)
                    u.Name(null, s);
                else
                    u.Name(s.substring(0, k+1), s.substring(k+1));

                super.addElement(u);
            }
        }
    }


    void Print ()
    {
        System.out.println("Units:");
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            Unit u = (Unit)e.nextElement();
            System.out.println(u.hashCode()+") "+u.Descr());
        }
        System.out.println("end of units");
    }
}



class UnitsWalk
{
    Units units;


    void Walk (Units uu)
    {
        units = uu;

        for (Enumeration e = units.elements(); e.hasMoreElements();)
            Walk((Unit)e.nextElement());

        units = null;
    }


    void Walk (Unit u)
    {
        if (u instanceof UnitPack)
            Walk((UnitPack)u);

        else if (u instanceof UnitClass)
            Walk((UnitClass)u);

        else if (u instanceof UnitConstructor)
            Walk((UnitConstructor)u);

        else if (u instanceof UnitMethod)
            Walk( (UnitMethod)u);

        else if (u instanceof UnitField)
            Walk((UnitField)u);
    }


    void Walk (UnitPack u)          {}

    void Walk (UnitClass u)         {}

    void Walk (UnitConstructor u)   {}

    void Walk (UnitMethod u)        {}

    void Walk (UnitField u)         {}
}



