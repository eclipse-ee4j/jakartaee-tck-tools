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
 *      This module contains main structure definitions for internal representation
 *      of the packages, classes and class members.
 */


package javasoft.sqe.apiCheck;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;



//--- Modifiers for classes, constructors, methods and fields


class XModifier
{
    static String[] words =
    {
        "public",
        "private",
        "protected",
        "static",
        "final",
        "synchronized",
        "volatile",
        "transient",
        "native",
        "interface",
        "abstract",
        "strictfp"
    };


    static final
    int
        xpublic      = 0x001,
        xprivate     = 0x002,
        xprotected   = 0x004,
        xstatic      = 0x008,
        xfinal       = 0x010,
        xsynchronized= 0x020,
        xvolatile    = 0x040,
        xtransient   = 0x080,
        xnative      = 0x100,
        xinterface   = 0x200,
        xabstract    = 0x400,
        xstrictfp    = 0x800;


    // bits of access_flags item : allowed in class files
    static final
    int
        flagclass  = xpublic    |
                                  xfinal    | xinterface | xabstract,

        flagfield  = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    |
                     xvolatile  | xtransient,

        flagmethod = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    |              xabstract | xstrictfp  |
                     xnative    | xsynchronized,

        flaginner  = xpublic    | xprivate  | xprotected |
                     xstatic    | xfinal    | xinterface | xabstract;


    static
    int Convert (String s)
    {
        for (int i = 0, m = 1; i < words.length; i++, m <<= 1)
            if (words[i].equals(s))
                return m;

        return 0;
    }


    static
    int ConvertNew (String s)
    {
        int m = Convert(s);

        if (m != 0)
            return m;
        else
        {
            //System.err.println("new modifier: "+s);
            int l = words.length;
            String[] w = new String[l+1];
            System.arraycopy(words, 0, w, 0, l);
            w[l] = s;
            words = w;
            return (1 << l);
        }
    }


    static
    int Access (int m)
    {
        return (m & XModifier.xpublic) != 0 
                        ? 3
                        :(m & XModifier.xprotected) != 0 
                                ? 2
                                :(m & XModifier.xprivate) != 0 
                                        ? 0
                                        : 1;
    }


    static
    boolean Is (int mask, int m)
    {
        return (mask & m) != 0;
    }


    static
    boolean IsAnd (int mask, int m)
    {
        return (mask & m) == m;
    }


    static
    String toString (int mask)
    {
        String s = "";

        for (int i = 0, m = 1; i < words.length; i++, m <<= 1)
            if ((mask & m) != 0)
            {
                if (s.length() != 0) s += " ";
                s += words[i];
            }

        return s;
    }
}


//--- Type structure


abstract
class XType
{
    public
    boolean equals (Object o)
    {
        return o instanceof XType && o.toString().equals(toString());
    }
}



class XTypePrimitive extends XType
{
	String name;
    char chr;


    XTypePrimitive (String s, char c)    {name = s; chr = c;}


    public
	String toString ()					 {return name;}
}



class XTypeClass extends XType
{
    XClass ref;


    XTypeClass (XClass r)                {ref = r;}


    public
	String toString ()					 {return ref.FullName();}
}



class XTypeArray extends XType
{
    XType ref;
    int   dims;


    XTypeArray (XType r, int d) throws Fatal
    {
        if (r instanceof XTypeArray) Fatal.Stop("XTypeArray");
        ref = r; dims = d;
    }


    public
	String toString ()
	{
		String name = ref.toString();
		for (int i = 0; i < dims; i++)
			name += "[]";
		return name;
	}
}



class XTypes extends Vector
{
    XTypes ()                           {}
    XTypes (int n)                      {super(n);}


    void Add (XType t)                  {addElement(t);}


