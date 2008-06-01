/*
 * $Id: SetupAndTest.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.OptionInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The purpose of this program is to create the signature file 
 * and make comparison in a one step.
 * This is a simple wrapper that parses command line options and
 * calls Setup first and SignatureTest next.
 *
 * @author Serguei Ivashin
 */
public class SetupAndTest extends Result {

    // specific SetupAndTest options
    public static final String REFERENCE_OPTION = "-reference";
    public static final String TEST_OPTION = "-test";

    // Sets of command line options for:
    private final List setupOptions = new ArrayList();
    private final List testOptions = new ArrayList();

    public static void main(String[] args) {

        SetupAndTest t = new SetupAndTest();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }


    public boolean run(String[] args, PrintWriter log, PrintWriter ref) {

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args.length == 1 && (parser.isOptionSpecified(args[0], SigTest.HELP_OPTION) || parser.isOptionSpecified(args[0], (SigTest.QUESTIONMARK)))) {
            usage();
            return true;
        }

        // Both Setup and SignatureTest always will work in the static mode
        addFlag(setupOptions, SigTest.STATIC_OPTION);
        addFlag(testOptions, SigTest.STATIC_OPTION);

        final String optionsDecoder = "decodeOptions";

        parser.addOption(REFERENCE_OPTION, OptionInfo.requiredOption(1), optionsDecoder);
        parser.addOption(TEST_OPTION, OptionInfo.requiredOption(1), optionsDecoder);

        parser.addOption(SigTest.PACKAGE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(SigTest.FILENAME_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(SigTest.WITHOUTSUBPACKAGES_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(SigTest.EXCLUDE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(SigTest.APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(SigTest.OUT_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(SigTest.CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(SigTest.FORMATPLAIN_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(SignatureTest.CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(SignatureTest.NOCHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(SigTest.VERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(SigTest.HELP_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(SigTest.QUESTIONMARK, OptionInfo.optionalFlag(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            usage();
            log.println(e.getMessage());
            return failed(e.getMessage());
        }

        // Assign temporary name for the sigfile if none was specified
        if (!parser.isOptionSpecified(SigTest.FILENAME_OPTION)) {
            String tmpsigfile = null;

            try {
                File f = File.createTempFile("tmpsigfile", ".sig");
                f.deleteOnExit();
                tmpsigfile = f.getPath();

                addOption(setupOptions, SigTest.FILENAME_OPTION, tmpsigfile);
                addOption(testOptions, SigTest.FILENAME_OPTION, tmpsigfile);
            }
            catch (IOException ioe) {
                return failed(i18n.getString("SetupAndTest.error.message.tempfile", tmpsigfile));
            }
        }

        // Run Setup        
        log.println(i18n.getString("SetupAndTest.message.invoke.setup"));
        Setup setup = new Setup();
        setup.run((String[]) setupOptions.toArray(new String[setupOptions.size()]), log, ref);

        // Run SignatureTest
        if (setup.isPassed()) {
            log.println(i18n.getString("SetupAndTest.message.invoke.sigtest"));
            SignatureTest sigtest = new SignatureTest();
            sigtest.run((String[]) testOptions.toArray(new String[testOptions.size()]), log, ref);
            sigtest.exit();
        } else
            setup.exit();

        return false; // never reached
    }

    private void addOption(List options, String optionName, String optionValue) {
        options.add(optionName);
        options.add(optionValue);
    }

    private void addFlag(List options, String flag) {
        options.add(flag);
    }


    public void decodeOptions(String optionName, String[] args) {

        if (optionName.equalsIgnoreCase(SigTest.HELP_OPTION) || optionName.equals(SigTest.QUESTIONMARK)) {
            usage();
        } else if (optionName.equalsIgnoreCase(REFERENCE_OPTION)) {

            addOption(setupOptions, SigTest.CLASSPATH_OPTION, args[0]);

        } else if (optionName.equalsIgnoreCase(TEST_OPTION)) {

            addOption(testOptions, SigTest.CLASSPATH_OPTION, args[0]);

        } else if (optionName.equalsIgnoreCase(SigTest.FILENAME_OPTION) ||
                optionName.equalsIgnoreCase(SigTest.PACKAGE_OPTION) ||
                optionName.equalsIgnoreCase(SigTest.WITHOUTSUBPACKAGES_OPTION) ||
                optionName.equalsIgnoreCase(SigTest.EXCLUDE_OPTION) ||
                optionName.equalsIgnoreCase(SigTest.APIVERSION_OPTION) ||
                optionName.equalsIgnoreCase(SigTest.CLASSCACHESIZE_OPTION)) {

            addOption(setupOptions, optionName, args[0]);
            addOption(testOptions, optionName, args[0]);

        } else if (optionName.equalsIgnoreCase(SigTest.VERBOSE_OPTION)) {

            addFlag(setupOptions, optionName);
            addFlag(testOptions, optionName);

        } else if (optionName.equalsIgnoreCase(SigTest.OUT_OPTION)) {

            addOption(testOptions, optionName, args[0]);

        } else if (optionName.equalsIgnoreCase(SigTest.FORMATPLAIN_OPTION) ||
                optionName.equalsIgnoreCase(SignatureTest.CHECKVALUE_OPTION)) {

            addFlag(testOptions, optionName);
        }
    }


    /*
    *  Prints the help text.
    *
    */
    public static void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(i18n.getString("SetupAndTest.usage.version", Version.Number));        
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.start"));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.reference", REFERENCE_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.test", TEST_OPTION));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.filename", Setup.FILENAME_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.package", SigTest.PACKAGE_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.packagewithoutsubpackages", SigTest.WITHOUTSUBPACKAGES_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.exclude", SigTest.EXCLUDE_OPTION));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.verbose", Setup.VERBOSE_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.checkvalue", Setup.CHECKVALUE_OPTION));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.out", SigTest.OUT_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.formatplain", SigTest.FORMATPLAIN_OPTION));
        sb.append(nl).append(i18n.getString("SetupAndTest.usage.classcachesize", new Object[]{SigTest.CLASSCACHESIZE_OPTION, new Integer(SigTest.DefaultCacheSize)}));

        sb.append(nl).append(i18n.getString("SetupAndTest.usage.help", SigTest.HELP_OPTION));

        System.err.println(sb.toString());
    }

}
