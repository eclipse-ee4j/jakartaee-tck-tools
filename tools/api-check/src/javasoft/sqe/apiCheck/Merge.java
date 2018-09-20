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
 *
 *      Compare program reads two sets of classes (base and test), list of updates
 *      to base classes. Program compares updated base classes against test classes
 *      and prints differencies.
 * 
 *      Program options
 *
 *      -base <src> 
 *              Defines the base API. <src> may be 
 *              1) classpath (list of directories and/or .zip/.jar files)
 *              2) "$" for JVM classpath ("sun.boot.class.path")
 *              3) name of the signature file.
 *              Default classpath used if this option omitted.
 *      -update <file>
 *              Name of the update-file. 
 *      -package <packs>
 *              List of packages to be tested.Use + or - to separate  names.
 *              If package prefixed with -, it excluded from testing.
 *              All classes from the base and test API tested if this options omitted.
 *      -expackage <packs>
 *              Package to be excluded from test.
 *      -out <file>
 *              Name of the report file.
 *              stdout used if this options omitted.
 *      -reflect
 *              Use reflection to get class information.
 *      -static
 *              Use class file parsing to get class information (default).
 *      -version
 *              Display program version.
 * 
 *      The following options are for maintenance/debugging only
 * 
 *      -debug
 *      -time
 * 
 *      Exit code
 *      ---------
 *
 *      0: ,
 *
 *      1: ,
 *
 *
 */



package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;



public 
class Merge
{
    static 
    String[] pars = {"base$m$s",  "update$m$s",  "package$m$s", "expackage$m$s", 
                     "sigout$s", 
                     "reflect", "static",      "version", 
                     "time",    "debug"};

    static 
    String help =
         "Options are:\n"
        +"-base <spec>\n"
        +"-update <file>\n"
        +"-package <packs>\n"
        +"-expackage <packs>\n"
        +"-sigout <file>\n"
        +"-reflect\n"
        +"-static\n"
        +"-version";


    public static 
    void main (String[] args)
    {
        if (!Main.args.Parse(args, pars) || Main.args.isEmpty())
        {
            System.err.println(help);
            return;
        }

        if (Main.args.getProperty("version") != null)
        {
            System.out.println("Version: " + Version.Ident());

            if (Main.args.size() == 1)
                return;
        }

    //  -package/-expackage processing

        PackageSet packs = new PackageSet(Main.args.getProperty("package"),
                                          Main.args.getProperty("expackage"));

    //  Get base API

        API baseapi = new API();
        String basepath = Main.args.getProperty("base");
        if (basepath == null)
        {
            System.err.println("base not defined - using default classpath");
            if (!Read(baseapi, null, packs))
                Stop("no classes found in default classpath");    
        }
        else
        {
            StringTokenizer st = new StringTokenizer(basepath, File.pathSeparator+' ',  false);
            while (st.hasMoreElements())
            {
                String fname = (String)st.nextElement();
                if (!Read(baseapi, fname, packs))
                    Stop("no classes found in \"" + fname + "\"");    
            }
        }

        if (baseapi.units != null)
        {
            Expander e = new Expander();
            e.Expand(baseapi.units);
            e.PrintResults(System.err);
        }

    //  Get update file

        if (Main.args.getProperty("update") != null)
        {
            Tran tran = new Tran();
            ReadJhu readupd = new ReadJhu(tran);

            StringTokenizer st = new StringTokenizer(Main.args.getProperty("update"), 
                                                     File.pathSeparator + ' ', 
                                                     false);
            while (st.hasMoreElements())
            {
                String fname = (String)st.nextElement();
                File f = new File(fname);
                if (!f.exists() && !fname.endsWith(".jhu"))
                {
                    f = new File(fname+".jhu");
                    if (f.exists() && f.isFile())
                        fname += ".jhu";
                }
                f = null;

                if (!readupd.Read(baseapi, fname))
                    Stop(null);
            }

    //  Transform the base API according to update

            Merger dec = new Merger(baseapi.GetUnits(), tran);
            if (!dec.Work(baseapi.props))
                Stop("update error");
            dec = null;

    //  Invalidate xprog part of API

            baseapi.xprog = null;
        }

    //  Write the signature file

        WriteJh writer = new WriteJh();
        if (Main.args.getProperty("sigout") == null)
            writer.Write(baseapi, System.out);
        else
            writer.Write(baseapi, Main.args.getProperty("sigout"));       

        writer.OutExtern(baseapi.GetXProg());
        writer.Close();

    //  Exit

        System.exit(0);
    }



//  Read API (base or test) classes from:
//    - signature file (new or old),
//    - zip/jar file,
//    - classpath (list of directories and/or .zip files).

    static
    boolean Read (API api, String name, PackageSet pset)
    {
    //  Check if we have a single file name or list of directories

        if (name != null 
         && name.indexOf(System.getProperty("path.separator")) == -1 
         && !name.equals("$"))
        {
        //  Have is a single file name

            File f = new File(name);
            if (!f.exists())
            {
                if (name.endsWith(".jh"))
                {
                    System.err.println("not found \"" + name + "\"");
                    return false;
                }
                else
                {
                    f = new File(name+".jh");
                    if (!f.exists() || !f.isFile())
                    {
                        System.err.println("not found \"" + name + "\"");
                        return false;
                    }
                    name += ".jh";
                }
            }

            if (f.isFile())
            {
                if (!f.canRead())
                {
                    System.err.println("cannot read \"" + name + "\"");
                    return false;
                }

                return api.ReadAll(name, pset) > 0;
            }
        }

    //  Classpath given. Read class files.

        return api.ReadAll(name, pset) > 0;
    }


    static void Stop (String msg)
    {
        if (msg != null)
            System.err.println(msg);

        System.err.println("Merge terminated.");
        System.exit(1);
    }
}
