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
 *      This module merges set of base elements and set of updates.
 *      All of updates are assumed implemented.
 */

package javasoft.sqe.apiCheck;

import java.util.Enumeration;



class Merger
{
    Units  baseunits;

    Tran tran;

    int errs;


    Merger (Units bu, Tran t)
    {
        baseunits = bu;
        tran      = t;
    }


    boolean Work (Props props)
    {
        long t0 = Main.GetTimer();

        errs = 0;

        //System.err.println("Phase1");
        Phase1();

        for (Enumeration e = tran.updates.elements(); e.hasMoreElements();)
        {
            Update u = (Update)e.nextElement();
            props.Add("update", u.name != null && u.name.length() != 0 ? u.name : u.fname);
        }

        Main.PrintTimer("Merge done", t0);

        return errs == 0;
    }


    void Phase1 ()
    {
        for (Enumeration e = tran.groups.elements(); e.hasMoreElements();)
        {
            Link head = (Link)e.nextElement();

            int waserrs = errs;

            for (Link l = head; l != null; l = l.next)
                if (l.prev != null)
                {
                    if (!l.prev.mark)
                        l.prev.mark = true;
                    else
                    {
                        errs++;
                        Err(l.stat, "ambigious statement");
                    }
                }
                
            Link found = null;

            for (Link l = head; l != null; l = l.next)
                if (!l.mark)
                {
                    if (found == null)
                        found = l;
                    else
                    {
                        // ??
                    }
                }

            if (found == null)
            {
                // ??
            }

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



    void Err (Statement stat, String msg)
    {
        System.err.println(msg);
        System.err.println(stat.toString());
    }
}
