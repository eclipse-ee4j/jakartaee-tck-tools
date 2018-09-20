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
 *      This module select which update statements are implemented and
 *      updates the base class set.
 *
 */

package javasoft.sqe.apiCheck;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;



class Decision
{
    Units  baseunits;
    Units  testunits;

    Tran tran;


    Decision (Units bu, Units tu, Tran t)
    {
        baseunits = bu;
        testunits = tu;
        tran      = t;
    }


    void Work (Props props)
    {
        long t0 = Main.GetTimer();

        NormUnits norm = new NormUnits();
        norm.Do(testunits);
        norm = null;

        //System.err.println("Phase1");
        Phase1();
        //System.err.println("Phase2");
        Phase2();

        for (Enumeration e = tran.updates.elements(); e.hasMoreElements();)
        {
            Update u = (Update)e.nextElement();
            String n = u.name != null && u.name.length() != 0 ? u.name : u.fname;

            props.Add("update", n + " (implemented: " + u.posstmnts + "/" + u.stmnts + ")");
        }

        Main.PrintTimer("Decision done", t0);
    }


    void Phase1 ()
    {
        for (Enumeration e = tran.stats.elements(); e.hasMoreElements();)
        {
            Statement s = (Statement)e.nextElement();
            s.poscount = 0;
            s.negcount = 0;
        }

        HashSet /*Statement*/ stmnts  = new HashSet();

        for (Enumeration e = tran.groups.elements(); e.hasMoreElements();)
        {
            Link head = (Link)e.nextElement();
            Link marked = null;
            Link nulled = null;

            for (Link l = head; l != null; l = l.next)
            {// all statements in the group
                if (l.stat != null)
                    stmnts.add(l.stat);

                if (l.unit != null)
                    if (testunits.contains(l.unit))
                        l.mark = true;

                if (l.mark)
                {
                    if (marked == null)
                        marked = l;
                    else
                        Err("?21", l.unit.toString());
                }    

                if (l.unit == null)
                    nulled =l;
            }
                
            if (marked == null && nulled != null)
            {
                marked = nulled;
                marked.mark = true;
            }

            for (Link l = marked; l != null; l = l.prev)
                if (l.stat != null)
                {
                    stmnts.remove(l.stat);
                    l.stat.poscount++;
                }

            for (Iterator f = stmnts.iterator(); f.hasNext();)
            {
                Statement s = (Statement)(f.next());
                s.negcount++;
            }

            stmnts.clear();
        }

        for (Enumeration e = tran.stats.elements(); e.hasMoreElements();)
        {
            Statement s = (Statement)e.nextElement();

            s.ok = (s.poscount > 0 && s.poscount >= s.negcount);

            s.update.stmnts++;
            if (s.ok)
                s.update.posstmnts++;
        }

        if (Main.IsDebugMode())
            PrintGroups();
    }


    void Phase2 ()
    {
        for (Enumeration e = tran.groups.elements(); e.hasMoreElements();)
        {
            Link head = (Link)e.nextElement();

        //  Find the longest available path

            int i = -1;
            Link found = null;

            for (Link l = head; l != null; l = l.next)
            {
                int k = LinkFollow(l);
                if (k > i)
                {
                    i = k;
                    found = l;
                }
            }

            if (Main.IsDebugMode())
                PrintPath(head, found);

            for (Link l = head; l != null; l = l.next)
                if (l != found && l.unit != null)
                    for (int k = 0; k < baseunits.size(); k++)
                    {
                        Unit u = (Unit)baseunits.get(k);
                        if (u == l.unit)
                        {
                            baseunits.removeElementAt(k);
                            break;
                        }
                    }
        }
    }


    int LinkFollow (Link l)
    {
        int i = 0;

        while (l != null)
            if (l.stat == null || l.stat.ok)
            {
                l = l.prev;
                i++;
            }
            else
                return -1;

        return i;

    }


    void PrintGroups ()
    {
        for (Enumeration e = tran.groups.elements(); e.hasMoreElements();)
        {
            Link head = (Link)e.nextElement();
            System.err.println("group");

            for (Link l = head; l != null; l = l.next)
            {
                System.err.println("     "+l);
                if (l.head != head) 
                    System.err.println("head??");
            }
        }

        for (Enumeration e = tran.stats.elements(); e.hasMoreElements();)
        {
            Statement s = (Statement)e.nextElement();
            System.err.println("statement "+s+": "+s.ok+" (+"+s.poscount+" -"+s.negcount+")");
        }
    }


    void PrintPath (Link head, Link found)
    {
        System.err.println("path for group "+head);
        for (Link l = found; l != null; l = l.prev)
            System.err.println("    "+l);
    }

 
    void Err (String msg, String descr)
    {
        System.err.println(msg);
        if (descr != null)
            System.err.println(descr);
    }
}
