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


package com.sun.tdk.signaturetest.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mikhail Ershov
 */
public class BatchFileParser {
    public static String [] processParameters(String [] args) throws CommandLineParserException {
        int filePos = -1;
        for (int i=0; i < args.length; i++) {
            if (args[i].startsWith("@")) {
                filePos = i;
                break;
            }
        }
        if (args == null || filePos == -1 ) {
            return args;
        }
        String fName = args[filePos].substring(1).trim();
        try {
            LineNumberReader r = new LineNumberReader(new FileReader(fName));
            String currLine;
            Properties props = new Properties();
            ArrayList options = new ArrayList();
            if (filePos > 0) {
                options.addAll(Arrays.asList(args).subList(0, filePos));
            }
            Pattern setPattern = Pattern.compile("Set\\s+(.+)=(.+)", Pattern.CASE_INSENSITIVE);
            Pattern nameAndValue = Pattern.compile("(.+)=(.+)");
            Pattern nameOnly = Pattern.compile("([^=]+)");
            while ((currLine = r.readLine()) != null) {
                currLine = currLine.trim();
                Matcher m = setPattern.matcher(currLine);
                if (currLine.startsWith("#") || "".equals(currLine)) { // comment or empty line
                    continue;
                } else if (m.find()) {
                    props.put(m.group(1), m.group(2));
                } else {
                    Matcher nv = nameAndValue.matcher(currLine);
                    if (nv.find()) {
                        String name = nv.group(1).trim();
                        String val = nv.group(2).trim();
                        if (!name.startsWith("-")) {
                            name = "-" + name;
                        }
                        options.add(name);
                        options.add(val);
                    } else {
                        Matcher no = nameOnly.matcher(currLine);
                        if (no.find()) {
                            String name = no.group(1).trim();
                            if (!name.startsWith("-")) {
                                name = "-" + name;
                            }
                            options.add(name);
                        }
                    }
                }
            }
            options.addAll(Arrays.asList(args).subList(filePos+1, args.length));
            resolveParams(options, props);
            return (String[]) options.toArray(new String[] {});
        } catch (FileNotFoundException ex) {
            throw new CommandLineParserException("File " + fName + " not found", ex);
        } catch (IOException ex) {
            throw new CommandLineParserException("Can't read file " + fName, ex);
        }
    }

    private static void resolveParams(ArrayList options, Properties props) 
            throws CommandLineParserException {
        Pattern macro = Pattern.compile("(\\$\\{(.+?)\\})");

        // resolve props

        boolean subst, resolved;
        do {
            subst = false;
            resolved = true;
            Iterator keys = props.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String val = props.getProperty(key);
                Matcher m = macro.matcher(val);
                if (m.find()) {
                    subst = true;
                    String keyToFind=m.group(2);
                    String newVal = props.getProperty(keyToFind);
                    // skip uresolved yet props
                    if (macro.matcher(newVal).find()) {
                        resolved = false;
                        continue;
                    }
                    if (newVal != null) {
                        val = m.replaceFirst(newVal);
                        props.put(key, val);
                        // resolved?
                        if (macro.matcher(val).find()) {
                            resolved = false;
                        }
                    } else throw new CommandLineParserException("Can't resolve ${" + keyToFind + "} property");
                }
            }
        } while (subst && !resolved);
        
        // resolve ops
        for (int i = 0; i < options.size(); i++) {
            String o = (String) options.get(i);
            Matcher m = macro.matcher(o);
            if (m.find()) {
                String newVal = props.getProperty(m.group(2));
                if (newVal != null) {
                    options.set(i, newVal);
                } else throw new CommandLineParserException("Can't resolve " + o + " property");
            }
        }
    }

}
