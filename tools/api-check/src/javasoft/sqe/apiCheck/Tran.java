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
 *      This module contains data structures for updates processing.
 */

package javasoft.sqe.apiCheck;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.sun.ts.lib.util.SigLogIntf;

class Update extends NamedObject
{
    int stmnts,         // total statements count
        posstmnts;      // implemented statements count

    String fname;       // source file name


    Update (String f)
    {
        fname = f;
    }
}



class Statement extends NamedObject
{
    Update update;      // ref to Update class
    int    linenb;      // source line number

    boolean ok;         // true, if statement implemented
    int poscount, negcount;


    Statement (Update u)
    {
        update = u;
    }


    public
    String toString ()
    {
        return update+" line:"+linenb+"  "+name;
    }
}



class Link
{
    Link (Link l, Unit u, Statement s)
    {
        prev = l;
        unit = u;
        stat = s;
        mark = false;
    }


    public
    String toString ()
    {
        return Addr()+") "+
               "prev:"+(prev != null ? prev.Addr() : "-   ")+" "+
               (mark ? "@ " : "  ")+
               (stat != null ? stat.name.charAt(1)+(stat.ok ? "+ " : "- ") : "   ")+
               (unit != null ? unit.Descr() : "*empty*");
    }


    String Addr ()
    {
        String s = String.valueOf(hashCode());
        return s.substring(s.length()-4);
    }


    Link prev;       // transformed from prev.unt
    Unit unit;       // transform to unit
    Statement stat;  // transformed by statement

    Link head;
    Link next;
    boolean mark;
}



class Tran
{
    Vector  /*Update*/    updates = new Vector();
    Vector  /*Statement*/ stats   = new Vector();
    HashMap /*Unit,Link*/ links   = new HashMap();
    Vector  /*Link*/      groups  = new Vector();

    Update    defupd  = null;
    Statement defstat = null;


    void NewUpdate (String s, String f)
    {
        defupd = new Update(f);
        defupd.name = s;
        updates.add(defupd);
    }


    void NewStatement (String s, int n)
    {
        defstat = new Statement(defupd);
        defstat.name   = s;
        defstat.linenb = n;
        stats.add(defstat);
    }


    boolean Transform (Unit o, Unit n)
    {
        if (o == null)
        {// ADD transformation - create new group
            Link l = new Link(null, n, defstat);
            l.head = l;
            l.next = null;
            links.put(n, l);
            groups.add(l);
            return true;
        }

    //  CHANGE/DELETE transformation (o != null)
        Link p = (Link)links.get(o);
        if (p == null)
        {// create new group
            p = new Link(null, o, null);
            p.head = p;
            p.next = null;
            links.put(o, p);
            groups.add(p);
        }
        else if (LookGroup(p, n))
        {
            //System.err.println("*duplicate transform*");
            return false; 
        }

    //  add transformation to group
        Link l = new Link(p, n, defstat);
        if (n != null)
            links.put(n, l);

        l.head = p.head;
        l.next = p.head.next;
        p.head.next = l;
        return true;
    }


    boolean LookGroup (Link p, Unit n)
    {
        for (Link l = p; l != null; l = l.next)
            if (l.unit == n)
                return true;

        return false;
    }


/*

    result is
        0 there are no implemented updates 
        1 there are implemented updates and unimplemented ones
        2 all updates are implemented

*/
    int Status ()
    {
        int posstmnts = 0,
            stmnts    = 0;

        for (Enumeration e = updates.elements(); e.hasMoreElements();)
        {
            Update p = (Update)e.nextElement();
            posstmnts += p.posstmnts;
            stmnts    += p.stmnts;
        }

        if (stmnts == 0)
            return 2;
        else
            return posstmnts == 0 ? 0 : (posstmnts != stmnts ? 1 : 2);
    }


    //    void Print (PrintWriter pw)
    void Print (SigLogIntf pw)
    {
        Update upd = null;
        for (Enumeration e = stats.elements(); e.hasMoreElements();)
        {
            Statement stat = (Statement)e.nextElement();
            if (stat.update != upd)
            {
                upd = stat.update;
                pw.println("UPDATE " + upd.name +
                           " file \"" + upd.fname + "\"");
            }
            pw.println("\tline:" + stat.linenb + " " + stat.name +
                       " implemented: " + (stat.ok ? "YES" : "NO ") +
                       " (+" + stat.poscount + " -" + stat.negcount + ")");
        }
        pw.println("");
    }
}


