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
 *      This module compares to sets of classes (base and test), represented by
 *      XProg structures.
 */


package javasoft.sqe.apiCheck;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import com.sun.ts.lib.util.SigLogIntf;

class TestBase extends XProgComp
{
//  Parameters of comparison
    //    PrintWriter out;
    SigLogIntf out;
    String  check,      // "bincomp"/"maint"
            mode;       // "public"/"protected"/"package"/"private"
    boolean sort,       // sort the report
            values,     // check the constant values
            verbose;    // verbose diagnostic

    Vector /*Msg*/ msgsort;
	int errs;

    static final 
    int 
        CHANGE_COMP  = 1 << 0,
        CHANGE_SUB   = 1 << 1,
        CHANGE_ADD   = 1 << 2,
        CHANGE_OTHER = 1 << 3;

    static final 
    int accmods = XModifier.xpublic | XModifier.xprotected | XModifier.xprivate;



    static
    class Msg implements Comparable
    {
        int type;

        String context,
               kind,
               text;


        public int compareTo (Object o)
        {
            Msg m = (Msg)o;
            return (kind+context+text).compareTo(m.kind+m.context+m.text);
        }
    }


    int Compare (PackageSet ps, API base, API test)
    {
        long t0 = Main.GetTimer();

        OutProps("BASE", base.props);
        OutProps("TEST", test.props);

        out.println("CHECK " + check);

        msgsort = sort ? new Vector() : null;
		
		errs = 0;
        PushHead("");
		
        base.GetXProg().Sort();
        Compare(ps, base.GetXProg(), test.GetXProg(), mode);

        PopHead();

        if (sort)
            ReportSorted();

        msgsort = null;
        out.flush();
        Main.PrintTimer("Compare", t0);
        return errs;
    }


    void OutProps (String msg, Props props)
    {
        if (props == null)
            return;

        out.println(msg);

        for (int i = 0; i < props.Size(); i++)
            out.println("//"+props.Key(i)+": "+props.Value(i));

        out.println();
    }


//  Package


    void Compare (XPack base, XPack test)
    {
        PopHead();
        PushHead("package \""+base.FullName()+"\"");

        super.Compare(base, test);
    }


    void Deleted (XPack xpack)
    {
        PopHead();
        PushHead("package \""+xpack.FullName()+"\"");

        super.Deleted(xpack);
    }


    void Added (XPack xpack)
    {
        PopHead();
        PushHead("package \""+xpack.FullName()+"\"");

        super.Added(xpack);
    }


//


    static
    boolean CompareClasses (NamedList /*XClass*/ base, NamedList /*XClass*/ test)
    {
        if (base.size() != test.size())
            return false;

        Vector temp = (Vector)test.clone();

        for (int i = 0; i < base.size(); i++)
        {
            String name = ((XClass)base.elementAt(i)).FullName();

            for (int k = 0; k < temp.size(); k++)
                if (name.equals(((XClass)temp.elementAt(k)).FullName()))
                {
                    temp.removeElementAt(k);
                    name = null;
                    break;
                }

            if (name != null)
                return false;
        }

        return temp.size() == 0;
    }


    static
    boolean CompareTypes (XTypes base, XTypes test)
    {
        if (base.size() != test.size())
            return false;

        Vector temp = (Vector)test.clone();

        for (int i = 0; i < base.size(); i++)
        {
            XType t = (XType)base.elementAt(i);

            for (int k = 0; k < temp.size(); k++)
                if (t.equals(temp.elementAt(k)))
                {
                    temp.removeElementAt(k);
                    t = null;
                    break;
                }

            if (t != null)
                return false;
        }

        return temp.size() == 0;
    }


    static
    String ClassesToString (NamedList tt)
    {
        String s = "";

        for (Enumeration e = tt.elements(); e.hasMoreElements();)
        {
            if (s.length() != 0) s += ',';
            s += ((XClass)e.nextElement()).FullName();
        }

        return s;
    }


