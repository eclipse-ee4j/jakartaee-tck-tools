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
 *
 *      This program reads classes and writes its decsriptions in pseudo-Java
 *      format to signature file.
 * 
 *      Program options
 *
 *      -in <src> 
 *              Defines the source classes to be tested. <src> may be 
 *              1) classpath (list of directories and/or .zip/.jar files)
 *              2) "$" for JVM classpath ("sun.boot.class.path")
 *              Default classpath used if this option omitted.
 *      -out <dst>
 *              Defines the signature file name.
 *      -package <packs>
 *              List of packages to be tested.Use + or - to separate  names.
 *              If package prefixed with -, it excluded from testing.
 *              All classes from the source tested if this options omitted.
 *      -expackage <packs>
 *              Package to be excluded from test.
 *      -constvalues
 *              If present, enables writing constant values to signature file.
 *      -reflect
 *              Use reflection to get class information.
 *      -static
 *              Use class file parsing to get class information (default).
 *      -version
 *              Display program version.
 * 
 * 
 *      Exit code
 *      ---------
 *      0 - success, signature file written
 *      1 - error, no signature file written
 */



package javasoft.sqe.apiCheck;



public 
class Setup
{
    static 
    String[] pars = {"in$s", "package$m$s", "expackage$m$s", "out$s", 
                     "defpack$s", "nodefpack", "xpriv",
                     "constvalues", "reflect", "static", "version"};

    static 
    String help = 
         "Options are:\n"
        +"-in <spec>\n"
        +"-out <file>\n"
        +"-package <packs>\n"
        +"-expackage <packs>\n"
        +"-constvalues\n"
        +"-defpack <pack>\n"
        +"-nodefpack\n"
        +"-xpriv\n"
        +"-reflect\n"
        +"-static\n"
        +"-version";


    public static 
    void main (String[] args)
    {
	Main.args.clear(); // remove previous command line args

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

    //  -package/expackage propcessing

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

    //  Read source classes

        String inpath = Main.args.getProperty("in");
        API api = new API();
        api.ReadPath(inpath, packs);
        if (api.IsEmpty())
            Stop("no classes found in \"" + api.props.Find("classpath") + "\"");

        api.GetXProg().Sort();

    //  Write the signature file

        WriteJh writer = new WriteJh();
        if (Main.args.getProperty("out") == null)
            writer.Write(api, System.out);
        else
            writer.Write(api, Main.args.getProperty("out"));
        writer.Close();

	/*
	 * Added 8/9/02
	 * Used to create a list of all packages tested by this signature test.  This
	 * list will be used to exclude valid sub-packages of the current package under
	 * test when the signature files are verified.  This was done to allow the API
	 * check utility to find additional non-compliant packages.  Originally the
	 * API check utility was modified so it would only check the specified package
	 * and none of that packages sub-packages.  This modification caused API check
	 * to not find additional non-compliant packages.  So the first change was backed
	 * out and now we use the -expackage option to specify the specific sub-packages
	 * that should not be verified.  In order to so this correctly we must know what
	 * the valid sub-packages of any given package.  This will allow the tool to
	 * exclude all the valid sub-packages and find any non-compliant additional
	 * packages.
	 */
	String packageSigFile    = Main.args.getProperty("out", "");
	String additionalPackage = Main.args.getProperty("package", "");
	String packageListFile   = System.getProperty("pkg.list.file.path", "");
	try {
	    PackageList pList = new PackageList(additionalPackage,
						packageSigFile,
						packageListFile);
	    pList.writePkgListFile();
	} catch (Exception e) {
	    System.err.println("Setup.main() - Error updating the package list file \"" +
			       packageListFile + "\" with the package named \"" +
			       additionalPackage + "\"");
	}
    }

    static void Stop (String msg)
    {
        if (msg != null)
            System.err.println(msg);
        
        System.err.println("Setup terminated.");
        System.exit(1);
    }
}
