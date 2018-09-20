/*
 * Copyright (c) 2005, 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ts.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

public class Extext implements Ecommand {

    private static final String USAGE = "usage: create_xml " +
            "[path to excludelist] " + "[exludelist version]";
    private String exclude_dtd =
            Etool.props.getProperty("exclude.dtd.location");
    private String monaco_loc =
            Etool.props.getProperty("monaco.detail.location");

    public Extext() {
    }

    public String getUsage() {
        return USAGE;
    }

    public void createXML(String excludelist_file, String version) {
        String s = "bug";
        String s2 = version;
        FileReader filereader;

        System.err.println("*** Applying transformation to \"" +
                excludelist_file + "\"");
        System.err.println("*** Creating \"" + excludelist_file +
                ".xml\" file");

        try {
            filereader = new FileReader(excludelist_file);
        } catch (FileNotFoundException filenotfoundexception) {
            System.out.println(excludelist_file + " File Not Found");
            return;
        }

        try {
            BufferedReader bufferedreader = new BufferedReader(filereader);
            BufferedWriter bufferedwriter = new BufferedWriter(
                    new FileWriter(excludelist_file + ".xml"));
            int i = 0;
            bufferedwriter.write(
                    "<?xml version = \"1.0\" encoding = \"UTF-8\"?>");
            bufferedwriter.newLine();
            bufferedwriter.write("<!DOCTYPE excludelist SYSTEM " +
                    "\"" + exclude_dtd + "\">");
            bufferedwriter.newLine();
            bufferedwriter.write("<excludelist title = \"CTS " + s2 +
                    " Exclude List\">");
            bufferedwriter.newLine();
            String s1;

            int bugcount = 0;
            while ((s1 = bufferedreader.readLine()) != null) {
                StringTokenizer stringtokenizer =
                        new StringTokenizer(s1, "\t /");

                if (s1.startsWith(" ")) {
                    continue;
                }

                if (s1.trim().startsWith("####")) {
                    s1 = bufferedreader.readLine();
                    StringTokenizer stringtokenizer1 = new StringTokenizer(s1,
                            "\t  ");

                    int j = stringtokenizer1.countTokens();
                    if (j != 0) {
                        if (i != 0) {
                            bufferedwriter.write("\t</excluded>");
                            bufferedwriter.newLine();
                        }
                        i++;
                        for (int k = 0; k < 2; k++) {
                            String s4 = stringtokenizer1.nextToken();
                            if (k == 1) {
                                bufferedwriter.write("\t<excluded technology = \"" +
                                        s4.toUpperCase() + "\">");
                                bufferedwriter.newLine();
                            }
                        }

                    }
                }

                if (s1.trim().startsWith("#")) {
                    while (stringtokenizer.hasMoreTokens()) {
                        String s3 = stringtokenizer.nextToken();

                        if (7 == s3.length() && Character.isDigit(s3.charAt(0))) {
                            try {
                                Long.parseLong(s3);
                            } catch (NumberFormatException nfe) {
                                nfe.printStackTrace();
                            }

                            if (bugcount > 0) {
                                bufferedwriter.write("\t\t<bid id = \"" + s3 +
                                        "\" url = \"" + monaco_loc + s3 +
                                        "\"/>");
                                bufferedwriter.newLine();
                            } else {
                                bufferedwriter.write("\t\t<bug>");
                                bufferedwriter.newLine();
                                bufferedwriter.write("\t\t<bid id = \"" + s3 +
                                        "\" url = \"" + monaco_loc + s3 +
                                        "\"/>");
                                bufferedwriter.newLine();
                            }
                            bugcount++;
                        }
                    }
                }

                if (s1.trim().startsWith("com/") || s1.trim().startsWith("a") ||
                        s1.trim().startsWith("c") || s1.trim().startsWith("e") ||
                        s1.trim().startsWith("i") || s1.trim().startsWith("j") ||
                        s1.trim().startsWith("r") || s1.trim().startsWith("s") ||
                        s1.trim().startsWith("x")) {

                    while (s1.trim().startsWith("com/") || s1.trim().startsWith(
                            "a") ||
                            s1.trim().startsWith("c") || s1.trim().startsWith(
                            "e") ||
                            s1.trim().startsWith("i") || s1.trim().startsWith(
                            "j") ||
                            s1.trim().startsWith("r") || s1.trim().startsWith(
                            "s") ||
                            s1.trim().startsWith("x") || s1.length() == 0) {

                        if (s1.length() != 0) {
                            bufferedwriter.write("\t\t\t<test>" + s1 + "</test>");
                            bufferedwriter.newLine();
                        }
                        if (s1.length() == 0) {
                            bufferedreader.mark(300);
                            s1 = bufferedreader.readLine();
                            if (!s1.trim().startsWith("#")) {
                                continue;
                            }
                            bufferedreader.reset();
                            break;
                        }
                        if ((s1 = bufferedreader.readLine()) == null) {
                            break;
                        }
                    }
//                    for (int ii = 0; bugcount > ii; ii++) {
//                        bufferedwriter.write("\t\t</bug>");
//                        bufferedwriter.newLine();
//                    }
                    bufferedwriter.write("\t\t</bug>");
                    bufferedwriter.newLine();
                    bugcount = 0;
                }
            }
            bufferedwriter.write("\t</excluded>");
            bufferedwriter.newLine();
            bufferedwriter.write("</excludelist>");
            bufferedwriter.newLine();
            filereader.close();
            bufferedwriter.close();
        } catch (IOException IOE) {
            System.out.println("IO Failure");
            IOE.printStackTrace();
        }
    }
}
