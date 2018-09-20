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
 * The Sigtest Tool
 *
 *      Program options (all option names are case-insensitive) :
 *
 *      -help
 *              Prints list of program options.
 *
 *      -version
 *              Prints program version.
 * 
 *      -check [src | bin | changes]
 *              Select check mode two-way source, two-way binary compatibility or 
 *              changes check.
 *              Default: src [two-way source compatibility check].
 *
 *      -ConstValues
 *              If present, constant values from the signature file are checked.
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
 *      -base <file> 
 *              Specifies the base (reference) signature file
 *              This is required argument, no default.
 *
 *      -test <cp>
 *              Specifies the test API in form of classpath or sigfile.
 *              Default: JVM classpath.
 *
 *      -access [reflect | static | sigfile]
 *              Selects access mode to the testing classes.
 *              In reflect mode, -test option can be omitted. In other modes,
 *              it is required.
 *              In sigfile mode, <cp> should specify sigfile. In other modes,
 *              it should specifu classpath.
 *
 *      -BootLoader
 *              (For reflect access mode) Use bootstrap class loader, even if
 *              classpath was specified in the -test option. 
 *
 *      -TestVersion
 *              Specifies version string for the testing classes.
 * 
 *      -out <file>
 *              Name of the report file.
 *              Default: stdout.
 *
 *      -sort
 *              Sort report by error type and classname.
 *
 * 
 *      Exit code
 *      ---------
 *
 *      0 - success, no differences are found,
 *      1 - error(s) in arguments, check was not run,
 *      2 - check found diffrences.
 *
 */


package com.sun.tdk.sigtest;

import com.sun.tdk.sigtest.api.*;
import java.io.*;
import java.net.*;
import java.util.*;


