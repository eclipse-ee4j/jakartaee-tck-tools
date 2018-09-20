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
 
 
 
class PairCommon
{
    List base,
         test,
         deleted, 
         added;


    PairCommon ()
    {
        base    = new ArrayList();
        test    = new ArrayList();
        deleted = new ArrayList();
        added   = new ArrayList();
    }


    void clear()
    {
           base.clear();
           test.clear();
        deleted.clear();
          added.clear();
    }


    void init (List b, List t)
    {
        clear();

        XClassMember x, y;

        if (t != null)
            for (Iterator k = t.iterator(); k.hasNext();) {
                if (visible(y = (XClassMember)k.next()))
                    added.add(y);
            }
                    
        if (b != null)
            for (Iterator i = b.iterator(); i.hasNext();) 
                if (visible(x = (XClassMember)i.next())) {
                    for (Iterator k = added.iterator(); k.hasNext();) {
                        y = (XClassMember)k.next();
                        if (ident(x, y)) {
                            base.add(x);
                            test.add(y);
                            x = null;
                            k.remove();
                            break;
                        }
                    }
                            
                    if (x != null)
                        deleted.add(x);
                }
    }
    
    
    static boolean ident (XClassMember b, XClassMember t)
    {
        return b.getSignature().equals(t.getSignature());
    }
    
    
    boolean visible (XClassMember m)
    {
        return true;
    }

}

 
 
abstract class CheckCommon 
{
	int errs;           // errors counter
    boolean sort;       // sort the report
    boolean verbose;    // verbose diagnostic
    PrintWriter out;    // report ouput stream

    Set/*XClass*/ basevisited,
                  testvisited;

    List/*Msg*/ msgsort;


    void open ()
    {
        msgsort = sort ? new ArrayList() : null;
		
		errs = 0;
        
        basevisited = new HashSet();
        testvisited = new HashSet();
    }
    
    
    void close ()
    {
        testvisited = null;
        basevisited = null;
    
        if (sort)
            reportSorted();

        msgsort = null;
    }
    
    
    abstract boolean compare (API base, API test, ClassFilter cf);


    //  Compares outer classes only (non-recursive).
    //
    void compareClasses (XClass base, XClass test)
    {
        final boolean bv = basevisited.contains(base),
                      tv = testvisited.contains(test);

        if (bv && tv)
            return;

        if (bv || tv)
            throw new Error("visited: "+base.getFullName()+" or "+test.getFullName());

        basevisited.add(base);
        testvisited.add(test);

        compare(base, test);
    }


    //  Compares members (recursive via xcompare).
    //
    void compareMembers (XClass base, XClass test, PairCommon tos)
    {
    
	//	Compare constructors
		
        int i;

		tos.init(base.ctors, test.ctors);

		for (i = 0; i < tos.base.size(); i++)
			compare((XClassCtor)tos.base.get(i), 
                    (XClassCtor)tos.test.get(i));
		
		for (i = 0; i < tos.deleted.size(); i++)
            deleted((XClassCtor)tos.deleted.get(i));
			
		for (i = 0; i < tos.added.size(); i++)
            added((XClassCtor)tos.added.get(i));
		
	//	Compare methods
			
		tos.init(base.methods, test.methods);
		
		for (i = 0; i < tos.base.size(); i++)
			compare((XClassMethod)tos.base.get(i), 
                    (XClassMethod)tos.test.get(i));
		
		for (i = 0; i < tos.deleted.size(); i++)
            deleted((XClassMethod)tos.deleted.get(i));
			
		for (i = 0; i < tos.added.size(); i++)
            added((XClassMethod)tos.added.get(i));
		
	//	Compare fields
			
		tos.init(base.fields, test.fields);
			
		for (i = 0; i < tos.base.size(); i++)
            compare((XClassField)tos.base.get(i), 
                    (XClassField)tos.test.get(i));
		
		for (i = 0; i < tos.deleted.size(); i++)
            deleted((XClassField)tos.deleted.get(i));
			
		for (i = 0; i < tos.added.size(); i++)
            added((XClassField)tos.added.get(i));
		
	//	Compare inner classes
			
		tos.init(base.inners, test.inners);
		
		for (i = 0; i < tos.base.size(); i++) {
            XClass b = (XClass)tos.base.get(i), 
                   t = (XClass)tos.test.get(i);
            xcompare(b, t);
         }
		
		for (i = 0; i < tos.deleted.size(); i++)
            xdeleted((XClass)tos.deleted.get(i));
			
		for (i = 0; i < tos.added.size(); i++)
            xadded((XClass)tos.added.get(i));
    }
    
        
// Methods to be overridden 
    
    void compare (XClass a, XClass b)               {}

        
    //  constructors     

    void compare (XClassCtor a, XClassCtor b)       {}
        
    void deleted (XClassCtor a)                     {}
        
    void added   (XClassCtor a)                     {}
    
       
    // methods
        
    void compare (XClassMethod a, XClassMethod b)   {}
    
    void deleted (XClassMethod a)                   {}
        
    void added   (XClassMethod a)                   {}
       
       
    //  fields
        
    void compare (XClassField a, XClassField b)     {}
            
    void deleted (XClassField a)                    {}
        
