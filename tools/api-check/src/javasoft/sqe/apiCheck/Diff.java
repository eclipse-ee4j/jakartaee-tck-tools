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
 *      -test <src>
 *              Defines the test API. The same as the -base options.
 *      -update <file>
 *              Name of the update-file. 
 *              May be omitted.
 *      -package <packs>
 *              List of packages to be tested.Use + or - to separate  names.
 *              If package prefixed with -, it excluded from testing.
 *              All classes from the base and test API tested if this options omitted.
 *      -expackage <packs>
 *              Package to be excluded from test.
 *      -out <file>
 *              Name of the report file.
 *              stdout used if this options omitted.
 *      -verbose
 *              Select the verbose report mode.
 *      -updateout <file>
 *              Name of the update file.
 *      -sort
 *              Sort report by error type and classname.
 *      -check <modifier> | bincomp
 *              Select check mode maintenance (access level or two-way binary compatibility
 *      -constvalues
 *              If present, enables writing constant values to signature file.
 *      -defpack <pack>
 *              Defines the default package for signature file
 *      -nodefpack
 *              No default package for signature file
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
 *      bit 0: 0 - compare was executed, 1 - not (errors in parameters),
 *
 *      bit 1: 0 - no differences, 1 - differences found,
 *
 *      bit 2: 0 - all statements in all updates are implemented or no updates, 
 *             1 - there are unimplemented statement(s).
 *
 */



package javasoft.sqe.apiCheck;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import com.sun.ts.lib.util.SigLogIntf;
import com.sun.ts.lib.util.SigLogAdapter;

public 
class Diff
{
    static 
    String[] pars = {"base$s",  "test$s",      "update$m$s",  "package$m$s", "expackage$m$s", 
                     "out$s",   "updateout$s", "sigout$s",    "verbose", 
                     "sort",    "check$s",     "constvalues", 
                     "defpack$s", "nodefpack",
                     "reflect", "static",      "version", 
                     "time",    "debug"};

    static 
    String help =
         "Options are:\n"
        +"-base <spec>\n"
        +"-test <spec>\n"
        +"-update <file>\n"
        +"-package <packs>\n"
        +"-expackage <packs>\n"
        +"-out <file>\n"
        +"-updateout <file>\n"
        +"-sigout <file>\n"
        +"-verbose\n"
        +"-sort\n"
        +"-check [bincomp | <modifier>]\n"
        +"-constvalues\n"
        +"-defpack <pack>\n"
        +"-nodefpack\n"
        +"-reflect\n"
        +"-static\n"
        +"-version";

    /*
     * Added a status member that can be queried from within a J2EE test.
     */
    private static int status = 0;
    public static boolean diffsFound() {
	return status >= 2;
    }

    public static 
    void main (String[] args) throws Exception
    {
	SigLogIntf pw = new SigLogAdapter();

	pw.println("### Diff.main() called");
	pw.println("### " + java.util.Arrays.asList(args));

	/*
	 * The args variable is a static variable.  Since the VM does not exit between
	 * invocations, the args variable must be cleared manually else the previous
	 * tests arguments are still available and processing this tests arguments
	 * causes "duplicate option" errors, see line 170 in class Util.java.
	 */
	Main.args.clear();

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

        boolean nonempty = false;
        PackageSet packs = new PackageSet(Main.args.getProperty("package"),
                                          Main.args.getProperty("expackage"));

    //  -defpack/-nodefpack processing

        String defpack = Main.args.getProperty("defpack");

        if (Main.args.getProperty("nodefpack") == null)
        {
            if (defpack == null)
                defpack = "java.lang";
        }
        else
        {
            if (defpack != null)
                Stop("error in options -defpack/-nodefpack");
        }

        if (defpack != null)
            Main.args.setProperty("defpack", defpack);

    //  Get base API

        String basepath = Main.args.getProperty("base");

	System.err.println("**** basepath = " + basepath);

        if (basepath == null)
            System.err.println("base not defined - using default classpath");
	
        API baseapi = Read(basepath, packs);
	if (baseapi == null)
            Stop("no classes found in \"" + basepath + "\"");    

        nonempty = nonempty | !baseapi.IsEmpty();

    //  Get update file

        Tran tran = null;
        String updtpath = Main.args.getProperty("update");
        if (updtpath != null)
        {
            ReadJhu readupd = null;

            StringTokenizer st = new StringTokenizer(updtpath, File.pathSeparator + ' ', false);
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

                if (tran == null)
                {
                    tran = new Tran();
                    readupd = new ReadJhu(tran);
                }

                if (!readupd.Read(baseapi, fname))
                    Stop(null);
            }

            if (readupd != null)
                readupd.PrintUnknown(System.err);
        }

    //  Get test API

        String testpath = Main.args.getProperty("test");
        if (testpath == null)
            System.err.println("test not defined - using default classpath");

        API testapi = Read(testpath, packs);
        if (testapi == null)
            Stop("no classes found in \"" + testpath + "\"");

        nonempty = nonempty | !testapi.IsEmpty();

    //  Transform the base API according to update

        if (tran != null)
        {                                                                            
            Decision dec = new Decision(baseapi.GetUnits(), testapi.GetUnits(), tran);
            dec.Work(baseapi.props);

        //  Invalidate xprog part of API
            baseapi.xprog = null;

            nonempty = nonempty | !baseapi.IsEmpty();
        }

        if (!nonempty)
            Stop("base and test empty");

    //  Open output file

	//        PrintWriter pw = null;
//        try {
//             if (Main.args.getProperty("out") == null)
//                 pw = new PrintWriter(System.out);
//             else
//                 pw = new PrintWriter(new FileWriter(Main.args.getProperty("out")));
//        }
//         catch (IOException x)
//         {
//             Stop("out error\n"+x.getMessage());
//         }

        if (Main.IsDebugMode())
        {
            WriteJh wjh = new WriteJh();
            pw.println("BASE");
            wjh.WriteAll(baseapi, pw);           
            pw.println("TEST");
            wjh.WriteAll(testapi, pw);
            pw.println("----");
        }


        PrintWriter pwupd = null;

        try
        {
            if (Main.args.getProperty("updateout") == null) 
                pwupd = null;
            else
                pwupd = new PrintWriter(new FileWriter(Main.args.getProperty("updateout")));
        }
        catch (IOException x)
        {
            Stop("updateout error\n"+x.getMessage());
        }


    //  Print result of update

        if (tran != null && Main.args.getProperty("verbose") != null)
            tran.Print(pw);

    //  Compare base and test

        TestBase tb;
        String   check = Main.args.getProperty("check");
        if (check != null && check.equals("bincomp"))
        {
            tb = new TestBincomp();
            tb.mode   = "protected";
            tb.check  = "bincomp";
            tb.values =  false;
            if (Main.args.getProperty("constvalues") != null)
                System.err.println("-constvalues option ignored in bincomp mode");
            if (pwupd != null)
                System.err.println("updates cannot be genereated in bincomp mode");
        }
        else // maintenance is the default mode
        {
            tb = new TestChanges();
            tb.mode   = check == null ? "protected" : check;
            tb.check  = "maint(" + tb.mode + ")";
            tb.values =  Main.args.getProperty("constvalues") != null;
            tb.upd.out = pwupd;
        }

        tb.out     = pw;
        tb.sort    =  Main.args.getProperty("sort")    != null;
        tb.verbose =  Main.args.getProperty("verbose") != null;
        tb.check  += ", verbose:" + (tb.verbose ? "yes" : "no");

        tb.Compare(packs, baseapi, testapi);

        if (pwupd != null)
        {
            pwupd.flush();
            pwupd.close();
            pwupd = null;
        }

    //  Print summary string

        if (tran == null)
        {// no update processing
            if (tb.errs == 0)
                pw.println("\n- No differences found -");
            else
                pw.println("\n- Differences found : "+tb.errs+" -");
        }
        else
        {
            String s1 = tb.errs == 0 ? "No unapproved differences found" 
                                     : "Unapproved differences found : "+tb.errs,
                   s2 = tran.Status() == 2 ? "All updates implemented"
                                           : "Not all updates umplemented";

            pw.println("\n - " + s1 + ". " + s2 + ". -");
        }

        pw.close();
        pw = null;

    //  Output sigfile for base

        if (Main.args.getProperty("sigout") != null) 
        {
            WriteJh writer = new WriteJh();
            writer.Write(baseapi, Main.args.getProperty("sigout"));
            writer.Close();
        }
        

    //  Exit

	status = 0 + 
	    (tb.errs == 0 ? 0 : 2) + 
	    (tran == null || tran.Status() == 2 ? 0 : 4);
	System.out.println("Diff.main() exiting status = \"" + status + "\"");
    }



//  Read API (base or test) classes from:
//    - signature file (new or old),
//    - zip/jar file,
//    - classpath (list of directories and/or .zip files).

    static
    API Read (String name, PackageSet pset)
    {
        API api = null;

    //  Check if we have a single file name or list of directories

        if (name != null 
         && name.indexOf(System.getProperty("path.separator")) == -1 
         && !name.equals("$"))
        {
        //  Have is a single file name

            File f = new File(name);

	    System.err.println("**** name = " + name);
	    System.err.println("**** f.exists() = " + f.exists());
	    System.err.println("**** f.isFile() = " + f.isFile());
	    System.err.println("**** f.canRead() = " + f.canRead());
	    System.err.println("**** pset = " + pset);
	
            if (!f.exists())
            {
                if (name.endsWith(".jh"))
                {
                    System.err.println("not found \"" + name + "\"");
                    return null;
                }
                else
                {
                    f = new File(name+".jh");
                    if (!f.exists() || !f.isFile())
                    {
                        System.err.println("not found \"" + name + "\"");
                        return null;
                    }
                    name += ".jh";
                }
            }

            if (f.isFile())
            {
                if (!f.canRead())
                {
                    System.err.println("cannot read \"" + name + "\"");
                    return null;
                }

                api = new API();
                return api.ReadAll(name, pset) > 0 ? api : null;
            }
        }

    //  Classpath given. Read class files.

        api = new API();
        return api.ReadAll(name, pset) > 0 ? api : null;
    }

    /*
     * Modified Stop to throw an exception instead of calling System.exit().
     * Now the Diff class can be run from within a J2EE container and the
     * app server will not be shutdown when Stop is invoked.
     */
    static void Stop (String msg) throws Exception
    {
        if (msg == null) {
            msg = "Diff terminated.  No error message provided.";
	}
	throw new Exception(msg);
    }
}