public class Check extends Main
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
        catch (OutOfMemoryError e) {
            System.err.println("OutOfMemoryError");
            e.printStackTrace();
            return 1;
        }
        catch (Error e) {
            //e.printStackTrace();
            System.err.println(e.getMessage());
            return 1;
        }
    }
    
    
    static int runx (String[] as)
    {
        Arguments args = new Arguments(as);
        
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
    
        ClassFilter cfilter = null;
        if (args.isArg("-package", true)) {
            cfilter = new ClassFilter();
            cfilter.parse(args.getArgString());
        }
        
        if (args.isArg("-expackage", true)) {
            if (cfilter == null)
                cfilter = new ClassFilter();
            cfilter.sub(args.getArgString(), true);
        }

    //  Get base API

        Base base = new Base();
        if (args.isArg("-base", true))
            base.path = args.getArgString();
        
    //  Get test API

        Test test = new Test();
        if (args.isArg("-test", true))
            test.path = args.getArgStringExpanded();

        test.access = "reflect";
        if (args.isArg("-access", true)) {
            test.access = args.getArgString();
            if (!test.access.equals("reflect") && !test.access.equals("static") 
             && !test.access.equals("sigfile"))
                stop("'reflect', 'static' or 'sigfile' expected in -access option");
        }
            
        if (test.access.equals("static") && test.path == null)
            stop("Classpath is required in static access mode");
            
        if (test.access.equals("sigfile") && 
            (test.path == null || test.path.indexOf(File.pathSeparatorChar) != -1))
            stop("File name is required in sigfile mode");
            
        if (args.isArg("-BootLoader", false))
            test.bootloader = true;
        
        String testversion = null;
        if (args.isArg("-TestVersion", true)) 
            testversion = args.getArgStringExpanded();
            
    //  Get report output file name

        String check = "src";
        if (args.isArg("-check", true))
            check = args.getArgString();
        
        CheckCommon checker = null;
        if (check.equals("bin") || check.equals("src"))
            checker = new CheckComp(check);

        else if (check.equals("changes"))
            checker = new CheckChanges();
            
        else
            stop("Invalid value for -check option: " + check);
            
    //  Get report output file name

        String outpath = null;
        if (args.isArg("-out", true))
            outpath = args.getArgString();

        boolean sort = args.isArg("-sort", false);
            
    //  Check for extra arguments
            
        if (!args.isEmpty())
            stop("Extra arguments: " + args.toString());

    //  Processing phase second

    //  Open files, etc ...

        base.open();
        
        test.open();
        
        if (testversion != null)
            test.api.setProp("Version", testversion);
        
        if (cfilter == null) {
            Object s = base.api.getProp("Included");
            if (s != null) {
                cfilter = new ClassFilter();
                cfilter.parse(s.toString());
            }
        }
            
        PrintWriter pw = null;
        try
        {
            if (outpath != null)
                pw = new PrintWriter(new FileWriter(outpath));
            else
                pw = new PrintWriter(System.out);
        }
        catch (IOException x)
        {
            stop("out error\n" + x.getMessage());
        }
        
    //  Now, main task : compare base and test

        checker.out  = pw;
        checker.sort = sort;
        
        boolean r = checker.compare(base.api, test.api, cfilter);
        
    //  Cleanup
    
        pw.flush();
        if (outpath != null)
            pw.close();
        pw = null;

        test.close();
        test = null;

        base.close();
        base = null;
            
        return r ? 0 : 2;
    }
    
    
    static void usage ()
    {
        System.err.println (
             "Options are (all names are case-insensitive):\n"
            +"-help              Prints this text\n"
            +"-version           Prints program version\n"
            +"-check [src | bin | changes] Selects check mode (default is src)\n"
            +"-ConstValues       Selects optional constants checking\n"
            +"-package <packs>   Specifies package(s) to be tested\n"
            +"-expackage <packs> Specifies package(s) excluded from testing\n"
            +"-base <file>       Specifies reference sigfile name\n"
            +"-test <cp>         Specifies classpath to the testing classes\n"
            +"-access [reflect | static | sigfile]\n"
            +"                   Selects access mode to the testing classes\n"
            +"-BootLoader        (for reflect access) Uses boot classloader\n"
            +"-TestVersion       Specifies version string for the testing classes\n"
            +"-out <file>        Specifies report file name\n"
            +"-sort              Sorts the report\n"
        );
    }
    
    
    static class Base
    {
        String path;
        BufferedReader rdr;
        APIBuffer api;

    
        void open ()
        {
            if (path == null || (path = path.trim()).length() == 0)
                stop("Sigfile not specified");

            try {
                URL url = new URL(new URL("file:"), path);
                InputStream is = url.openStream();
                rdr = new BufferedReader(new InputStreamReader(is, "UTF8"));
                //rdr = new BufferedReader(new FileReader(path));
            }
            catch (IOException x) {
                stop("Failed to open sigfile \"" + path + "\"");
            }
        
            boolean r = false;
            
        //  Check the new (.jh) format
            
            try {
                rdr.mark(80);
                r = APIReaderJh.check(rdr);
                rdr.reset();
            }
            catch (IOException e) {
                r = false;
            }
            
            if (r) {
                api = new APIBuffer(new APIReaderJh(rdr, path), null);
                api.sort();
                return;
            }
            
        //  Check the old (.sig) format
        
            try {
                rdr.mark(80);
                r = APIReaderSig.check(rdr);
                rdr.reset();
            }
            catch (IOException e) {
                r = false;
            }
            
            if (r) {
                api = new APIBuffer(new APIReaderSig(rdr, path), null);
                //debugShowAPI(api, "debugAPI.jh");
                
                api.sort();
                return;
            }
            
        //  Error
            
            stop("Invalid sigfile \"" + path + "\"");
        }
        
        
        void close ()
        {
            if (api != null) {
                api.close();
                api = null;
            }
        
            if (rdr != null) {
                try {
                    rdr.close();
                }
                catch (IOException x) {
                }

                rdr = null;
            }
            
            path = null;
        }
    }
    
    
    static class Test
    {
        String  access;
        boolean valuemode  = false;
        boolean bootloader = false;
        String path;
        API api;
        
    
        void open ()
        {
            if (access.equals("reflect")) 
                api = new APICached(new APILoaderReflect(path, valuemode, bootloader));
                
            else if (access.equals("static"))
                api = new APICached(new APILoaderStatic(path, valuemode));
                
            else if (access.equals("sigfile")) {
                BufferedReader rdr = null;
                try {
                    rdr = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
                }
                catch (IOException e) {
                    stop("Failed to open sigfile \"" + path + "\"");
                }
                
                if (APIReaderJh.check(rdr)) {
                    api = new APIReaderJh(rdr, path);
                    api = new APIBuffer(api, null);
                    return;
                }

                try {   
                    rdr.close();
                }
                catch (IOException e) {
                }
            }
            
            else
                stop("? access \"" + access + "\"");
        }
        
        
        void close ()
        {
            if (api != null) {
                api.close();
                api = null;
            }
            
            path = null;
        }
    }
 
 
    static void debugShowAPI (API api, String file)
    {        
        FileWriter fw = null;
        try {    
            fw = new FileWriter(file);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
            return;
        }

    	APIWriterJh wjh = new APIWriterJh();
        wjh.debug = true;
        wjh.write(new PrintWriter(fw), api, null, null);

        try {
            fw.close();
        }
        catch (IOException e) {
        }
    }
 
}