    void added   (XClassField a)                    {}
    
        
    // inner classes
        
    void xcompare (XClass a, XClass b)              {}
    
    void xdeleted (XClass a)                        {}
        
    void xadded   (XClass a)                        {}
    
    
//  Common utility methods


    void showProps (String head, Map props)
    {
        if (props != null) {
            out.println(head);
            for (Iterator it = props.entrySet().iterator(); it.hasNext();) {
                Map.Entry e = (Map.Entry)it.next();
                String key = (String)e.getKey();
                String val = e.getValue() == null ? null : e.getValue().toString();
            
                if (val == null || val.equals("[default]"))
                    out.println("  " + key);
                else
                    out.println("  " + key + " = \"" + val + "\"");
            }
        }
    }
    
    
    void testMods (String pref, int bm, int tm, int check)
    {
        final int accmods = XModifier.xpublic | XModifier.xprotected | XModifier.xprivate;
    
    //  First, check only access modifiers
    
        if ((bm & accmods) != (tm & accmods)) 
            report("access changed:", 
                   pref + "from '" + XModifier.toString(bm & accmods) + 
                         "' to '" + XModifier.toString(tm & accmods) + "'");
                         
    //  Second, check all others

        testBits(pref, bm & ~accmods & check,  tm & ~accmods & check);
    }
    
    
    void testBits (String pref, int mb, int mt)
    {
        for (int mx = mb ^ mt, m = 1; mx != 0;  mx >>= 1, m <<= 1)
        {
            final int bb = mb & m,
                      bt = mt & m;

            if (bb != bt)
                if (bb != 0)         
                    report("modifier deleted:", 
                           pref + "'" + XModifier.toString(bb) + "'");
                else            
                    report("modifier added:", 
                           pref + "'" + XModifier.toString(bt) + "'");
        }
    }

    
    //  Compares two array of strings.
    //
    static boolean equal (String[] b, String[] t)
    {
        if ((b == null || b.length == 0) && (t == null || t.length == 0))
            return true;
            
        if (b == null || t == null)
            return false;
            
        if (b.length != t.length)
            return false;
            
        for (int i = 0; i < b.length; i++) 
            if (!b[i].equals(t[i]))
                return false;
        
        return true;
    }
    
    
    //  Compares two array of strings, order ignored.
    //
    static boolean compareTypes (String[] b, String[] t)
    {
        if ((b == null || b.length == 0) && (t == null || t.length == 0))
            return true;
            
        if (b == null || t == null)
            return false;
            
        if (b.length != t.length)
            return false;
            
        ArrayList temp = new ArrayList(t.length);
        for (int i = 0; i < t.length; i++) 
            temp.add(t[i]);
        
        for (int i = 0; i < b.length; i++) {
            String s = b[i];
            for (int k = 0; k < temp.size(); k++) 
                if (s.equals(temp.get(k))) {
                    temp.remove(k);
                    s = null;
                    break;
                }
                
            if (s != null)
                return false;
        }
        
        return temp.size() == 0;
    }
    
    
//  Report generator


    static class Msg implements Comparable
    {
        String context,
               kind,
               text;


        public int compareTo (Object o)
        {
            Msg m = (Msg)o;
            return (kind+context+text).compareTo(m.kind+m.context+m.text);
        }
    }


    static final int hmax = 32;
    String[]   htexts = new String[hmax];
    boolean[]  hflags = new boolean[hmax];
    String htext  = "";
    int    htos = -1,
           htosold = 0;
        
        
    boolean stackEmpty ()
    {
        return htos == -1;
    }


    void pushHead (String hd)
    {
        if (htos == hmax) 
            throw new Error("Heads stack overflow");

        htos++;
        htexts[htos] = new String(hd);
        hflags[htos] = true;
    }


    void popHead ()
    {
        htexts[htos] = null;
        htos--;
    }


    void reportSorted ()
    {
        Collections.sort(msgsort);

        String kind = "",
               context = "";

        for (Iterator it = msgsort.iterator(); it.hasNext();) {
            Msg m = (Msg)it.next();
            if (!m.kind.equals(kind)) {
                kind = m.kind;
                out.println("\n" + kind);
                context = "";
            }
            if (!m.context.equals(context)) {
                context = m.context;
                out.println("\t" + context);
            }

            out.println("\t\t" +  m.text);
        }
    }
        

    void report (String kind, String text)
    {
        if (msgsort == null) {
            for (int i = 0; i <= htos; i++)
                if (hflags[i]) {
                    hflags[i] = false;
                    out.println();
                    out.print(indent(i));
                    out.println(htexts[i]);
                    htosold = htos;
                }

            if (htos < htosold) {
                out.println();
                htosold = htos;
            }

            out.print(indent(htos+1));
            out.print(kind + ' ' + text);
            out.println();
        }
        else {
            Msg m = new Msg();
            m.context = (htos < 0) ? "" : htexts[htos];
            m.kind = kind;
            m.text = text;
            msgsort.add(m);
        }

		errs++;
    }


    static String indent (int n)
    {
        StringBuffer sb = new StringBuffer();

        for (int i = n; i > 0; i--)
            sb.append("\t");

        return sb.toString();
    }

}
 
