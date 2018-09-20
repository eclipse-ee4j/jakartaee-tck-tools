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
 *      This module reads the signature file and stores results 
 *      in the Units or XProg structures.
 */


package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;



class ReadJh extends JReader
{
    Units  units;
    PackageSet pset;


    int Read (API api, String f, PackageSet packs)
    {
        if (OpenStream(f) != null)
            return 0;

        String s = null;
        try
        {
            s = is.readLine();
        }
        catch (IOException x)
        {
        }

        if (s == null || !s.regionMatches(0, WriteJh.magic, 0, WriteJh.magic.length()))
        {
            Close();
            return 0;
        }

        if ((units = api.units) == null)
           units = api.units = new Units();

        pset  = packs;

        if (api.props != null)
        {
            ReadProps(api.props);
            api.props.Add("file", f);
        }

        Close();

        if (OpenStream(f) != null)
            return 0;

        OpenScan();

        long t0 = Main.GetTimer();

        boolean r = Read();

        Main.PrintTimer("ReadJh "+f, t0);

        pset  = null;
        units = null;

        Close();
        return r ? +1 : -1;
    }


    void ReadProps (Props props)
    {
        try
        {
            while (true)
            {
                String s = is.readLine();
                if (s == null) return;
                int i = s.indexOf(':');
                if (!s.startsWith("//") || i == -1) return;
                props.Add(s.substring(2, i).trim(), s.substring(i+1).trim());
            }
        }
        catch (IOException x)
        {
        }
    }


    boolean Read ()
    {
        String pack = "";
        errors = 0;

        //trace = true;
        try
        {
            ReadWord();

            while (syn != syn_eof)
            {
                Vector uu = new Vector();
                if (ReadUnit(uu, pack) && uu.size() > 0)
                {
                    Unit u = (Unit)uu.firstElement();
                    if (u instanceof UnitPack)
                    {
                        pack = u.toString()+".";
                    }
                    else if (u instanceof UnitClass)
                    {
                        if (!pset.InPath(pack))
                            ((UnitClass)u).defined = false;
                    }
                    else
                    {
                        return Err("syntax error", u);
                    }
                    
                    units.addAll(uu);
                }
                else
                {
                    return Err("invalid symbol");
                }
            }

            units.AddPath();

            return errors == 0;
        }
        catch (Error x)
        {
            return false;
        }       
    }
}
