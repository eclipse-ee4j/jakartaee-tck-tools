/*
 * $Id: FileManager.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.sigfile;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Mikhail Ershov
 */
public class FileManager {

    private static final String DEFAULT_PROTOCOL = "file:";

    public static URL getURL(String testURL, String fileName) throws MalformedURLException {

        URL result;
        File f = new File(fileName);

        if (f.isAbsolute()) {
            result = f.toURL();
        } else {
            // check that protocol specified
            if (testURL.indexOf(':') == -1)
                testURL = DEFAULT_PROTOCOL + testURL;
            result = new URL(new URL(testURL), fileName);
        }

        return result;
    }

    private static String getFormat(URL fileURL) {
        String currentLine;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(fileURL.openStream(), "UTF8"));
            if ((currentLine = in.readLine()) == null)
                return null;
            currentLine = currentLine.trim();
            in.close();
        } catch (IOException e) {
            return null;
        }
        return currentLine;
    }

    /**
     * Returns the latest Writer for format supported given set of features
     */
    public static Writer getWriter(Set features) {

        List applicableFormats = new ArrayList(formats.size());

        for (int i = 0; i < formats.size(); i++) {
            Format format = (Format) formats.get(i);
            Set formatFeatures = format.getSupportedFeatures();
            if (features.equals(formatFeatures))
                applicableFormats.add(format);
        }

        double latestVersion = 0;
        Writer latestWriter = null;

        for (int i = 0; i < applicableFormats.size(); i++) {
            Format f = (Format) applicableFormats.get(i);

            String[] sv = f.getVersion().split(" ");
            double v = Double.parseDouble(sv[sv.length - 1].substring(1));

            if (v > latestVersion) {
                latestVersion = v;
                latestWriter = f.getWriter();
            }
        }
        return latestWriter;
    }


    public static Reader getReader(URL fileURL) {
        String format = getFormat(fileURL);
        if (format != null) {
            Iterator it = formats.iterator();
            while (it.hasNext()) {
                Format f = (Format) it.next();
                if (f.isApplicable(format)) {
                    return f.getReader();
                }
            }
        }
        return null;
    }

    public static Format getDefaultFormat() {
        return defaultFormat;
    }

    public static void addFormat(Format frm, boolean useByDefault) {
        formats.add(frm);
        if (useByDefault)
            defaultFormat = frm;
    }

    public static void setFormat(Format frm) {
        formats.clear();
        formats.add(frm);
        defaultFormat = frm;
    }

    private static Format defaultFormat = new F41Format();
    private static List formats = new ArrayList();

    static {
        formats.add(defaultFormat);
        formats.add(new F21Format());
        formats.add(new F31Format());
        formats.add(new F40Format());
    }
}
