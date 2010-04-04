/*
 * $Id$
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

import com.sun.tdk.signaturetest.util.BatchFileParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

public class Main {
    // Command line options
    private static final String COMMAND_SETUP = "Setup";
    private static final String COMMAND_TEST = "Test";
    private static final String COMMAND_TEST_ALT = "SignatureTest";
    private static final String COMMAND_SETUP_AND_TEST = "SetupAndTest";
    private static final String COMMAND_MERGE = "Merge";
    private static final String COMMAND_HELP = "-help";
    
    private static I18NResourceBundle i18n =
            I18NResourceBundle.getBundleForClass(Main.class);
    
    // Is sigtestdev.jar run (or sigtest.jar). In other words, is Setup class
    // available
    private static boolean isSigtestdevJar;
    // jar file name
    private static String jar;
    
    // Prints JAR's usage instructions
    private static void commonUsage() {
        System.out.println(i18n.getString("Main.usage.version",
                Version.Number));
        System.out.println(i18n.getString("Main.usage.variant.command",
                "java -jar " + jar));
        System.out.println(i18n.getString("Main.usage.variant.help",
                new Object[]{"java -jar " + jar, "-help"}));
        System.out.println();
        if (isSigtestdevJar) {
            System.out.println(i18n.getString("Main.usage.variant.canbedev",
                    new Object[]{COMMAND_SETUP, COMMAND_TEST,
                    COMMAND_SETUP_AND_TEST, COMMAND_MERGE}));
        } else {
            System.out.println(i18n.getString("Main.usage.variant.canbe",
                    new Object[]{COMMAND_TEST, COMMAND_MERGE}));
        }
    }
    
    public static void main(String[] args) {
        // Checking for Setup class availability
        try {
            Class.forName("com.sun.tdk.signaturetest.Setup");
            isSigtestdevJar = true;
        } catch (ClassNotFoundException e) {
            isSigtestdevJar = false;
        }
        jar = isSigtestdevJar ? "sigtestdev.jar" : "sigtest.jar";

        try {
            args = BatchFileParser.processParameters(args);
        } catch (CommandLineParserException ex) {
            ex.printStackTrace();
        }
        
        if (args.length == 0
                || (args.length == 1
                && args[0].equalsIgnoreCase(COMMAND_HELP))) {
            commonUsage();
        } else {
            // removing first argument from args
            String[] otherArgs = new String[args.length - 1];
            System.arraycopy(args, 1, otherArgs, 0, args.length - 1);
            
            // Running another class (Setup, Test, etc) with given arguments
            if (args[0].equalsIgnoreCase(COMMAND_SETUP)) {
                if (isSigtestdevJar) {
                    Setup.main(otherArgs);
                } else {
                    System.out.println(i18n.getString("Main.command.absent",
                            new Object[]{COMMAND_SETUP, jar}));
                }
            } else if (args[0].equalsIgnoreCase(COMMAND_TEST)
                    || args[0].equalsIgnoreCase(COMMAND_TEST_ALT)) {
                SignatureTest.main(otherArgs);
            } else if (args[0].equalsIgnoreCase(COMMAND_SETUP_AND_TEST)) {
                if (isSigtestdevJar) {
                    SetupAndTest.main(otherArgs);
                } else {
                    System.out.println(i18n.getString("Main.command.absent",
                            new Object[]{COMMAND_SETUP_AND_TEST, jar}));
                }
            } else if (args[0].equalsIgnoreCase(COMMAND_MERGE)) {
                Merge.main(otherArgs);
            } else if (args[0].equalsIgnoreCase(SigTest.VERSION_OPTION)) {
                printVersionInfo();
                System.exit(1);
            } else {
                commonUsage();
                System.exit(1);
            }
        }
    }

    private static void printVersionInfo() {
        System.out.println(Version.getVersionInfo());
    }
}
