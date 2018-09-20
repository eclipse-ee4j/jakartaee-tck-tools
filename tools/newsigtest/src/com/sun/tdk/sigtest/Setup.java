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

/*
 *
 * Serguei Ivashin <isl@nbsp.nsk.su>
 *
 * The Sigtest Tool
 *
 *      Program options
 *
 *      -help
 *              Prints list of program options.
 *
 *      -version
 *              Prints program version.
 * 
 *      -ConstValues
 *              Writes values of constant fields to the sigfile.
 *
 *      -AllMembers
 *              Writes all (not only public) class members to the sigfile.
 *
 *      -package <packs>
 *              List of packages to be tested. Use + or - to separate names.
 *              If package prefixed with -, it is excluded from being tested.
 *              Default: all available classes are tested.
 *
 *      -expackage <packs>
 *              Package to be excluded from test.
 *              Default. nothing is excluded.
 *
 *      -test <cp>
 *              Specifies the test API in form of classpath.
 *              This is required argument, no default.
 *
 *      -access [reflect | static]
 *              Selects access mode to the testing classes.
 *
 *      -BootLoader
 *              (For reflect access mode) Use bootstrap class loader, even if
 *              classpath was specified in the -test option. 
 *
 *      -TestVersion
 *              Specifies version string for the testing classes.
 *
 *      -out <file>
 *              Name of the output signature file.
 *              Default: stdout.
 * 
 * 
 *      Exit code
 *      ---------
 *
 *      0 - success, 
 *      1 - error.
 *
 */

 
package com.sun.tdk.sigtest;

import com.sun.tdk.sigtest.api.*;
import java.io.*;
import java.util.*;


public class Setup extends Main
{

    public static void main (String[] args) 
    {
        System.exit(run(args));
    }

    
    public static int run (String[] args)
    {
        try {
            return runx(args);
        }
        catch (Error e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
    
    
    static int runx (String[] pars) 
    {
    	Arguments args = new Arguments(pars);
        
    //  Arguments check phase first        
    
        if (args.isArg("-version", false)) {
            System.err.println("version " + Main.getVersion());
            if (args.isEmpty())
                return 0; 
        }
        if (args.isEmpty() || args.isArg("-help", false)) {
            usage();
            return 0; 
        }

    //  -package/-expackage processing
    
        ClassFilter cfilter = new ClassFilter();
        if (args.isArg("-package", true))
            cfilter.parse(args.getArgString());
        
        if (args.isArg("-expackage", true))
            cfilter.sub(args.getArgString(), true);

    //  Get test API

        Test test = new Test();
        if (args.isArg("-test", true) || args.isArg("-in", true))
            test.path = args.getArgStringExpanded();

        if (test.path == null)
            stop("Testing classes not specified");

        test.access = "reflect";
        if (args.isArg("-access", true)) {
            test.access = args.getArgString();
            if (!test.access.equals("reflect") && !test.access.equals("static"))
                stop("'reflect' or 'static' expected in -access option");
        }
        
        if (args.isArg("-BootLoader", false))
            test.bootloader = true;
        
        boolean allmembers = false;    
        if (args.isArg("-AllMembers", false))
            allmembers = true;
            
        String testversion = null;
        if (args.isArg("-TestVersion", true)) 
            testversion = args.getArgStringExpanded();

    //  Get output file name

        String outpath = null;
        if (args.isArg("-out", true))
            outpath = args.getArgString();
            
    //  Check for extra arguments
            
        if (!args.isEmpty())
            stop("Extra arguments: " + args.toString());

    //  Processing phase second

        test.cfilter = cfilter;
        test.open();
        
        System.err.println("Classpath: " + test.path);
        
        if (testversion != null)
            test.api.setProp("Version", testversion);
        
        if (cfilter != null)
            test.api.setProp("Included", cfilter.toString());

        ClassMemberFilterSet cmf = null;
        if (!allmembers) {
            test.api.setProp("Members", "Filtered");
            cmf = new ClassMemberFilterSet(test.api);
        }
        else {
            test.api.setProp("Members", "All");
        }
	
        Writer fw = null;
        if (outpath != null) {
            try {    
                fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outpath), "UTF8"));
            }
            catch (IOException e) {
                stop(e.getMessage());
            }
        }
    
        PrintWriter pw; 
        if (fw == null)
            pw = new PrintWriter(System.out);
        else
            pw = new PrintWriter(fw);
    
	    APIWriterJh wjh = new APIWriterJh();
        wjh.write(pw, test.api, cfilter, cmf);
        
        if (fw != null) {
            try {
                fw.close();
            }
            catch (IOException e) {
            }
            
            fw = null;
        }
        
        if (cmf != null && cmf.undefined.size() > 0) {
        }
        
        if (wjh.outercount == 0)
            System.err.println("Empty sigfile created");
        else
            System.err.println("Written to sigfile " + wjh.outercount + 
                               " classes (and " + wjh.innercount + " inner classes)");
        wjh = null;
        