    void TestBits (String pref, int mb, int mt, int type)
    {
        for (int mx = mb ^ mt, m = 1; mx != 0;  mx >>= 1, m <<= 1)
        {
            final int bb = mb & m,
                      bt = mt & m;

            if (bb != bt)
                if (bb != 0)         
                    Report(type,  
                           "modifier deleted:", 
                           pref + "\"" + XModifier.toString(bb) + "\"");
                else            
                    Report(type,  
                           "modifier added:", 
                           pref + "\"" + XModifier.toString(bt) + "\"");
        }
    }

/*
    boolean Visible (XClassMember b, XClassMember t)
    {
        return XModifier.Access(b.modifier) > 1 && XModifier.Access(t.modifier) > 1 && Visible();
    }


    boolean Visible (XClassMember x)
    {
        return XModifier.Access(x.modifier) > 1 && Visible();
    }
*/



//  Reports


    static final int hmax = 32;
    String[]   htexts = new String[hmax];
    boolean[]  hflags = new boolean[hmax];
    String htext  = "";
    int    htos = 0,
        htosold = 0;


    void PushHead (String hd)
    {
        if (htos == hmax) 
            Fatal.Stop("Heads stack overflow");

        htexts[htos] = new String(hd);
        hflags[htos] = true;
        htos++;
    }


    void PopHead ()
    {
        //if (htos == 0) Fatal.Stop();
        htexts[htos] = null;
        htos--;
    }


    void ReportSorted ()
    {
        Collections.sort(msgsort);

        String kind = "",
               context = "";

        for (int i = 0; i < msgsort.size(); i++)
        {
            Msg m = (Msg)msgsort.elementAt(i);
            if (!m.kind.equals(kind))
            {
                kind = m.kind;
                out.println("\n"+kind);
                context = "";
            }
            if (!m.context.equals(context))
            {
                context = m.context;
                out.println("\t"+context);
            }

            out.print(String.valueOf(TypeChar(m.type)));
            out.println("\t\t"+ m.text);
        }
    }
        

    void Report (int type, String kind, String text)
    {
        if (msgsort == null)
        {
            for (int i = 0; i < htos; i++)
                if (hflags[i])
                {
                    hflags[i] = false;
                    out.println();
                    out.print(Indent(i));
                    out.println(htexts[i]);
                    htosold = htos;
                }

            if (htos < htosold)
            {
                out.println();
                htosold = htos;
            }

            out.print(String.valueOf(TypeChar(type)));
            out.print(Indent(htos));
            out.print(kind + ' ' + text);
            out.println();
        }
        else
        {
            Msg m = new Msg();
            m.context = (String)htexts[htos-1];
            m.type = type;
            m.kind = kind;
            m.text = text;
            msgsort.addElement(m);
        }

		errs++;
    }


    static
    char TypeChar (int t)
    {
        switch (t)
        {
            case 0:
                return ' ';

            case CHANGE_COMP:
                return ' ';

            case CHANGE_SUB:
                return '-';

            case CHANGE_ADD:
                return '+';

            case CHANGE_OTHER:
                return '#';
        }

        return '?';
    }


    static
    String Indent (int n)
    {
        String s = "";

        for (int i = n; i > 0; i--)
            s += "\t";

        return s;
    }



//  Utility class for Upd


    static
    class Context
    {
        String defpack  = "",
               defclass = "";


        Context ()
        {
            defpack = defclass = "";
        }


        Context (XClassMember x)
        {
            defpack = x instanceof XClass ? ((XClass)x).pack.FullName()
                                          : x.home.pack.FullName();

            if (x instanceof XClass)
            {
                XClass w = (XClass)x;
                defpack  = w.pack.FullName();
                defclass = w.home == null ? "" : w.home.FullName();
            }
            else
            {
                defpack  = x.home.pack.FullName();
                defclass = x.home.FullName();
            }

            int i = defpack.length();
            if (i != 0 && i < defclass.length())
                defclass = defclass.substring(i+1);
        }


        boolean Empty ()
        {
            return defpack.length() == 0 && defpack.length() == 0;
        }
    }


//  Update Generator


    class Upd
    {
        PrintWriter out;

        final static
        int CHANGE = 0,
            ADD    = 1,
            DELETE = 2;

        int curop = -1;                 // last operation (CHANGE/ADD/DELETE)

        Context cur = new Context();    // current context


        void Change (XClassMember x, XClassMember y)
        {
            if (out != null)
            {
                Context cx = new Context(x),
                        cy = new Context(y);
                
                if (cx.defpack.equals(cy.defpack) &&
                    cx.defclass.equals(cy.defclass))
                    SetContext(cx);
                else
                    SetContext(new Context());

                curop = CHANGE;
                out.println();
                out.println("#CHANGE");
                out.println( Descr(x));
                out.println("#TO");
                out.println( Descr(y));
                out.println("#END");
            }
        }


