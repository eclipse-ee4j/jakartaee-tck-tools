/*
 * $Id$
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

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.*;
import com.sun.tdk.signaturetest.sigfile.Reader;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;

import java.io.*;

/**
 * @author Roman Makarchuk
 */
public class Converter extends Result {

    private static PrintWriter log = new PrintWriter(System.err, true);

    private String oldFileName;
    private String newFileName;

    public static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Converter.class);

    private static final String OLDFILE_OPTION = "-OldFile";
    private static final String NEWFILE_OPTION = "-NewFile";
    private static final String HELP_OPTION = "-Help";
    private static final String QUESTIONMARK = "-?";

    public static void main(String[] args) throws IOException {

        Converter converter = new Converter();

        System.err.println(i18n.getString("Converter.message.startup"));

        if (converter.parseParameters(args))
            converter.run();

        log.flush();
    }

    private void run() throws IOException {

        Format oldFormat = new F21Format();
        Format newFormat = new F40Format();

        Reader reader = oldFormat.getReader();
        reader.readSignatureFile(new File(oldFileName).toURL());

        //write header to the signature file
        Writer writer = newFormat.getWriter();
        writer.init(new PrintWriter(new OutputStreamWriter(new FileOutputStream(newFileName), "UTF8")));

        writer.setApiVersion(reader.getApiVersion());

        if (reader.hasFeature(FeaturesHolder.ConstInfo))
            writer.addFeature(FeaturesHolder.ConstInfo);

        writer.writeHeader();

        ClassDescription currentClass;
        while ((currentClass = reader.readNextClass()) != null) {
            writer.write(currentClass);
        }

        reader.close();
        writer.close();

        System.err.println(i18n.getString("Converter.message.success_conversion"));
    }

    private boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args == null || args.length == 0 ||
                (args.length == 1 && (parser.isOptionSpecified(args[0], HELP_OPTION) || parser.isOptionSpecified(args[0], QUESTIONMARK))))
        {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(OLDFILE_OPTION, OptionInfo.requiredOption(1), optionsDecoder);
        parser.addOption(NEWFILE_OPTION, OptionInfo.requiredOption(1), optionsDecoder);


        try {
            parser.processArgs(args);
        }
        catch (CommandLineParserException e) {
            usage();
            log.println(e.getMessage());
            return false;
        }

        return passed();
    }

    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        if (optionName.equalsIgnoreCase(OLDFILE_OPTION))
            oldFileName = args[0];
        else if (optionName.equalsIgnoreCase(NEWFILE_OPTION))
            newFileName = args[0];
    }

    private static void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(i18n.getString("Converter.usage.start"));
        sb.append(nl).append(i18n.getString("Converter.usage.oldfilename", OLDFILE_OPTION));
        sb.append(nl).append(i18n.getString("Converter.usage.newfilename", NEWFILE_OPTION));
        System.err.println(sb.toString());
    }

}