        pw = null;
        
        cmf = null;
        
        test.close();
        test = null;
            
        return 0;
    }
    
    
    static void usage ()
    {
        System.err.println (
             "Options are (all names are case-insensitive):\n"
            +"-help              Prints this text\n"
            +"-version           Prints program version\n"
            +"-ConstValues       Writes values of constant fields to the sigfile\n"
            +"-AllMembers        Writes all (not only public) class members to the sigfile\n"
            +"-package <packs>   Specifies package(s) to be tested\n"
            +"-expackage <packs> Specifies package(s) excluded from testing\n"
            +"-test <cp>         Specifies classpath to the testing classes\n"
            +"-access [reflect | static] Selects access mode to the testing classes\n"
            +"-BootLoader        (for reflect access) Uses boot classloader\n"
            +"-TestVersion       Specifies version string of the testing classes\n"
            +"-out <file>        Specifies output sigfile name\n"
        );
    }
    
    
    static class Test
    {
        String  access;
        boolean valuemode  = false;
        boolean bootloader = false;
        String path;
        ClassFilter cfilter;
        APIBuffer api;
        
    
        void open ()
        {
            API tmp = null;
        
            if (access.equals("reflect"))
                tmp = new APILoaderReflect(path, valuemode, bootloader);
                
            else if (access.equals("static"))
                tmp = new APILoaderStatic(path, valuemode);
            
            else
                stop("? access \"" + access + "\"");
                
            api = new APIBuffer(tmp, cfilter);
            api.sort();
        }
        
        
        void close ()
        {
            if (api != null) {
                api.close();
                api = null;
            }
            
            path = null;
            cfilter = null;
        }
    }
    
    
    static class ClassMemberFilterSet implements ClassMemberFilter
    {
        API api;
        Set/*XClass*/ important;
        Set/*XClass*/ undefined;
        
    
        ClassMemberFilterSet (API a)
        {
            api = a;
            
            important = new HashSet();
            undefined = new HashSet();

            Set supers = new HashSet();
            
            api.rewind();
            for (XClass xclass; (xclass = api.getXClass()) != null; )
                if (xclass.home == null && XModifier.isPublic(xclass.modifiers))
                    scanSupers(xclass, supers);

            while (!supers.isEmpty()) {
                Set tmp = new HashSet();
                for (Iterator it = supers.iterator(); it.hasNext();) 
                    scanSupers((XClass)it.next(), tmp);
                supers = tmp;
            }            
        }
        
        
        void scanSupers (XClass xclass, Set supers)
        {
            register(xclass.extend, supers);
            if (xclass.implement != null)
                for (int i = 0; i < xclass.implement.length; i++)
                    register(xclass.implement[i], supers);
                    
            if (xclass.inners != null)
                for (Iterator it = xclass.inners.iterator(); it.hasNext(); ) {
                    XClass x = (XClass)it.next();
                    if (XModifier.isPublic(x.modifiers)) {
                        important.add(x);
                        scanSupers(x, supers);
                    }
                }
        }

        
        void register (String fqn, Set supers)
        {
            if (fqn != null) {
                XClass xsuper = api.getXClass(fqn);
                if (xsuper == null)
                    undefined.add(fqn);
                else if (!important.contains(xsuper)) {
                    important.add(xsuper);
                    supers.add(xsuper);
                }
            }
        }


    //  All classes can be used as superclasses or superinterfaces.
    
        public boolean ok (XClass x)
        {
            if (x.home == null)
                return XModifier.isPublic(x.modifiers) || important.contains(x);
            else 
                return important.contains(x) || hides(x, x.home);
        }
        
        
    //  Only private class members never inherited.

        
        public boolean ok (XClassCtor x)
        {
            return XModifier.isPublic(x.modifiers);
        }


        public boolean ok (XClassMethod x)
        {
            return XModifier.isPublic(x.modifiers);
        }


        public boolean ok (XClassField x)
        {
            return XModifier.isPublic(x.modifiers) || hides(x, x.home);
        }
        

        boolean hides (XClassMember x, XClass xclass)
        {
            List tmp = new ArrayList();
            
            XClass xsuper;
            
            if (xclass.extend != null) {
                if ((xsuper = api.getXClass(xclass.extend)) != null) {
                    if (hiding(x, xsuper))
                        return true;
                    tmp.add(xsuper);
                }
            }
            
            if (xclass.implement != null)
                for (int i = 0; i < xclass.implement.length; i++) {
                    if ((xsuper = api.getXClass(xclass.implement[i])) != null) {
                        if (hiding(x, xsuper))
                            return true;
                        tmp.add(xsuper);
                    }
                }
                
            for (Iterator it = tmp.iterator(); it.hasNext();)
                if (hides(x, (XClass)it.next()))
                    return true;
        
            return false;
        }


        boolean hiding (XClassMember x, XClass xsuper)
        {
            return x.home.isAccessible(x.findSame(xsuper));
        }
    }
    
}