    public
    boolean equals (Object o)
    {
		if (!(o instanceof XTypes))
			return false;

		XTypes x = (XTypes)o;

        if (size() != x.size())
            return false;

        for (int i = 0; i < size(); i++)
            if (!((XType)elementAt(i)).equals((XType)x.elementAt(i)))
                return false;

        return true;
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


//--- Packages tree structures


abstract
class XNode extends NamedObject
{
    String fname;       // source file name (if exist)
    int    linenb;      // source line number

    abstract
    String FullName ();
}



class XPack extends XNode
{
    XPack top;
    NamedList /*XPack*/  packs   = new NamedList();
    NamedList /*XClass*/ classes = new NamedList();


    void Link (XPack p)
    {
        top = p;
        top.packs.Add(this);
    }


	String FullName ()
	{
		return name == null ? ""
							: top == null || top.name == null ? name
															  : top.FullName() + "." + name;
	}
}


//--- Class structure


abstract
class XClassMember extends XNode
{
	XClass home;
    int modifier;


	String FullName ()
	{
		return home.FullName() + "." + name;
	}


    abstract
    String KindName ();


    abstract
    void Link (XClass home);


    boolean Visible ()
    {
        return (modifier & (XModifier.xpublic | XModifier.xprotected)) != 0 &&
               (home == null ? true : home.Visible());
    }
}



class XClass extends XClassMember
{
    boolean   defined = false;
    XPack     pack;
    XClass    extend;
    NamedList implement    = new NamedList();   // of XClass
//  class members
    NamedList constructors = new NamedList();   // of XClassConstructor
    NamedList methods      = new NamedList();   // of XClassMethod
    NamedList fields       = new NamedList();   // of XClassField
    NamedList inners       = new NamedList();   // of XClass


	void Link (XPack p)
    {
        pack = p;
        pack.classes.Add(this);
    }


    void Link (XClass c)
    {
        home = c;
        pack = home.pack;
        home.inners.Add(this);
    }


	String FullName ()
	{
        String s;
        if (home == null)
        {
            s = pack.FullName();
            if (s != null && s.length() != 0)
                s += '.';
        }
        else
            s = home.FullName() + '.'; //'$';

        return s + name;
	}


    String KindName ()
    {
        return "class \"" + toString() + "\"";
    }


	boolean IsInterface ()
	{
		return (modifier & XModifier.xinterface) != 0;
	}
}


abstract
class XClassFunction extends XClassMember
{
    XTypes args;
    XTypes xthrows;


    public
    String toString ()
    {
        return name+"("+args.toString()+")";
    }
}


class XClassConstructor extends XClassFunction
{
    void Link (XClass h)
    {
        home = h;
        home.constructors.Add(this);
    }


    String KindName ()
    {
        return "constructor \"" + toString() + "\"";
    }
}



class XClassMethod extends XClassFunction
{
    XType  type;


    void Link (XClass h)
    {
        home = h;
        home.methods.Add(this);
    }


    String KindName ()
    {
        return "method \"" + toString() + "\"";
    }
}



class XClassField extends XClassMember
{
    XType  type;
    Object value;


	void Link (XClass h)
    {
        home = h;
        home.fields.Add(this);
    }


    String KindName ()
    {
        return "field \"" + toString() + "\"";
    }
}


//--- Program structure


class XProg
{
    XPack packs = new XPack();

    HashMap classhash = new HashMap();

    Vector typesclass = new Vector(),
           typesarray = new Vector();


    static XTypePrimitive[] typesprimitive =
    {
        new XTypePrimitive("void",    'V'),
        new XTypePrimitive("boolean", 'Z'),
        new XTypePrimitive("byte",    'B'),
        new XTypePrimitive("short",   'S'),
        new XTypePrimitive("int",     'I'),
        new XTypePrimitive("long",    'J'),
        new XTypePrimitive("char",    'C'),
        new XTypePrimitive("float",   'F'),
        new XTypePrimitive("double",  'D')
    };


    XType DefineVMType (StringBuffer s)
    {
        int dims = 0;

        while (s.charAt(0) == '[')
        {
            dims++;
            s.deleteCharAt(0);
        }

        XType ref = null;

        char chr = s.charAt(0);
        s.deleteCharAt(0);

        if (chr == 'L')
        {
            int k = 0;

            while (k < s.length() && s.charAt(k) != ';')
                k++;

            if (k > 0)
            {
                ref = DefineType(s.substring(0, k).replace('/', '.'));
                s.delete(0, k+1);
            }
        }
        else
        {
            ref = DefineTypePrimitive(chr);
        }

        if (ref == null)
            Fatal.Stop("invalid VM type "+s);

        if (dims == 0)
            return ref;
        else
            return DefineTypeArray(ref, dims);
    }


    XTypes DefineTypes (String[] ss)
    {
        XTypes tt;

        if (ss != null)
        {
            tt = new XTypes(ss.length);
            for (int i = 0; i < ss.length; i++)
                tt.Add(DefineType(ss[i]));
        }
        else
            tt = new XTypes(0);

        return tt;
    }


    XType DefineType (String s)
    {

//      First, check for array types.

        int dims;

        // VM-like array [ ...

        if (s.startsWith("["))
        {
            for (dims = 0; s.charAt(dims) == '[';)
                dims++;

            char  chr = s.charAt(dims);
            XType ref = null;

            if (chr == 'L')
                ref = DefineType(s.substring(dims+1, s.length()-1));
            else
                ref = DefineTypePrimitive(chr);

            return DefineTypeArray(ref, dims);
        }

        // Java-like array T []

        for (dims = 0; s.endsWith("[]");)
        {
            s = s.substring(0, s.length()-2);
            dims++;
        }

        if (dims > 0)
            return DefineTypeArray(DefineType(s), dims);

//      Second, check for primitive types.

        XTypePrimitive tp = DefineTypePrimitive(s);
        if (tp != null)
            return tp;

//      Must be class reference.

        XTypeClass tc;
        XClass r = DefineClass(s);

        for (Enumeration e = typesclass.elements(); e.hasMoreElements();)
        {
            tc = (XTypeClass)e.nextElement();
            if (tc.ref == r)
                return tc;
        }

        tc = new XTypeClass(r);
        typesclass.addElement(tc);
        return tc;
    }


    static
    XTypePrimitive DefineTypePrimitive (char c)
    {
        for (int i = 0; i < typesprimitive.length; i++)
        {
            XTypePrimitive t = typesprimitive[i];
            if (t.chr == c)
                return t;
        }

        return null;
    }


    static
    XTypePrimitive DefineTypePrimitive (String s)
    {
        for (int i = 0; i < typesprimitive.length; i++)
        {
            XTypePrimitive t = typesprimitive[i];
            if (t.name.equals(s))
                return t;
        }

        return null;
    }


    XTypeArray DefineTypeArray (XType ref, int dims)
    {
        if (ref == null) Fatal.Stop("DefineTypeArray");

        XTypeArray t = null;

        for (Enumeration e = typesarray.elements(); e.hasMoreElements();)
        {
            t = (XTypeArray)e.nextElement();
            if (t.ref == ref && t.dims == dims)
                return t;
        }

        t = new XTypeArray(ref, dims);
        typesarray.addElement(t);
        return t;
    }


    XPack DefinePack (Vector/*String*/ pnames, boolean create)
    {
        XPack p = packs;

        for (int i = 0; i < pnames.size(); i++)
        {
            String s = (String)pnames.elementAt(i);
            XPack  q = (XPack)p.packs.FindFirst(s);
            if (q == null)
            {
                if (!create)
                    Fatal.Stop("Package not found: "+s);
                q = new XPack();
                q.name = s;
                q.Link(p);
            }
            p = q;
        }

        //System.out.println("-DefinePack: "+p.FullName());
        return p;
    }


    XClass DefineClass (String qname)
    {
        XClass c = (XClass)classhash.get(qname);
        if (c != null)
            return c;

        //System.err.println("-DefineClass: "+qname);

        String err = "Invalid class name: \""+qname+"\"";
        String n = null;
        XPack  p = packs;

    //  name ".C" is a special case
        if (qname.charAt(0) == '.')
            qname = qname.substring(1);

        for (StringTokenizer st = new StringTokenizer(qname+"=", ".$=", true);
             st.hasMoreTokens();)
        {
            String s = (String)st.nextToken();

            if (s.equals("."))
            {
                if (n == null) Fatal.Stop(err);
                if (c == null)
                {// previous node was not a class, so "n" may be class or package name
                    XClass cc = (XClass)p.classes.FindFirst(n);
                    if (cc != null)
                    {// found class
                        c = cc;
                        p = null;
                    }
                    else
                    {
                        XPack pp = (XPack)p.packs.FindFirst(n);
                        if (pp == null)
                        {// assume "n" is a package
                            pp = new XPack();
                            pp.name = n;
                            pp.Link(p);
                        }
                        p = pp;
                    }
                }
                else
                {// previous node was a class, so "n" have to be a class name
                    XClass cc = (XClass)c.inners.FindFirst(n);
                    if (cc == null)
                    {
                        cc = new XClass();
                        cc.name = n;
                        cc.Link(c);
                    }
                    c = cc;
                    p = null;
                }
                n = null;
            }

            else if (s.equals("$") || s.equals("="))
            {// "n" must be a class name
                if (n == null) Fatal.Stop(err);
                XClass cc;
                if (c == null)
                {
                    cc = (XClass)p.classes.FindFirst(n);
                    if (cc == null)
                    {
                        if (p.packs.FindFirst(n) != null)
                            System.err.println("package/class name clash "+n);

                        cc = new XClass();
                        cc.name = n;
                        cc.Link(p);
                    }
                }
                else
                {
                    cc = (XClass)c.inners.FindFirst(n);
                    if (cc == null)
                    {
                        cc = new XClass();
                        cc.name = n;
                        cc.Link(c);
                    }
                }
                c = cc;
                n = null;
            }

            else
            {// just save the name for later
                if (n != null) Fatal.Stop(err);
                n = s;
            }
        }

        if (c == null || n != null) Fatal.Stop(err);

        classhash.put(qname, c);
        return c;
    }


//
//  Sort package, classes and members by name
//


    static
    class Sorter extends XProgWalk
    {
        ObjComparator oc = new ObjComparator();

        void Walk (XPack x)
        {
            Collections.sort(x.packs, oc);
            Collections.sort(x.classes, oc);
            super.Walk(x);
        }


        void Walk (XClass x)
        {
            Collections.sort(x.constructors, oc);
            Collections.sort(x.methods, oc);
            Collections.sort(x.fields, oc);
            Collections.sort(x.inners, oc);
            super.Walk(x);
        }
    }


    static
    class ObjComparator implements Comparator
    {
        public
        int compare (Object o1, Object o2)
        {
            return o1.toString().compareTo(o2.toString());
        }
    }


    void Sort ()
    {
        Sorter sort = new Sorter();
        sort.Walk(this, null);
    }


//
//  Remove extra interface declarations from implementsa clause
//


/***
    static
    class IfsCleaner extends XProgWalk
    {
        HashSet supifs = new HashSet();


        void AddIfs (XClass x)
        {
            if (x.extend != null)
                AddIfs(x.extend);

            for (Enumeration e = x.implement.elements(); e.hasMoreElements();)
            {
                XClass y = (XClass)e.nextElement();
                supifs.add(y.FullName());
                AddIfs(y);
            }
        }

        void Walk (XClass x)
        {
            if (x.extend != null)
                AddIfs(x.extend);

            for (Enumeration e = x.implement.elements(); e.hasMoreElements();)
                AddIfs((XClass)e.nextElement());

            for (int i = x.implement.size(); --i >= 0;)
                if (supifs.contains(((XClass)x.implement.elementAt(i)).FullName()))
                    x.implement.removeElementAt(i);

            supifs.clear();

            super.Walk(x);
        }
    }


    void IfsClear ()
    {
        IfsCleaner clr = new IfsCleaner();
        clr.Walk(this, null);
    }
***/


//
//  Check if there are any class definitions
//


    boolean IsEmpty ()
    {
        return IsEmpty(packs);
    }



    static
    boolean IsEmpty (XPack xpack)
    {
        for (Enumeration e = xpack.classes.elements(); e.hasMoreElements();)
            if (((XClass)e.nextElement()).defined)
                return false;

        for (Enumeration e = xpack.packs.elements(); e.hasMoreElements();)
            if (!IsEmpty((XPack)e.nextElement()))
                return false;

        return true;
    }
}



class XProgWalk
{
    XProg prog;
    PackageSet pset;


    void Walk (XProg p, PackageSet s)
    {
        prog = p;
        pset = s;

        Walk(prog.packs);

        pset = null;
        prog = null;
    }


    void Walk (XPack x)
    {
        if (pset == null || pset.InPath(x.FullName()))
        {
            for (Enumeration e = x.classes.elements(); e.hasMoreElements();)
                Walk((XClass)e.nextElement());

            for (Enumeration e = x.packs.elements(); e.hasMoreElements();)
                Walk((XPack)e.nextElement());
        }
    }


    void Walk (XClass x)
    {
        for (Enumeration e = x.constructors.elements(); e.hasMoreElements();)
            Walk((XClassConstructor)e.nextElement());

        for (Enumeration e = x.methods.elements(); e.hasMoreElements();)
            Walk((XClassMethod)e.nextElement());

        for (Enumeration e = x.fields.elements(); e.hasMoreElements();)
            Walk((XClassField)e.nextElement());

        for (Enumeration e = x.inners.elements(); e.hasMoreElements();)
            Walk((XClass)e.nextElement());
    }


    void Walk (XClassConstructor x)     {}

    void Walk (XClassMethod x)          {}

    void Walk (XClassField x)           {}
}