        void Add (XClassMember x)
        {
            if (out != null)
            {
                if (SetContext(new Context(x)) || curop != ADD)
                {
                    curop = ADD;
                    out.println();
                    out.println("#ADD");
                }
                out.println(Descr(x));
            }
        }


        void Delete (XClassMember x)
        {
            if (out != null)
            {
                if (SetContext(new Context(x)) || curop != DELETE)
                {
                    curop = DELETE;
                    out.println();
                    out.println("#DELETE");
                }
                out.println(Descr(x));
            }
        }


        String Descr (XClassMember x)
        {
            if (x instanceof XClass)
                return Descr((XClass)x);
            else if (x instanceof XClassConstructor)
                return Descr((XClassConstructor)x);
            else if (x instanceof XClassMethod)
                return Descr((XClassMethod)x);
            else if (x instanceof XClassField)
                return Descr((XClassField)x);
            else
                return "???";
        }


        int depth = 0;


        String Descr (XClass x)
        {
            String s = "";

            s += XModifier.toString(x.modifier & ~XModifier.xinterface);
            if (s.length() != 0)
                s += " ";

            if (x.IsInterface())
            {
                s += "interface " + RelativeName(x);

                if (x.implement.size() != 0)
                    s += " extends " + ClassesToString(x.implement);
            }
            else
            {
                s += "class " + RelativeName(x);

                if (x.extend != null && !x.extend.FullName().equals("java.lang.Object"))
                    s += " extends " + x.extend.FullName();

                if (x.implement.size() != 0)
                    s += " implements " + ClassesToString(x.implement);
            }

            if (curop == ADD)
            {
                s += " {\n";
                depth++;

                for (Enumeration e = x.constructors.elements(); e.hasMoreElements();)
                    s += Indent(depth) + Descr((XClassConstructor)e.nextElement()) + "\n";

                for (Enumeration e = x.methods.elements(); e.hasMoreElements();)
                    s += Indent(depth) + Descr((XClassMethod)e.nextElement()) + "\n";

                for (Enumeration e = x.fields.elements(); e.hasMoreElements();)
                    s += Indent(depth) + Descr((XClassField)e.nextElement()) + "\n";

                for (Enumeration e = x.inners.elements(); e.hasMoreElements();)
                    s += Indent(depth) + Descr((XClass)e.nextElement()) + "\n";

                depth--;
                return s + Indent(depth) + "}";
            }
            else
                return s + " {}";
        }


        String Descr (XClassConstructor x)
        {
            String s = XModifier.toString(x.modifier);
            if (s.length() != 0)
                s += " ";

            s += RelativeName(x);

            if (x.xthrows.size() != 0)
                s += " throws " + x.xthrows.toString();

            return s + ";";
        }


        String Descr (XClassMethod x)
        {
            String s = XModifier.toString(x.modifier);
            if (s.length() != 0)
                s += " ";

            s += x.type.toString() + " " + RelativeName(x);

            if (x.xthrows.size() != 0)
                s += " throws " + x.xthrows.toString();

            return s + ";";
        }


        String Descr (XClassField x)
        {
            String s = XModifier.toString(x.modifier);
            if (s.length() != 0)
                s += " ";

            return s + x.type.toString() + " " + RelativeName(x) + ";";
        }


        String RelativeName (XClassMember x)
        {
            if (depth == 0 && cur.Empty())
            {
                if (x instanceof XClass)
                    return x.FullName();
                else
                    return x.home.FullName()+"."+x.toString();
            }
            else
                return x.toString();
        }



        boolean SetContext (Context c)
        {
            boolean changed = false;

            if (!cur.defpack.equals(c.defpack))
            {
                cur.defpack = c.defpack;
                cur.defclass = "";
                out.println();
                out.println("#PACKAGE "+cur.defpack);
                changed = true;
            }

            if (!cur.defclass.equals(c.defclass))
            {
                cur.defclass = c.defclass;
                if (!changed)
                    out.println();
                out.println("#CLASS "+cur.defclass);
                changed = true;
            }

            return changed;
        }

    }


    Upd upd = new Upd();
}
