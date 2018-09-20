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
 *      This module contains various useful classes.
 */


package javasoft.sqe.apiCheck;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;



class Fatal extends Error // Exception
{
    Fatal (String msg)                                  {super(msg); }


    static
    void Stop (String msg) throws Fatal                 {throw new Fatal(msg);}
}



class NamedObject implements Comparable
{
    String name;


    public
    int compareTo (Object o)
    {
        return name.compareTo(((NamedObject)o).name);
    }


    public
    String toString ()                                  {return name == null ? "" : name;}
}



class NamedList extends Vector
{
    void Add (NamedObject o)                            {super.addElement(o);}


    void AddFirst (NamedObject o) throws Fatal
    {
        if (FindFirst(o.name) != null)
            Fatal.Stop("NamedList duplicate <"+o.name+">");

        super.addElement(o);
    }


    NamedObject FindFirst (String n)
    {
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            NamedObject o = (NamedObject)e.nextElement();
            if (n.equals(o.name))
                return o;
        }

        return null;
    }


    public
    String toString ()
    {
        String s = "";
        for (Enumeration e = super.elements(); e.hasMoreElements();)
        {
            if (s.length() != 0) s += ",";
            s += e.nextElement().toString();
        }
        return s;
    }
}



class VectorString extends Vector
{
	Enumeration e = null;


	String First ()
	{
		e = super.elements();
		return Next();
	}


	String Next ()
	{
		if (e != null && e.hasMoreElements())
			return (String)e.nextElement();

		e = null;
		return null;
	}
}



class Path extends Vector //VectorString
{
    Path (String s)
    {
        for (StringTokenizer st = new StringTokenizer(s, ".", false); st.hasMoreElements();)
            super.addElement(st.nextToken());
    }
}



class Arguments extends Properties
{
    boolean Parse (String[] args, String[] pars)
    {
        errs = 0;

        for (int i = 0; i < args.length; i++)
        {
            String a = args[i];
            if (!a.startsWith("-"))
                Err("invalid option \""+a+"\"");
            else
            {
                a = a.substring(1);

                for (int k = 0; k < pars.length; k++)
                {
                    String p = pars[k];
                    int    l = pars[k].length();
                    String flags = "";
                    while (l > 2 && p.charAt(l-2) == '$')
                    {
                        flags += p.charAt(l-1);
                        l -= 2;
                    }

                    String arg = p.substring(0, l);

                    String val = "";
                    if (a.length() == l && p.regionMatches(0, a, 0, l))
                    {
                        if (flags.indexOf('s') != -1)
                        {// option must be followed by value
                            if (i+1 == args.length || args[i+1].startsWith("-"))
                                Err("missing value for option \""+a+"\"");
                            else
                                val = args[++i];
                        }

                        if (flags.indexOf('m') == -1)
                        {// only one option allowed
                            if (super.containsKey(a))
                                Err("duplicate option \""+a+"\"");
                        }
                        else
                        {// multiple options allowed
                            String tmp = super.getProperty(arg);
                            if (tmp != null)
                                val = tmp + ' ' + val;
                        }

                        super.put(arg, val);

                        a = null;
                        break;
                    }
                }

                if (a != null)
                    Err("unknown option \""+a+"\"");
            }
        }
        return errs == 0;
    }


    private int errs;

    private void Err (String s)
    {
        System.err.println(s);
        errs++;
    }
}



class Props implements Cloneable
{
    Vector /*String*/ keys   = new Vector();
    Vector /*String*/ values = new Vector();


    void Add (Props other)
    {
        for (int i = 0; i < other.keys.size(); i++)
        {
            keys.addElement(other.keys.elementAt(i));
            values.addElement(other.values.elementAt(i));
        }
    }


    void Add (String k, String v)
    {
        keys.addElement(k.intern());
        values.addElement(v.intern());
    }


    String Find (String k)
    {
        String s = k.intern();

        for (int i = 0; i < keys.size(); i++)
            if (keys.elementAt(i) == s)
                return (String)values.elementAt(i);

        return null;
    }


    int Size ()
    {
        return keys.size();
    }


    String Key (int i)
    {
        return (String)keys.elementAt(i);
    }


    String Value (int i)
    {
        return (String)values.elementAt(i);
    }


    public
    Object clone ()
    {
        Props x = new Props();
        x.keys   = (Vector)keys.clone();
        x.values = (Vector)values.clone();
        return x;
    }
}



class Main      //  static members only
{
//  Program arguments

    static
    Arguments args = new Arguments();


    static
    boolean IsDebugMode ()
    {
        return args.getProperty("debug") != null;
    }


//  Execution time check

    static
    long GetTimer ()
    {
        return System.currentTimeMillis();
    }


    static
    void PrintTimer (String msg, long t)
    {
        if (args.getProperty("time") != null)
            System.err.println(msg+" in "+(System.currentTimeMillis() - t)+" ms");
    }


//  Free memory check

    static Runtime rt = Runtime.getRuntime();

    static long free0  = rt.freeMemory(),
                total0 = rt.totalMemory();

    static long free  = free0,
                total = total0;


    static
    void MemSnap ()
    {
        long x = rt.freeMemory();
        if (x < free)
            free = x;
    }


    static
    void MemReport ()
    {
        MemSnap();
        System.err.println("Initial free="+free0);
        System.err.println("Minimum free="+free+" maximum used="+(free0-free));
    }


//  Prints string in a hex format

    static
    String Hex (String s)
    {
        String w = "";
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            w += c;
            int x = (int)c;
            w += "<"+Integer.toHexString(x)+"> ";
        }
        return w;
    }		
}


