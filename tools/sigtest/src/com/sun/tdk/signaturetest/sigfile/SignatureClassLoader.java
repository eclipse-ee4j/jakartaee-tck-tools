/*
 * $Id: SignatureClassLoader.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * <b>SignatureClassLoader</b> implements input stream sequentially
 * reading <b>ClassDescription</b> instances from signature file.
 * This program merges several signature files into a single one.
 *
 * @author Maxim Sokolnikov
 * @version 05/09/09
 * @see ClassDescription
 */

abstract class SignatureClassLoader implements Reader {

    protected Format format;
    protected Set features = new HashSet();

    private BufferedReader in;

    private Parser parser;
    private final int BUFSIZE = 0x8000;

    /**
     * API version found in <code>this</code> signature file.
     */
    protected String apiVersion = "";

    /**
     * Sigfile format version found in <code>this</code> signature file.
     */
    protected String signatureFileFormat = "";


    protected SignatureClassLoader(Format format) {
        this.format = format;
        features = format.getAllSupportedFeatures();
        parser = getParser();
    }

    protected abstract Parser getParser();

    public boolean hasFeature(Format.Feature feature) {
        return features.contains(feature);
    }

    public void close() throws IOException {
        in.close();
    }

    /**
     * Return the next <b>SigFileClassDescription</code> read from <code>this</code>
     * signature file.
     *
     * @see ClassDescription
     */
    public ClassDescription readNextClass() throws IOException {

        String currentLine;
        String classDescr = null;
        List definitions = new ArrayList();

        for (; ;) {
            in.mark(BUFSIZE);
            if ((currentLine = in.readLine()) == null)
                break;

            currentLine = currentLine.trim();
            if (currentLine.length() == 0 || currentLine.startsWith("#"))
                continue;

            MemberType type = MemberType.getItemType(currentLine);
            if (type == MemberType.CLASS) {
                if (classDescr == null) {
                    classDescr = currentLine;
                } else
                    break;
            } else {
                if (classDescr == null)
                    throw new Error();

                definitions.add(currentLine);
            }
        }
        in.reset();

        if (classDescr == null && definitions.size() == 0)
            return null;

        classDescr = convertClassDescr(classDescr);
        definitions = convertClassDefinitions(definitions);

        return parser.parseClassDescription(classDescr, definitions);
    }


    protected abstract String convertClassDescr(String descr);

    protected abstract List convertClassDefinitions(List definitions);

    /**
     * Open <code>fileURL</code> for input, and parse comments to initialize fields
     */
    public boolean readSignatureFile(URL fileURL) throws IOException {
        in = new BufferedReader(new InputStreamReader(fileURL.openStream(), "UTF8"), BUFSIZE);
        assert in.markSupported();
        return readHeaders(in);
    }

    protected boolean readHeaders(BufferedReader in) throws IOException {

        String currentLine;

        if ((currentLine = in.readLine()) == null)
            return false;

        //  Check for the required headers (first two lines)

        signatureFileFormat = currentLine.trim();

        if (!signatureFileFormat.equals(format.getVersion()))
            return false;

        if ((currentLine = in.readLine()) == null)
            return false;

        currentLine += ' ';
        if (!currentLine.startsWith(Format.VERSION))
            return false;

        apiVersion = currentLine.substring(Format.VERSION.length()).trim();

        in.mark(BUFSIZE);
        while ((currentLine = in.readLine()) != null && currentLine.startsWith("#")) {
            removeMissingFeature(currentLine);
        }
        in.reset();

        return true;
    }

    private void removeMissingFeature(String currentLine) {

        Format.Feature f = null;
        boolean remove = false;
        Iterator it = features.iterator();
        while (it.hasNext()) {
            f = (Format.Feature) it.next();
            if (f.match(currentLine)) {
                remove = true;
                break;
            }
        }

        if (f != null && remove)
            features.remove(f);
    }

    public String getApiVersion() {
        return apiVersion;
    }
}
